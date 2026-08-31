package com.alessiomartini.dispensa.ui.list

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.alessiomartini.dispensa.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * @param initialDate what "Cancel"/dismiss reverts to - the date as it was before this dialog
 *   opened (null if the item had none yet).
 * @param preselectedDate what the picker shows pre-selected when it opens; defaults to
 *   [initialDate], but callers offering an auto-suggested date (not yet actually set) pass it
 *   here separately so cancelling doesn't silently apply it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryDatePickerDialog(
    initialDate: LocalDate?,
    preselectedDate: LocalDate? = initialDate,
    onDismiss: (LocalDate?) -> Unit
) {
    val initialMillis = preselectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = { onDismiss(initialDate) },
        confirmButton = {
            TextButton(onClick = {
                val date = datePickerState.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                }
                onDismiss(date)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(initialDate) }) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
