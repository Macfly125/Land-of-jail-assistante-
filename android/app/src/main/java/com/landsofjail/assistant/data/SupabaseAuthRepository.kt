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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import com.landsofjail.assistant.model.AlliancePlayerProfile
import com.landsofjail.assistant.model.AllianceRank
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

    data class AllianceJoinRequest(
        val id: String,
        val playerName: String,
        val requestedRank: String,
        val createdAt: String
    )

    data class AllianceMessage(
        val id: String,
        val authorUserId: String,
        val authorName: String,
        val body: String,
        val createdAt: String
    )

    data class AllianceProfileConfirmation(
        val verifierUserId: String,
        val verifierName: String,
        val verifiedAt: String
    )

    data class AllianceProfileVerification(
        val memberUserId: String,
        val profileUpdatedAt: String?,
        val confirmationCount: Int,
        val verified: Boolean,
        val confirmations: List<AllianceProfileConfirmation>
    )

    data class AllianceProfileVerificationResult(
        val confirmationCount: Int,
        val verified: Boolean,
        val alreadyConfirmed: Boolean
    )

    data class ImprovementRequest(
        val id: String,
        val userId: String,
        val title: String,
        val description: String,
        val category: String,
        val status: String,
        val developerReply: String,
        val aiSummary: String,
        val aiCategory: String,
        val aiQuestions: List<String>,
        val createdAt: String
    )

    suspend fun signUp(email: String, password: String): Result<AuthOutcome> =
        authenticate("/auth/v1/signup", email, password, "Inscription envoyée. Vérifie ta boîte mail si la confirmation est activée.")

    suspend fun signIn(email: String, password: String): Result<AuthOutcome> =
        authenticate("/auth/v1/token?grant_type=password", email, password, "Connexion réussie.")

    suspend fun resendSignupConfirmation(email: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedEmail = email.trim()
            require(normalizedEmail.contains("@") && normalizedEmail.length <= 254) { "Adresse e-mail invalide." }
            val payload = buildJsonObject {
                put("type", "signup")
                put("email", normalizedEmail)
            }.toString()
            val request = Request.Builder()
                .url("$BASE_URL/auth/v1/resend")
                .header("apikey", PUBLISHABLE_KEY)
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                    error(
                        obj?.get("msg")?.jsonPrimitive?.contentOrNull
                            ?: obj?.get("message")?.jsonPrimitive?.contentOrNull
                            ?: "Impossible de renvoyer le mail (HTTP ${response.code})."
                    )
                }
                "Demande envoyée. Vérifie aussi les courriers indésirables et attends quelques minutes."
            }
        }
    }

    private suspend fun authenticate(
        path: String,
        email: String,
        password: String,
        successMessage: String,
        emailRedirectTo: String? = null
    ): Result<AuthOutcome> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedEmail = email.trim()
            require(normalizedEmail.contains("@") && normalizedEmail.length <= 254) { "Adresse e-mail invalide." }
            require(password.length >= 8) { "Le mot de passe doit contenir au moins 8 caractères." }
            val payload = buildJsonObject {
                put("email", normalizedEmail)
                put("password", password)
                emailRedirectTo?.let { redirect ->
                    put("options", buildJsonObject { put("emailRedirectTo", redirect) })
                }
            }.toString()
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
                val session = obj["access_token"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
                    ?.let { access ->
                    val refresh = obj["refresh_token"]?.jsonPrimitive?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                    if (refresh == null) null else Session(
                        access_token = access,
                        refresh_token = refresh,
                        token_type = obj["token_type"]?.jsonPrimitive?.contentOrNull ?: "bearer",
                        expires_in = obj["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L,
                        email = obj["user"]?.jsonObject?.get("email")?.jsonPrimitive?.contentOrNull ?: normalizedEmail,
                        user_id = obj["user"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull.orEmpty()
                    )
                }
                session?.let(::saveSession)
                AuthOutcome(
                    session = session,
                    message = if (session == null) {
                        if (path.endsWith("/signup")) {
                            "Compte créé. Une confirmation par e-mail est peut-être nécessaire avant la connexion."
                        } else {
                            "La connexion n'a pas fourni de session valide. Réessaie."
                        }
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
                    ?.takeIf { it.isNotBlank() }
                    ?: run {
                        clearSession()
                        error("Jeton d'accès absent ou vide de la réponse.")
                    }
                val refresh = obj["refresh_token"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
                    ?: run {
                        clearSession()
                        error("Jeton de renouvellement absent ou vide de la réponse.")
                    }
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



    /** Loads current-revision verification counts for every alliance member. */
    suspend fun fetchAllianceProfileVerifications(
        allianceId: String
    ): Result<List<AllianceProfileVerification>> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedAllianceId = allianceId.trim()
            require(isUuid(normalizedAllianceId)) { "Identifiant d'alliance invalide." }
            val result = callAllianceRpcJson("fetch_alliance_profile_verifications", buildJsonObject {
                put("p_alliance_id", normalizedAllianceId)
            }).getOrThrow()
            result.jsonArray.map { element ->
                val item = element.jsonObject
                val memberId = item["member_user_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                require(isUuid(memberId)) { "Le serveur a retourné un membre invalide." }
                val confirmations = item["confirmations"]?.jsonArray.orEmpty().map { confirmation ->
                    val value = confirmation.jsonObject
                    val verifierId = value["verifier_user_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val verifierName = value["verifier_name"]?.jsonPrimitive?.contentOrNull
                        ?.trim()?.takeIf { it.isNotEmpty() } ?: "Membre"
                    val verifiedAt = value["verified_at"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    require(isUuid(verifierId) && verifierId != memberId) {
                        "Le serveur a retourné un vérificateur invalide."
                    }
                    require(verifiedAt.isNotBlank()) { "Date de vérification absente." }
                    AllianceProfileConfirmation(verifierId, verifierName, verifiedAt)
                }
                val count = item["verification_count"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    ?: error("Nombre de confirmations invalide.")
                val verified = item["verified"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                    ?: error("Statut de vérification invalide.")
                require(count in 0..5 && count == confirmations.size) {
                    "Le nombre de confirmations retourné est incohérent."
                }
                require(confirmations.map { it.verifierUserId }.distinct().size == confirmations.size) {
                    "Le serveur a retourné des confirmations en double."
                }
                require(verified == (count >= 2)) { "Le statut de vérification est incohérent." }
                AllianceProfileVerification(
                    memberUserId = memberId,
                    profileUpdatedAt = item["profile_updated_at"]?.jsonPrimitive?.contentOrNull,
                    confirmationCount = count,
                    verified = verified,
                    confirmations = confirmations
                )
            }
        }
    }

    /** Adds one distinct member's confirmation for the latest synchronized profile revision. */
    suspend fun verifyAllianceMemberProfile(
        allianceId: String,
        memberUserId: String
    ): Result<AllianceProfileVerificationResult> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedAllianceId = allianceId.trim()
            val normalizedMemberId = memberUserId.trim()
            require(isUuid(normalizedAllianceId)) { "Identifiant d'alliance invalide." }
            require(isUuid(normalizedMemberId)) { "Identifiant de membre invalide." }
            val result = callAllianceRpcJson("verify_alliance_member_profile", buildJsonObject {
                put("p_alliance_id", normalizedAllianceId)
                put("p_member_user_id", normalizedMemberId)
            }).getOrThrow().jsonObject
            val count = result["count"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                ?: error("Nombre de confirmations invalide.")
            val verified = result["verified"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                ?: error("Statut de vérification invalide.")
            val alreadyConfirmed = result["already_confirmed"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                ?: error("Résultat de confirmation invalide.")
            require(count in 0..5 && verified == (count >= 2)) {
                "Le résultat de vérification retourné est incohérent."
            }
            AllianceProfileVerificationResult(count, verified, alreadyConfirmed)
        }
    }

    /** Creates the alliance and assigns its creator as R5 atomically on the server. */
    suspend fun createAlliance(serverNumber: String, allianceName: String): Result<String> = runCatching {
        val normalizedServer = serverNumber.trim()
        val normalizedTag = allianceName.trim()
        require(normalizedServer.matches(Regex("^[0-9]{1,6}$"))) {
            "Le numéro de serveur doit contenir 1 à 6 chiffres."
        }
        require(normalizedTag.length in 1..12) {
            "Le tag d'alliance doit contenir de 1 à 12 caractères."
        }
        callAllianceRpc("create_alliance_as_r5", buildJsonObject {
            put("p_server_number", normalizedServer)
            put("p_alliance_tag", normalizedTag)
        }).getOrThrow()
    }

    /**
     * Loads alliance membership first, then player profiles. Profile visibility is
     * intentionally enforced by Supabase RLS (R5: all, R4: R4/R3/R2/R1, others: self).
     */
    suspend fun fetchAllianceRoster(allianceId: String): Result<List<AlliancePlayerProfile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(allianceId.matches(Regex("[0-9a-fA-F-]{36}"))) {
                    "Identifiant d'alliance invalide."
                }
                val active = refreshSession().getOrThrow()
                val membersRequest = Request.Builder()
                    .url("$BASE_URL/rest/v1/alliance_members?select=user_id,game_rank,joined_at&alliance_id=eq.$allianceId&order=joined_at.asc")
                    .header("apikey", PUBLISHABLE_KEY)
                    .header("Authorization", "Bearer ${active.access_token}")
                    .get()
                    .build()
                val memberRows = client.newCall(membersRequest).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error(extractApiError(body, response.code))
                    json.parseToJsonElement(body).jsonArray
                }
                val memberships = memberRows.mapNotNull { row ->
                    val obj = row.jsonObject
                    val userId = obj["user_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val gameRank = obj["game_rank"]?.jsonPrimitive?.contentOrNull ?: "R1"
                    userId to gameRank
                }.distinctBy { it.first }
                if (memberships.isEmpty()) return@runCatching emptyList()

                val userIds = memberships.map { it.first }.filter {
                    it.matches(Regex("[0-9a-fA-F-]{36}"))
                }
                val profilesById = if (userIds.isEmpty()) {
                    emptyMap()
                } else {
                    val inFilter = userIds.joinToString(",")
                    val profilesRequest = Request.Builder()
                        .url("$BASE_URL/rest/v1/player_profiles?select=user_id,player_name,power_text,hero_data,robot_data,updated_at&user_id=in.($inFilter)")
                        .header("apikey", PUBLISHABLE_KEY)
                        .header("Authorization", "Bearer ${active.access_token}")
                        .get()
                        .build()
                    client.newCall(profilesRequest).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) error(extractApiError(body, response.code))
                        json.parseToJsonElement(body).jsonArray.associateBy { row ->
                            row.jsonObject["user_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        }
                    }
                }

                memberships.filterNot { it.first == active.user_id }.map { (userId, rawRank) ->
                    val profile = profilesById[userId]?.jsonObject
                    val rank = runCatching { AllianceRank.valueOf(rawRank) }.getOrDefault(AllianceRank.R1)
                    val heroSummary = profile?.get("hero_data")?.let { element ->
                        runCatching {
                            element.jsonArray.joinToString("\n") { heroElement ->
                                val hero = heroElement.jsonObject
                                val name = hero["nom"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                val skills = hero["competences"]?.let { skillsElement ->
                                    runCatching {
                                        skillsElement.jsonArray.joinToString("; ") { skillElement ->
                                            val skill = skillElement.jsonObject
                                            listOf(
                                                skill["nom"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                                                skill["niveau"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                                                skill["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                            ).filter { it.isNotBlank() }.joinToString(" ")
                                        }
                                    }.getOrDefault("")
                                }.orEmpty()
                                if (skills.isBlank()) name else "$name: $skills"
                            }.take(6000)
                        }.getOrDefault("")
                    }.orEmpty()
                    val robotSummary = profile?.get("robot_data")?.let { element ->
                        runCatching {
                            element.jsonObject["summary"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        }.getOrDefault("")
                    }.orEmpty()
                    AlliancePlayerProfile(
                        id = userId,
                        playerName = profile?.get("player_name")?.jsonPrimitive?.contentOrNull
                            ?.takeIf { it.isNotBlank() } ?: "Membre ${userId.take(8)}",
                        rank = rank,
                        powerText = profile?.get("power_text")?.jsonPrimitive?.contentOrNull.orEmpty(),
                        heroSkillSummary = heroSummary,
                        robotSummary = robotSummary,
                        updatedAtIso = profile?.get("updated_at")?.jsonPrimitive?.contentOrNull.orEmpty()
                    )
                }
            }
        }

    suspend fun createAllianceInvite(allianceId: String, maxUses: Int = 1): Result<String> = runCatching {
        val normalizedAllianceId = allianceId.trim()
        require(isUuid(normalizedAllianceId)) { "Identifiant d'alliance invalide." }
        require(maxUses in 1..105) { "Le nombre d'utilisations doit être compris entre 1 et 105." }
        callAllianceRpc("create_alliance_invite", buildJsonObject {
            put("p_alliance_id", normalizedAllianceId)
            put("p_max_uses", maxUses)
        }).getOrThrow()
    }

    suspend fun acceptAllianceInvite(token: String): Result<String> = runCatching {
        val normalizedToken = token.trim()
        require(normalizedToken.isNotEmpty()) { "Le code d'invitation est obligatoire." }
        callAllianceRpc("accept_alliance_invite", buildJsonObject {
            put("p_token", normalizedToken)
        }).getOrThrow()
    }

    /** A player can request membership, but cannot assign their own effective alliance rank. */
    suspend fun submitAllianceJoinRequest(
        serverNumber: String,
        allianceTag: String,
        playerName: String,
        requestedRank: String
    ): Result<String> = runCatching {
        val normalizedServer = serverNumber.trim()
        val normalizedTag = allianceTag.trim()
        val normalizedPlayerName = playerName.trim()
        val normalizedRank = requestedRank.trim().uppercase()
        require(normalizedServer.matches(Regex("^[0-9]{1,6}$"))) {
            "Le numéro de serveur doit contenir 1 à 6 chiffres."
        }
        require(normalizedTag.length in 1..12) {
            "Le tag d'alliance doit contenir de 1 à 12 caractères."
        }
        require(normalizedPlayerName.length in 1..40) {
            "Le nom du joueur doit contenir de 1 à 40 caractères."
        }
        require(normalizedRank in setOf("R1", "R2", "R3", "R4", "R5")) {
            "Rang demandé invalide."
        }
        callAllianceRpc("submit_alliance_join_request", buildJsonObject {
            put("p_server_number", normalizedServer)
            put("p_alliance_tag", normalizedTag)
            put("p_player_name", normalizedPlayerName)
            put("p_requested_rank", normalizedRank)
        }).getOrThrow()
    }

    suspend fun fetchPendingAllianceJoinRequests(allianceId: String): Result<List<AllianceJoinRequest>> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(allianceId.matches(Regex("[0-9a-fA-F-]{36}"))) { "Identifiant d'alliance invalide." }
                val active = refreshSession().getOrThrow()
                val request = Request.Builder()
                    .url("$BASE_URL/rest/v1/alliance_join_requests?select=id,player_name,requested_rank,created_at&alliance_id=eq.$allianceId&status=eq.pending&order=created_at.asc")
                    .header("apikey", PUBLISHABLE_KEY)
                    .header("Authorization", "Bearer ${active.access_token}")
                    .get().build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error(extractApiError(body, response.code))
                    json.parseToJsonElement(body).jsonArray.map { row ->
                        val obj = row.jsonObject
                        AllianceJoinRequest(
                            id = obj["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            playerName = obj["player_name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            requestedRank = obj["requested_rank"]?.jsonPrimitive?.contentOrNull
                                ?.uppercase()
                                ?.takeIf { it in setOf("R1", "R2", "R3", "R4", "R5") }
                                ?: "R1",
                            createdAt = obj["created_at"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        )
                    }
                }
            }
        }

    suspend fun reviewAllianceJoinRequest(
        requestId: String,
        approve: Boolean,
        approvedRank: String = "R1"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedRequestId = requestId.trim()
            require(normalizedRequestId.matches(
                Regex("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
            )) { "Identifiant de demande invalide." }
            val normalizedRank = approvedRank.trim().uppercase()
            require(normalizedRank in setOf("R1", "R2", "R3", "R4", "R5")) {
                "Rang attribué invalide."
            }
            callAllianceRpc("review_alliance_join_request", buildJsonObject {
                put("p_request_id", normalizedRequestId)
                put("p_approve", approve)
                put("p_approved_rank", normalizedRank)
            }).getOrThrow()
            Unit
        }
    }

    suspend fun fetchMyAllianceRank(allianceId: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            require(allianceId.matches(Regex("[0-9a-fA-F-]{36}"))) { "Identifiant d'alliance invalide." }
            val active = refreshSession().getOrThrow()
            val request = Request.Builder()
                .url("$BASE_URL/rest/v1/alliance_members?select=game_rank&alliance_id=eq.$allianceId&user_id=eq.${active.user_id}&limit=1")
                .header("apikey", PUBLISHABLE_KEY)
                .header("Authorization", "Bearer ${active.access_token}")
                .get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error(extractApiError(body, response.code))
                json.parseToJsonElement(body).jsonArray.firstOrNull()
                    ?.jsonObject?.get("game_rank")?.jsonPrimitive?.contentOrNull
            }
        }
    }

    suspend fun fetchAllianceMessages(allianceId: String): Result<List<AllianceMessage>> = withContext(Dispatchers.IO) {
        runCatching {
            require(allianceId.matches(Regex("[0-9a-fA-F-]{36}"))) { "Identifiant d'alliance invalide." }
            val active = refreshSession().getOrThrow()
            val request = Request.Builder()
                .url("$BASE_URL/rest/v1/alliance_messages?select=id,author_user_id,author_name,body,created_at&alliance_id=eq.$allianceId&order=created_at.desc&limit=100")
                .header("apikey", PUBLISHABLE_KEY)
                .header("Authorization", "Bearer ${active.access_token}")
                .get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error(extractApiError(body, response.code))
                json.parseToJsonElement(body).jsonArray.map { row ->
                    val obj = row.jsonObject
                    AllianceMessage(
                        id = obj["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        authorUserId = obj["author_user_id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        authorName = obj["author_name"]?.jsonPrimitive?.contentOrNull ?: "Membre",
                        body = obj["body"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        createdAt = obj["created_at"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    )
                }
            }
        }
    }

    suspend fun postAllianceMessage(allianceId: String, body: String): Result<Unit> = runCatching {
        val normalizedAllianceId = allianceId.trim()
        require(isUuid(normalizedAllianceId)) { "Identifiant d'alliance invalide." }
        val normalizedBody = body.trim()
        require(normalizedBody.isNotEmpty() && normalizedBody.length <= 2000) {
            "Le message doit contenir entre 1 et 2000 caractères."
        }
        callAllianceRpc("post_alliance_message", buildJsonObject {
            put("p_alliance_id", normalizedAllianceId)
            put("p_body", normalizedBody)
        }).getOrThrow()
        Unit
    }

    suspend fun deleteAllianceMessage(messageId: String): Result<Unit> = runCatching {
        val normalizedMessageId = messageId.trim()
        require(isUuid(normalizedMessageId)) { "Identifiant de message invalide." }
        callAllianceRpc("delete_alliance_message", buildJsonObject {
            put("p_message_id", normalizedMessageId)
        }).getOrThrow()
        Unit
    }

    suspend fun transferAllianceR5(allianceId: String, newR5UserId: String): Result<Unit> = runCatching {
        val normalizedAllianceId = allianceId.trim()
        val normalizedUserId = newR5UserId.trim()
        require(isUuid(normalizedAllianceId)) { "Identifiant d'alliance invalide." }
        require(isUuid(normalizedUserId)) { "Identifiant du nouveau R5 invalide." }
        callAllianceRpc("transfer_alliance_r5", buildJsonObject {
            put("p_alliance_id", normalizedAllianceId)
            put("p_new_r5_user_id", normalizedUserId)
        }).getOrThrow()
        Unit
    }

    suspend fun setAllianceMemberRank(
        allianceId: String,
        targetUserId: String,
        newRank: String
    ): Result<Unit> = runCatching {
        val normalizedAllianceId = allianceId.trim()
        val normalizedUserId = targetUserId.trim()
        val normalizedRank = newRank.trim().uppercase()
        require(isUuid(normalizedAllianceId)) { "Identifiant d'alliance invalide." }
        require(isUuid(normalizedUserId)) { "Identifiant du membre invalide." }
        require(normalizedRank in setOf("R1", "R2", "R3", "R4", "R5")) { "Rang attribué invalide." }
        callAllianceRpc("set_alliance_member_rank", buildJsonObject {
            put("p_alliance_id", normalizedAllianceId)
            put("p_target_user_id", normalizedUserId)
            put("p_new_rank", normalizedRank)
        }).getOrThrow()
        Unit
    }

    suspend fun isAppDeveloper(): Result<Boolean> = callAllianceRpc("is_app_developer", buildJsonObject {}).map { it == "true" }

    suspend fun submitAppImprovement(
        title: String,
        description: String,
        category: String,
        aiSummary: String,
        aiCategory: String,
        aiQuestions: List<String>
    ): Result<String> = callAllianceRpc("submit_app_improvement", buildJsonObject {
        put("p_title", title.trim())
        put("p_description", description.trim())
        put("p_category", category)
        put("p_ai_summary", aiSummary.take(1200))
        put("p_ai_category", aiCategory)
        put("p_ai_questions", kotlinx.serialization.json.JsonArray(aiQuestions.take(5).map { kotlinx.serialization.json.JsonPrimitive(it.take(240)) })
        )
    })

    suspend fun fetchAppImprovementRequests(): Result<List<ImprovementRequest>> = withContext(Dispatchers.IO) {
        runCatching {
            val active = refreshSession().getOrThrow()
            val request = Request.Builder()
                .url("$BASE_URL/rest/v1/app_improvement_requests?select=id,user_id,title,description,category,status,developer_reply,ai_summary,ai_category,ai_questions,created_at&order=created_at.desc&limit=100")
                .header("apikey", PUBLISHABLE_KEY)
                .header("Authorization", "Bearer ${active.access_token}")
                .get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error(extractApiError(body, response.code))
                json.parseToJsonElement(body).jsonArray.map { row ->
                    val obj = row.jsonObject
                    ImprovementRequest(
                        id = obj["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        userId = obj["user_id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        description = obj["description"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        category = obj["category"]?.jsonPrimitive?.contentOrNull ?: "other",
                        status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "pending",
                        developerReply = obj["developer_reply"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        aiSummary = obj["ai_summary"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        aiCategory = obj["ai_category"]?.jsonPrimitive?.contentOrNull ?: "other",
                        aiQuestions = obj["ai_questions"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
                        createdAt = obj["created_at"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    )
                }
            }
        }
    }

    suspend fun reviewAppImprovement(requestId: String, status: String, reply: String): Result<Unit> =
        callAllianceRpc("review_app_improvement", buildJsonObject {
            put("p_request_id", requestId)
            put("p_status", status)
            put("p_developer_reply", reply.take(3000))
        }).map { Unit }

    private suspend fun callAllianceRpc(functionName: String, payload: kotlinx.serialization.json.JsonObject): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(functionName in setOf("is_app_developer", "submit_app_improvement", "review_app_improvement", "create_alliance_as_r5", "transfer_alliance_r5", "create_alliance_invite", "accept_alliance_invite", "submit_alliance_join_request", "review_alliance_join_request", "post_alliance_message", "delete_alliance_message", "set_alliance_member_rank")) {
                    "Action d'alliance non autorisée."
                }
                require(payload.toString().length <= 4096) { "Données d'action d'alliance trop longues." }
                val active = refreshSession().getOrThrow()
                val request = Request.Builder()
                    .url("$BASE_URL/rest/v1/rpc/$functionName")
                    .header("apikey", PUBLISHABLE_KEY)
                    .header("Authorization", "Bearer ${active.access_token}")
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error(extractApiError(body, response.code))
                    runCatching { json.parseToJsonElement(body).jsonPrimitive.contentOrNull }
                        .getOrNull()?.takeIf { it.isNotBlank() }
                        ?: error("Le serveur n'a pas retourné le résultat attendu.")
                }
            }
        }

    private suspend fun callAllianceRpcJson(
        functionName: String,
        payload: kotlinx.serialization.json.JsonObject
    ): Result<kotlinx.serialization.json.JsonElement> = withContext(Dispatchers.IO) {
        runCatching {
            require(functionName in setOf("fetch_alliance_profile_verifications", "verify_alliance_member_profile")) {
                "Action de vérification non autorisée."
            }
            require(payload.toString().length <= 4096) { "Données de vérification trop longues." }
            val active = refreshSession().getOrThrow()
            val request = Request.Builder()
                .url("$BASE_URL/rest/v1/rpc/$functionName")
                .header("apikey", PUBLISHABLE_KEY)
                .header("Authorization", "Bearer ${active.access_token}")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error(extractApiError(body, response.code))
                json.parseToJsonElement(body)
            }
        }
    }

    private fun isUuid(value: String): Boolean =
        value.matches(Regex("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))

    private fun extractApiError(body: String, code: Int): String {
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        return obj?.get("message")?.jsonPrimitive?.contentOrNull
            ?: obj?.get("msg")?.jsonPrimitive?.contentOrNull
            ?: obj?.get("details")?.jsonPrimitive?.contentOrNull
            ?: "Erreur serveur (HTTP $code)."
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
                .header("Authorization", "Bearer ${active.access_token}")
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
                        ?: "Synchronisation refusée (HTTP ${response.code})."
                    error(message)
                }
            }
        }
    }


    /** A cloud save belongs to one game profile, not to the whole login account. */
    data class GameAccountProfileCloud(
        val profileId: String,
        val profileName: String,
        val stateJson: String,
        val updatedAt: String
    )

    /**
     * Reads only the signed-in user's game profiles. The legacy player_profiles row
     * is deliberately not touched by these methods.
     */
    suspend fun fetchGameAccountProfiles(): Result<List<GameAccountProfileCloud>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val active = refreshSession().getOrThrow()
                require(active.user_id.isNotBlank()) { "Identifiant du compte absent." }
                val request = Request.Builder()
                    .url("$BASE_URL/rest/v1/game_account_profiles?select=profile_id,profile_name,state,updated_at&user_id=eq.${active.user_id}&order=updated_at.desc")
                    .header("apikey", PUBLISHABLE_KEY)
                    .header("Authorization", "Bearer ${active.access_token}")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error(extractApiError(body, response.code))
                    json.parseToJsonElement(body).jsonArray.map { row ->
                        val obj = row.jsonObject
                        GameAccountProfileCloud(
                            profileId = obj["profile_id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            profileName = obj["profile_name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            stateJson = obj["state"]?.toString() ?: "{}",
                            updatedAt = obj["updated_at"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        )
                    }
                }
            }
        }

    /**
     * Saves one stable profile ID. Callers must first persist a local backup;
     * no remote profile is deleted and no legacy player_profiles row is changed.
     */
    suspend fun syncGameAccountProfile(
        profileId: String,
        profileName: String,
        stateJson: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(profileId.matches(Regex("[A-Za-z0-9_-]{8,80}"))) { "Identifiant de profil invalide." }
            require(profileName.trim().length in 1..32) { "Le nom du profil doit contenir de 1 à 32 caractères." }
            require(stateJson.toByteArray(Charsets.UTF_8).size <= 750_000) { "Sauvegarde trop volumineuse pour une synchronisation fiable." }
            val state = json.parseToJsonElement(stateJson)
            require(state is kotlinx.serialization.json.JsonObject) { "Format de sauvegarde invalide." }
            val active = refreshSession().getOrThrow()
            require(active.user_id.isNotBlank()) { "Identifiant du compte absent. Reconnecte-toi." }
            val payload = buildJsonObject {
                put("user_id", active.user_id)
                put("profile_id", profileId)
                put("profile_name", profileName.trim())
                put("state", state)
                put("updated_at", Instant.now().toString())
            }
            val request = Request.Builder()
                .url("$BASE_URL/rest/v1/game_account_profiles?on_conflict=user_id,profile_id")
                .header("apikey", PUBLISHABLE_KEY)
                .header("Authorization", "Bearer ${active.access_token}")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error(extractApiError(body, response.code))
            }
        }
    }

    fun currentSession(): Session? = readSession()

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = readSession()
            try {
                if (session != null) {
                    val request = Request.Builder()
                        .url("$BASE_URL/auth/v1/logout")
                        .header("apikey", PUBLISHABLE_KEY)
                        .header("Authorization", "Bearer ${session.access_token}")
                        .post("{}".toRequestBody("application/json".toMediaType()))
                        .build()
                    // Remote revocation is best-effort: a network outage must not trap
                    // the user in this account or prevent switching to another account.
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful && response.code != 401) {
                            android.util.Log.w(
                                "SupabaseAuth",
                                "Remote sign-out returned HTTP ${response.code}; clearing local session anyway."
                            )
                        }
                    }
                }
            } catch (error: Exception) {
                android.util.Log.w(
                    "SupabaseAuth",
                    "Remote sign-out unavailable; clearing local session anyway.",
                    error
                )
            } finally {
                // signOut runs on Dispatchers.IO: wait for the local deletion to
                // reach disk so an immediate process kill cannot restore this account.
                if (!prefs.edit().clear().commit()) {
                    error("Impossible d'effacer durablement la session locale.")
                }
            }
            Unit
        }
    }

    private fun saveSession(session: Session) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(json.encodeToString(session).toByteArray(Charsets.UTF_8))
        val persisted = prefs.edit()
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("payload", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .commit()
        check(persisted) { "Impossible d'enregistrer durablement la session locale." }
    }

    private fun readSession(): Session? {
        val iv = prefs.getString("iv", null)
        val payload = prefs.getString("payload", null)
        if (iv == null && payload == null) return null
        if (iv.isNullOrBlank() || payload.isNullOrBlank()) {
            clearSession()
            return null
        }

        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
            json.decodeFromString<Session>(
                String(cipher.doFinal(Base64.decode(payload, Base64.NO_WRAP)), Charsets.UTF_8)
            ).also { session ->
                require(session.access_token.isNotBlank() && session.refresh_token.isNotBlank()) {
                    "Session locale incomplète."
                }
            }
        }.getOrElse {
            // A corrupted payload or an invalidated Android Keystore key must not
            // leave an unusable session behind or block the next sign-in attempt.
            clearSession()
            null
        }
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            val isUsable = runCatching {
                Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.ENCRYPT_MODE, existingKey)
                }
            }.isSuccess
            if (isUsable) return existingKey

            // Android may retain an alias whose key was invalidated (for example
            // after a lock-screen/security change). Remove it before creating a
            // replacement, otherwise every subsequent sign-in would fail to save.
            keyStore.deleteEntry(KEY_ALIAS)
        }
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
        // Public website used as the post-confirmation destination for account emails.
    }
}
