package com.alessiomartini.dispensa.ui.list

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.alessiomartini.dispensa.R
import com.alessiomartini.dispensa.data.Categories
import com.alessiomartini.dispensa.data.FoodCatalog
import com.alessiomartini.dispensa.data.FoodCatalogItem
import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemStatus
import java.io.File

private val categoryOrderIndex: Map<String, Int> =
    Categories.SUGGESTED.withIndex().associate { (index, category) -> category to index }

private fun groupByCategory(items: List<GroceryItem>): List<Pair<String, List<GroceryItem>>> =
    items.groupBy { it.category }
        .toList()
        .sortedBy { (category, _) -> categoryOrderIndex[category] ?: Int.MAX_VALUE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(viewModel: ListViewModel, onSettingsClick: () -> Unit) {
    val items by viewModel.items.collectAsState()
    val receiptScanState by viewModel.receiptScanState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemPendingExpiry by remember { mutableStateOf<GroceryItem?>(null) }
    var itemEditing by remember { mutableStateOf<GroceryItem?>(null) }

    val toBuyItems = items.filter { it.status == ItemStatus.TO_BUY }
    val pantryItems = items.filter { it.status == ItemStatus.IN_PANTRY }
    val suggestedItems = FoodCatalog.quickAddCandidates(items.map { it.name })

    val context = LocalContext.current
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingPhotoFile?.let { viewModel.scanReceipt(it) }
        }
    }

    val doneMessage = stringResource(R.string.receipt_scan_done, (receiptScanState as? ReceiptScanUiState.Done)?.count ?: 0)
    val noKeyMessage = stringResource(R.string.receipt_scan_no_key)
    val errorMessage = stringResource(R.string.receipt_scan_error, (receiptScanState as? ReceiptScanUiState.Error)?.message ?: "")

    LaunchedEffect(receiptScanState) {
        when (receiptScanState) {
            is ReceiptScanUiState.Done -> snackbarHostState.showSnackbar(doneMessage)
            is ReceiptScanUiState.NoApiKey -> snackbarHostState.showSnackbar(noKeyMessage)
            is ReceiptScanUiState.Error -> snackbarHostState.showSnackbar(errorMessage)
            else -> Unit
        }
        if (receiptScanState !is ReceiptScanUiState.Idle && receiptScanState !is ReceiptScanUiState.Scanning) {
            viewModel.dismissReceiptScanState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(
                        onClick = {
                            val photosDir = File(context.cacheDir, "receipts").apply { mkdirs() }
                            val file = File(photosDir, "receipt_${System.currentTimeMillis()}.jpg")
                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            pendingPhotoFile = file
                            takePictureLauncher.launch(uri)
                        },
                        enabled = receiptScanState !is ReceiptScanUiState.Scanning
                    ) {
                        if (receiptScanState is ReceiptScanUiState.Scanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Filled.CameraAlt, contentDescription = stringResource(R.string.scan_receipt))
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_item))
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (suggestedItems.isNotEmpty()) {
                item {
                    SuggestedSection(suggestedItems) { suggestion ->
                        viewModel.addItem(
                            name = suggestion.name,
                            quantity = 1,
                            unit = "",
                            category = suggestion.category
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.empty_list),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                if (toBuyItems.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.section_to_buy)) }
                    groupByCategory(toBuyItems).forEach { (category, categoryItems) ->
                        item {
                            CategoryGroup(
                                category = category,
                                items = categoryItems,
                                onTap = { itemPendingExpiry = it },
                                onLongPress = { itemEditing = it }
                            )
                        }
                    }
                }
                if (pantryItems.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.section_in_pantry)) }
                    groupByCategory(pantryItems).forEach { (category, categoryItems) ->
                        item {
                            CategoryGroup(
                                category = category,
                                items = categoryItems,
                                onTap = { viewModel.markAsFinished(it) },
                                onLongPress = { itemEditing = it }
                            )
                        }
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
        val suggestedDate = FoodCatalog.suggestedExpiryDate(pendingItem.name, pendingItem.category)
        ExpiryDatePickerDialog(initialDate = null, preselectedDate = suggestedDate) { date ->
            viewModel.markAsBought(pendingItem, date)
            itemPendingExpiry = null
        }
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

@Composable
private fun SuggestedSection(suggestions: List<FoodCatalogItem>, onAdd: (FoodCatalogItem) -> Unit) {
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
                AssistChip(
                    onClick = { onAdd(suggestion) },
                    label = { Text("${suggestion.icon} ${suggestion.name}") }
                )
            }
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
