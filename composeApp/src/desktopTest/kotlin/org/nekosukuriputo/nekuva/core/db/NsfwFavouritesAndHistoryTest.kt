package org.nekosukuriputo.nekuva.core.db

import androidx.room.Room
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.russhwolf.settings.PreferencesSettings
import java.io.File
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.nekosukuriputo.nekuva.backups.data.model.CategoryBackup
import org.nekosukuriputo.nekuva.core.parser.MangaDataRepository
import org.nekosukuriputo.nekuva.core.prefs.AppSettings
import org.nekosukuriputo.nekuva.favourites.domain.FavouritesRepository
import org.nekosukuriputo.nekuva.history.data.HistoryRepository
import org.nekosukuriputo.nekuva.list.domain.ListSortOrder
import org.nekosukuriputo.nekuva.parsers.model.ContentRating
import org.nekosukuriputo.nekuva.parsers.model.Manga
import org.nekosukuriputo.nekuva.parsers.model.MangaParserSource
import org.nekosukuriputo.nekuva.parsers.model.MangaSource
import org.nekosukuriputo.nekuva.parsers.model.MangaState

class NsfwFavouritesAndHistoryTest {

	private class TestHarness : AutoCloseable {
		private val tempDir = File(System.getProperty("java.io.tmpdir"), "nekuva-nsfw-test-${UUID.randomUUID()}").apply {
			mkdirs()
		}
		val dbFile = File(tempDir, "test.db")
		private val prefsNode = Preferences.userRoot().node("NekuvaTest_${UUID.randomUUID()}")
		val settings = AppSettings(PreferencesSettings(prefsNode))
		lateinit var db: MangaDatabase
			private set
		lateinit var mangaDataRepository: MangaDataRepository
			private set
		lateinit var favouritesRepository: FavouritesRepository
			private set
		lateinit var historyRepository: HistoryRepository
			private set

		fun openRoom(queryCoroutineContext: Boolean = false) {
			val builder = Room.databaseBuilder<MangaDatabase>(name = dbFile.absolutePath)
			if (queryCoroutineContext) {
				builder.setQueryCoroutineContext(Dispatchers.IO)
			}
			db = getRoomDatabase(builder)
			mangaDataRepository = MangaDataRepository(db)
			favouritesRepository = FavouritesRepository(db, mangaDataRepository, settings)
			historyRepository = HistoryRepository(db, settings, mangaDataRepository)
		}

		override fun close() {
			if (::db.isInitialized) {
				runCatching { db.close() }
			}
			runCatching { prefsNode.removeNode() }
			tempDir.deleteRecursively()
		}
	}

	private fun sampleManga(
		id: Long,
		title: String,
		source: MangaSource = MangaParserSource.SHINIGAMI,
		contentRating: ContentRating? = ContentRating.SAFE,
	) = Manga(
		id = id,
		title = title,
		altTitles = emptySet(),
		url = "/manga/$id",
		publicUrl = "https://example.com/manga/$id",
		rating = 0.8f,
		contentRating = contentRating,
		coverUrl = "https://example.com/cover/$id.jpg",
		tags = emptySet(),
		state = MangaState.ONGOING,
		authors = setOf("Author $id"),
		source = source,
	)

