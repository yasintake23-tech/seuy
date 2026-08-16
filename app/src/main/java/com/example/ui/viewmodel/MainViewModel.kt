package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.GoogleAuthHelper
import com.example.model.AuthDiagnosticState
import com.example.model.GoogleSignInOutcome
import com.example.model.PairingResult
import com.example.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application.applicationContext)

    val diagnosticState: StateFlow<AuthDiagnosticState> = repository.diagnosticState

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _partnerUser = MutableStateFlow<UserProfile?>(null)
    val partnerUser: StateFlow<UserProfile?> = _partnerUser.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isSignUpMode = MutableStateFlow(true)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    private val _profileCompletionData =
        MutableStateFlow<GoogleSignInOutcome.NeedsProfileCompletion?>(null)
    val profileCompletionData: StateFlow<GoogleSignInOutcome.NeedsProfileCompletion?> =
        _profileCompletionData.asStateFlow()

    private val _pairingCodeInput = MutableStateFlow("")
    val pairingCodeInput: StateFlow<String> = _pairingCodeInput.asStateFlow()

    private val _isPairingInProgress = MutableStateFlow(false)
    val isPairingInProgress: StateFlow<Boolean> = _isPairingInProgress.asStateFlow()

    private val _pairingError = MutableStateFlow<String?>(null)
    val pairingError: StateFlow<String?> = _pairingError.asStateFlow()

    private val _pairingSuccessMessage = MutableStateFlow<String?>(null)
    val pairingSuccessMessage: StateFlow<String?> = _pairingSuccessMessage.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showUnpairConfirmDialog = MutableStateFlow(false)
    val showUnpairConfirmDialog: StateFlow<Boolean> = _showUnpairConfirmDialog.asStateFlow()

    private var currentUserJob: Job? = null
    private var partnerJob: Job? = null

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val authUser = repository.getCurrentFirebaseUser()
        if (authUser == null) {
            // Unauthenticated state: do not attach any Firestore snapshot listeners
            _currentUser.value = null
            _partnerUser.value = null
            return
        }

        val local = repository.getLocalProfile()
        if (local != null && local.userId == authUser.uid) {
            _currentUser.value = local
            listenToUserUpdates(authUser.uid)
            if (local.isPaired && !local.partnerId.isNullOrEmpty()) {
                listenToPartnerUpdates(local.partnerId)
            }
        } else {
            // User authenticated in Firebase Auth; listen directly to Firestore
            listenToUserUpdates(authUser.uid)
        }
    }

    fun toggleAuthMode() {
        _isSignUpMode.value = !_isSignUpMode.value
        _authError.value = null
    }

    fun setSignUpMode(isSignUp: Boolean) {
        _isSignUpMode.value = isSignUp
        _authError.value = null
    }

    fun signUp(
        email: String,
        password: String,
        confirmPassword: String,
        name: String,
        birthDate: String,
        avatarPreset: String,
        avatarBase64: String?
    ) {
        if (name.isBlank()) {
            _authError.value = "Lütfen isminizi girin."
            return
        }
        if (birthDate.isBlank()) {
            _authError.value = "Lütfen doğum tarihinizi seçin."
            return
        }
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authError.value = "Lütfen geçerli bir e-posta adresi girin."
            return
        }
        if (password.length < 6) {
            _authError.value = "Şifre en az 6 karakter olmalıdır."
            return
        }
        if (password != confirmPassword) {
            _authError.value = "Şifreler birbiriyle uyuşmuyor."
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null

            val result = repository.signUp(
                email = email,
                pass = password,
                name = name,
                birthDate = birthDate,
                avatarPreset = avatarPreset,
                avatarBase64 = avatarBase64
            )

            _isAuthLoading.value = false
            result.onSuccess { profile ->
                _currentUser.value = profile
                listenToUserUpdates(profile.userId)
            }.onFailure { error ->
                _authError.value = error.message ?: "Hesap oluşturulamadı."
            }
        }
    }

    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Lütfen e-posta ve şifrenizi girin."
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null

            val result = repository.signIn(email, pass)
            _isAuthLoading.value = false

            result.onSuccess { profile ->
                _currentUser.value = profile
                listenToUserUpdates(profile.userId)
                if (profile.isPaired && !profile.partnerId.isNullOrEmpty()) {
                    listenToPartnerUpdates(profile.partnerId)
                }
            }.onFailure { error ->
                _authError.value = error.message ?: "Giriş yapılamadı."
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null

            val result = repository.signInWithGoogle(context)
            _isAuthLoading.value = false

            result.onSuccess { outcome ->
                when (outcome) {
                    is GoogleSignInOutcome.Success -> {
                        _currentUser.value = outcome.profile
                        listenToUserUpdates(outcome.profile.userId)
                        if (outcome.profile.isPaired && !outcome.profile.partnerId.isNullOrEmpty()) {
                            listenToPartnerUpdates(outcome.profile.partnerId)
                        }
                    }
                    is GoogleSignInOutcome.NeedsProfileCompletion -> {
                        _profileCompletionData.value = outcome
                    }
                }
            }.onFailure { error ->
                _authError.value = error.message ?: "Google ile giriş başarısız oldu."
            }
        }
    }

    fun completeGoogleProfile(
        name: String,
        birthDate: String,
        avatarPreset: String,
        avatarBase64: String?
    ) {
        val completion = _profileCompletionData.value ?: return

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null

            val result = repository.completeGoogleProfile(
                uid = completion.uid,
                name = name,
                birthDate = birthDate,
                email = completion.email,
                avatarPreset = avatarPreset,
                avatarBase64 = avatarBase64
            )

            _isAuthLoading.value = false
            result.onSuccess { profile ->
                _profileCompletionData.value = null
                _currentUser.value = profile
                listenToUserUpdates(profile.userId)
            }.onFailure { error ->
                _authError.value = error.message ?: "Profil kaydedilemedi."
            }
        }
    }

    fun dismissProfileCompletion() {
        _profileCompletionData.value = null
        signOut()
    }

    private fun listenToUserUpdates(userId: String) {
        currentUserJob?.cancel()
        currentUserJob = viewModelScope.launch {
            repository.observeCurrentUser(userId).collectLatest { updated ->
                if (updated != null) {
                    val wasPaired = _currentUser.value?.isPaired == true
                    _currentUser.value = updated

                    if (updated.isPaired && !updated.partnerId.isNullOrEmpty()) {
                        listenToPartnerUpdates(updated.partnerId)
                    } else if (wasPaired && !updated.isPaired) {
                        partnerJob?.cancel()
                        _partnerUser.value = null
                    }
                }
            }
        }
    }

    private fun listenToPartnerUpdates(partnerId: String) {
        partnerJob?.cancel()
        partnerJob = viewModelScope.launch {
            repository.observePartner(partnerId).collectLatest { partner ->
                _partnerUser.value = partner
            }
        }
    }

    fun setPairingInput(code: String) {
        _pairingCodeInput.value = code.uppercase().filter { it.isLetterOrDigit() }.take(8)
        _pairingError.value = null
    }

    fun pairWithPartner() {
        val user = _currentUser.value ?: return
        val targetCode = _pairingCodeInput.value.trim()

        if (targetCode.isEmpty()) {
            _pairingError.value = "Lütfen sevgilinizin eşleşme kodunu girin."
            return
        }

        viewModelScope.launch {
            _isPairingInProgress.value = true
            _pairingError.value = null
            _pairingSuccessMessage.value = null

            val result = repository.pairWithCode(user.userId, targetCode)
            _isPairingInProgress.value = false

            when (result) {
                is PairingResult.Success -> {
                    _pairingSuccessMessage.value = "${result.partnerName} ile başarıyla eşleştiniz! 💖"
                    _pairingCodeInput.value = ""
                    val updated = repository.getLocalProfile()
                    if (updated != null) {
                        _currentUser.value = updated
                        if (updated.partnerId != null) {
                            listenToPartnerUpdates(updated.partnerId)
                        }
                    }
                }
                is PairingResult.Error -> {
                    _pairingError.value = result.message
                }
            }
        }
    }

    fun unpair() {
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            _isPairingInProgress.value = true
            _showUnpairConfirmDialog.value = false
            _showSettingsDialog.value = false

            val result = repository.unpair(user)
            _isPairingInProgress.value = false

            result.onSuccess {
                partnerJob?.cancel()
                _partnerUser.value = null
                val updated = repository.getLocalProfile()
                _currentUser.value = updated
            }.onFailure { error ->
                _pairingError.value = error.message
            }
        }
    }

    fun openSettings() {
        _showSettingsDialog.value = true
    }

    fun closeSettings() {
        _showSettingsDialog.value = false
    }

    fun openUnpairConfirm() {
        _showUnpairConfirmDialog.value = true
    }

    fun closeUnpairConfirm() {
        _showUnpairConfirmDialog.value = false
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearPairingMessages() {
        _pairingError.value = null
        _pairingSuccessMessage.value = null
    }

    fun signOut() {
        currentUserJob?.cancel()
        partnerJob?.cancel()
        repository.signOut()
        _currentUser.value = null
        _partnerUser.value = null
        _profileCompletionData.value = null
        _showSettingsDialog.value = false
        _showUnpairConfirmDialog.value = false
    }
}
