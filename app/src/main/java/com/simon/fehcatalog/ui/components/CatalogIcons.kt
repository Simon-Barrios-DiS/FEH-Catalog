package com.simon.fehcatalog.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.simon.fehcatalog.R
import com.simon.fehcatalog.model.FireEmblemGame
import com.simon.fehcatalog.model.MovementType
import com.simon.fehcatalog.model.UnitType
import com.simon.fehcatalog.model.WeaponColor
import com.simon.fehcatalog.model.WeaponType

/** Icono del arma combinando su color y su tipo (por ejemplo, espada roja). */
@Composable
fun WeaponIcon(
    color: WeaponColor,
    type: WeaponType,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    contentDescription: String? = "${color.label} · ${type.label}"
) {
    Image(
        painter = painterResource(weaponIconRes(color, type)),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/** Icono del color de arma, sin el tipo. */
@Composable
fun WeaponColorIcon(
    color: WeaponColor,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    contentDescription: String? = color.label
) {
    Image(
        painter = painterResource(weaponColorIconRes(color)),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/** Icono del tipo de arma, sin el color. */
@Composable
fun WeaponTypeIcon(
    type: WeaponType,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    contentDescription: String? = type.label
) {
    Image(
        painter = painterResource(weaponTypeIconRes(type)),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/** Icono del tipo de movimiento. */
@Composable
fun MovementIcon(
    movement: MovementType,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String? = movement.label
) {
    Image(
        painter = painterResource(movementIconRes(movement)),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/** Icono del juego de origen. `UNKNOWN` no tiene icono. */
@Composable
fun GameIcon(
    game: FireEmblemGame,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    contentDescription: String? = game.label
) {
    val icon = gameIconRes(game) ?: return
    Image(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/** Icono de la rareza o categoría de disponibilidad del héroe. */
@Composable
fun UnitTypeIcon(
    type: UnitType,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    contentDescription: String? = type.label
) {
    Image(
        painter = painterResource(unitTypeIconRes(type)),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/** Icono de la etiqueta Special Rate. */
@Composable
fun SpecialRateIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    contentDescription: String? = "Special Rate"
) {
    Image(
        painter = painterResource(R.drawable.ic_rarity_special_rate),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/**
 * Fila compacta de iconos de rareza para el catálogo. No repite el mismo icono
 * cuando dos categorías comparten símbolo; el nombre completo queda en la
 * descripción accesible.
 */
@Composable
fun UnitTypeIcons(
    types: Collection<UnitType>,
    specialRate: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    if (types.isEmpty() && !specialRate) return
    val labels = buildList {
        if (specialRate) add("Special Rate")
        types.forEach { add(it.label) }
    }.joinToString(" · ")
    val icons = buildList {
        if (specialRate) add(R.drawable.ic_rarity_special_rate)
        types.forEach { add(unitTypeIconRes(it)) }
    }.distinct()
    Row(
        modifier = modifier.semantics { contentDescription = labels },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icons.forEach { icon ->
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Fit
            )
        }
    }
}

internal fun unitTypeIconRes(type: UnitType): Int = when (type) {
    UnitType.REGULAR_5 -> R.drawable.ic_rarity_regular_5
    UnitType.REGULAR_4 -> R.drawable.ic_rarity_regular_1_4
    UnitType.SPECIAL -> R.drawable.ic_rarity_special
    UnitType.SPECIAL_4_5 -> R.drawable.ic_rarity_regular_1_4
    UnitType.LEGENDARY -> R.drawable.ic_rarity_legendary
    UnitType.MYTHIC -> R.drawable.ic_rarity_mythic
    UnitType.DUO -> R.drawable.ic_rarity_duo
    UnitType.HARMONIZED -> R.drawable.ic_rarity_harmonized
    UnitType.ASCENDED -> R.drawable.ic_rarity_ascended
    UnitType.REARMED -> R.drawable.ic_rarity_rearmed
    UnitType.ATTUNED -> R.drawable.ic_rarity_attuned
    UnitType.EMBLEM -> R.drawable.ic_rarity_emblem
    UnitType.AIDED -> R.drawable.ic_rarity_aided
    UnitType.ENTWINED -> R.drawable.ic_rarity_entwined
    UnitType.CHOSEN -> R.drawable.ic_rarity_chosen
    UnitType.VISTA -> R.drawable.ic_rarity_vista
    UnitType.BRAVE -> R.drawable.ic_rarity_brave
    UnitType.STORY -> R.drawable.ic_rarity_heroic_grail
    UnitType.GHB -> R.drawable.ic_rarity_heroic_grail
    UnitType.TEMPEST -> R.drawable.ic_rarity_heroic_grail
}

private fun weaponIconRes(color: WeaponColor, type: WeaponType): Int = when (color) {
    WeaponColor.RED -> when (type) {
        WeaponType.SWORD -> R.drawable.ic_weapon_red_sword
        WeaponType.TOME -> R.drawable.ic_weapon_red_tome
        WeaponType.BOW -> R.drawable.ic_weapon_red_bow
        WeaponType.DAGGER -> R.drawable.ic_weapon_red_dagger
        WeaponType.BREATH -> R.drawable.ic_weapon_red_breath
        WeaponType.BEAST -> R.drawable.ic_weapon_red_beast
        else -> weaponTypeIconRes(type)
    }

    WeaponColor.BLUE -> when (type) {
        WeaponType.LANCE -> R.drawable.ic_weapon_blue_lance
        WeaponType.TOME -> R.drawable.ic_weapon_blue_tome
        WeaponType.BOW -> R.drawable.ic_weapon_blue_bow
        WeaponType.DAGGER -> R.drawable.ic_weapon_blue_dagger
        WeaponType.BREATH -> R.drawable.ic_weapon_blue_breath
        WeaponType.BEAST -> R.drawable.ic_weapon_blue_beast
        else -> weaponTypeIconRes(type)
    }

    WeaponColor.GREEN -> when (type) {
        WeaponType.AXE -> R.drawable.ic_weapon_green_axe
        WeaponType.TOME -> R.drawable.ic_weapon_green_tome
        WeaponType.BOW -> R.drawable.ic_weapon_green_bow
        WeaponType.DAGGER -> R.drawable.ic_weapon_green_dagger
        WeaponType.BREATH -> R.drawable.ic_weapon_green_breath
        WeaponType.BEAST -> R.drawable.ic_weapon_green_beast
        else -> weaponTypeIconRes(type)
    }

    WeaponColor.COLORLESS -> when (type) {
        WeaponType.STAFF -> R.drawable.ic_weapon_colorless_staff
        WeaponType.TOME -> R.drawable.ic_weapon_colorless_tome
        WeaponType.BOW -> R.drawable.ic_weapon_colorless_bow
        WeaponType.DAGGER -> R.drawable.ic_weapon_colorless_dagger
        WeaponType.BREATH -> R.drawable.ic_weapon_colorless_breath
        WeaponType.BEAST -> R.drawable.ic_weapon_colorless_beast
        else -> weaponTypeIconRes(type)
    }
}

private fun weaponTypeIconRes(type: WeaponType): Int = when (type) {
    WeaponType.SWORD -> R.drawable.ic_weapon_type_sword
    WeaponType.LANCE -> R.drawable.ic_weapon_type_lance
    WeaponType.AXE -> R.drawable.ic_weapon_type_axe
    WeaponType.BOW -> R.drawable.ic_weapon_type_bow
    WeaponType.DAGGER -> R.drawable.ic_weapon_type_dagger
    WeaponType.TOME -> R.drawable.ic_weapon_type_tome
    WeaponType.STAFF -> R.drawable.ic_weapon_type_staff
    WeaponType.BREATH -> R.drawable.ic_weapon_type_breath
    WeaponType.BEAST -> R.drawable.ic_weapon_type_beast
}

private fun weaponColorIconRes(color: WeaponColor): Int = when (color) {
    WeaponColor.RED -> R.drawable.ic_weapon_color_red
    WeaponColor.BLUE -> R.drawable.ic_weapon_color_blue
    WeaponColor.GREEN -> R.drawable.ic_weapon_color_green
    WeaponColor.COLORLESS -> R.drawable.ic_weapon_color_colorless
}

private fun movementIconRes(movement: MovementType): Int = when (movement) {
    MovementType.INFANTRY -> R.drawable.ic_movement_infantry
    MovementType.ARMORED -> R.drawable.ic_movement_armored
    MovementType.CAVALRY -> R.drawable.ic_movement_cavalry
    MovementType.FLYING -> R.drawable.ic_movement_flying
}

private fun gameIconRes(game: FireEmblemGame): Int? = when (game) {
    FireEmblemGame.HEROES -> R.drawable.ic_game_heroes
    FireEmblemGame.SHADOW_DRAGON_NEW_MYSTERY ->
        R.drawable.ic_game_shadow_dragon_and_the_blade_of_light
    FireEmblemGame.ECHOES -> R.drawable.ic_game_echoes
    FireEmblemGame.GENEALOGY_OF_THE_HOLY_WAR -> R.drawable.ic_game_genealogy
    FireEmblemGame.THRACIA_776 -> R.drawable.ic_game_thracia_776
    FireEmblemGame.BINDING_BLADE -> R.drawable.ic_game_binding_blade
    FireEmblemGame.BLAZING_BLADE -> R.drawable.ic_game_blazing_blade
    FireEmblemGame.SACRED_STONES -> R.drawable.ic_game_sacred_stones
    FireEmblemGame.PATH_OF_RADIANCE -> R.drawable.ic_game_path_of_radiance
    FireEmblemGame.RADIANT_DAWN -> R.drawable.ic_game_radiant_dawn
    FireEmblemGame.AWAKENING -> R.drawable.ic_game_awakening
    FireEmblemGame.FATES -> R.drawable.ic_game_fates
    FireEmblemGame.THREE_HOUSES -> R.drawable.ic_game_three_houses
    FireEmblemGame.TOKYO_MIRAGE_SESSIONS -> R.drawable.ic_game_tokyo_mirage
    FireEmblemGame.ENGAGE -> R.drawable.ic_game_engage
    FireEmblemGame.SHADOWS -> R.drawable.ic_game_shadows
    FireEmblemGame.FORTUNES_WEAVE -> R.drawable.ic_game_fortunes_weave
    FireEmblemGame.UNKNOWN -> null
}
