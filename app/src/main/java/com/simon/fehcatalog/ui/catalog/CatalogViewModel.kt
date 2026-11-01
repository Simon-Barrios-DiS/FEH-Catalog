package com.simon.fehcatalog.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simon.fehcatalog.data.HeroRepository
import com.simon.fehcatalog.data.HeroWithCollection
import com.simon.fehcatalog.model.FireEmblemGame
import com.simon.fehcatalog.model.MovementType
import com.simon.fehcatalog.model.UnitType
import com.simon.fehcatalog.model.WeaponColor
import com.simon.fehcatalog.model.searchKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CollectionFilter(val label: String) {
    ALL("Todos"),
    OWNED("Tengo"),
    KEPT("Conservo"),
    MISSING("Me faltan"),
    NOT_KEPT("Ya no"),
    FAVORITES("Favoritos")
}

data class CatalogFilters(
    val query: String = "",
    val collection: CollectionFilter = CollectionFilter.ALL,
    val game: FireEmblemGame? = null,
    val unitTypes: Set<UnitType> = emptySet(),
    val weaponColor: WeaponColor? = null,
    val movement: MovementType? = null,
    val specialRateOnly: Boolean = false
)

data class CatalogUiState(
    val heroes: List<HeroWithCollection> = emptyList(),
    val totalCount: Int = 0,
    val ownedCount: Int = 0,
    val query: String = "",
    val collectionFilter: CollectionFilter = CollectionFilter.ALL,
    val game: FireEmblemGame? = null,
    val unitTypes: Set<UnitType> = emptySet(),
    val weaponColor: WeaponColor? = null,
    val movement: MovementType? = null,
    val specialRateOnly: Boolean = false,
    val availableGames: List<FireEmblemGame> = emptyList(),
    val availableUnitTypes: List<UnitType> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() ||
            collectionFilter != CollectionFilter.ALL ||
            game != null ||
            unitTypes.isNotEmpty() ||
            weaponColor != null ||
            movement != null ||
            specialRateOnly

    val nonQueryFilterCount: Int
        get() = listOf(
            collectionFilter != CollectionFilter.ALL,
            game != null,
            unitTypes.isNotEmpty(),
            weaponColor != null,
            movement != null,
            specialRateOnly
        ).count { it }
}

class CatalogViewModel(
    private val repository: HeroRepository
) : ViewModel() {

    private val filters = MutableStateFlow(CatalogFilters())
    private val isPreparing = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)

    init {
        loadCatalog()
    }

    val uiState: StateFlow<CatalogUiState> = combine(
        repository.observeHeroes(),
        filters,
        isPreparing,
        errorMessage
    ) { allHeroes, currentFilters, preparing, error ->
        val normalizedQuery = currentFilters.query.trim().searchKey()
        val filtered = allHeroes
            .filter { item ->
                val hero = item.hero
                (normalizedQuery.isEmpty() ||
                    hero.name.searchKey().contains(normalizedQuery) ||
                    hero.title.searchKey().contains(normalizedQuery) ||
                    hero.page.searchKey().contains(normalizedQuery)) &&
                    matchesCollection(item, currentFilters.collection) &&
                    (currentFilters.game == null ||
                        currentFilters.game in hero.originGames) &&
                    (currentFilters.unitTypes.isEmpty() ||
                        currentFilters.unitTypes.any { it in hero.unitTypes }) &&
                    (currentFilters.weaponColor == null ||
                        hero.weaponColor == currentFilters.weaponColor) &&
                    (currentFilters.movement == null ||
                        hero.movement == currentFilters.movement) &&
                    (!currentFilters.specialRateOnly || hero.revival)
            }
            .sortedWith(
                compareByDescending<HeroWithCollection> { it.isFavorite }
                    .thenBy { it.hero.name.searchKey() }
                    .thenBy { it.hero.title.searchKey() }
            )

        CatalogUiState(
            heroes = filtered,
            totalCount = allHeroes.size,
            ownedCount = allHeroes.count { it.owned },
            query = currentFilters.query,
            collectionFilter = currentFilters.collection,
            game = currentFilters.game,
            unitTypes = currentFilters.unitTypes,
            weaponColor = currentFilters.weaponColor,
            movement = currentFilters.movement,
            specialRateOnly = currentFilters.specialRateOnly,
            availableGames = FireEmblemGame.entries.filter { game ->
                game != FireEmblemGame.UNKNOWN &&
                    allHeroes.any { game in it.hero.originGames }
            },
            availableUnitTypes = UnitType.entries.filter { type ->
                allHeroes.any { type in it.hero.unitTypes }
            },
            isLoading = preparing || (allHeroes.isEmpty() && error == null),
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState()
    )

    private fun loadCatalog() {
        viewModelScope.launch {
            isPreparing.value = true
            errorMessage.value = null
            runCatching {
                repository.ensureCatalogSeeded()
            }.onFailure { error ->
                errorMessage.value = error.message ?: "No se pudo cargar el catálogo"
            }
            isPreparing.value = false
        }
    }

    fun retryLoading() {
        viewModelScope.launch {
            isPreparing.value = true
            errorMessage.value = null
            runCatching {
                repository.ensureCatalogSeeded(force = true)
            }.onFailure { error ->
                errorMessage.value = error.message ?: "No se pudo cargar el catálogo"
            }
            isPreparing.value = false
        }
    }

    fun onQueryChange(value: String) {
        filters.update { it.copy(query = value) }
    }

    fun onCollectionFilterChange(value: CollectionFilter) {
        filters.update { it.copy(collection = value) }
    }

    fun onGameSelected(value: FireEmblemGame?) {
        filters.update { it.copy(game = value) }
    }

    fun toggleUnitType(value: UnitType) {
        filters.update { current ->
            current.copy(
                unitTypes = if (value in current.unitTypes) {
                    current.unitTypes - value
                } else {
                    current.unitTypes + value
                }
            )
        }
    }

    fun onWeaponColorSelected(value: WeaponColor?) {
        filters.update { it.copy(weaponColor = value) }
    }

    fun onMovementSelected(value: MovementType?) {
        filters.update { it.copy(movement = value) }
    }

    fun toggleSpecialRate() {
        filters.update { it.copy(specialRateOnly = !it.specialRateOnly) }
    }

    fun clearFilters() {
        filters.value = CatalogFilters()
    }

    fun onOwnedChange(heroId: Long, owned: Boolean) {
        viewModelScope.launch {
            repository.setOwned(heroId, owned)
        }
    }

    private fun matchesCollection(
        item: HeroWithCollection,
        filter: CollectionFilter
    ): Boolean = when (filter) {
        CollectionFilter.ALL -> true
        CollectionFilter.OWNED -> item.owned
        CollectionFilter.KEPT -> item.owned && item.kept
        CollectionFilter.MISSING -> !item.owned
        CollectionFilter.NOT_KEPT -> item.owned && !item.kept
        CollectionFilter.FAVORITES -> item.isFavorite
    }

    companion object {
        fun factory(repository: HeroRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CatalogViewModel(repository) }
        }
    }
}
