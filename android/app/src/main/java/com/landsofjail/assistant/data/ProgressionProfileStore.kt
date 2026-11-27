package com.landsofjail.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.landsofjail.assistant.model.ForticladInventory
import com.landsofjail.assistant.model.ProgressionPlayerProfile
import com.landsofjail.assistant.model.ProgressionProfileCodec
import com.landsofjail.assistant.model.upsertForticladInventoryStocks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

/**
 * Transactional, per-player progression storage.
 *
 * The first read imports the previous SharedPreferences profile and Forticlad
 * inventory without deleting either legacy file. Migration is guarded by a
 * DataStore marker so it is safe across process restarts.
 */
class ProgressionProfileStore(context: Context, profileId: String) {
    private val appContext = context.applicationContext
    private val safeProfileId = profileId.trim().ifBlank { "default" }
    private val dataStore = synchronized(stores) { stores.getOrPut("${appContext.filesDir.absolutePath}:$safeProfileId") {
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { appContext.preferencesDataStoreFile("progression_profile_$safeProfileId.preferences_pb") }
        )
    } }

    suspend fun load(): ProgressionPlayerProfile {
        migrateLegacyIfNeeded()
        val preferences = dataStore.data.first()
        return ProgressionProfileCodec.decodeOrDefault(preferences[PROFILE_JSON], safeProfileId)
    }

    suspend fun save(profile: ProgressionPlayerProfile) {
        require(profile.profileId == safeProfileId) { "Profile ID mismatch" }
        dataStore.edit { preferences ->
            preferences[PROFILE_JSON] = ProgressionProfileCodec.encode(profile)
            preferences[MIGRATION_COMPLETE] = true
        }
    }

    private suspend fun migrateLegacyIfNeeded() {
        dataStore.edit { target ->
            if (target[MIGRATION_COMPLETE] == true) return@edit

            val legacyProfilePrefs = appContext.getSharedPreferences(
                "progression_profile_$safeProfileId",
                Context.MODE_PRIVATE
            )
            val legacyInventoryPrefs = appContext.getSharedPreferences(
                "forticlad_inventory_$safeProfileId",
                Context.MODE_PRIVATE
            )
            val decoded = ProgressionProfileCodec.decodeOrDefault(
                legacyProfilePrefs.getString("profile", null),
                safeProfileId
            )
            val hasForticlad = decoded.confirmedStock("forticlad cores") != null
            val hasAdvanced = decoded.confirmedStock("advanced forticlad cores") != null
            val hasLegacyInventory = legacyInventoryPrefs.contains("forticlad_cores") ||
                legacyInventoryPrefs.contains("advanced_cores")

            val migrated = if (hasLegacyInventory && (!hasForticlad || !hasAdvanced)) {
                val inventory = ForticladInventory(
                    legacyInventoryPrefs.getLong("forticlad_cores", 0L).coerceAtLeast(0L),
                    legacyInventoryPrefs.getLong("advanced_cores", 0L).coerceAtLeast(0L)
                )
                decoded.copy(resourceStocks = upsertForticladInventoryStocks(
                    decoded.resourceStocks,
                    decoded.confirmedStock("forticlad cores") ?: inventory.forticladCores,
                    decoded.confirmedStock("advanced forticlad cores") ?: inventory.advancedCores,
                    System.currentTimeMillis()
                ))
            } else decoded

            if (legacyProfilePrefs.contains("profile") || hasLegacyInventory) {
                target[PROFILE_JSON] = ProgressionProfileCodec.encode(migrated)
            }
            target[MIGRATION_COMPLETE] = true
        }
    }

    companion object {
        private val PROFILE_JSON = stringPreferencesKey("profile_json")
        private val MIGRATION_COMPLETE = booleanPreferencesKey("legacy_migration_complete")
        private val stores = mutableMapOf<String, DataStore<Preferences>>()
    }
}
