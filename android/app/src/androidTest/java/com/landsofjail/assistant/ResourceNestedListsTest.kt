package com.landsofjail.assistant

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.landsofjail.assistant.model.AppLanguage
import com.landsofjail.assistant.ui.ProgressionCoachScreen
import com.landsofjail.assistant.ui.SvSPreparationScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResourceNestedListsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun progressionCoachMeasuresInsideParentLazyColumn() {
        composeRule.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        ProgressionCoachScreen(
                            profileId = "instrumented-progression",
                            language = AppLanguage.FR,
                            resources = emptyList()
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Nouvel objectif").assertIsDisplayed()
        composeRule.onNodeWithText("Enregistrer l’objectif")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun svsPlannerMeasuresAndScrollsInsideParentLazyColumn() {
        composeRule.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        SvSPreparationScreen(language = AppLanguage.FR)
                    }
                }
            }
        }

        composeRule.onNodeWithText("SVS · Préparation en 5 jours").assertIsDisplayed()
        composeRule.onNodeWithText("Calculateur de points SVS")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
