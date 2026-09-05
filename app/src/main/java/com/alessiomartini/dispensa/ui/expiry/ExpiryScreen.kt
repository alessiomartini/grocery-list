package com.alessiomartini.dispensa.ui.expiry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alessiomartini.dispensa.R
import com.alessiomartini.dispensa.data.GroceryItem
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Header lives in the shared top bar in [com.alessiomartini.dispensa.ui.DispensaApp] - this is just the content. */
@Composable
fun ExpiryScreen(viewModel: ExpiryViewModel) {
    val items by viewModel.items.collectAsState()

    if (items.isEmpty()) {
        Text(
            text = stringResource(R.string.expiry_screen_empty),
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            textAlign = TextAlign.Center
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { groceryItem -> ExpiryCard(groceryItem) }
        }
    }
}

@Composable
private fun ExpiryCard(item: GroceryItem) {
    val expiryDate = item.expiryDate ?: return
    val status = expiryStatusOf(expiryDate)
    val color = colorForUrgency(status.urgency)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.category, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(expiryDate.format(dateFormatter), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = when (status.urgency) {
                        ExpiryUrgency.EXPIRED -> stringResource(R.string.expiry_expired)
                        ExpiryUrgency.TODAY -> stringResource(R.string.expiry_today)
                        else -> stringResource(R.string.expiry_soon, status.daysUntil.toInt())
                    },
                    color = color,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
