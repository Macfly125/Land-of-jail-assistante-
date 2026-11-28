package com.landsofjail.assistant

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
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
    val composeRule = createComposeRule()

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

        // DataStore loads the profile asynchronously; first wait for the visible header.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Coach de progression").fetchSemanticsNodes().isNotEmpty()
        }
        // Scroll the nested LazyColumn to item 3; lazy content is composed on demand.
        composeRule.onNodeWithTag("progression-coach-list").performScrollToIndex(2)
        composeRule.onNodeWithText("Nouvel objectif").assertIsDisplayed()
        composeRule.onNodeWithText("Enregistrer l’objectif")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun forticladLevelSixSelectionIsPreservedAndTargetAdjusted() {
        composeRule.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        ProgressionCoachScreen(
                            profileId = "instrumented-forticlad-level-six",
                            language = AppLanguage.FR,
                            resources = emptyList()
                        )
                    }
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Coach de progression").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("progression-coach-list").performScrollToIndex(1)
        composeRule.onNodeWithTag("forticlad-current-level-row").performScrollToIndex(5)
        composeRule.onNodeWithTag("forticlad-current-level-6").performClick().assertIsSelected()
        composeRule.onNodeWithTag("forticlad-target-level-7").assertIsSelected()
    }

    @Test
    fun forticladCanPlanFirstUpgradeFromBaseLevelThirty() {
        composeRule.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        ProgressionCoachScreen(
                            profileId = "instrumented-forticlad-initial-upgrade",
                            language = AppLanguage.FR,
                            resources = emptyList()
                        )
                    }
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Coach de progression").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("progression-coach-list").performScrollToIndex(1)
        composeRule.onNodeWithTag("forticlad-current-level-row").performScrollToIndex(0)
        composeRule.onNodeWithTag("forticlad-current-level-0").assertIsSelected()
        composeRule.onNodeWithTag("forticlad-target-level-1").assertIsSelected()
        composeRule.onNodeWithTag("forticlad-estimated-forticlad-cores")
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
