package com.simon.fehcatalog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.simon.fehcatalog.model.WeaponColor
import com.simon.fehcatalog.ui.theme.toUiColor

private val IMAGE_WIDTH_PATTERN = Regex("/scale-to-width-down/\\d+")

/** Pide a la CDN de Fandom un tamaño de imagen distinto al del catálogo. */
fun String.withImageWidth(width: Int): String =
    replace(IMAGE_WIDTH_PATTERN, "/scale-to-width-down/$width")

/** Retrato del héroe con placeholder cuando todavía no tiene imagen. */
@Composable
fun HeroPortrait(
    model: Any?,
    name: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "Retrato de $name",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Punto de color que representa el color de arma. */
@Composable
fun WeaponColorDot(
    color: WeaponColor,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color.toUiColor())
    )
}
