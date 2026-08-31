package com.alessiomartini.dispensa.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alessiomartini.dispensa.data.ItemRepository
import com.alessiomartini.dispensa.data.ItemStatus
import com.alessiomartini.dispensa.network.RecipeResult
import com.alessiomartini.dispensa.network.RecipeSuggestion
import com.alessiomartini.dispensa.network.RecipeSuggestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface RecipesUiState {
    data object Idle : RecipesUiState
    data object Loading : RecipesUiState
    data class Success(val recipes: List<RecipeSuggestion>) : RecipesUiState
    data object NoApiKey : RecipesUiState
    data object EmptyPantry : RecipesUiState
    data class Error(val message: String) : RecipesUiState
}

class RecipesViewModel(
    private val itemRepository: ItemRepository,
    private val recipeSuggestionRepository: RecipeSuggestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipesUiState>(RecipesUiState.Idle)
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    fun suggestRecipes() {
        viewModelScope.launch {
            _uiState.value = RecipesUiState.Loading

            val pantryItemNames = itemRepository.observeAll().first()
                .filter { it.status == ItemStatus.IN_PANTRY }
                .map { it.name }

            if (pantryItemNames.isEmpty()) {
                _uiState.value = RecipesUiState.EmptyPantry
                return@launch
            }

            _uiState.value = when (val result = recipeSuggestionRepository.suggestRecipes(pantryItemNames)) {
                is RecipeResult.Success -> RecipesUiState.Success(result.recipes)
                is RecipeResult.NoApiKey -> RecipesUiState.NoApiKey
                is RecipeResult.Error -> RecipesUiState.Error(result.message)
            }
        }
    }
}
