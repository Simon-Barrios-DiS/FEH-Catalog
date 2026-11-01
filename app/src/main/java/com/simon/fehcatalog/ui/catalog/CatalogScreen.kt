package com.simon.fehcatalog.ui.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simon.fehcatalog.model.FireEmblemGame
import com.simon.fehcatalog.model.MovementType
import com.simon.fehcatalog.model.UnitType
import com.simon.fehcatalog.model.WeaponColor
import com.simon.fehcatalog.ui.components.GameIcon
import com.simon.fehcatalog.ui.components.MovementIcon
import com.simon.fehcatalog.ui.components.SpecialRateIcon
import com.simon.fehcatalog.ui.components.UnitTypeIcon
import com.simon.fehcatalog.ui.components.WeaponColorIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onHeroClick: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Catálogo FEH",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = heroCountLabel(state),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showFilters = true }) {
                        Text(
                            if (state.nonQueryFilterCount > 0) {
                                "Filtros (${state.nonQueryFilterCount})"
                            } else {
                                "Filtros"
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Buscar por nombre o título") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            CollectionFilterRow(
                selected = state.collectionFilter,
                onSelected = viewModel::onCollectionFilterChange
            )

            if (state.hasActiveFilters) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.heroes.size} de ${state.totalCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = viewModel::clearFilters) {
                        Text("Limpiar filtros")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when {
                state.isLoading -> LoadingCatalog()

                state.errorMessage != null && state.totalCount == 0 -> {
                    ErrorCatalog(
                        message = state.errorMessage.orEmpty(),
                        onRetry = viewModel::retryLoading
                    )
                }

                state.totalCount == 0 -> {
                    EmptyCatalog(onRetry = viewModel::retryLoading)
                }

                state.heroes.isEmpty() -> {
                    NoResults(onClearFilters = viewModel::clearFilters)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items = state.heroes, key = { it.id }) { item ->
                            HeroCard(
                                item = item,
                                onClick = { onHeroClick(item.id) },
                                onToggleOwned = { owned ->
                                    viewModel.onOwnedChange(item.id, owned)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            state = state,
            onCollectionFilterChange = viewModel::onCollectionFilterChange,
            onGameSelected = viewModel::onGameSelected,
            onUnitTypeToggle = viewModel::toggleUnitType,
            onColorSelected = viewModel::onWeaponColorSelected,
            onMovementSelected = viewModel::onMovementSelected,
            onSpecialRateToggle = viewModel::toggleSpecialRate,
            onClearFilters = viewModel::clearFilters,
            onDismiss = { showFilters = false }
        )
    }
}

@Composable
private fun CollectionFilterRow(
    selected: CollectionFilter,
    onSelected: (CollectionFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CollectionFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label) },
                leadingIcon = if (filter == CollectionFilter.FAVORITES) {
                    {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    state: CatalogUiState,
    onCollectionFilterChange: (CollectionFilter) -> Unit,
    onGameSelected: (FireEmblemGame?) -> Unit,
    onUnitTypeToggle: (UnitType) -> Unit,
    onColorSelected: (WeaponColor?) -> Unit,
    onMovementSelected: (MovementType?) -> Unit,
    onSpecialRateToggle: () -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Filtros",
                style = MaterialTheme.typography.titleLarge
            )

            FilterSection("Colección") {
                CollectionFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.collectionFilter == filter,
                        onClick = { onCollectionFilterChange(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }

            FilterSection("Juego") {
                FilterChip(
                    selected = state.game == null,
                    onClick = { onGameSelected(null) },
                    label = { Text("Todos") }
                )
                state.availableGames.forEach { game ->
                    FilterChip(
                        selected = state.game == game,
                        onClick = {
                            onGameSelected(if (state.game == game) null else game)
                        },
                        label = { GameIcon(game = game, size = 30.dp) }
                    )
                }
            }

            FilterSection("Tipo de unidad") {
                if (state.availableUnitTypes.isEmpty()) {
                    Text(
                        text = "Sin datos todavía",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.availableUnitTypes.forEach { type ->
                        FilterChip(
                            selected = type in state.unitTypes,
                            onClick = { onUnitTypeToggle(type) },
                            label = { Text(type.label) },
                            leadingIcon = {
                                UnitTypeIcon(
                                    type = type,
                                    size = FilterChipDefaults.IconSize,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }

            FilterSection("Special Rate") {
                FilterChip(
                    selected = state.specialRateOnly,
                    onClick = onSpecialRateToggle,
                    label = { Text("Special Rate") },
                    leadingIcon = {
                        SpecialRateIcon(
                            size = FilterChipDefaults.IconSize,
                            contentDescription = null
                        )
                    }
                )
            }

            FilterSection("Movimiento") {
                FilterChip(
                    selected = state.movement == null,
                    onClick = { onMovementSelected(null) },
                    label = { Text("Cualquiera") }
                )
                MovementType.entries.forEach { movement ->
                    FilterChip(
                        selected = state.movement == movement,
                        onClick = {
                            onMovementSelected(
                                if (state.movement == movement) null else movement
                            )
                        },
                        label = { MovementIcon(movement = movement, size = 26.dp) }
                    )
                }
            }

            FilterSection("Color de arma") {
                FilterChip(
                    selected = state.weaponColor == null,
                    onClick = { onColorSelected(null) },
                    label = { Text("Todos") }
                )
                WeaponColor.entries.forEach { color ->
                    FilterChip(
                        selected = state.weaponColor == color,
                        onClick = {
                            onColorSelected(
                                if (state.weaponColor == color) null else color
                            )
                        },
                        label = { WeaponColorIcon(color = color, size = 28.dp) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClearFilters) {
                    Text("Limpiar todo")
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = onDismiss) {
                    Text("Listo")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    content: @Composable FlowRowScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

@Composable
private fun LoadingCatalog() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Preparando catálogo…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyCatalog(onRetry: () -> Unit) {
    CatalogMessage(
        title = "El catálogo base está vacío",
        message = "No se pudo importar la lista de héroes.",
        buttonText = "Reintentar",
        onButtonClick = onRetry
    )
}

@Composable
private fun ErrorCatalog(message: String, onRetry: () -> Unit) {
    CatalogMessage(
        title = "No se pudo cargar el catálogo",
        message = message,
        buttonText = "Reintentar",
        onButtonClick = onRetry
    )
}

@Composable
private fun NoResults(onClearFilters: () -> Unit) {
    CatalogMessage(
        title = "Sin resultados",
        message = "Prueba con otro nombre o quita los filtros activos.",
        buttonText = "Limpiar filtros",
        onButtonClick = onClearFilters
    )
}

@Composable
private fun CatalogMessage(
    title: String,
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onButtonClick) {
            Text(buttonText)
        }
    }
}

private fun heroCountLabel(state: CatalogUiState): String = when {
    state.isLoading -> "Preparando catálogo…"
    state.totalCount == 0 -> "Sin catálogo"
    state.ownedCount == 0 -> "${state.totalCount} héroes"
    state.ownedCount == 1 -> "${state.totalCount} héroes · 1 en tu colección"
    else -> "${state.totalCount} héroes · ${state.ownedCount} en tu colección"
}
