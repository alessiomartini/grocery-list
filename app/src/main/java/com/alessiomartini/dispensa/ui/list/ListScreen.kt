package com.alessiomartini.dispensa.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alessiomartini.dispensa.R
import com.alessiomartini.dispensa.data.Categories
import com.alessiomartini.dispensa.data.FoodCatalog
import com.alessiomartini.dispensa.data.FoodCatalogItem
import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemStatus

private val categoryOrderIndex: Map<String, Int> =
    Categories.SUGGESTED.withIndex().associate { (index, category) -> category to index }

private fun groupByCategory(items: List<GroceryItem>): List<Pair<String, List<GroceryItem>>> =
    items.groupBy { it.category }
        .toList()
        .sortedBy { (category, _) -> categoryOrderIndex[category] ?: Int.MAX_VALUE }

/** Shared screen for the "To buy" and "In pantry" tabs - same data, filtered to one [status]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(viewModel: ListViewModel, status: ItemStatus, onSettingsClick: () -> Unit) {
    val items by viewModel.items.collectAsState()
    val dismissedSuggestions by viewModel.dismissedSuggestions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemEditing by remember { mutableStateOf<GroceryItem?>(null) }

    val visibleItems = items.filter { it.status == status }
    val suggestedItems = if (status == ItemStatus.TO_BUY) {
        FoodCatalog.quickAddCandidates(items.map { it.name } + dismissedSuggestions)
    } else {
        emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (status == ItemStatus.TO_BUY) R.string.section_to_buy else R.string.section_in_pantry
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            if (status == ItemStatus.TO_BUY) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_item))
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (suggestedItems.isNotEmpty()) {
                item {
                    SuggestedSection(
                        suggestions = suggestedItems,
                        onAdd = { suggestion ->
                            viewModel.addItem(
                                name = suggestion.name,
                                quantity = 1,
                                unit = "",
                                category = suggestion.category
                            )
                        },
                        onDismiss = { suggestion -> viewModel.dismissSuggestion(suggestion.name) }
                    )
                }
            }

            if (visibleItems.isEmpty()) {
                item {
                    Text(
                        text = stringResource(
                            if (status == ItemStatus.TO_BUY) R.string.empty_list else R.string.pantry_screen_empty
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                groupByCategory(visibleItems).forEach { (category, categoryItems) ->
                    item {
                        CategoryGroup(
                            category = category,
                            items = categoryItems,
                            onTap = { tappedItem ->
                                if (status == ItemStatus.TO_BUY) {
                                    val estimatedExpiry =
                                        FoodCatalog.suggestedExpiryDate(tappedItem.name, tappedItem.category)
                                    viewModel.markAsBought(tappedItem, estimatedExpiry)
                                } else {
                                    viewModel.markAsFinished(tappedItem)
                                }
                            },
                            onLongPress = { itemEditing = it }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, quantity, unit, category ->
                viewModel.addItem(name, quantity, unit, category)
                showAddDialog = false
            }
        )
    }

    itemEditing?.let { editingItem ->
        EditItemDialog(
            item = editingItem,
            onDismiss = { itemEditing = null },
            onConfirm = { name, quantity, unit, category, expiryDate ->
                viewModel.updateItem(editingItem, name, quantity, unit, category, expiryDate)
                itemEditing = null
            },
            onDelete = { viewModel.deleteItem(editingItem) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryGroup(
    category: String,
    items: List<GroceryItem>,
    onTap: (GroceryItem) -> Unit,
    onLongPress: (GroceryItem) -> Unit
) {
    Column {
        Text(
            text = category,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { groceryItem ->
                ItemTile(
                    item = groceryItem,
                    onTap = { onTap(groceryItem) },
                    onLongPress = { onLongPress(groceryItem) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestedSection(
    suggestions: List<FoodCatalogItem>,
    onAdd: (FoodCatalogItem) -> Unit,
    onDismiss: (FoodCatalogItem) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.suggested_section_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.combinedClickable(
                        onClick = { onAdd(suggestion) },
                        onLongClick = { onDismiss(suggestion) }
                    )
                ) {
                    Text(
                        text = "${suggestion.icon} ${suggestion.name}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
