package com.simon.fehcatalog.data

import com.simon.fehcatalog.model.FireEmblemGame
import com.simon.fehcatalog.model.UnitType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `game sets round trip`() {
        val games = setOf(
            FireEmblemGame.HEROES,
            FireEmblemGame.THREE_HOUSES,
            FireEmblemGame.FORTUNES_WEAVE
        )
        assertEquals(games, converters.stringToGameSet(converters.gameSetToString(games)))
    }

    @Test
    fun `unit type sets round trip`() {
        val types = setOf(UnitType.SPECIAL, UnitType.DUO, UnitType.ASCENDED)
        assertEquals(types, converters.stringToUnitTypeSet(converters.unitTypeSetToString(types)))
    }

    @Test
    fun `unknown enum names are ignored`() {
        assertEquals(
            setOf(FireEmblemGame.HEROES),
            converters.stringToGameSet("HEROES,NOT_A_GAME")
        )
        assertEquals(
            setOf(UnitType.GHB),
            converters.stringToUnitTypeSet("GHB,NOT_A_TYPE")
        )
    }
}
