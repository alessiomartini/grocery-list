package com.alessiomartini.dispensa.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alessiomartini.dispensa.R
import com.alessiomartini.dispensa.network.RecipeSuggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(viewModel: RecipesViewModel, onSettingsClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recipes_title)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.suggestRecipes() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Restaurant, contentDescription = null)
                Text(" " + stringResource(R.string.recipes_suggest_button))
            }

            when (val state = uiState) {
                is RecipesUiState.Idle -> Unit
                is RecipesUiState.Loading -> LoadingState()
                is RecipesUiState.EmptyPantry -> MessageState(stringResource(R.string.recipes_empty_pantry))
                is RecipesUiState.NoApiKey -> MessageState(stringResource(R.string.recipes_missing_key))
                is RecipesUiState.Error -> MessageState(
                    stringResource(R.string.recipes_error, state.message)
                )
                is RecipesUiState.Success -> RecipeList(state.recipes)
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.recipes_loading),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun MessageState(message: String) {
    Text(message, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun RecipeList(recipes: List<RecipeSuggestion>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(recipes) { recipe -> RecipeCard(recipe) }
    }
}

@Composable
private fun RecipeCard(recipe: RecipeSuggestion) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(recipe.title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)

            if (recipe.ingredientsUsed.isNotEmpty()) {
                Text(
                    stringResource(R.string.recipes_uses, recipe.ingredientsUsed.joinToString(", ")),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            }
            if (recipe.missingIngredients.isNotEmpty()) {
                Text(
                    stringResource(R.string.recipes_missing_ingredients, recipe.missingIngredients.joinToString(", ")),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
            recipe.steps.forEachIndexed { index, step ->
                Text("${index + 1}. $step", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