	@Test
	fun migrationV1ToV2PreservesExistingTabsWithNsfwFalseAndAllowsToggle() = runBlocking {
		TestHarness().use { h ->
			val driver = BundledSQLiteDriver()
			val conn = driver.open(h.dbFile.absolutePath)
			try {
				val v1Statements = listOf(
					"CREATE TABLE IF NOT EXISTS `manga` (`manga_id` INTEGER NOT NULL, `title` TEXT NOT NULL, `alt_title` TEXT, `url` TEXT NOT NULL, `public_url` TEXT NOT NULL, `rating` REAL NOT NULL, `nsfw` INTEGER NOT NULL, `content_rating` TEXT, `cover_url` TEXT NOT NULL, `large_cover_url` TEXT, `state` TEXT, `author` TEXT, `source` TEXT NOT NULL, PRIMARY KEY(`manga_id`))",
					"CREATE TABLE IF NOT EXISTS `tags` (`tag_id` INTEGER NOT NULL, `title` TEXT NOT NULL, `key` TEXT NOT NULL, `source` TEXT NOT NULL, `pinned` INTEGER NOT NULL, PRIMARY KEY(`tag_id`))",
					"CREATE TABLE IF NOT EXISTS `history` (`manga_id` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `chapter_id` INTEGER NOT NULL, `page` INTEGER NOT NULL, `scroll` REAL NOT NULL, `percent` REAL NOT NULL, `deleted_at` INTEGER NOT NULL, `chapters` INTEGER NOT NULL, PRIMARY KEY(`manga_id`), FOREIGN KEY(`manga_id`) REFERENCES `manga`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
					"CREATE TABLE IF NOT EXISTS `manga_tags` (`manga_id` INTEGER NOT NULL, `tag_id` INTEGER NOT NULL, PRIMARY KEY(`manga_id`, `tag_id`), FOREIGN KEY(`manga_id`) REFERENCES `manga`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tag_id`) REFERENCES `tags`(`tag_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
					"CREATE TABLE IF NOT EXISTS `chapters` (`chapter_id` INTEGER NOT NULL, `manga_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `number` REAL NOT NULL, `volume` INTEGER NOT NULL, `url` TEXT NOT NULL, `scanlator` TEXT, `upload_date` INTEGER NOT NULL, `branch` TEXT, `source` TEXT NOT NULL, `index` INTEGER NOT NULL, PRIMARY KEY(`manga_id`, `chapter_id`), FOREIGN KEY(`manga_id`) REFERENCES `manga`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
					"CREATE TABLE IF NOT EXISTS `favourite_categories` (`category_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `created_at` INTEGER NOT NULL, `sort_key` INTEGER NOT NULL, `title` TEXT NOT NULL, `order` TEXT NOT NULL, `track` INTEGER NOT NULL, `show_in_lib` INTEGER NOT NULL, `deleted_at` INTEGER NOT NULL)",
					"CREATE TABLE IF NOT EXISTS `favourites` (`manga_id` INTEGER NOT NULL, `category_id` INTEGER NOT NULL, `sort_key` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `deleted_at` INTEGER NOT NULL, PRIMARY KEY(`manga_id`, `category_id`), FOREIGN KEY(`manga_id`) REFERENCES `manga`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`category_id`) REFERENCES `favourite_categories`(`category_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
					"CREATE TABLE IF NOT EXISTS `preferences` (`manga_id` INTEGER NOT NULL, `mode` INTEGER NOT NULL, `cf_brightness` REAL NOT NULL, `cf_contrast` REAL NOT NULL, `cf_invert` INTEGER NOT NULL, `cf_grayscale` INTEGER NOT NULL, `cf_book` INTEGER NOT NULL, `title_override` TEXT, `cover_override` TEXT, `content_rating_override` TEXT, PRIMARY KEY(`manga_id`), FOREIGN KEY(`manga_id`) REFERENCES `manga`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
					"CREATE TABLE IF NOT EXISTS `tracks` (`manga_id` INTEGER NOT NULL, `last_chapter_id` INTEGER NOT NULL, `chapters_new` INTEGER NOT NULL, `last_check_time` INTEGER NOT NULL, `last_chapter_date` INTEGER NOT NULL, `last_result` INTEGER NOT NULL, `last_error` TEXT, PRIMARY KEY(`manga_id`), FOREIGN KEY(`manga_id`) REFERENCES `manga`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
					"CREATE TABLE IF NOT EXISTS `track_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manga_id` INTEGER NOT NULL, `chapters` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `unread` INTEGER NOT NULL, FOREIGN KEY(`manga_id`) REFERENCES `manga`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
					"CREATE TABLE IF NOT EXISTS `suggestions` (`manga_id` INTEGER NOT NULL, `relevance` REAL NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`manga_id`), FOREIGN KEY(`manga_id`) REFERENCES `manga`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
					"CREATE TABLE IF NOT EXISTS `bookmarks` (`manga_id` INTEGER NOT NULL, `page_id` INTEGER NOT NULL, `chapter_id` INTEGER NOT NULL, `page` INTEGER NOT NULL, `scroll` INTEGER NOT NULL, `image` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `percent` REAL NOT NULL, PRIMARY KEY(`manga_id`, `page_id`), FOREIGN KEY(`manga_id`) REFERENCES `manga`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
					"CREATE TABLE IF NOT EXISTS `scrobblings` (`scrobbler` INTEGER NOT NULL, `id` INTEGER NOT NULL, `manga_id` INTEGER NOT NULL, `target_id` INTEGER NOT NULL, `status` TEXT, `chapter` INTEGER NOT NULL, `comment` TEXT, `rating` REAL NOT NULL, PRIMARY KEY(`scrobbler`, `id`, `manga_id`))",
					"CREATE TABLE IF NOT EXISTS `sources` (`source` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `sort_key` INTEGER NOT NULL, `added_in` INTEGER NOT NULL, `used_at` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `cf_state` INTEGER NOT NULL, PRIMARY KEY(`source`))",
					"CREATE TABLE IF NOT EXISTS `stats` (`manga_id` INTEGER NOT NULL, `started_at` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `pages` INTEGER NOT NULL, PRIMARY KEY(`manga_id`, `started_at`), FOREIGN KEY(`manga_id`) REFERENCES `history`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
					"CREATE TABLE IF NOT EXISTS `local_index` (`manga_id` INTEGER NOT NULL, `path` TEXT NOT NULL, PRIMARY KEY(`manga_id`), FOREIGN KEY(`manga_id`) REFERENCES `manga`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
					"CREATE INDEX IF NOT EXISTS `index_manga_tags_manga_id` ON `manga_tags` (`manga_id`)",
					"CREATE INDEX IF NOT EXISTS `index_manga_tags_tag_id` ON `manga_tags` (`tag_id`)",
					"CREATE INDEX IF NOT EXISTS `index_favourites_manga_id` ON `favourites` (`manga_id`)",
					"CREATE INDEX IF NOT EXISTS `index_favourites_category_id` ON `favourites` (`category_id`)",
					"CREATE INDEX IF NOT EXISTS `index_track_logs_manga_id` ON `track_logs` (`manga_id`)",
					"CREATE INDEX IF NOT EXISTS `index_suggestions_manga_id` ON `suggestions` (`manga_id`)",
					"CREATE INDEX IF NOT EXISTS `index_bookmarks_manga_id` ON `bookmarks` (`manga_id`)",
					"CREATE INDEX IF NOT EXISTS `index_bookmarks_page_id` ON `bookmarks` (`page_id`)",
					"CREATE INDEX IF NOT EXISTS `index_sources_sort_key` ON `sources` (`sort_key`)",
					"CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
					"INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '312a90087a7000d1f371efd39c52d434')",
					"INSERT INTO favourite_categories (category_id, created_at, sort_key, title, `order`, track, show_in_lib, deleted_at) VALUES (1, 1000, 1, 'Favorit Lama', 'ALPHABETIC', 0, 1, 0)",
					"INSERT INTO manga (manga_id, title, alt_title, url, public_url, rating, nsfw, content_rating, cover_url, large_cover_url, state, author, source) VALUES (101, 'Old SFW Manga', '', '/m/101', 'https://example.com/m/101', 0.9, 0, 'SAFE', 'https://example.com/101.jpg', NULL, 'ONGOING', 'Author', 'SHINIGAMI')",
					"INSERT INTO favourites (manga_id, category_id, sort_key, pinned, created_at, deleted_at) VALUES (101, 1, 0, 0, 1000, 0)",
					"INSERT INTO history (manga_id, created_at, updated_at, chapter_id, page, scroll, percent, deleted_at, chapters) VALUES (101, 1000, 1000, 1, 0, 0.0, 0.5, 0, 10)",
					"PRAGMA user_version = 1",
				)
				v1Statements.forEach { conn.execSQL(it) }
			} finally {
				conn.close()
			}

			h.openRoom()

			val migrated = h.favouritesRepository.getCategory(1L)
			assertEquals("Favorit Lama", migrated.title)
			assertFalse(migrated.isNsfw)
			assertFalse(migrated.isTrackingEnabled)
			assertTrue(migrated.isVisibleInLibrary)

			h.favouritesRepository.updateCategoryTitleAndNsfw(1L, "Favorit Lama", isNsfw = true)
			val updated = h.favouritesRepository.getCategory(1L)
			assertTrue(updated.isNsfw)
			assertFalse(updated.isTrackingEnabled)
			val entity = h.db.getFavouriteCategoriesDao().find(1)
			assertEquals(ListSortOrder.ALPHABETIC.name, entity.order)
		}
	}

