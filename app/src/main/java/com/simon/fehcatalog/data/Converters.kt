package com.simon.fehcatalog.data

import androidx.room.TypeConverter
import com.simon.fehcatalog.model.FireEmblemGame
import com.simon.fehcatalog.model.MovementType
import com.simon.fehcatalog.model.UnitType
import com.simon.fehcatalog.model.WeaponColor
import com.simon.fehcatalog.model.WeaponType

/**
 * Room guarda los enums por nombre. Si en el futuro se elimina un valor del
 * enum, el fallback evita que la app falle al leer datos antiguos.
 */
class Converters {

    @TypeConverter
    fun movementToString(value: MovementType): String = value.name

    @TypeConverter
    fun stringToMovement(value: String): MovementType =
        runCatching { MovementType.valueOf(value) }.getOrDefault(MovementType.INFANTRY)

    @TypeConverter
    fun weaponColorToString(value: WeaponColor): String = value.name

    @TypeConverter
    fun stringToWeaponColor(value: String): WeaponColor =
        runCatching { WeaponColor.valueOf(value) }.getOrDefault(WeaponColor.RED)

    @TypeConverter
    fun weaponTypeToString(value: WeaponType): String = value.name

    @TypeConverter
    fun stringToWeaponType(value: String): WeaponType =
        runCatching { WeaponType.valueOf(value) }.getOrDefault(WeaponType.SWORD)

    @TypeConverter
    fun gameSetToString(value: Set<FireEmblemGame>): String =
        value.joinToString(separator = ",") { it.name }

    @TypeConverter
    fun stringToGameSet(value: String): Set<FireEmblemGame> =
        value.split(',')
            .mapNotNull { FireEmblemGame.fromName(it.trim()) }
            .toSet()

    @TypeConverter
    fun unitTypeSetToString(value: Set<UnitType>): String =
        value.joinToString(separator = ",") { it.name }

    @TypeConverter
    fun stringToUnitTypeSet(value: String): Set<UnitType> =
        value.split(',')
            .mapNotNull { UnitType.fromName(it.trim()) }
            .toSet()
}
