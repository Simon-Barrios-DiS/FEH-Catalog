package com.simon.fehcatalog.ui.theme

import androidx.compose.ui.graphics.Color
import com.simon.fehcatalog.model.FavoriteType
import com.simon.fehcatalog.model.WeaponColor

// Paleta inspirada en la interfaz de Fire Emblem Heroes: azul noche y dorado.
val FehNavy = Color(0xFF0B1220)
val FehSurface = Color(0xFF141F33)
val FehSurfaceVariant = Color(0xFF1F2D45)
val FehOnSurface = Color(0xFFF2EEE3)
val FehMuted = Color(0xFFA9B4C6)
val FehGold = Color(0xFFE8C46A)
val FehCream = Color(0xFFF7F3E8)
val FehInk = Color(0xFF172033)
val FehRed = Color(0xFFE06868)
val FehBlue = Color(0xFF6A9BE8)
val FehGreen = Color(0xFF71C07A)
val FehColorless = Color(0xFFC4CBD6)

/** Color de interfaz asociado al color de arma. */
fun WeaponColor.toUiColor(): Color = when (this) {
    WeaponColor.RED -> FehRed
    WeaponColor.BLUE -> FehBlue
    WeaponColor.GREEN -> FehGreen
    WeaponColor.COLORLESS -> FehColorless
}

// Los ocho colores de favorito de FEH. Se usan para el corazón de la ficha y
// de la tarjeta, y para el selector de tipo de favorito.
val FavoriteLightPink = Color(0xFFF48FB1)
val FavoriteOrange = Color(0xFFF57C00)
val FavoriteYellow = Color(0xFFFBC02D)
val FavoriteGreen = Color(0xFF43A047)
val FavoriteCyan = Color(0xFF00ACC1)
val FavoriteBlue = Color(0xFF1E88E5)
val FavoritePurple = Color(0xFF8E24AA)
val FavoriteIntensePink = Color(0xFFD81B60)

/** Color de interfaz asociado al tipo de favorito. */
fun FavoriteType.toUiColor(): Color = when (this) {
    FavoriteType.TYPE_1 -> FavoriteLightPink
    FavoriteType.TYPE_2 -> FavoriteOrange
    FavoriteType.TYPE_3 -> FavoriteYellow
    FavoriteType.TYPE_4 -> FavoriteGreen
    FavoriteType.TYPE_5 -> FavoriteCyan
    FavoriteType.TYPE_6 -> FavoriteBlue
    FavoriteType.TYPE_7 -> FavoritePurple
    FavoriteType.TYPE_8 -> FavoriteIntensePink
}
