package com.alessiomartini.dispensa.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alessiomartini.dispensa.R
import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemStatus
import com.alessiomartini.dispensa.ui.expiry.colorForUrgency
import com.alessiomartini.dispensa.ui.expiry.expiryStatusOf
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun ItemRow(
    item: GroceryItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEditExpiry: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(checked = item.status == ItemStatus.IN_PANTRY, onCheckedChange = { onToggle() })

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = item.status == ItemStatus.IN_PANTRY) { onEditExpiry() }
        ) {
            val quantityLabel = if (item.unit.isNotBlank()) {
                "${item.quantity} ${item.unit}"
            } else {
                "${item.quantity}"
            }
            Text("${item.name} · $quantityLabel", style = MaterialTheme.typography.bodyLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.category, style = MaterialTheme.typography.bodySmall)

                if (item.status == ItemStatus.IN_PANTRY) {
                    val expiryDate = item.expiryDate
                    if (expiryDate != null) {
                        val status = expiryStatusOf(expiryDate)
                        Text(
                            text = expiryDate.format(dateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorForUrgency(status.urgency)
                        )
                    }
                }
            }
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
        }
    }
}
