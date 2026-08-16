package com.example.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

object GoogleAuthHelper {
    suspend fun getGoogleIdToken(context: Context): Result<String> {
        return try {
            val credentialManager = CredentialManager.create(context)

            val webClientIdResId = context.resources.getIdentifier(
                "default_web_client_id", "string", context.packageName
            )
            val serverClientId = if (webClientIdResId != 0) {
                context.getString(webClientIdResId)
            } else {
                "746721522258.apps.googleusercontent.com"
            }

            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(googleIdTokenCredential.idToken)
            } else {
                Result.failure(Exception("Beklenmeyen kimlik bilgisi türü alındı."))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Google ile giriş iptal edildi."))
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Google ile giriş başarısız oldu (${e.type}). Gerçek cihazda Google Play Services ile test edin."))
        } catch (e: Exception) {
            Result.failure(Exception("Google ile giriş başlatılamadı: ${e.localizedMessage}"))
        }
    }
}
