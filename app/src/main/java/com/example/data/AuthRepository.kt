package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.model.AuthDiagnosticState
import com.example.model.AuthStageLog
import com.example.model.GoogleSignInOutcome
import com.example.model.PairingResult
import com.example.model.StepStatus
import com.example.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID

class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ikimiz_prefs", Context.MODE_PRIVATE)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _diagnosticState = MutableStateFlow(AuthDiagnosticState())
    val diagnosticState: StateFlow<AuthDiagnosticState> = _diagnosticState.asStateFlow()

    private fun initDiagnostics(operation: String) {
        _diagnosticState.value = AuthDiagnosticState(
            operation = operation,
            timestamp = System.currentTimeMillis(),
            isRunning = true,
            overallSuccess = null,
            stages = emptyList()
        )
    }

    private fun recordStage(
        stageNumber: Int,
        stageTitle: String,
        status: StepStatus,
        info: String = "",
        exceptionClass: String? = null,
        errorCode: String? = null,
        exceptionMessage: String? = null
    ) {
        val currentStages = _diagnosticState.value.stages.toMutableList()
        val existingIndex = currentStages.indexOfFirst { it.stageNumber == stageNumber }
        val newLog = AuthStageLog(
            stageNumber = stageNumber,
            stageTitle = stageTitle,
            status = status,
            info = info,
            exceptionClass = exceptionClass,
            errorCode = errorCode,
            exceptionMessage = exceptionMessage
        )
        if (existingIndex >= 0) {
            currentStages[existingIndex] = newLog
        } else {
            currentStages.add(newLog)
        }
        _diagnosticState.value = _diagnosticState.value.copy(
            stages = currentStages
        )
    }

    private fun finishDiagnostics(success: Boolean) {
        _diagnosticState.value = _diagnosticState.value.copy(
            isRunning = false,
            overallSuccess = success
        )
    }

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
        val cleanEmail = email.trim().lowercase(Locale.getDefault())
        initDiagnostics("HESAP OLUŞTUR (Email/Password)")
        Log.i(TAG, "==================================================")
        Log.i(TAG, ">>> [SIGN_UP] STAGE 1: Calling FirebaseAuth.createUserWithEmailAndPassword($cleanEmail)")
        recordStage(
            stageNumber = 1,
            stageTitle = "createUserWithEmailAndPassword çağrıldı",
            status = StepStatus.RUNNING,
            info = "E-posta: $cleanEmail"
        )

        val authResult = try {
            val res = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
            recordStage(
                stageNumber = 1,
                stageTitle = "createUserWithEmailAndPassword çağrıldı",
                status = StepStatus.SUCCESS,
                info = "E-posta: $cleanEmail"
            )
            recordStage(
                stageNumber = 2,
                stageTitle = "Firebase Authentication sonucu",
                status = StepStatus.SUCCESS,
                info = "Auth kullanıcısı başarıyla oluşturuldu."
            )
            res
        } catch (authEx: FirebaseAuthException) {
            Log.e(TAG, ">>> [SIGN_UP] STAGE 2 [FAIL - AUTH]: ErrorCode=[${authEx.errorCode}], Message=[${authEx.message}]", authEx)
            recordStage(
                stageNumber = 2,
                stageTitle = "Firebase Authentication sonucu",
                status = StepStatus.FAIL,
                info = "Auth başarısız oldu.",
                exceptionClass = authEx.javaClass.name,
                errorCode = authEx.errorCode,
                exceptionMessage = authEx.message
            )
            recordStage(
                stageNumber = 6,
                stageTitle = "Sonuç",
                status = StepStatus.FAIL,
                info = "Hesap oluşturma Auth aşamasında durdu."
            )
            finishDiagnostics(false)
            val trMsg = mapAuthException(authEx)
            return Result.failure(Exception(trMsg))
        } catch (e: Exception) {
            Log.e(TAG, ">>> [SIGN_UP] STAGE 2 [FAIL - GENERAL]: ${e.javaClass.simpleName}: ${e.message}", e)
            recordStage(
                stageNumber = 2,
                stageTitle = "Firebase Authentication sonucu",
                status = StepStatus.FAIL,
                info = "Beklenmeyen hata.",
                exceptionClass = e.javaClass.name,
                errorCode = "UNKNOWN",
                exceptionMessage = e.message
            )
            recordStage(
                stageNumber = 6,
                stageTitle = "Sonuç",
                status = StepStatus.FAIL,
                info = "Hesap oluşturma başarısız."
            )
            finishDiagnostics(false)
            val trMsg = mapGeneralAuthError(e.message ?: "Kayıt olurken bir hata oluştu.")
            return Result.failure(Exception(trMsg))
        }

        Log.i(TAG, ">>> [SIGN_UP] STAGE 2 [SUCCESS]: FirebaseAuth user created successfully!")

        val currentFbUser = auth.currentUser
        if (currentFbUser == null) {
            Log.e(TAG, ">>> [SIGN_UP] STAGE 3 [FAIL]: auth.currentUser is null after createUserWithEmailAndPassword!")
            recordStage(
                stageNumber = 3,
                stageTitle = "FirebaseAuth.currentUser durumu",
                status = StepStatus.FAIL,
                info = "auth.currentUser NULL"
            )
            finishDiagnostics(false)
            return Result.failure(Exception("Kayıt oluşturuldu fakat kullanıcı oturumu başlatılamadı."))
        }
        Log.i(TAG, ">>> [SIGN_UP] STAGE 3 [SUCCESS]: auth.currentUser is present: email=[${currentFbUser.email}]")
        recordStage(
            stageNumber = 3,
            stageTitle = "FirebaseAuth.currentUser durumu",
            status = StepStatus.SUCCESS,
            info = "Mevcut oturum aktif: ${currentFbUser.email}"
        )

        val uid = currentFbUser.uid
        if (uid.isBlank()) {
            Log.e(TAG, ">>> [SIGN_UP] STAGE 4 [FAIL]: currentUser.uid is blank!")
            recordStage(
                stageNumber = 4,
                stageTitle = "currentUser.uid",
                status = StepStatus.FAIL,
                info = "currentUser.uid BOŞ"
            )
            finishDiagnostics(false)
            return Result.failure(Exception("Kullanıcı kimliği alınamadı."))
        }
        Log.i(TAG, ">>> [SIGN_UP] STAGE 4 [SUCCESS]: currentUser.uid obtained: [$uid]")
        recordStage(
            stageNumber = 4,
            stageTitle = "currentUser.uid",
            status = StepStatus.SUCCESS,
            info = "UID: $uid"
        )

        val code = generatePairingCode()
        val profile = UserProfile(
            userId = uid,
            displayName = name.trim(),
            birthDate = birthDate.trim(),
            email = cleanEmail,
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

        Log.i(TAG, ">>> [SIGN_UP] STAGE 5: Writing user profile and pairing code to Firestore...")
        recordStage(
            stageNumber = 5,
            stageTitle = "Firestore users/{uid} ve pairing_codes/{code} yazma",
            status = StepStatus.RUNNING,
            info = "Firestore dokümanları yazılıyor..."
        )

        try {
            firestore.collection("users").document(uid).set(profile.toMap()).await()
            firestore.collection("pairing_codes").document(code).set(
                mapOf("userId" to uid, "createdAt" to System.currentTimeMillis())
            ).await()
            Log.i(TAG, ">>> [SIGN_UP] STAGE 6 [SUCCESS]: Firestore documents written successfully.")
            recordStage(
                stageNumber = 5,
                stageTitle = "Firestore users/{uid} ve pairing_codes/{code} yazma",
                status = StepStatus.SUCCESS,
                info = "Dokümanlar Firestore'a başarıyla yazıldı."
            )
            recordStage(
                stageNumber = 6,
                stageTitle = "Sonuç",
                status = StepStatus.SUCCESS,
                info = "Hesap oluşturma ve profil kaydı başarıyla tamamlandı!"
            )
            finishDiagnostics(true)
            saveLocalProfile(profile)
            Log.i(TAG, "==================================================")
            return Result.success(profile)
        } catch (firestoreEx: FirebaseFirestoreException) {
            Log.e(TAG, ">>> [SIGN_UP] STAGE 6 [FAIL - FIRESTORE]: Code=[${firestoreEx.code}], Message=[${firestoreEx.message}]", firestoreEx)
            recordStage(
                stageNumber = 5,
                stageTitle = "Firestore users/{uid} ve pairing_codes/{code} yazma",
                status = StepStatus.FAIL,
                info = "Firestore yazma hatası",
                exceptionClass = firestoreEx.javaClass.name,
                errorCode = firestoreEx.code.name,
                exceptionMessage = firestoreEx.message
            )
            recordStage(
                stageNumber = 6,
                stageTitle = "Sonuç",
                status = StepStatus.FAIL,
                info = "Auth başarılı, ancak Firestore yazma engellendi: ${firestoreEx.code}"
            )
            finishDiagnostics(false)
            return Result.failure(Exception("Firestore Hatası [${firestoreEx.code}]: ${firestoreEx.message}"))
        } catch (e: Exception) {
            Log.e(TAG, ">>> [SIGN_UP] STAGE 6 [FAIL - WRITE]: ${e.message}", e)
            recordStage(
                stageNumber = 5,
                stageTitle = "Firestore users/{uid} ve pairing_codes/{code} yazma",
                status = StepStatus.FAIL,
                info = "Yazma hatası",
                exceptionClass = e.javaClass.name,
                errorCode = "UNKNOWN",
                exceptionMessage = e.message
            )
            recordStage(
                stageNumber = 6,
                stageTitle = "Sonuç",
                status = StepStatus.FAIL,
                info = "Firestore işlemi tamamlanamadı: ${e.message}"
            )
            finishDiagnostics(false)
            return Result.failure(e)
        }
    }

    suspend fun signIn(email: String, pass: String): Result<UserProfile> {
        val cleanEmail = email.trim().lowercase(Locale.getDefault())
        initDiagnostics("GİRİŞ YAP (Email/Password)")
        Log.i(TAG, "==================================================")
        Log.i(TAG, ">>> [SIGN_IN] STAGE 1: Calling FirebaseAuth.signInWithEmailAndPassword($cleanEmail)")
        recordStage(
            stageNumber = 1,
            stageTitle = "signInWithEmailAndPassword çağrıldı",
            status = StepStatus.RUNNING,
            info = "E-posta: $cleanEmail"
        )

        // STAGE 1 & 2: Authenticate with Firebase Auth
        val authResult = try {
            val res = auth.signInWithEmailAndPassword(cleanEmail, pass).await()
            recordStage(
                stageNumber = 1,
                stageTitle = "signInWithEmailAndPassword çağrıldı",
                status = StepStatus.SUCCESS,
                info = "E-posta: $cleanEmail"
            )
            recordStage(
                stageNumber = 2,
                stageTitle = "Firebase Authentication sonucu",
                status = StepStatus.SUCCESS,
                info = "AUTH SUCCESS (Kimlik doğrulandı)"
            )
            res
        } catch (authEx: FirebaseAuthException) {
            Log.e(TAG, ">>> [SIGN_IN] STAGE 2 [FAIL - AUTH_EXCEPTION]: ErrorCode=[${authEx.errorCode}], Message=[${authEx.message}]", authEx)
            recordStage(
                stageNumber = 1,
                stageTitle = "signInWithEmailAndPassword çağrıldı",
                status = StepStatus.SUCCESS,
                info = "E-posta: $cleanEmail"
            )
            recordStage(
                stageNumber = 2,
                stageTitle = "Firebase Authentication sonucu",
                status = StepStatus.FAIL,
                info = "AUTH FAILED",
                exceptionClass = authEx.javaClass.name,
                errorCode = authEx.errorCode,
                exceptionMessage = authEx.message
            )
            recordStage(
                stageNumber = 6,
                stageTitle = "Sonuç",
                status = StepStatus.FAIL,
                info = "Giriş işlemi Auth aşamasında durduruldu: ${authEx.errorCode}"
            )
            finishDiagnostics(false)
            val trMsg = mapAuthException(authEx)
            return Result.failure(Exception(trMsg))
        } catch (e: Exception) {
            Log.e(TAG, ">>> [SIGN_IN] STAGE 2 [FAIL - GENERAL_EXCEPTION]: ${e.javaClass.simpleName}: ${e.message}", e)
            recordStage(
                stageNumber = 2,
                stageTitle = "Firebase Authentication sonucu",
                status = StepStatus.FAIL,
                info = "AUTH FAILED (Genel istisna)",
                exceptionClass = e.javaClass.name,
                errorCode = "UNKNOWN",
                exceptionMessage = e.message
            )
            recordStage(
                stageNumber = 6,
                stageTitle = "Sonuç",
                status = StepStatus.FAIL,
                info = "Giriş işlemi tamamlanamadı."
            )
            finishDiagnostics(false)
            val trMsg = mapGeneralAuthError(e.message ?: "Giriş yapılırken bir hata oluştu.")
            return Result.failure(Exception(trMsg))
        }

        Log.i(TAG, ">>> [SIGN_IN] STAGE 2 [SUCCESS]: FirebaseAuth.signInWithEmailAndPassword succeeded!")

        // STAGE 3: Check FirebaseAuth.currentUser
        val currentFbUser = auth.currentUser
        if (currentFbUser == null) {
            Log.e(TAG, ">>> [SIGN_IN] STAGE 3 [FAIL]: auth.currentUser is NULL despite successful signIn call!")
            recordStage(
                stageNumber = 3,
                stageTitle = "FirebaseAuth.currentUser durumu",
                status = StepStatus.FAIL,
                info = "auth.currentUser NULL"
            )
            finishDiagnostics(false)
            return Result.failure(Exception("Oturum açıldı ancak currentUser oluşturulamadı."))
        }
        Log.i(TAG, ">>> [SIGN_IN] STAGE 3 [SUCCESS]: auth.currentUser is present: email=[${currentFbUser.email}]")
        recordStage(
            stageNumber = 3,
            stageTitle = "FirebaseAuth.currentUser durumu",
            status = StepStatus.SUCCESS,
            info = "Oturum aktif: ${currentFbUser.email}"
        )

        // STAGE 4: Extract currentUser.uid
        val uid = currentFbUser.uid
        if (uid.isBlank()) {
            Log.e(TAG, ">>> [SIGN_IN] STAGE 4 [FAIL]: currentUser.uid is blank!")
            recordStage(
                stageNumber = 4,
                stageTitle = "currentUser.uid",
                status = StepStatus.FAIL,
                info = "currentUser.uid BOŞ"
            )
            finishDiagnostics(false)
            return Result.failure(Exception("Kullanıcı kimliği (UID) alınamadı."))
        }
        Log.i(TAG, ">>> [SIGN_IN] STAGE 4 [SUCCESS]: currentUser.uid obtained successfully: [$uid]")
        recordStage(
            stageNumber = 4,
            stageTitle = "currentUser.uid",
            status = StepStatus.SUCCESS,
            info = "UID: $uid"
        )

        // STAGE 5: Fetch User Profile from Firestore
        Log.i(TAG, ">>> [SIGN_IN] STAGE 5: Reading Firestore document users/$uid ...")
        recordStage(
            stageNumber = 5,
            stageTitle = "Firestore users/{uid} okuma sonucu",
            status = StepStatus.RUNNING,
            info = "users/$uid dokümanı okunuyor..."
        )

        var profile: UserProfile? = null

        try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists() && doc.data != null) {
                Log.i(TAG, ">>> [SIGN_IN] STAGE 6 [SUCCESS - DOC_FOUND]: users/$uid document loaded successfully!")
                profile = UserProfile.fromMap(doc.data!!)
                recordStage(
                    stageNumber = 5,
                    stageTitle = "Firestore users/{uid} okuma sonucu",
                    status = StepStatus.SUCCESS,
                    info = "DOC FOUND: ${profile.displayName} (Eşleşti: ${profile.isPaired})"
                )
            } else {
                Log.w(TAG, ">>> [SIGN_IN] STAGE 6 [WARNING - NOT_FOUND]: users/$uid document does not exist in Firestore. Creating fallback...")
                recordStage(
                    stageNumber = 5,
                    stageTitle = "Firestore users/{uid} okuma sonucu",
                    status = StepStatus.FAIL,
                    info = "DOC NOT FOUND: users/$uid dokümanı Firestore'da henüz mevcut değil.",
                    errorCode = "NOT_FOUND",
                    exceptionMessage = "users/$uid Firestore koleksiyonunda bulunamadı. Lütfen önce 'Hesap Oluştur' sekmesinden kaydınızı tamamlayın."
                )
                recordStage(
                    stageNumber = 6,
                    stageTitle = "Sonuç",
                    status = StepStatus.FAIL,
                    info = "Profil veritabanında bulunamadı (NOT_FOUND)."
                )
                finishDiagnostics(false)
                return Result.failure(Exception("Profil dokümanı Firestore'da bulunamadı (NOT_FOUND). Lütfen önce kayıt olun."))
            }
        } catch (firestoreEx: FirebaseFirestoreException) {
            Log.e(TAG, ">>> [SIGN_IN] STAGE 6 [FAIL - FIRESTORE_EXCEPTION]: Code=[${firestoreEx.code}], Message=[${firestoreEx.message}]", firestoreEx)
            recordStage(
                stageNumber = 5,
                stageTitle = "Firestore users/{uid} okuma sonucu",
                status = StepStatus.FAIL,
                info = "FIRESTORE READ FAILED",
                exceptionClass = firestoreEx.javaClass.name,
                errorCode = firestoreEx.code.name,
                exceptionMessage = firestoreEx.message
            )
            recordStage(
                stageNumber = 6,
                stageTitle = "Sonuç",
                status = StepStatus.FAIL,
                info = "AUTH SUCCESS, ANCAK FIRESTORE ERİŞİMİ REDDEDİLDİ: ${firestoreEx.code}"
            )
            finishDiagnostics(false)
            return Result.failure(Exception("FIRESTORE_ERROR [${firestoreEx.code}]: ${firestoreEx.message}"))
        } catch (e: Exception) {
            Log.e(TAG, ">>> [SIGN_IN] STAGE 6 [FAIL - GENERAL]: ${e.javaClass.simpleName}: ${e.message}", e)
            recordStage(
                stageNumber = 5,
                stageTitle = "Firestore users/{uid} okuma sonucu",
                status = StepStatus.FAIL,
                info = "FIRESTORE READ ERROR",
                exceptionClass = e.javaClass.name,
                errorCode = "UNKNOWN",
                exceptionMessage = e.message
            )
            recordStage(
                stageNumber = 6,
                stageTitle = "Sonuç",
                status = StepStatus.FAIL,
                info = "Firestore okunamadı: ${e.message}"
            )
            finishDiagnostics(false)
            return Result.failure(e)
        }

        if (profile != null) {
            saveLocalProfile(profile)
            recordStage(
                stageNumber = 6,
                stageTitle = "Sonuç",
                status = StepStatus.SUCCESS,
                info = "Tüm aşamalar başarılı! Giriş tamamlandı."
            )
            finishDiagnostics(true)
            Log.i(TAG, ">>> [SIGN_IN] COMPLETE [SUCCESS]: User [$uid] is fully signed in and ready!")
            Log.i(TAG, "==================================================")
            return Result.success(profile)
        } else {
            recordStage(
                stageNumber = 6,
                stageTitle = "Sonuç",
                status = StepStatus.FAIL,
                info = "Profil yüklenemedi."
            )
            finishDiagnostics(false)
            Log.e(TAG, ">>> [SIGN_IN] COMPLETE [FAIL]: Profile could not be initialized.")
            Log.i(TAG, "==================================================")
            return Result.failure(Exception("Giriş tamamlanamadı. Lütfen tekrar deneyin."))
        }
    }


    suspend fun signInWithGoogle(activityContext: Context): Result<GoogleSignInOutcome> {
        initDiagnostics("GOOGLE İLE GİRİŞ (Google Sign-In)")
        Log.i(TAG, "==================================================")
        Log.i(TAG, ">>> [GOOGLE_AUTH] Starting 10-stage Google Sign-In pipeline...")

        // STAGE G1: Credential Manager oluşturuldu
        Log.i(TAG, ">>> [GOOGLE_AUTH] STAGE G1: Initializing CredentialManager...")
        recordStage(
            stageNumber = 1,
            stageTitle = "STAGE G1: Credential Manager oluşturuldu",
            status = StepStatus.RUNNING,
            info = "CredentialManager hazırlanıyor..."
        )

        val credentialManager = try {
            val cm = CredentialManager.create(activityContext)
            recordStage(
                stageNumber = 1,
                stageTitle = "STAGE G1: Credential Manager oluşturuldu",
                status = StepStatus.SUCCESS,
                info = "CredentialManager başarıyla oluşturuldu."
            )
            cm
        } catch (e: Exception) {
            Log.e(TAG, ">>> [GOOGLE_AUTH] STAGE G1 FAIL: ${e.message}", e)
            recordStage(
                stageNumber = 1,
                stageTitle = "STAGE G1: Credential Manager oluşturuldu",
                status = StepStatus.FAIL,
                info = "CredentialManager başlatılamadı",
                exceptionClass = e.javaClass.name,
                errorCode = "CM_INIT_FAILED",
                exceptionMessage = e.message
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.FAIL,
                info = "CredentialManager başlatılamadı: ${e.message}"
            )
            finishDiagnostics(false)
            return Result.failure(e)
        }

        // STAGE G2: GetGoogleIdOption oluşturuldu
        Log.i(TAG, ">>> [GOOGLE_AUTH] STAGE G2: Creating GetGoogleIdOption...")
        recordStage(
            stageNumber = 2,
            stageTitle = "STAGE G2: GetGoogleIdOption oluşturuldu",
            status = StepStatus.RUNNING,
            info = "Web Client ID ve parametreler taranıyor..."
        )

        val webClientIdResId = activityContext.resources.getIdentifier(
            "default_web_client_id", "string", activityContext.packageName
        )
        val defaultWebClientId = if (webClientIdResId != 0) {
            try { activityContext.getString(webClientIdResId) } catch (_: Exception) { null }
        } else null

        val serverClientId = when {
            !defaultWebClientId.isNullOrBlank() -> defaultWebClientId
            else -> "746721522258.apps.googleusercontent.com"
        }

        val isDefaultClientIdValid = !defaultWebClientId.isNullOrBlank()

        val googleIdOption = try {
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val infoText = if (isDefaultClientIdValid) {
                "Client ID (R.string.default_web_client_id): $serverClientId"
            } else {
                "Client ID: $serverClientId (UYARI: R.string.default_web_client_id google-services.json içinde tanımlı değil)"
            }

            recordStage(
                stageNumber = 2,
                stageTitle = "STAGE G2: GetGoogleIdOption oluşturuldu",
                status = StepStatus.SUCCESS,
                info = infoText
            )
            option
        } catch (e: Exception) {
            Log.e(TAG, ">>> [GOOGLE_AUTH] STAGE G2 FAIL: ${e.message}", e)
            recordStage(
                stageNumber = 2,
                stageTitle = "STAGE G2: GetGoogleIdOption oluşturuldu",
                status = StepStatus.FAIL,
                info = "GetGoogleIdOption oluşturulamadı",
                exceptionClass = e.javaClass.name,
                errorCode = "OPTION_BUILD_FAILED",
                exceptionMessage = e.message
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.FAIL,
                info = "GetGoogleIdOption oluşturulamadı: ${e.message}"
            )
            finishDiagnostics(false)
            return Result.failure(e)
        }

        // STAGE G3: Google credential isteği başlatıldı
        Log.i(TAG, ">>> [GOOGLE_AUTH] STAGE G3: Starting Google credential request...")
        recordStage(
            stageNumber = 3,
            stageTitle = "STAGE G3: Google credential isteği başlatıldı",
            status = StepStatus.RUNNING,
            info = "Google Play Services hesap seçici diyalogu açılıyor..."
        )

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialResponse = try {
            val response = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            recordStage(
                stageNumber = 3,
                stageTitle = "STAGE G3: Google credential isteği başlatıldı",
                status = StepStatus.SUCCESS,
                info = "Google hesap seçimi tamamlandı."
            )
            response
        } catch (cancelEx: GetCredentialCancellationException) {
            Log.w(TAG, ">>> [GOOGLE_AUTH] STAGE G3/G4: User canceled Google Sign-In.")
            recordStage(
                stageNumber = 3,
                stageTitle = "STAGE G3: Google credential isteği başlatıldı",
                status = StepStatus.FAIL,
                info = "Kullanıcı Google oturum açma penceresini kapattı/iptal etti."
            )
            recordStage(
                stageNumber = 4,
                stageTitle = "STAGE G4: Credential başarıyla alındı",
                status = StepStatus.FAIL,
                info = "İşlem iptal edildi",
                exceptionClass = cancelEx.javaClass.name,
                errorCode = "USER_CANCELED",
                exceptionMessage = cancelEx.message
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.FAIL,
                info = "Google ile giriş kullanıcı tarafından iptal edildi."
            )
            finishDiagnostics(false)
            return Result.failure(Exception("Google ile giriş iptal edildi."))
        } catch (credEx: GetCredentialException) {
            val errorType = credEx.type
            val isNoCredential = errorType.contains("TYPE_NO_CREDENTIAL", ignoreCase = true)
            val isDeveloperError = errorType.contains("DEVELOPER_ERROR", ignoreCase = true) ||
                    (credEx.message?.contains("DEVELOPER_ERROR") == true) ||
                    (credEx.message?.contains("10") == true)

            val explanation = when {
                isNoCredential -> "TYPE_NO_CREDENTIAL: Cihazda uygun hesap bulunamadı veya Firebase Console'da Web Client ID / SHA-1 uyuşmazlığı var."
                isDeveloperError -> "DEVELOPER_ERROR (10): OAuth İstemci / SHA-1 yapılandırma hatası. Firebase Console'da SHA-1 parmak izi eklenmeli."
                else -> "GetCredentialException [$errorType]: ${credEx.message}"
            }

            Log.e(TAG, ">>> [GOOGLE_AUTH] STAGE G4 FAIL: $explanation", credEx)
            recordStage(
                stageNumber = 3,
                stageTitle = "STAGE G3: Google credential isteği başlatıldı",
                status = StepStatus.FAIL,
                info = "İstek reddedildi veya hata oluştu."
            )
            recordStage(
                stageNumber = 4,
                stageTitle = "STAGE G4: Credential başarıyla alındı",
                status = StepStatus.FAIL,
                info = explanation,
                exceptionClass = credEx.javaClass.name,
                errorCode = errorType,
                exceptionMessage = credEx.message
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.FAIL,
                info = "Google Credential alınamadı: $errorType"
            )
            finishDiagnostics(false)
            return Result.failure(Exception("Google Giriş Hatası [$errorType]: $explanation"))
        } catch (e: Exception) {
            Log.e(TAG, ">>> [GOOGLE_AUTH] STAGE G4 FAIL (Unexpected): ${e.message}", e)
            recordStage(
                stageNumber = 3,
                stageTitle = "STAGE G3: Google credential isteği başlatıldı",
                status = StepStatus.FAIL,
                info = "Beklenmeyen hata: ${e.message}"
            )
            recordStage(
                stageNumber = 4,
                stageTitle = "STAGE G4: Credential başarıyla alındı",
                status = StepStatus.FAIL,
                info = "Hata: ${e.message}",
                exceptionClass = e.javaClass.name,
                errorCode = "UNKNOWN",
                exceptionMessage = e.message
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.FAIL,
                info = "Google Credential alınamadı: ${e.message}"
            )
            finishDiagnostics(false)
            return Result.failure(e)
        }

        // STAGE G4: Credential başarıyla alındı
        val credential = credentialResponse.credential
        recordStage(
            stageNumber = 4,
            stageTitle = "STAGE G4: Credential başarıyla alındı",
            status = StepStatus.SUCCESS,
            info = "Credential Tipi: ${credential.type}"
        )

        // STAGE G5: Google ID token alındı
        Log.i(TAG, ">>> [GOOGLE_AUTH] STAGE G5: Extracting Google ID token...")
        recordStage(
            stageNumber = 5,
            stageTitle = "STAGE G5: Google ID token alındı",
            status = StepStatus.RUNNING,
            info = "ID Token çözümleniyor..."
        )

        val idToken = try {
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val token = googleIdTokenCredential.idToken
                if (token.isBlank()) {
                    throw Exception("Alınan ID Token boş.")
                }
                recordStage(
                    stageNumber = 5,
                    stageTitle = "STAGE G5: Google ID token alındı",
                    status = StepStatus.SUCCESS,
                    info = "ID Token başarıyla alındı (Uzunluk: ${token.length}, Email: ${googleIdTokenCredential.id})"
                )
                token
            } else {
                throw Exception("Beklenmeyen credential tipi: ${credential.type}")
            }
        } catch (e: Exception) {
            Log.e(TAG, ">>> [GOOGLE_AUTH] STAGE G5 FAIL: ${e.message}", e)
            recordStage(
                stageNumber = 5,
                stageTitle = "STAGE G5: Google ID token alındı",
                status = StepStatus.FAIL,
                info = "ID Token alınamadı: ${e.message}",
                exceptionClass = e.javaClass.name,
                errorCode = "TOKEN_PARSE_FAILED",
                exceptionMessage = e.message
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.FAIL,
                info = "Google ID Token çözümlenemedi."
            )
            finishDiagnostics(false)
            return Result.failure(e)
        }

        // STAGE G6: FirebaseAuth.signInWithCredential() çağrıldı
        Log.i(TAG, ">>> [GOOGLE_AUTH] STAGE G6: Calling FirebaseAuth.signInWithCredential...")
        recordStage(
            stageNumber = 6,
            stageTitle = "STAGE G6: FirebaseAuth.signInWithCredential() çağrıldı",
            status = StepStatus.RUNNING,
            info = "Firebase Auth GoogleProvider ile kimlik doğrulanıyor..."
        )

        val authResult = try {
            val fbCredential = GoogleAuthProvider.getCredential(idToken, null)
            val res = auth.signInWithCredential(fbCredential).await()
            recordStage(
                stageNumber = 6,
                stageTitle = "STAGE G6: FirebaseAuth.signInWithCredential() çağrıldı",
                status = StepStatus.SUCCESS,
                info = "signInWithCredential isteği başarıyla iletildi."
            )
            recordStage(
                stageNumber = 7,
                stageTitle = "STAGE G7: Firebase Authentication sonucu",
                status = StepStatus.SUCCESS,
                info = "AUTH SUCCESS (Google ile Firebase kimliği doğrulandı)"
            )
            res
        } catch (authEx: FirebaseAuthException) {
            Log.e(TAG, ">>> [GOOGLE_AUTH] STAGE G7 FAIL (FirebaseAuthException): ${authEx.errorCode}", authEx)
            recordStage(
                stageNumber = 6,
                stageTitle = "STAGE G6: FirebaseAuth.signInWithCredential() çağrıldı",
                status = StepStatus.FAIL,
                info = "Firebase Auth isteği başarısız oldu"
            )
            recordStage(
                stageNumber = 7,
                stageTitle = "STAGE G7: Firebase Authentication sonucu",
                status = StepStatus.FAIL,
                info = "FirebaseAuthException: ${authEx.errorCode}",
                exceptionClass = authEx.javaClass.name,
                errorCode = authEx.errorCode,
                exceptionMessage = authEx.message
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.FAIL,
                info = "Firebase Auth kimlik doğrulaması başarısız: ${authEx.errorCode}"
            )
            finishDiagnostics(false)
            return Result.failure(authEx)
        } catch (e: Exception) {
            Log.e(TAG, ">>> [GOOGLE_AUTH] STAGE G7 FAIL (Unexpected): ${e.message}", e)
            recordStage(
                stageNumber = 6,
                stageTitle = "STAGE G6: FirebaseAuth.signInWithCredential() çağrıldı",
                status = StepStatus.FAIL,
                info = "Hata: ${e.message}"
            )
            recordStage(
                stageNumber = 7,
                stageTitle = "STAGE G7: Firebase Authentication sonucu",
                status = StepStatus.FAIL,
                info = "Beklenmeyen hata: ${e.message}",
                exceptionClass = e.javaClass.name,
                errorCode = "AUTH_UNKNOWN",
                exceptionMessage = e.message
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.FAIL,
                info = "Firebase Auth oturumu açılamadı: ${e.message}"
            )
            finishDiagnostics(false)
            return Result.failure(e)
        }

        // STAGE G8: FirebaseAuth.currentUser ve UID kontrolü
        Log.i(TAG, ">>> [GOOGLE_AUTH] STAGE G8: Checking currentUser and UID...")
        val fbUser = auth.currentUser
        if (fbUser == null || fbUser.uid.isBlank()) {
            recordStage(
                stageNumber = 8,
                stageTitle = "STAGE G8: FirebaseAuth.currentUser ve UID kontrolü",
                status = StepStatus.FAIL,
                info = "currentUser null veya UID boş!",
                errorCode = "NULL_USER"
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.FAIL,
                info = "Firebase kullanıcısı doğrulanamadı."
            )
            finishDiagnostics(false)
            return Result.failure(Exception("Firebase kullanıcısı doğrulanamadı."))
        }

        val uid = fbUser.uid
        val userEmail = fbUser.email ?: ""
        val userDisplayName = fbUser.displayName ?: ""
        recordStage(
            stageNumber = 8,
            stageTitle = "STAGE G8: FirebaseAuth.currentUser ve UID kontrolü",
            status = StepStatus.SUCCESS,
            info = "UID: $uid | Email: $userEmail | İsim: $userDisplayName"
        )

        // STAGE G9: Firestore users/{uid} kontrolü
        Log.i(TAG, ">>> [GOOGLE_AUTH] STAGE G9: Checking Firestore users/$uid...")
        recordStage(
            stageNumber = 9,
            stageTitle = "STAGE G9: Firestore users/{uid} kontrolü",
            status = StepStatus.RUNNING,
            info = "Firestore users/$uid dokümanı okunuyor..."
        )

        val docSnapshot = try {
            val snap = firestore.collection("users").document(uid).get().await()
            snap
        } catch (firestoreEx: FirebaseFirestoreException) {
            Log.e(TAG, ">>> [GOOGLE_AUTH] STAGE G9 FAIL (Firestore): ${firestoreEx.code}", firestoreEx)
            recordStage(
                stageNumber = 9,
                stageTitle = "STAGE G9: Firestore users/{uid} kontrolü",
                status = StepStatus.FAIL,
                info = "Firestore Erişim Reddedildi (${firestoreEx.code})",
                exceptionClass = firestoreEx.javaClass.name,
                errorCode = firestoreEx.code.name,
                exceptionMessage = firestoreEx.message
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.FAIL,
                info = "Google Auth başarılı ancak Firestore izni engellendi: ${firestoreEx.code}"
            )
            finishDiagnostics(false)
            return Result.failure(Exception("FIRESTORE_ERROR [${firestoreEx.code}]: ${firestoreEx.message}"))
        } catch (e: Exception) {
            Log.e(TAG, ">>> [GOOGLE_AUTH] STAGE G9 FAIL: ${e.message}", e)
            recordStage(
                stageNumber = 9,
                stageTitle = "STAGE G9: Firestore users/{uid} kontrolü",
                status = StepStatus.FAIL,
                info = "Firestore Okuma Hatası: ${e.message}",
                exceptionClass = e.javaClass.name,
                errorCode = "FIRESTORE_UNKNOWN",
                exceptionMessage = e.message
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.FAIL,
                info = "Firestore okunamadı: ${e.message}"
            )
            finishDiagnostics(false)
            return Result.failure(e)
        }

        // Check if profile exists and valid
        if (docSnapshot.exists() && docSnapshot.data != null) {
            val profile = UserProfile.fromMap(docSnapshot.data!!)
            if (profile.displayName.isNotBlank() && profile.birthDate.isNotBlank()) {
                recordStage(
                    stageNumber = 9,
                    stageTitle = "STAGE G9: Firestore users/{uid} kontrolü",
                    status = StepStatus.SUCCESS,
                    info = "DOC FOUND: ${profile.displayName} (Eşleşti: ${profile.isPaired})"
                )
                saveLocalProfile(profile)
                recordStage(
                    stageNumber = 10,
                    stageTitle = "STAGE G10: Sonuç",
                    status = StepStatus.SUCCESS,
                    info = "Tüm aşamalar başarılı! Mevcut profille giriş yapıldı."
                )
                finishDiagnostics(true)
                return Result.success(GoogleSignInOutcome.Success(profile))
            } else {
                recordStage(
                    stageNumber = 9,
                    stageTitle = "STAGE G9: Firestore users/{uid} kontrolü",
                    status = StepStatus.SUCCESS,
                    info = "DOC FOUND (Eksik Bilgi): Profil tamamlama gerekiyor."
                )
                recordStage(
                    stageNumber = 10,
                    stageTitle = "STAGE G10: Sonuç",
                    status = StepStatus.SUCCESS,
                    info = "Google Auth başarılı. Profil tamamlama ekranı açılıyor."
                )
                finishDiagnostics(true)
                return Result.success(
                    GoogleSignInOutcome.NeedsProfileCompletion(
                        uid = uid,
                        email = userEmail.ifBlank { profile.email },
                        displayName = profile.displayName.ifBlank { userDisplayName },
                        photoUrl = fbUser.photoUrl?.toString()
                    )
                )
            }
        } else {
            recordStage(
                stageNumber = 9,
                stageTitle = "STAGE G9: Firestore users/{uid} kontrolü",
                status = StepStatus.SUCCESS,
                info = "DOC NOT FOUND (Yeni Kullanıcı): users/$uid dokümanı henüz yok. Profil tamamlama açılıyor."
            )
            recordStage(
                stageNumber = 10,
                stageTitle = "STAGE G10: Sonuç",
                status = StepStatus.SUCCESS,
                info = "Google Auth başarılı! Yeni profil oluşturulması için yönlendiriliyor."
            )
            finishDiagnostics(true)
            return Result.success(
                GoogleSignInOutcome.NeedsProfileCompletion(
                    uid = uid,
                    email = userEmail,
                    displayName = userDisplayName,
                    photoUrl = fbUser.photoUrl?.toString()
                )
            )
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

    private fun mapAuthException(e: FirebaseAuthException): String {
        val code = e.errorCode.uppercase(Locale.getDefault())
        val msg = e.message ?: ""
        Log.w(TAG, "Mapping FirebaseAuthException: errorCode=[${e.errorCode}], message=[$msg]")
        return when {
            code.contains("ERROR_INVALID_CUSTOM_TOKEN") ->
                "Özel oturum jetonu geçersiz."
            code.contains("ERROR_CUSTOM_TOKEN_MISMATCH") ->
                "Özel oturum jetonu eşleşmedi."
            code.contains("ERROR_INVALID_CREDENTIAL") || code.contains("INVALID_CREDENTIAL") || msg.contains("invalid-credential", ignoreCase = true) ->
                "E-posta veya şifre hatalı. Lütfen bilgilerinizi kontrol edin."
            code.contains("ERROR_INVALID_EMAIL") || code.contains("INVALID_EMAIL") || msg.contains("invalid-email", ignoreCase = true) ->
                "Geçersiz e-posta adresi formatı."
            code.contains("ERROR_WRONG_PASSWORD") || code.contains("WRONG_PASSWORD") || msg.contains("wrong-password", ignoreCase = true) ->
                "Girdiğiniz şifre hatalı. Lütfen tekrar deneyin."
            code.contains("ERROR_USER_MISMATCH") ->
                "Kullanıcı kimlik bilgileri eşleşmedi."
            code.contains("ERROR_REQUIRES_RECENT_LOGIN") ->
                "Bu işlem için yeniden giriş yapmanız gerekiyor."
            code.contains("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL") ->
                "Bu e-posta adresi ile ilişkili farklı bir hesap mevcut."
            code.contains("ERROR_EMAIL_ALREADY_IN_USE") || code.contains("EMAIL_ALREADY_IN_USE") || msg.contains("email-already-in-use", ignoreCase = true) ->
                "Bu e-posta adresi zaten kayıtlı. Lütfen giriş yapın."
            code.contains("ERROR_WEAK_PASSWORD") || code.contains("WEAK_PASSWORD") || msg.contains("weak-password", ignoreCase = true) ->
                "Şifreniz çok zayıf. Lütfen en az 6 karakterli bir şifre belirleyin."
            code.contains("ERROR_CREDENTIAL_ALREADY_IN_USE") ->
                "Bu kimlik bilgisi başka bir hesap tarafından kullanılıyor."
            code.contains("ERROR_USER_DISABLED") || code.contains("USER_DISABLED") || msg.contains("user-disabled", ignoreCase = true) ->
                "Bu kullanıcı hesabı yönetici tarafından devre dışı bırakılmış."
            code.contains("ERROR_USER_NOT_FOUND") || code.contains("USER_NOT_FOUND") || msg.contains("user-not-found", ignoreCase = true) ->
                "Bu e-posta adresine ait bir hesap bulunamadı. Lütfen önce kayıt olun."
            code.contains("ERROR_OPERATION_NOT_ALLOWED") || code.contains("OPERATION_NOT_ALLOWED") || msg.contains("operation-not-allowed", ignoreCase = true) ->
                "Firebase Console'da E-posta/Şifre sağlayıcısı etkinleştirilmemiş (operation-not-allowed)."
            code.contains("ERROR_TOO_MANY_REQUESTS") || code.contains("TOO_MANY_REQUESTS") || msg.contains("too-many-requests", ignoreCase = true) ->
                "Çok fazla başarısız deneme yapıldı. Güvenlik nedeniyle hesap geçici olarak kilitlendi. Lütfen birkaç dakika sonra tekrar deneyin."
            else ->
                if (msg.isNotBlank()) msg else "Kimlik doğrulama hatası: ${e.errorCode}"
        }
    }

    private fun mapGeneralAuthError(raw: String): String {
        return when {
            raw.contains("email-already-in-use", ignoreCase = true) ->
                "Bu e-posta adresi ile kayıtlı bir hesap zaten var. Lütfen giriş yapın."
            raw.contains("invalid-email", ignoreCase = true) ->
                "Lütfen geçerli bir e-posta adresi girin."
            raw.contains("weak-password", ignoreCase = true) ->
                "Şifre çok zayıf. Lütfen en az 6 karakterden oluşan bir şifre seçin."
            raw.contains("user-not-found", ignoreCase = true) ->
                "Bu e-posta adresine ait hesap bulunamadı. Lütfen kayıt olun."
            raw.contains("wrong-password", ignoreCase = true) || raw.contains("invalid-credential", ignoreCase = true) ->
                "E-posta veya şifre hatalı. Lütfen bilgilerinizi kontrol edin."
            raw.contains("user-disabled", ignoreCase = true) ->
                "Bu kullanıcı hesabı devre dışı bırakılmış."
            raw.contains("too-many-requests", ignoreCase = true) ->
                "Çok fazla deneme yapıldı. Lütfen biraz bekleyin."
            raw.contains("operation-not-allowed", ignoreCase = true) ->
                "Firebase Console'da Email/Password sağlayıcısı aktif değil."
            raw.contains("network-request-failed", ignoreCase = true) ->
                "İnternet bağlantınızı kontrol edin."
            raw.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Firestore erişim yetkisi reddedildi (PERMISSION_DENIED)."
            raw.contains("UNAVAILABLE", ignoreCase = true) ->
                "Firebase servisine şu anda ulaşılamıyor (UNAVAILABLE). Lütfen internetinizi kontrol edin."
            else -> raw
        }
    }

    private fun mapFirebaseError(raw: String): String {
        return mapGeneralAuthError(raw)
    }
}
