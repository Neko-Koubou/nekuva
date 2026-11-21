package org.nekosukuriputo.nekuva.favourites.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.nekosukuriputo.nekuva.core.model.FavouriteCategory
import nekuva.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    onBackClick: () -> Unit
) {
    val viewModel = koinViewModel<CategoryListViewModel>()
    val categories by viewModel.categories.collectAsState()
    val isAllFavouritesVisible by viewModel.isAllFavouritesVisible.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<FavouriteCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.favourites_categories)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingCategory = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.all_favourites)) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.toggleAllFavouritesVisibility() }) {
                            Icon(
                                if (isAllFavouritesVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Visibility"
                            )
                        }
                    }
                )
                Divider()
            }
            
            itemsIndexed(categories, key = { _, it -> it.id }) { index, category ->
                ListItem(
                    headlineContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(category.title)
                            if (category.isNsfw) {
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
                    trailingContent = {
                        Row {
                            if (index > 0) {
                                IconButton(onClick = {
                                    val newList = categories.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = temp
                                    viewModel.reorderCategories(newList.map { it.id })
                                }) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                                }
                            }
                            if (index < categories.size - 1) {
                                IconButton(onClick = {
                                    val newList = categories.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = temp
                                    viewModel.reorderCategories(newList.map { it.id })
                                }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                                }
                            }
                            IconButton(onClick = { viewModel.toggleTracking(category) }) {
                                Icon(
                                    if (category.isTrackingEnabled) {
                                        Icons.Default.Notifications
                                    } else {
                                        Icons.Default.NotificationsOff
                                    },
                                    contentDescription = stringResource(Res.string.check_for_new_chapters),
                                    tint = if (category.isTrackingEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            IconButton(onClick = {
                                editingCategory = category
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = {
                                viewModel.deleteCategory(category.id)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                )
                Divider()
            }
        }

        if (showDialog) {
            val target = editingCategory
            CategoryDialog(
                initialName = target?.title ?: "",
                initialIsNsfw = target?.isNsfw ?: false,
                isEditing = target != null,
                onDismiss = { showDialog = false },
                onConfirm = { name, isNsfw ->
                    showDialog = false
                    if (target == null) {
                        viewModel.createCategory(name, isNsfw)
                    } else {
                        viewModel.updateCategory(target, name, isNsfw)
                    }
                }
            )
        }
    }
}

@Composable
fun CategoryDialog(
    initialName: String,
    initialIsNsfw: Boolean = false,
    isEditing: Boolean = initialName.isNotEmpty(),
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var name by remember(initialName, initialIsNsfw) { mutableStateOf(initialName) }
    var isNsfw by remember(initialName, initialIsNsfw) { mutableStateOf(initialIsNsfw) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (!isEditing) Res.string.create_category else Res.string.edit_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.enter_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
            TextButton(
                onClick = { onConfirm(name, isNsfw) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
