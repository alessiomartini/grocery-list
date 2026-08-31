package com.alessiomartini.dispensa.ui.list

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(viewModel: ListViewModel, onSettingsClick: () -> Unit) {
    val items by viewModel.items.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemPendingExpiry by remember { mutableStateOf<GroceryItem?>(null) }
    var itemEditingExpiry by remember { mutableStateOf<GroceryItem?>(null) }

    val toBuyItems = items.filter { it.status == ItemStatus.TO_BUY }
    val pantryItems = items.filter { it.status == ItemStatus.IN_PANTRY }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_item))
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_list),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                if (toBuyItems.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.section_to_buy)) }
                    items(toBuyItems, key = { it.id }) { groceryItem ->
                        ItemRow(
                            item = groceryItem,
                            onToggle = { itemPendingExpiry = groceryItem },
                            onDelete = { viewModel.deleteItem(groceryItem) },
                            onEditExpiry = {}
                        )
                    }
                }
                if (pantryItems.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.section_in_pantry)) }
                    items(pantryItems, key = { it.id }) { groceryItem ->
                        ItemRow(
                            item = groceryItem,
                            onToggle = { viewModel.markAsFinished(groceryItem) },
                            onDelete = { viewModel.deleteItem(groceryItem) },
                            onEditExpiry = { itemEditingExpiry = groceryItem }
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

    itemPendingExpiry?.let { pendingItem ->
        ExpiryDatePickerDialog(initialDate = null) { date ->
            viewModel.markAsBought(pendingItem, date)
            itemPendingExpiry = null
        }
    }

    itemEditingExpiry?.let { editingItem ->
        ExpiryDatePickerDialog(initialDate = editingItem.expiryDate) { date ->
            viewModel.updateExpiryDate(editingItem, date)
            itemEditingExpiry = null
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}
