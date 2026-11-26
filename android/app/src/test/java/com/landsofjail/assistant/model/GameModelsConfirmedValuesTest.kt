package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameModelsConfirmedValuesTest {
    @Test
    fun uncertainWardenLevelIsNeverUsedAsConfirmedProgress() {
        val confirmed = PowerMetric(
            puissanceTotaleTexte = "12,345",
            niveauPrisonOuQG = "Niv. 18"
        )
        val uncertain = confirmed.copy(
            niveauPrisonOuQG = "Niv. 19",
            incertain = true,
            raisonIncertitude = "lecture partielle"
        )

        assertEquals(18, confirmed.confirmedWardenLevel())
        assertNull(uncertain.confirmedWardenLevel())
    }

    @Test
    fun uncertainOrInvalidResourceQuantitiesAreNeverUsedAsConfirmedStock() {
        val confirmed = ResourceRecord("Bois", "1 200", 1200.0)
        val uncertain = confirmed.copy(quantiteNumerique = 900.0, incertain = true)
        val missing = confirmed.copy(quantiteNumerique = null)
        val negative = confirmed.copy(quantiteNumerique = -1.0)
        val infinite = confirmed.copy(quantiteNumerique = Double.POSITIVE_INFINITY)

        assertEquals(1200.0, confirmed.confirmedQuantity()!!, 0.0)
        assertNull(uncertain.confirmedQuantity())
        assertNull(missing.confirmedQuantity())
        assertNull(negative.confirmedQuantity())
        assertNull(infinite.confirmedQuantity())
    }
}
