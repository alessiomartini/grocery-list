package com.alessiomartini.dispensa.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alessiomartini.dispensa.R
import com.alessiomartini.dispensa.data.Categories
import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemDialog(
    item: GroceryItem,
    onDismiss: () -> Unit,
    onConfirm: (name: String, quantity: Int, unit: String, category: String, expiryDate: LocalDate?) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var quantityText by remember { mutableStateOf(item.quantity.toString()) }
    var unit by remember { mutableStateOf(item.unit) }
    var category by remember { mutableStateOf(item.category) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var expiryDate by remember { mutableStateOf(item.expiryDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.item_name)) },
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { value -> quantityText = value.filter { it.isDigit() } },
                        label = { Text(stringResource(R.string.item_quantity)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(100.dp)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(stringResource(R.string.item_unit)) },
                        singleLine = true,
                        modifier = Modifier.width(120.dp)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.item_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        Categories.SUGGESTED.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                if (item.status == ItemStatus.IN_PANTRY) {
                    val expiryLabel = expiryDate?.format(dateFormatter)
                        ?: stringResource(R.string.item_expiry_none)
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(stringResource(R.string.item_expiry_date_value, expiryLabel))
                    }
                }

                TextButton(onClick = {
                    onDelete()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.delete), color = Color(0xFFC62828))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(name, quantityText.toIntOrNull() ?: 1, unit, category, expiryDate)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )

    if (showDatePicker) {
        ExpiryDatePickerDialog(initialDate = expiryDate) { date ->
            expiryDate = date
            showDatePicker = false
        }
    }
}
