package com.simon.fehcatalog.model

/**
 * Tipos de movimiento de un héroe en Fire Emblem Heroes.
 */
enum class MovementType(val label: String) {
    INFANTRY("Infantería"),
    ARMORED("Acorazado"),
    CAVALRY("Jinete"),
    FLYING("Volador")
}

/**
 * Color del arma. En FEH determina las ventajas y desventajas del triángulo.
 */
enum class WeaponColor(val label: String) {
    RED("Rojo"),
    BLUE("Azul"),
    GREEN("Verde"),
    COLORLESS("Incoloro")
}

/**
 * Tipo de arma. Se combina con [WeaponColor] (por ejemplo: Tomo + Rojo).
 */
enum class WeaponType(val label: String) {
    SWORD("Espada"),
    LANCE("Lanza"),
    AXE("Hacha"),
    BOW("Arco"),
    DAGGER("Daga"),
    TOME("Tomo"),
    STAFF("Bastón"),
    BREATH("Aliento"),
    BEAST("Garra")
}

/**
 * Juegos según el estándar de FEH.
 *
 * [sort] coincide con el valor de GameSort de la wiki y con el orden que usa
 * el juego para agrupar héroes. Three Hopes se agrupa dentro de Three Houses,
 * y Shadow Dragon / New Mystery / Mystery of the Emblem comparten categoría.
 */
enum class FireEmblemGame(
    val label: String,
    val shortLabel: String,
    val sort: Int
) {
    HEROES("Heroes", "Heroes", 0),
    SHADOW_DRAGON_NEW_MYSTERY("Shadow Dragon / (New) Mystery", "Shadow Dragon", 1),
    ECHOES("Echoes", "Echoes", 2),
    GENEALOGY_OF_THE_HOLY_WAR("Genealogy of the Holy War", "Genealogy", 3),
    THRACIA_776("Thracia 776", "Thracia 776", 4),
    BINDING_BLADE("The Binding Blade", "Binding Blade", 5),
    BLAZING_BLADE("The Blazing Blade", "Blazing Blade", 6),
    SACRED_STONES("The Sacred Stones", "Sacred Stones", 7),
    PATH_OF_RADIANCE("Path of Radiance", "Path of Radiance", 8),
    RADIANT_DAWN("Radiant Dawn", "Radiant Dawn", 9),
    AWAKENING("Awakening", "Awakening", 10),
    FATES("Fates", "Fates", 11),
    THREE_HOUSES("Three Houses", "Three Houses", 12),
    TOKYO_MIRAGE_SESSIONS("Tokyo Mirage Sessions #FE Encore", "TMS #FE", 13),
    ENGAGE("Engage", "Engage", 14),
    SHADOWS("Fire Emblem Shadows", "Shadows", 15),
    FORTUNES_WEAVE("Fortune's Weave", "Fortune's Weave", 16),
    UNKNOWN("Desconocido", "Desconocido", 99);

    companion object {
        fun fromName(value: String): FireEmblemGame? =
            entries.firstOrNull { it.name == value }

        fun fromSort(value: Int): FireEmblemGame? =
            entries.firstOrNull { it.sort == value && it != UNKNOWN }
    }
}

/**
 * Categorías de disponibilidad / tipo de unidad.
 *
 * Un héroe puede tener varias a la vez (por ejemplo, Special + Duo). No son
 * excluyentes: describen cómo se obtiene el héroe y qué tan limitado es.
 */
enum class UnitType(val label: String) {
    REGULAR_5("Regular 5★"),
    REGULAR_4("Regular 3-4★"),
    SPECIAL("Special"),
    SPECIAL_4_5("4-5★"),
    LEGENDARY("Legendary"),
    MYTHIC("Mythic"),
    DUO("Duo"),
    HARMONIZED("Harmonized"),
    ASCENDED("Ascended"),
    REARMED("Rearmed"),
    ATTUNED("Attuned"),
    EMBLEM("Emblem"),
    AIDED("Aided"),
    ENTWINED("Entwined"),
    CHOSEN("Chosen"),
    VISTA("Vista"),
    BRAVE("Brave"),
    STORY("Story"),
    GHB("GHB"),
    TEMPEST("Tempest");

    companion object {
        fun fromName(value: String): UnitType? =
            entries.firstOrNull { it.name == value }
    }
}

/**
 * Los ocho tipos de favorito de FEH. El valor [slot] se guarda en Room;
 * `0` queda reservado para "sin favorito".
 */
enum class FavoriteType(val slot: Int, val label: String) {
    TYPE_1(1, "1"),
    TYPE_2(2, "2"),
    TYPE_3(3, "3"),
    TYPE_4(4, "4"),
    TYPE_5(5, "5"),
    TYPE_6(6, "6"),
    TYPE_7(7, "7"),
    TYPE_8(8, "8");

    companion object {
        fun fromSlot(value: Int): FavoriteType? =
            entries.firstOrNull { it.slot == value }
    }
}

const val MAX_MERGES = 10
const val MAX_COPIES = 99
const val MAX_FAVORITE_SLOT = 8

/** Los merges en FEH van de +0 a +10. */
fun normalizeMerges(value: Int): Int = value.coerceIn(0, MAX_MERGES)

/** Copias actuales de un héroe. 0 significa que ya no se conserva. */
fun normalizeCopies(value: Int): Int = value.coerceIn(0, MAX_COPIES)

/** Tipo de favorito: 0 = sin favorito; 1..8 = uno de los ocho colores. */
fun normalizeFavoriteSlot(value: Int): Int = value.coerceIn(0, MAX_FAVORITE_SLOT)
