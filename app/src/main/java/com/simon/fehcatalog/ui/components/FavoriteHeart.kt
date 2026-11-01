package com.simon.fehcatalog.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.simon.fehcatalog.model.FavoriteType
import com.simon.fehcatalog.ui.theme.toUiColor

/** Corazón de favorito con el color del tipo elegido. */
@Composable
fun FavoriteHeart(
    favoriteType: FavoriteType?,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    contentDescription: String? = null
) {
    Icon(
        imageVector = if (favoriteType == null) {
            Icons.Default.FavoriteBorder
        } else {
            Icons.Default.Favorite
        },
        contentDescription = contentDescription,
        tint = favoriteType?.toUiColor() ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(size)
    )
}
