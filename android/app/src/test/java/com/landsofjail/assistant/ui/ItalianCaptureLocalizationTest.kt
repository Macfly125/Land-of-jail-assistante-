package com.landsofjail.assistant.ui

import com.landsofjail.assistant.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class ItalianCaptureLocalizationTest {
    @Test
    fun captureCategoriesAreItalian() {
        assertEquals("Risorse", localizedCaptureCategory(AppLanguage.IT, "resources"))
        assertEquals("Hangar armature potenziate", localizedCaptureCategory(AppLanguage.IT, "power_armor"))
        assertEquals("Miglioramento equipaggiamento", localizedCaptureCategory(AppLanguage.IT, "equipment"))
    }

    @Test
    fun allSixResourceNamesAreItalian() {
        assertEquals("Cibo", localizedResourceName(AppLanguage.IT, "Food"))
        assertEquals("Legno", localizedResourceName(AppLanguage.IT, "Wood"))
        assertEquals("Acciaio", localizedResourceName(AppLanguage.IT, "Steel"))
        assertEquals("Benzina", localizedResourceName(AppLanguage.IT, "Gasoline"))
        assertEquals("Diamanti", localizedResourceName(AppLanguage.IT, "Gems"))
        assertEquals("Oro", localizedResourceName(AppLanguage.IT, "Gold"))
    }

    @Test
    fun equipmentPiecesAreItalian() {
        assertEquals("Arma", localizedEquipmentPiece(AppLanguage.IT, "Weapon"))
        assertEquals("Armatura", localizedEquipmentPiece(AppLanguage.IT, "Armor"))
        assertEquals("Elmo", localizedEquipmentPiece(AppLanguage.IT, "Helmet"))
        assertEquals("Guanti", localizedEquipmentPiece(AppLanguage.IT, "Gloves"))
        assertEquals("Stivali", localizedEquipmentPiece(AppLanguage.IT, "Boots"))
        assertEquals("Accessorio", localizedEquipmentPiece(AppLanguage.IT, "Accessory"))
    }
}
