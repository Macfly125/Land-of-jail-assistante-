package com.landsofjail.assistant.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import com.landsofjail.assistant.model.AlliancePlayerProfile
import com.landsofjail.assistant.model.HeroRecord
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.concurrent.TimeUnit

/**
 * Supabase email/password authentication.
 * The publishable key is intentionally public; never put a service_role key in the Android app.
 */
class SupabaseAuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("loj_secure_auth_v1", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class Session(
        val access_token: String,
        val refresh_token: String,
        val token_type: String = "bearer",
        val expires_in: Long = 3600,
        val email: String = "",
        val user_id: String = ""
    )

    data class AuthOutcome(
        val session: Session?,
        val message: String
    )

    suspend fun signUp(email: String, password: String): Result<AuthOutcome> =
        authenticate("/auth/v1/signup", email, password, "Inscription envoyée. Vérifie ta boîte mail si la confirmation est activée.")

    suspend fun signIn(email: String, password: String): Result<AuthOutcome> =
        authenticate("/auth/v1/token?grant_type=password", email, password, "Connexion réussie.")

    private suspend fun authenticate(
        path: String,
        email: String,
        password: String,
        successMessage: String
    ): Result<AuthOutcome> = withContext(Dispatchers.IO) {
        runCatching {
            require(email.contains("@") && email.length <= 254) { "Adresse e-mail invalide." }
            require(password.length >= 8) { "Le mot de passe doit contenir au moins 8 caractères." }
            val payload = """{"email":${Json.encodeToString(email)},"password":${Json.encodeToString(password)}}"""
            val request = Request.Builder()
                .url("$BASE_URL$path")
                .header("apikey", PUBLISHABLE_KEY)
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                if (!response.isSuccessful) {
                    val obj = parsed
                    val message = obj?.get("msg")?.jsonPrimitive?.contentOrNull
                        ?: obj?.get("message")?.jsonPrimitive?.contentOrNull
                        ?: obj?.get("error_description")?.jsonPrimitive?.contentOrNull
                        ?: "Erreur d'authentification (HTTP ${response.code})."
                    error(message)
                }
                val obj = parsed ?: error("Réponse d'authentification illisible.")
                val session = obj["access_token"]?.jsonPrimitive?.contentOrNull?.let { access ->
                    val refresh = obj["refresh_token"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (refresh.isBlank()) null else Session(
                        access_token = access,
                        refresh_token = refresh,
                        token_type = obj["token_type"]?.jsonPrimitive?.contentOrNull ?: "bearer",
                        expires_in = obj["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L,
                        email = obj["user"]?.jsonObject?.get("email")?.jsonPrimitive?.contentOrNull ?: email,
                        user_id = obj["user"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull.orEmpty()
                    )
                }
                session?.let(::saveSession)
                AuthOutcome(
                    session = session,
                    message = if (session == null && path.endsWith("/signup")) {
                        "Compte créé. Une confirmation par e-mail est peut-être nécessaire avant la connexion."
                    } else successMessage
                )
            }
        }
    }

    suspend fun refreshSession(): Result<Session> = withContext(Dispatchers.IO) {
        runCatching {
            val current = readSession() ?: error("Aucune session enregistrée.")
            val payload = """{"refresh_token":${Json.encodeToString(current.refresh_token)}}"""
            val request = Request.Builder()
                .url("$BASE_URL/auth/v1/token?grant_type=refresh_token")
                .header("apikey", PUBLISHABLE_KEY)
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                if (!response.isSuccessful) {
                    if (response.code == 400 || response.code == 401) clearSession()
                    error(
                        parsed?.get("msg")?.jsonPrimitive?.contentOrNull
                            ?: parsed?.get("message")?.jsonPrimitive?.contentOrNull
                            ?: "Impossible d'actualiser la session (HTTP ${response.code})."
                    )
                }
                val obj = parsed ?: error("Réponse de renouvellement illisible.")
                val access = obj["access_token"]?.jsonPrimitive?.contentOrNull
                    ?: error("Jeton d'accès absent de la réponse.")
                val refresh = obj["refresh_token"]?.jsonPrimitive?.contentOrNull
                    ?: error("Jeton de renouvellement absent de la réponse.")
                Session(
                    access_token = access,
                    refresh_token = refresh,
                    token_type = obj["token_type"]?.jsonPrimitive?.contentOrNull ?: "bearer",
                    expires_in = obj["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L,
                    email = obj["user"]?.jsonObject?.get("email")?.jsonPrimitive?.contentOrNull ?: current.email,
                    user_id = obj["user"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull ?: current.user_id
                ).also(::saveSession)
            }
        }
    }


    /**
     * Uploads only the signed-in player's own profile. RLS remains the authority:
     * user_id must match auth.uid(); the locally selected game rank is never uploaded.
     */
    suspend fun syncPlayerProfile(
        profile: AlliancePlayerProfile,
        heroes: List<HeroRecord>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(profile.playerName.isNotBlank()) { "Renseigne ton pseudo avant la synchronisation." }
            val active = refreshSession().getOrThrow()
            require(active.user_id.isNotBlank()) { "Identifiant du compte absent. Reconnecte-toi." }
            val payload = buildJsonObject {
                put("user_id", active.user_id)
                put("player_name", profile.playerName.trim())
                put("power_text", profile.powerText.trim())
                put("hero_data", json.parseToJsonElement(Json.encodeToString(heroes)))
                put("robot_data", buildJsonObject { put("summary", profile.robotSummary.trim()) })
                put("troop_data", buildJsonObject { put("status", "not_provided") })
                put("updated_at", Instant.now().toString())
            }
            val request = Request.Builder()
                .url("$BASE_URL/rest/v1/player_profiles?on_conflict=user_id")
                .header("apikey", PUBLISHABLE_KEY)
                .header("Authorization", "Bearer \${active.access_token}")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                    val message = parsed?.get("message")?.jsonPrimitive?.contentOrNull
                        ?: parsed?.get("details")?.jsonPrimitive?.contentOrNull
                        ?: parsed?.get("hint")?.jsonPrimitive?.contentOrNull
                        ?: "Synchronisation refusée (HTTP \${response.code})."
                    error(message)
                }
            }
        }
    }

    fun currentSession(): Session? = readSession()

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = readSession()
            if (session != null) {
                val request = Request.Builder()
                    .url("$BASE_URL/auth/v1/logout")
                    .header("apikey", PUBLISHABLE_KEY)
                    .header("Authorization", "Bearer ${session.access_token}")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != 401) {
                        error("Déconnexion distante impossible (HTTP ${response.code}).")
                    }
                }
            }
            clearSession()
        }
    }

    private fun saveSession(session: Session) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(json.encodeToString(session).toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("payload", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    private fun readSession(): Session? = runCatching {
        val iv = prefs.getString("iv", null) ?: return null
        val payload = prefs.getString("payload", null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
        )
        json.decodeFromString<Session>(
            String(cipher.doFinal(Base64.decode(payload, Base64.NO_WRAP)), Charsets.UTF_8)
        )
    }.getOrNull()

    private fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val BASE_URL = "https://cegtwxvidiiqgyfqzdix.supabase.co"
        private const val PUBLISHABLE_KEY = "sb_publishable_bf7lfL-PK1g5Ct9eWfwrxQ_av8OC4hH"
        private const val KEY_ALIAS = "loj_supabase_auth_aes_key_v1"
    }
}
