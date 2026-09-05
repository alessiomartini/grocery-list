package com.alessiomartini.dispensa.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alessiomartini.dispensa.data.FoodCatalog
import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemStatus
import com.alessiomartini.dispensa.ui.expiry.colorForUrgency
import com.alessiomartini.dispensa.ui.expiry.expiryStatusOf
import java.time.format.DateTimeFormatter

private val tileDateFormatter = DateTimeFormatter.ofPattern("dd/MM")

/** A grid tile for one item: tap toggles to-buy/in-pantry, long-press opens editing. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemTile(
    item: GroceryItem,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    // Pantry items get the app's accent tint ("you have this"); to-buy stays neutral ("still needed"),
    // so the two lists read apart at a glance even scrolled past their section header.
    val containerColor = if (item.status == ItemStatus.IN_PANTRY) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .width(96.dp)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .width(80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(FoodCatalog.iconFor(item.name, item.category), fontSize = 28.sp)

            Text(
                text = item.name,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            val quantityLabel = if (item.unit.isNotBlank()) {
                "${item.quantity} ${item.unit}"
            } else {
                "${item.quantity}"
            }
            Text(quantityLabel, style = MaterialTheme.typography.labelSmall)

            if (item.status == ItemStatus.IN_PANTRY) {
                item.expiryDate?.let { expiryDate ->
                    val status = expiryStatusOf(expiryDate)
                    Text(
                        text = expiryDate.format(tileDateFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorForUrgency(status.urgency)
                    )
                }
            }
        }
    }
}