	@Test
	fun nsfwFavouriteTabHidesReactivelyWhenNsfwDisabled() = runBlocking {
		TestHarness().use { h ->
			h.openRoom()
			val sfwCat = h.favouritesRepository.createCategory("SFW Tab", ListSortOrder.NEWEST, true, true, isNsfw = false)
			val nsfwCat = h.favouritesRepository.createCategory("NSFW Tab", ListSortOrder.NEWEST, true, true, isNsfw = true)

			val sfwManga = sampleManga(1001L, "Safe Hero")
			val nsfwCatManga = sampleManga(1002L, "Secret Romance")
			h.favouritesRepository.addToCategory(sfwCat.id, listOf(sfwManga))
			h.favouritesRepository.addToCategory(nsfwCat.id, listOf(nsfwCatManga))

			h.settings.isNsfwContentDisabled = false
			val allCatsBefore = withTimeout(5_000) {
				h.favouritesRepository.observeCategories().first { list ->
					list.any { it.title == "SFW Tab" } && list.any { it.title == "NSFW Tab" }
				}
			}
			assertEquals(setOf("Read Later", "SFW Tab", "NSFW Tab"), allCatsBefore.map { it.title }.toSet())
			val visibleBefore = withTimeout(5_000) {
				h.favouritesRepository.observeCategoriesForLibrary().first { it.size == 3 }
			}
			assertEquals(3, visibleBefore.size)
			val allFavsBefore = withTimeout(5_000) {
				h.favouritesRepository.observeAll(ListSortOrder.NEWEST, emptySet(), 50).first { it.size == 2 }
			}
			assertEquals(setOf(1001L, 1002L), allFavsBefore.map { it.id }.toSet())
			assertEquals(1, h.favouritesRepository.search("Secret", 10).size)

			// Enable Disable NSFW -> NSFW tab and its items are hidden reactively
			h.settings.isNsfwContentDisabled = true
			val catsHidden = withTimeout(5_000) {
				h.favouritesRepository.observeCategories().first { list ->
					list.any { it.title == "SFW Tab" } && list.none { it.isNsfw }
				}
			}
			assertEquals(setOf("Read Later", "SFW Tab"), catsHidden.map { it.title }.toSet())
			val visibleHidden = withTimeout(5_000) {
				h.favouritesRepository.observeCategoriesForLibrary().first { list ->
					list.any { it.title == "SFW Tab" } && list.none { it.isNsfw }
				}
			}
			assertEquals(setOf("Read Later", "SFW Tab"), visibleHidden.map { it.title }.toSet())
			val allFavsHidden = withTimeout(5_000) {
				h.favouritesRepository.observeAll(ListSortOrder.NEWEST, emptySet(), 50).first { it.size == 1 }
			}
			assertEquals(listOf(1001L), allFavsHidden.map { it.id })
			val nsfwTabItemsHidden = withTimeout(5_000) {
				h.favouritesRepository.observeAll(nsfwCat.id, ListSortOrder.NEWEST, emptySet(), 50).first { it.isEmpty() }
			}
			assertTrue(nsfwTabItemsHidden.isEmpty())
			assertTrue(h.favouritesRepository.search("Secret", 10).isEmpty())

			// Turn Disable NSFW back off -> everything reappears intact
			h.settings.isNsfwContentDisabled = false
			val restoredCats = withTimeout(5_000) {
				h.favouritesRepository.observeCategories().first { list ->
					list.any { it.title == "NSFW Tab" }
				}
			}
			assertEquals(setOf("Read Later", "SFW Tab", "NSFW Tab"), restoredCats.map { it.title }.toSet())
			val restoredAllFavs = withTimeout(5_000) {
				h.favouritesRepository.observeAll(ListSortOrder.NEWEST, emptySet(), 50).first { it.size == 2 }
			}
			assertEquals(setOf(1001L, 1002L), restoredAllFavs.map { it.id }.toSet())
		}
	}

