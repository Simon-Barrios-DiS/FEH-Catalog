package com.simon.fehcatalog.model

import java.text.Normalizer
import java.util.Locale

private val DIACRITICS = "\\p{Mn}+".toRegex()

/**
 * Clave de búsqueda tolerante a mayúsculas y acentos.
 *
 * Permite que "otr" encuentre a "Ótr" y que "jotun" encuentre "Jötun".
 * También translitera letras nórdicas que aparecen en algunos nombres.
 */
fun String.searchKey(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase(Locale.ROOT)
        .replace("þ", "th")
        .replace("ð", "d")
        .replace("ø", "o")
        .replace("æ", "ae")
        .replace("œ", "oe")
