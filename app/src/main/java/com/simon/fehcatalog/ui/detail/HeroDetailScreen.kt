package com.simon.fehcatalog.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simon.fehcatalog.data.HeroWithCollection
import com.simon.fehcatalog.model.MAX_COPIES
import com.simon.fehcatalog.model.MAX_MERGES
import com.simon.fehcatalog.ui.components.FavoriteHeart
import com.simon.fehcatalog.ui.components.FavoritePickerDialog
import com.simon.fehcatalog.ui.components.GameIcon
import com.simon.fehcatalog.ui.components.HeroPortrait
import com.simon.fehcatalog.ui.components.MiniTag
import com.simon.fehcatalog.ui.components.MovementIcon
import com.simon.fehcatalog.ui.components.SpecialRateIcon
import com.simon.fehcatalog.ui.components.UnitTypeIcon
import com.simon.fehcatalog.ui.components.WeaponColorIcon
import com.simon.fehcatalog.ui.components.WeaponIcon
import com.simon.fehcatalog.ui.components.WeaponTypeIcon
import com.simon.fehcatalog.ui.components.withImageWidth
import com.simon.fehcatalog.ui.theme.toUiColor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroDetailScreen(
    viewModel: HeroDetailViewModel,
    onBack: () -> Unit
) {
    val item by viewModel.hero.collectAsStateWithLifecycle()
    var showNotesDialog by rememberSaveable { mutableStateOf(false) }
    var showFavoriteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item?.hero?.name ?: "Héroe",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    item?.let { current ->
                        IconButton(onClick = { showFavoriteDialog = true }) {
                            FavoriteHeart(
                                favoriteType = current.favoriteType,
                                size = 24.dp,
                                contentDescription = current.favoriteType?.let {
                                    "Favorito ${it.label}"
                                } ?: "Elegir tipo de favorito"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        val current = item
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            HeroDetailContent(
                item = current,
                onOwnedChange = viewModel::setOwned,
                onCopiesChange = viewModel::setCopies,
                onKeptChange = viewModel::setKept,
                onMergesChange = viewModel::setMerges,
                onEditFavorite = { showFavoriteDialog = true },
                onEditNotes = { showNotesDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            )
        }
    }

    val current = item
    if (showNotesDialog && current != null) {
        NotesDialog(
            initialNotes = current.collection.notes,
            onDismiss = { showNotesDialog = false },
            onSave = { notes ->
                viewModel.saveNotes(notes)
                showNotesDialog = false
            }
        )
    }

    if (showFavoriteDialog && current != null) {
        FavoritePickerDialog(
            current = current.favoriteType,
            onSelect = { favoriteSlot ->
                viewModel.setFavorite(favoriteSlot)
                showFavoriteDialog = false
            },
            onDismiss = { showFavoriteDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroDetailContent(
    item: HeroWithCollection,
    onOwnedChange: (Boolean) -> Unit,
    onCopiesChange: (Int) -> Unit,
    onKeptChange: (Boolean) -> Unit,
    onMergesChange: (Int) -> Unit,
    onEditFavorite: () -> Unit,
    onEditNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hero = item.hero
    val games = hero.originGames.sortedBy { it.sort }
    val types = hero.unitTypes.sortedBy { it.ordinal }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeroPortrait(
            model = hero.imageUrl?.withImageWidth(300),
            name = hero.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = hero.name,
                style = MaterialTheme.typography.headlineSmall
            )
            if (hero.title.isNotBlank()) {
                Text(
                    text = hero.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeaponIcon(
                    color = hero.weaponColor,
                    type = hero.weaponType,
                    size = 30.dp
                )
                MovementIcon(
                    movement = hero.movement,
                    size = 22.dp
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            games.forEach { game -> GameIcon(game = game, size = 30.dp) }
            if (hero.revival) {
                MiniTag(
                    text = "Special Rate",
                    leadingIcon = {
                        SpecialRateIcon(size = 14.dp, contentDescription = null)
                    }
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            types.forEach { type ->
                MiniTag(
                    text = type.label,
                    leadingIcon = {
                        UnitTypeIcon(
                            type = type,
                            size = 14.dp,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        CollectionCard(
            item = item,
            onOwnedChange = onOwnedChange,
            onCopiesChange = onCopiesChange,
            onKeptChange = onKeptChange,
            onMergesChange = onMergesChange,
            onEditFavorite = onEditFavorite
        )

        NotesCard(item = item, onEditNotes = onEditNotes)

        BaseInfoCard(item = item)

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CollectionCard(
    item: HeroWithCollection,
    onOwnedChange: (Boolean) -> Unit,
    onCopiesChange: (Int) -> Unit,
    onKeptChange: (Boolean) -> Unit,
    onMergesChange: (Int) -> Unit,
    onEditFavorite: () -> Unit
) {
    val unitTint = item.hero.weaponColor.toUiColor()
        .copy(alpha = 0.18f)
        .compositeOver(MaterialTheme.colorScheme.surface)
    val favoriteBorder = item.favoriteType?.let {
        BorderStroke(2.dp, it.toUiColor())
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = unitTint),
        border = favoriteBorder
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Mi colección",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                MiniTag(collectionStatus(item))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lo tengo",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (item.owned) {
                            "Este héroe está en tu colección."
                        } else {
                            "Márcalo cuando lo consigas."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = item.owned,
                    onCheckedChange = onOwnedChange
                )
            }

            if (item.owned) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )

                CopiesStepper(
                    copies = item.copies,
                    onCopiesChange = onCopiesChange
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lo conservo",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = if (item.kept) {
                                "Todavía tienes al menos una copia."
                            } else {
                                "Ya no lo conservas, pero queda el registro."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = item.kept,
                        onCheckedChange = onKeptChange
                    )
                }

                if (item.kept) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                    MergesControl(
                        merges = item.merges,
                        onMergesChange = onMergesChange
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Favorito",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = item.favoriteType?.let { "Tipo ${it.label}" }
                            ?: "Sin favorito",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onEditFavorite) {
                    FavoriteHeart(
                        favoriteType = item.favoriteType,
                        size = 18.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Elegir")
                }
            }
        }
    }
}

@Composable
private fun CopiesStepper(
    copies: Int,
    onCopiesChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Copias",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Incluye la copia que usas como base.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = { onCopiesChange(copies - 1) },
            enabled = copies > 1
        ) {
            Text(
                text = "−",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Text(
            text = "$copies",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(32.dp)
        )
        IconButton(
            onClick = { onCopiesChange(copies + 1) },
            enabled = copies < MAX_COPIES
        ) {
            Icon(Icons.Default.Add, contentDescription = "Más copias")
        }
    }
}

@Composable
private fun MergesControl(
    merges: Int,
    onMergesChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Merges",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            MiniTag("+$merges")
        }
        Slider(
            value = merges.toFloat(),
            onValueChange = { value -> onMergesChange(value.roundToInt()) },
            valueRange = 0f..MAX_MERGES.toFloat(),
            steps = MAX_MERGES - 1
        )
        Text(
            text = "De +0 a +$MAX_MERGES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotesCard(
    item: HeroWithCollection,
    onEditNotes: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Notas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditNotes) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar notas")
                }
            }
            Text(
                text = item.collection.notes.ifBlank { "Sin notas" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.collection.notes.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun BaseInfoCard(item: HeroWithCollection) {
    val hero = item.hero
    val games = hero.originGames.sortedBy { it.sort }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            InfoIconRow("Movimiento") {
                MovementIcon(movement = hero.movement, size = 24.dp)
            }
            InfoIconRow("Color de arma") {
                WeaponColorIcon(color = hero.weaponColor, size = 24.dp)
            }
            InfoIconRow("Tipo de arma") {
                WeaponTypeIcon(type = hero.weaponType, size = 24.dp)
            }
            InfoIconRow("Juego de origen") {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    games.forEach { game -> GameIcon(game = game, size = 26.dp) }
                }
            }
            InfoRow("Origen", hero.bannerType ?: "No aplica")
            if (hero.revival) {
                InfoIconRow("Etiqueta") {
                    SpecialRateIcon(size = 24.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Special Rate",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (!hero.gameVersion.isNullOrBlank()) {
                InfoRow(
                    "Versión",
                    hero.gameVersion,
                    showDivider = !hero.releaseDate.isNullOrBlank()
                )
            }
            if (!hero.releaseDate.isNullOrBlank()) {
                InfoRow("Lanzamiento", hero.releaseDate, showDivider = false)
            }
        }
    }
}

@Composable
private fun InfoIconRow(
    label: String,
    showDivider: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1.2f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
    if (showDivider) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
    }
    if (showDivider) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun NotesDialog(
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var notes by rememberSaveable(initialNotes) { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notas") },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("IVs, build, habilidades clave…") },
                minLines = 3,
                maxLines = 6
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(notes) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun collectionStatus(item: HeroWithCollection): String = when {
    item.owned && item.kept -> "En tu colección"
    item.owned -> "Ya no lo conservas"
    else -> "No lo tienes"
}
