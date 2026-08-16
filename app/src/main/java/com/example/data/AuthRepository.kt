package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.GoogleSignInOutcome
import com.example.model.PairingResult
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Locale

class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ikimiz_prefs", Context.MODE_PRIVATE)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun getCurrentFirebaseUser(): FirebaseUser? {
        return try {
            auth.currentUser
        } catch (e: Exception) {
            null
        }
    }

    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    // Generates a clean, unambiguous 6-character pairing code (e.g. 7K4M9Q)
    fun generatePairingCode(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val random = SecureRandom()
        val sb = StringBuilder(6)
        for (i in 0 until 6) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    suspend fun signUp(
        email: String,
        pass: String,
        name: String,
        birthDate: String,
        avatarPreset: String,
        avatarBase64: String?
    ): Result<UserProfile> {
        return try {
            // 1. Firebase Authentication User Creation
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Kullanıcı kimliği alınamadı.")

            // 2. Generate unique pairing code
            val code = generatePairingCode()

            val profile = UserProfile(
                userId = uid,
                displayName = name.trim(),
                birthDate = birthDate.trim(),
                email = email.trim().lowercase(Locale.getDefault()),
                avatarPreset = avatarPreset,
                avatarBase64 = avatarBase64,
                pairingCode = code,
                partnerId = null,
                partnerName = null,
                isPaired = false,
                pairedAt = null,
                createdAt = System.currentTimeMillis(),
                lastActive = System.currentTimeMillis()
            )

            // 3. Write profile to Firestore users collection
            firestore.collection("users").document(uid).set(profile.toMap()).await()

            // 4. Register pairing code index for quick lookup
            firestore.collection("pairing_codes").document(code).set(
                mapOf("userId" to uid, "createdAt" to System.currentTimeMillis())
            ).await()

            // 5. Cache locally
            saveLocalProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Error in signUp", e)
            val trMessage = mapFirebaseError(e.message ?: "Kayıt olurken bir hata oluştu.")
            Result.failure(Exception(trMessage))
        }
    }

    suspend fun signIn(email: String, pass: String): Result<UserProfile> {
        return try {
            // 1. Authenticate with Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Kullanıcı kimliği alınamadı.")

            // 2. Fetch User Profile from Firestore
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists() && doc.data != null) {
                val profile = UserProfile.fromMap(doc.data!!)
                saveLocalProfile(profile)
                Result.success(profile)
            } else {
                // Profile document not found in Firestore, initialize and save
                val code = generatePairingCode()
                val profile = UserProfile(
                    userId = uid,
                    displayName = email.substringBefore("@"),
                    birthDate = "",
                    email = email.trim(),
                    pairingCode = code
                )
                firestore.collection("users").document(uid).set(profile.toMap()).await()
                firestore.collection("pairing_codes").document(code).set(
                    mapOf("userId" to uid, "createdAt" to System.currentTimeMillis())
                ).await()
                saveLocalProfile(profile)
                Result.success(profile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in signIn", e)
            val trMessage = mapFirebaseError(e.message ?: "Giriş yapılırken bir hata oluştu.")
            Result.failure(Exception(trMessage))
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<GoogleSignInOutcome> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val fbUser = authResult.user ?: throw Exception("Google kullanıcı kimliği alınamadı.")

            val uid = fbUser.uid
            val doc = firestore.collection("users").document(uid).get().await()

            if (doc.exists() && doc.data != null) {
                val profile = UserProfile.fromMap(doc.data!!)
                // If required fields exist, user is fully setup
                if (profile.displayName.isNotBlank() && profile.birthDate.isNotBlank()) {
                    saveLocalProfile(profile)
                    Result.success(GoogleSignInOutcome.Success(profile))
                } else {
                    Result.success(
                        GoogleSignInOutcome.NeedsProfileCompletion(
                            uid = uid,
                            email = fbUser.email ?: profile.email,
                            displayName = profile.displayName.ifBlank { fbUser.displayName ?: "" },
                            photoUrl = fbUser.photoUrl?.toString()
                        )
                    )
                }
            } else {
                // First-time Google user -> needs to complete profile details (birth date, avatar, etc.)
                Result.success(
                    GoogleSignInOutcome.NeedsProfileCompletion(
                        uid = uid,
                        email = fbUser.email ?: "",
                        displayName = fbUser.displayName ?: "",
                        photoUrl = fbUser.photoUrl?.toString()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in signInWithGoogleIdToken", e)
            val trMessage = mapFirebaseError(e.message ?: "Google ile giriş yapılırken bir hata oluştu.")
            Result.failure(Exception(trMessage))
        }
    }

    suspend fun completeGoogleProfile(
        uid: String,
        name: String,
        birthDate: String,
        email: String,
        avatarPreset: String,
        avatarBase64: String?
    ): Result<UserProfile> {
        return try {
            val currentAuthUser = auth.currentUser
            if (currentAuthUser == null || currentAuthUser.uid != uid) {
                throw Exception("Oturum doğrulaması başarısız oldu. Lütfen tekrar Google ile giriş yapın.")
            }

            val code = generatePairingCode()
            val profile = UserProfile(
                userId = uid,
                displayName = name.trim(),
                birthDate = birthDate.trim(),
                email = email.trim().lowercase(Locale.getDefault()),
                avatarPreset = avatarPreset,
                avatarBase64 = avatarBase64,
                pairingCode = code,
                partnerId = null,
                partnerName = null,
                isPaired = false,
                pairedAt = null,
                createdAt = System.currentTimeMillis(),
                lastActive = System.currentTimeMillis()
            )

            // Write profile document
            firestore.collection("users").document(uid).set(profile.toMap()).await()

            // Write pairing code index
            firestore.collection("pairing_codes").document(code).set(
                mapOf("userId" to uid, "createdAt" to System.currentTimeMillis())
            ).await()

            saveLocalProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Error in completeGoogleProfile", e)
            val trMessage = mapFirebaseError(e.message ?: "Profil kaydedilirken hata oluştu.")
            Result.failure(Exception(trMessage))
        }
    }

    fun observeCurrentUser(userId: String): Flow<UserProfile?> = callbackFlow {
        // Prevent unauthenticated query that would cause PERMISSION_DENIED
        val currentFbUser = auth.currentUser
        if (currentFbUser == null || userId.isEmpty() || currentFbUser.uid != userId) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val registration = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for user: $userId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                    val profile = UserProfile.fromMap(snapshot.data!!)
                    saveLocalProfile(profile)
                    trySend(profile)
                } else {
                    trySend(getLocalProfile())
                }
            }
        awaitClose { registration.remove() }
    }

    fun observePartner(partnerId: String): Flow<UserProfile?> = callbackFlow {
        // Prevent unauthenticated query
        if (auth.currentUser == null || partnerId.isEmpty()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val registration = firestore.collection("users").document(partnerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for partner: $partnerId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                    val partnerProfile = UserProfile.fromMap(snapshot.data!!)
                    saveLocalPartner(partnerProfile)
                    trySend(partnerProfile)
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun pairWithCode(currentUserId: String, targetCodeRaw: String): PairingResult {
        val targetCode = targetCodeRaw.trim().uppercase(Locale.getDefault())
        if (targetCode.length < 4) {
            return PairingResult.Error("Lütfen geçerli bir eşleşme kodu girin.")
        }

        val currentFbUser = auth.currentUser
        if (currentFbUser == null || currentFbUser.uid != currentUserId) {
            return PairingResult.Error("Oturum süresi dolmuş. Lütfen tekrar giriş yapın.")
        }

        try {
            val currentUser = getLocalProfile() ?: run {
                val doc = firestore.collection("users").document(currentUserId).get().await()
                if (doc.exists() && doc.data != null) UserProfile.fromMap(doc.data!!) else null
            } ?: return PairingResult.Error("Oturum bilgisi alınamadı. Lütfen tekrar giriş yapın.")

            if (currentUser.pairingCode.equals(targetCode, ignoreCase = true)) {
                return PairingResult.Error("Kendi eşleşme kodunuzu giremezsiniz. Sevgilinizin kodunu girmelisiniz.")
            }

            // Direct document get on pairing_codes collection
            val codeDoc = firestore.collection("pairing_codes").document(targetCode).get().await()
            if (!codeDoc.exists() || codeDoc.data == null) {
                return PairingResult.Error("Eşleşme kodu bulunamadı. Lütfen sevgilinizin kodunu doğru girdiğinizden emin olun.")
            }

            val partnerUid = codeDoc.getString("userId")
            if (partnerUid.isNullOrEmpty() || partnerUid == currentUserId) {
                return PairingResult.Error("Geçersiz veya kendinize ait eşleşme kodu.")
            }

            val partnerDoc = firestore.collection("users").document(partnerUid).get().await()
            if (!partnerDoc.exists() || partnerDoc.data == null) {
                return PairingResult.Error("Sevgili profili bulunamadı.")
            }

            val partnerProfile = UserProfile.fromMap(partnerDoc.data!!)

            if (partnerProfile.isPaired && partnerProfile.partnerId != currentUserId) {
                return PairingResult.Error("Bu kullanıcı zaten başka bir hesapla eşleşmiş.")
            }

            val now = System.currentTimeMillis()

            // Atomically update both user documents in Firestore
            val batch = firestore.batch()

            val userRef = firestore.collection("users").document(currentUserId)
            val userUpdates = mapOf(
                "isPaired" to true,
                "partnerId" to partnerUid,
                "partnerName" to partnerProfile.displayName,
                "pairedAt" to now
            )
            batch.set(userRef, userUpdates, SetOptions.merge())

            val partnerRef = firestore.collection("users").document(partnerUid)
            val partnerUpdates = mapOf(
                "isPaired" to true,
                "partnerId" to currentUserId,
                "partnerName" to currentUser.displayName,
                "pairedAt" to now
            )
            batch.set(partnerRef, partnerUpdates, SetOptions.merge())

            batch.commit().await()

            val updatedUser = currentUser.copy(
                isPaired = true,
                partnerId = partnerUid,
                partnerName = partnerProfile.displayName,
                pairedAt = now
            )
            saveLocalProfile(updatedUser)
            saveLocalPartner(partnerProfile)

            return PairingResult.Success(partnerProfile.displayName)
        } catch (e: Exception) {
            Log.e(TAG, "Error in pairWithCode", e)
            val trMessage = mapFirebaseError(e.message ?: "Eşleşme sağlanamadı.")
            return PairingResult.Error(trMessage)
        }
    }

    suspend fun unpair(currentUser: UserProfile): Result<Unit> {
        return try {
            val partnerId = currentUser.partnerId
            val newCode1 = generatePairingCode()

            val batch = firestore.batch()

            // Reset Current User in Firestore
            val userRef = firestore.collection("users").document(currentUser.userId)
            val userUpdates = mapOf(
                "isPaired" to false,
                "partnerId" to null,
                "partnerName" to null,
                "pairedAt" to null,
                "pairingCode" to newCode1
            )
            batch.set(userRef, userUpdates, SetOptions.merge())
            batch.set(
                firestore.collection("pairing_codes").document(newCode1),
                mapOf("userId" to currentUser.userId, "createdAt" to System.currentTimeMillis())
            )

            // Reset Partner's pairing status if exists
            if (!partnerId.isNullOrEmpty()) {
                val partnerRef = firestore.collection("users").document(partnerId)
                val partnerUpdates = mapOf(
                    "isPaired" to false,
                    "partnerId" to null,
                    "partnerName" to null,
                    "pairedAt" to null
                )
                batch.set(partnerRef, partnerUpdates, SetOptions.merge())
            }

            batch.commit().await()

            // Update local cache
            val unlinkedUser = currentUser.copy(
                isPaired = false,
                partnerId = null,
                partnerName = null,
                pairedAt = null,
                pairingCode = newCode1
            )
            saveLocalProfile(unlinkedUser)
            clearLocalPartner()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unpairing", e)
            val trMessage = mapFirebaseError(e.message ?: "Ayrılma işlemi gerçekleştirilemedi.")
            Result.failure(Exception(trMessage))
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error during signOut", e)
        }
        prefs.edit().clear().apply()
    }

    fun getCurrentAuthUserUid(): String? {
        return auth.currentUser?.uid
    }

    // Local Storage Helpers
    fun saveLocalProfile(profile: UserProfile) {
        prefs.edit()
            .putString("user_id", profile.userId)
            .putString("display_name", profile.displayName)
            .putString("birth_date", profile.birthDate)
            .putString("email", profile.email)
            .putString("avatar_preset", profile.avatarPreset)
            .putString("avatar_base64", profile.avatarBase64)
            .putString("pairing_code", profile.pairingCode)
            .putString("partner_id", profile.partnerId)
            .putString("partner_name", profile.partnerName)
            .putBoolean("is_paired", profile.isPaired)
            .putLong("paired_at", profile.pairedAt ?: 0L)
            .apply()
    }

    fun getLocalProfile(): UserProfile? {
        val uid = prefs.getString("user_id", null) ?: return null
        val pairedAtVal = prefs.getLong("paired_at", 0L)
        return UserProfile(
            userId = uid,
            displayName = prefs.getString("display_name", "") ?: "",
            birthDate = prefs.getString("birth_date", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            avatarPreset = prefs.getString("avatar_preset", "heart_rose") ?: "heart_rose",
            avatarBase64 = prefs.getString("avatar_base64", null),
            pairingCode = prefs.getString("pairing_code", "") ?: "",
            partnerId = prefs.getString("partner_id", null),
            partnerName = prefs.getString("partner_name", null),
            isPaired = prefs.getBoolean("is_paired", false),
            pairedAt = if (pairedAtVal > 0) pairedAtVal else null
        )
    }

    fun saveLocalPartner(partner: UserProfile) {
        prefs.edit()
            .putString("partner_saved_id", partner.userId)
            .putString("partner_saved_name", partner.displayName)
            .putString("partner_saved_birth", partner.birthDate)
            .putString("partner_saved_preset", partner.avatarPreset)
            .putString("partner_saved_base64", partner.avatarBase64)
            .apply()
    }

    fun getLocalPartner(): UserProfile? {
        val uid = prefs.getString("partner_saved_id", null) ?: return null
        return UserProfile(
            userId = uid,
            displayName = prefs.getString("partner_saved_name", "Sevgilim") ?: "Sevgilim",
            birthDate = prefs.getString("partner_saved_birth", "") ?: "",
            avatarPreset = prefs.getString("partner_saved_preset", "flower_pink") ?: "flower_pink",
            avatarBase64 = prefs.getString("partner_saved_base64", null),
            isPaired = true
        )
    }

    fun clearLocalPartner() {
        prefs.edit()
            .remove("partner_saved_id")
            .remove("partner_saved_name")
            .remove("partner_saved_birth")
            .remove("partner_saved_preset")
            .remove("partner_saved_base64")
            .apply()
    }

    private fun mapFirebaseError(raw: String): String {
        return when {
            raw.contains("email-already-in-use", ignoreCase = true) ->
                "Bu e-posta adresi ile kayıtlı bir hesap zaten var. Lütfen giriş yapın."
            raw.contains("invalid-email", ignoreCase = true) ->
                "Lütfen geçerli bir e-posta adresi girin."
            raw.contains("weak-password", ignoreCase = true) ->
                "Şifre çok zayıf. Lütfen en az 6 karakterden oluşan bir şifre seçin."
            raw.contains("user-not-found", ignoreCase = true) || raw.contains("wrong-password", ignoreCase = true) || raw.contains("invalid-credential", ignoreCase = true) ->
                "E-posta veya şifre hatalı. Lütfen bilgilerinizi kontrol edin."
            raw.contains("network-request-failed", ignoreCase = true) ->
                "İnternet bağlantınızı kontrol edin."
            raw.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Firebase yetkilendirme hatası: Lütfen oturum açın."
            else -> raw
        }
    }
}
