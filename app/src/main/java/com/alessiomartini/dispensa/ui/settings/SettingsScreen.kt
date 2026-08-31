package com.alessiomartini.dispensa.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.alessiomartini.dispensa.BuildConfig
import com.alessiomartini.dispensa.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var model by remember(settings.model) { mutableStateOf(settings.model) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.settings_saved)
    val updateState by updateViewModel.uiState.collectAsState()

    LaunchedEffect(updateState) {
        if (updateState is UpdateUiState.ReadyToInstall) {
            updateViewModel.install((updateState as UpdateUiState.ReadyToInstall).file)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.settings_api_key_description))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(stringResource(R.string.settings_api_key_title)) },
                placeholder = { Text(stringResource(R.string.settings_api_key_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text(stringResource(R.string.settings_model_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = {
                viewModel.save(apiKey, model)
                scope.launch { snackbarHostState.showSnackbar(savedMessage) }
            }) {
                Text(stringResource(R.string.save))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            UpdateSection(updateState, updateViewModel)
        }
    }
}

@Composable
private fun UpdateSection(state: UpdateUiState, viewModel: UpdateViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.update_section_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.update_current_version, BuildConfig.VERSION_NAME))

        Button(
            onClick = { viewModel.checkForUpdate() },
            enabled = state !is UpdateUiState.Checking && state !is UpdateUiState.Downloading
        ) {
            Text(stringResource(R.string.update_check_button))
        }

        when (state) {
            is UpdateUiState.Idle -> Unit

            is UpdateUiState.Checking -> LoadingRow(stringResource(R.string.update_checking))

            is UpdateUiState.UpToDate -> Text(stringResource(R.string.update_up_to_date))

            is UpdateUiState.Available -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.update_available, state.version))
                if (state.notes.isNotBlank()) {
                    Text(state.notes, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { viewModel.downloadUpdate(state.downloadUrl) }) {
                    Text(stringResource(R.string.update_download_button))
                }
            }

            is UpdateUiState.Downloading -> LoadingRow(stringResource(R.string.update_downloading))

            is UpdateUiState.ReadyToInstall -> Button(onClick = { viewModel.install(state.file) }) {
                Text(stringResource(R.string.update_ready_button))
            }

            is UpdateUiState.Error -> Text(
                text = stringResource(R.string.update_error, state.message),
                color = Color(0xFFC62828)
            )
        }
    }
}

@Composable
private fun LoadingRow(label: String) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
        Text(label)
    }
}
