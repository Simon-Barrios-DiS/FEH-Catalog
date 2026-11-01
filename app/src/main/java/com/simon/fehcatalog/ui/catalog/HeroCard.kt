package com.simon.fehcatalog.ui.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simon.fehcatalog.data.HeroWithCollection
import com.simon.fehcatalog.ui.components.GameIcon
import com.simon.fehcatalog.ui.components.HeroPortrait
import com.simon.fehcatalog.ui.components.MiniTag
import com.simon.fehcatalog.ui.components.MovementIcon
import com.simon.fehcatalog.ui.components.UnitTypeIcons
import com.simon.fehcatalog.ui.components.WeaponIcon
import com.simon.fehcatalog.ui.components.withImageWidth
import com.simon.fehcatalog.ui.theme.toUiColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroCard(
    item: HeroWithCollection,
    onClick: () -> Unit,
    onToggleOwned: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val hero = item.hero
    val types = hero.unitTypes.sortedBy { it.ordinal }
    val unitTint = hero.weaponColor.toUiColor()
        .copy(alpha = 0.18f)
        .compositeOver(MaterialTheme.colorScheme.surface)
    val favoriteBorder = item.favoriteType?.let {
        BorderStroke(2.dp, it.toUiColor())
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                item.favoriteType?.let {
                    stateDescription = "Favorito ${it.label}"
                }
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = unitTint),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = favoriteBorder
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroPortrait(
                model = hero.imageUrl?.withImageWidth(150),
                name = hero.name,
                modifier = Modifier.size(76.dp)
            )

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = hero.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                if (hero.title.isNotBlank()) {
                    Text(
                        text = hero.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WeaponIcon(
                        color = hero.weaponColor,
                        type = hero.weaponType,
                        size = 26.dp
                    )
                    MovementIcon(
                        movement = hero.movement,
                        size = 20.dp
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    hero.originGames.sortedBy { it.sort }.forEach { game ->
                        GameIcon(game = game, size = 24.dp)
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.owned) {
                        MiniTag(if (item.kept) "×${item.copies}" else "Ya no")
                    }
                    if (item.merges > 0) {
                        MiniTag("+${item.merges}")
                    }
                    UnitTypeIcons(
                        types = types,
                        specialRate = hero.revival,
                        size = 20.dp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Checkbox(
                checked = item.owned,
                onCheckedChange = onToggleOwned,
                modifier = Modifier.semantics {
                    contentDescription = if (item.owned) {
                        "Quitar de mi colección"
                    } else {
                        "Añadir a mi colección"
                    }
                }
            )
        }
    }
}