	@Test
	fun historyHidesNsfwIncludingLegacyHentaiSourceRowsWithoutLosingPagination() = runBlocking {
		TestHarness().use { h ->
			h.openRoom()

			// Insert directly via SQL to control exact updated_at order and test legacy nsfw=0 HENTAI rows.
			h.db.getMangaDao().run {
				// 2001: SFW manga (oldest)
				val mSfw = sampleManga(2001L, "SFW Adventure", MangaParserSource.SHINIGAMI, ContentRating.SAFE)
				// 2002: ADULT manga from general source (middle)
				val mAdult = sampleManga(2002L, "Adult Drama", MangaParserSource.SHINIGAMI, ContentRating.ADULT)
				h.mangaDataRepository.storeManga(mSfw, replaceExisting = true)
				h.mangaDataRepository.storeManga(mAdult, replaceExisting = true)
			}
			h.db.getHistoryDao().upsert(
				org.nekosukuriputo.nekuva.history.data.HistoryEntity(
					mangaId = 2001L, createdAt = 1000L, updatedAt = 1000L, chapterId = 1L, page = 1, scroll = 0f, percent = 0.2f, chaptersCount = 10, deletedAt = 0L,
				),
			)
			h.db.getHistoryDao().upsert(
				org.nekosukuriputo.nekuva.history.data.HistoryEntity(
					mangaId = 2002L, createdAt = 2000L, updatedAt = 2000L, chapterId = 2L, page = 2, scroll = 0f, percent = 0.4f, chaptersCount = 10, deletedAt = 0L,
				),
			)
			// 2003: Legacy HENTAI-source row stored with nsfw=0, content_rating=NULL (newest!)
			h.db.close()
			val driver = BundledSQLiteDriver()
			val conn = driver.open(h.dbFile.absolutePath)
			try {
				conn.execSQL(
					"INSERT INTO manga (manga_id, title, alt_title, url, public_url, rating, nsfw, content_rating, cover_url, large_cover_url, state, author, source) " +
						"VALUES (2003, 'Legacy Hentai Title', '', '/m/2003', 'https://example.com/m/2003', 0.9, 0, NULL, 'https://example.com/2003.jpg', NULL, 'ONGOING', 'Author 2003', '${MangaParserSource.DOUJINDESU.name}')",
				)
				conn.execSQL(
					"INSERT INTO history (manga_id, created_at, updated_at, chapter_id, page, scroll, percent, deleted_at, chapters) " +
						"VALUES (2003, 3000, 3000, 3, 3, 0.0, 0.6, 0, 5)",
				)
			} finally {
				conn.close()
			}
			h.openRoom()

			h.settings.isNsfwContentDisabled = false
			val fullHistory = withTimeout(5_000) {
				h.historyRepository.observeAllWithHistory(ListSortOrder.LAST_READ, emptySet(), 20).first { it.size == 3 }
			}
			assertEquals(listOf(2003L, 2002L, 2001L), fullHistory.map { it.manga.id })
			val lastWhenEnabled = withTimeout(5_000) { h.historyRepository.observeLast().first { it?.id == 2003L } }
			assertNotNull(lastWhenEnabled)
			assertTrue(h.historyRepository.getPopularSources(5).any { it.name == MangaParserSource.DOUJINDESU.name })

			// Enable Disable NSFW -> newest NSFW rows must NOT starve limit=1 pagination
			h.settings.isNsfwContentDisabled = true
			val limitedSfw = withTimeout(5_000) {
				h.historyRepository.observeAllWithHistory(ListSortOrder.LAST_READ, emptySet(), limit = 1)
					.first { it.size == 1 && it.first().manga.id == 2001L }
			}
			assertEquals(listOf(2001L), limitedSfw.map { it.manga.id })

			val lastSfw = withTimeout(5_000) { h.historyRepository.observeLast().first { it?.id == 2001L } }
			assertEquals(2001L, lastSfw?.id)

			val shortcutsList = withTimeout(5_000) { h.historyRepository.observeAll(4).first { it.size == 1 } }
			assertEquals(listOf(2001L), shortcutsList.map { it.id })

			assertEquals(listOf(2001L), h.historyRepository.getList(0, 10).map { it.id })
			assertEquals(2001L, h.historyRepository.getLastOrNull()?.id)
			assertTrue(h.historyRepository.search("Legacy", 10).isEmpty())
			assertTrue(h.historyRepository.search("Adult", 10).isEmpty())
			assertEquals(listOf(2001L), h.historyRepository.search("Adventure", 10).map { it.id })
			assertTrue(h.historyRepository.getPopularSources(5).none { it.name == MangaParserSource.DOUJINDESU.name })

			// Turn Disable NSFW back off -> all 3 preserved history items reappear
			h.settings.isNsfwContentDisabled = false
			val restored = withTimeout(5_000) {
				h.historyRepository.observeAllWithHistory(ListSortOrder.LAST_READ, emptySet(), 20).first { it.size == 3 }
			}
			assertEquals(listOf(2003L, 2002L, 2001L), restored.map { it.manga.id })
		}
	}

	@Test
	fun categoryBackupIsBackwardsCompatibleAndPreservesNsfwFlag() {
		val legacyJson = """{"category_id":2,"created_at":1234,"sort_key":1,"title":"Old Backup","order":"NEWEST","track":true,"show_in_lib":true}"""
		val legacyDecoded = Json.decodeFromString<CategoryBackup>(legacyJson)
		assertFalse(legacyDecoded.isNsfw)
		assertFalse(legacyDecoded.toEntity().isNsfw)

		val nsfwBackup = CategoryBackup(
			categoryId = 3,
			createdAt = 5678L,
			sortKey = 2,
			title = "18+ Vault",
			isNsfw = true,
		)
		val encoded = Json.encodeToString(CategoryBackup.serializer(), nsfwBackup)
		val decoded = Json.decodeFromString<CategoryBackup>(encoded)
		assertTrue(decoded.isNsfw)
		assertTrue(decoded.toEntity().isNsfw)
	}
}
