package com.landsofjail.assistant.data

import android.content.Context
import com.landsofjail.assistant.model.ForticladInventory

/** Stores the player's Forticlad stock independently for each local player profile. */
class ForticladInventoryStore(context: Context, profileId: String) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "forticlad_inventory_${profileId.trim().ifBlank { "default" }}",
        Context.MODE_PRIVATE
    )

    fun load(): ForticladInventory = ForticladInventory(
        forticladCores = preferences.getLong(KEY_FORTICLAD, 0L).coerceAtLeast(0L),
        advancedCores = preferences.getLong(KEY_ADVANCED, 0L).coerceAtLeast(0L)
    )

    fun save(inventory: ForticladInventory) {
        preferences.edit()
            .putLong(KEY_FORTICLAD, inventory.forticladCores.coerceAtLeast(0L))
            .putLong(KEY_ADVANCED, inventory.advancedCores.coerceAtLeast(0L))
            .apply()
    }

    companion object {
        private const val KEY_FORTICLAD = "forticlad_cores"
        private const val KEY_ADVANCED = "advanced_cores"
    }
}
