package org.nekosukuriputo.nekuva.favourites.ui.container

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.nekosukuriputo.nekuva.favourites.ui.list.FavouritesListScreen
import nekuva.composeapp.generated.resources.Res
import nekuva.composeapp.generated.resources.all_favourites
import nekuva.composeapp.generated.resources.cancel
import nekuva.composeapp.generated.resources.category_nsfw
import nekuva.composeapp.generated.resources.category_nsfw_summary
import nekuva.composeapp.generated.resources.delete
import nekuva.composeapp.generated.resources.edit_category
import nekuva.composeapp.generated.resources.hide
import nekuva.composeapp.generated.resources.manage
import nekuva.composeapp.generated.resources.name
import nekuva.composeapp.generated.resources.nsfw
import nekuva.composeapp.generated.resources.save

/** One tab descriptor: [id] = -1 for "All favourites", else a real category. */
private data class FavTab(
    val id: Long,
    val title: String,
    val isAll: Boolean,
    val isNsfw: Boolean = false,
)

/**
 * Favourites container (Doki FavouritesContainerFragment): "All favourites" tab (when visible) + a tab per
 * library-visible category, each long-pressable for Edit / Delete / Hide (Doki popup_fav_tab). No own
 * toolbar — the main shell search bar + overflow ("List options" / "Manage categories") is the toolbar.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavouritesScreen(
    onMangaClick: (Long) -> Unit,
    onManageCategoriesClick: () -> Unit,
) {
    val viewModel = koinViewModel<FavouritesViewModel>()
    val categories by viewModel.categories.collectAsState()
    val isAllFavouritesVisible by viewModel.isAllFavouritesVisible.collectAsState()

    val allLabel = stringResource(Res.string.all_favourites)
    val tabs = buildList {
        if (isAllFavouritesVisible) add(FavTab(FavouritesViewModel.ALL_CATEGORY_ID, allLabel, isAll = true))
        categories.forEach { add(FavTab(it.id, it.title, isAll = false, isNsfw = it.isNsfw)) }
    }
    if (tabs.isEmpty()) {
        // No visible categories and "All" hidden — show the empty all-favourites list as a fallback.
        FavouritesListScreen(categoryId = FavouritesViewModel.ALL_CATEGORY_ID, onMangaClick = onMangaClick)
        return
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    LaunchedEffect(tabs.size) {
        if (pagerState.currentPage >= tabs.size) {
            pagerState.scrollToPage(tabs.lastIndex)
        }
    }
    val coroutineScope = rememberCoroutineScope()
    var menuForTab by remember { mutableStateOf<FavTab?>(null) }
    var renameTab by remember { mutableStateOf<FavTab?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = pagerState.currentPage.coerceIn(0, tabs.lastIndex), edgePadding = 8.dp) {
            tabs.forEachIndexed { index, tab ->
                Box {
                    Tab(
                        selected = pagerState.currentPage == index,
                        // Click selects the tab (Doki: switch by tapping, not only by swiping). Tab's own
                        // selectable handles the click; long-press (Edit/Delete/Hide) is detected with a
                        // non-consuming gesture so it doesn't steal the tap.
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        modifier = Modifier.pointerInput(tab.id) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                // Right-click (Desktop) → menu immediately.
                                if (currentEvent.buttons.isSecondaryPressed) {
                                    menuForTab = tab
                                    return@awaitEachGesture
                                }
                                // Long-press (Android + Desktop) → menu only if held past the threshold.
                                // withTimeout (not withTimeoutOrNull) so a normal click — whose up is
                                // CONSUMED by Tab's selectable, making waitForUpOrCancellation return null —
                                // isn't mistaken for a long-press (that's what popped the menu on a tap).
                                val isLongPress = try {
                                    withTimeout(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation() }
                                    false
                                } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                                    true
                                }
                                if (isLongPress) menuForTab = tab
                            }
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(tab.title)
                                if (tab.isNsfw) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.nsfw),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        )
                                    }
                                }
                            }
                        },
                    )
                    FavTabMenu(
                        tab = if (menuForTab?.id == tab.id) tab else null,
                        onDismiss = { menuForTab = null },
                        onEdit = { renameTab = tab },
                        onDelete = { viewModel.deleteCategory(tab.id) },
                        onHide = { viewModel.hide(tab.id) },
                        onManage = { onManageCategoriesClick() },
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            key = { page -> tabs.getOrNull(page)?.id ?: page },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val tab = tabs.getOrNull(page) ?: return@HorizontalPager
            FavouritesListScreen(categoryId = tab.id, onMangaClick = onMangaClick)
        }
    }

    renameTab?.let { tab ->
        EditCategoryDialog(
            currentTitle = tab.title,
            currentIsNsfw = tab.isNsfw,
            onDismiss = { renameTab = null },
            onConfirm = { newTitle, newIsNsfw ->
                viewModel.updateCategory(tab.id, newTitle, newIsNsfw)
                renameTab = null
            },
        )
    }
}

@Composable
private fun EditCategoryDialog(
    currentTitle: String,
    currentIsNsfw: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit,
) {
    var text by remember(currentTitle, currentIsNsfw) { mutableStateOf(currentTitle) }
    var isNsfw by remember(currentTitle, currentIsNsfw) { mutableStateOf(currentIsNsfw) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.edit_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text(stringResource(Res.string.name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isNsfw = !isNsfw }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(Res.string.category_nsfw),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(Res.string.category_nsfw_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = isNsfw,
                        onCheckedChange = { isNsfw = it },
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(text, isNsfw) },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

@Composable
private fun FavTabMenu(
    tab: FavTab?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit,
    onManage: () -> Unit,
) {
    DropdownMenu(expanded = tab != null, onDismissRequest = onDismiss) {
        if (tab?.isAll == true) {
            // "All favourites" tab (Doki popup_fav_tab_all): Hide / Manage.
            DropdownMenuItem(text = { Text(stringResource(Res.string.hide)) }, onClick = { onDismiss(); onHide() })
            DropdownMenuItem(text = { Text(stringResource(Res.string.manage)) }, onClick = { onDismiss(); onManage() })
        } else if (tab != null) {
            // A category tab (Doki popup_fav_tab): Edit / Delete / Hide.
            DropdownMenuItem(text = { Text(stringResource(Res.string.edit_category)) }, onClick = { onDismiss(); onEdit() })
            DropdownMenuItem(text = { Text(stringResource(Res.string.delete)) }, onClick = { onDismiss(); onDelete() })
            DropdownMenuItem(text = { Text(stringResource(Res.string.hide)) }, onClick = { onDismiss(); onHide() })
        }
    }
}
