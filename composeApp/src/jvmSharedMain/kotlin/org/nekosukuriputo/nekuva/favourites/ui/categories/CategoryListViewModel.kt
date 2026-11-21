package org.nekosukuriputo.nekuva.favourites.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.nekosukuriputo.nekuva.core.model.FavouriteCategory
import org.nekosukuriputo.nekuva.favourites.domain.FavouritesRepository
import org.nekosukuriputo.nekuva.list.domain.ListSortOrder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.nekosukuriputo.nekuva.core.prefs.AppSettings

class CategoryListViewModel(
    private val repository: FavouritesRepository,
    private val settings: AppSettings
) : ViewModel() {

    val categories: StateFlow<List<FavouriteCategory>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isAllFavouritesVisible = settings.isAllFavouritesVisibleFlow

    fun toggleAllFavouritesVisibility() {
        settings.isAllFavouritesVisible = !settings.isAllFavouritesVisible
    }

    fun createCategory(title: String, isNsfw: Boolean = false) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.createCategory(
                title = trimmed,
                sortOrder = ListSortOrder.NEWEST,
                isTrackerEnabled = true, // track new chapters by default (Doki parity → Feed tab)
                isVisibleOnShelf = true,
                isNsfw = isNsfw,
            )
        }
    }

    fun updateCategory(category: FavouriteCategory, newTitle: String, isNsfw: Boolean = category.isNsfw) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.updateCategoryTitleAndNsfw(
                id = category.id,
                title = trimmed,
                isNsfw = isNsfw,
            )
        }
    }

    /** Toggle new-chapter tracking for a category (drives the Feed tab). */
    fun toggleTracking(category: FavouriteCategory) {
        viewModelScope.launch {
            repository.setCategoryTracking(
                id = category.id,
                isEnabled = !category.isTrackingEnabled,
            )
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            repository.removeCategories(listOf(categoryId))
        }
    }

    fun reorderCategories(orderedIds: List<Long>) {
        viewModelScope.launch {
            repository.reorderCategories(orderedIds)
        }
    }
}
