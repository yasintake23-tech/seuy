package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.CoupleDataRepository
import com.example.data.ProfileRepository
import com.example.data.R2StorageRepository
import com.example.model.BottomNavTab
import com.example.model.BucketItem
import com.example.model.ChatMessage
import com.example.model.CoupleMemory
import com.example.model.DailyCoupleQuestion
import com.example.model.MemoryPin
import com.example.model.PairingResult
import com.example.model.PartnerStatus
import com.example.model.SecretLoveNote
import com.example.model.UserProfile
import com.example.service.CoupleMessageForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.util.NotificationHelper

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application.applicationContext)
    private val coupleRepository = CoupleDataRepository(application.applicationContext)
    private val r2StorageRepository = R2StorageRepository(application.applicationContext)
    private val profileRepository = ProfileRepository(application.applicationContext, authRepository, r2StorageRepository)
    private val prefs = application.getSharedPreferences("ikimiz_prefs", android.content.Context.MODE_PRIVATE)

    private val notifiedMessageIds = mutableSetOf<String>()
    private var isFirstChatLoad = true

    // Double Tap Reaction Emoji Preference (Default: 🤍)
    private val _doubleTapEmoji = MutableStateFlow(prefs.getString("double_tap_emoji", "🤍") ?: "🤍")
    val doubleTapEmoji: StateFlow<String> = _doubleTapEmoji.asStateFlow()

    fun setDoubleTapEmoji(emoji: String) {
        _doubleTapEmoji.value = emoji
        prefs.edit().putString("double_tap_emoji", emoji).apply()
    }

    // User & Partner State
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _partnerUser = MutableStateFlow<UserProfile?>(null)
    val partnerUser: StateFlow<UserProfile?> = _partnerUser.asStateFlow()

    // Auth State
    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isSignUpMode = MutableStateFlow(true)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    // Pairing State
    private val _pairingCodeInput = MutableStateFlow("")
    val pairingCodeInput: StateFlow<String> = _pairingCodeInput.asStateFlow()

    private val _isPairingInProgress = MutableStateFlow(false)
    val isPairingInProgress: StateFlow<Boolean> = _isPairingInProgress.asStateFlow()

    private val _pairingError = MutableStateFlow<String?>(null)
    val pairingError: StateFlow<String?> = _pairingError.asStateFlow()

    private val _pairingSuccessMessage = MutableStateFlow<String?>(null)
    val pairingSuccessMessage: StateFlow<String?> = _pairingSuccessMessage.asStateFlow()

    // Dialogs State
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showUnpairConfirmDialog = MutableStateFlow(false)
    val showUnpairConfirmDialog: StateFlow<Boolean> = _showUnpairConfirmDialog.asStateFlow()

    // ----------------------------------------------------
    // BOTTOM NAVIGATION & LIVE DATA STATE (NO MOCK DATA)
    // ----------------------------------------------------
    private val _currentTab = MutableStateFlow(BottomNavTab.HOME)
    val currentTab: StateFlow<BottomNavTab> = _currentTab.asStateFlow()

    private val _isDataLoading = MutableStateFlow(false)
    val isDataLoading: StateFlow<Boolean> = _isDataLoading.asStateFlow()

    // Games & Activities
    private val _bucketList = MutableStateFlow<List<BucketItem>>(emptyList())
    val bucketList: StateFlow<List<BucketItem>> = _bucketList.asStateFlow()

    private val _secretNotes = MutableStateFlow<List<SecretLoveNote>>(emptyList())
    val secretNotes: StateFlow<List<SecretLoveNote>> = _secretNotes.asStateFlow()

    private val _dailyQuestions = MutableStateFlow<List<DailyCoupleQuestion>>(emptyList())
    val dailyQuestions: StateFlow<List<DailyCoupleQuestion>> = _dailyQuestions.asStateFlow()

    // Couple Map & Status
    private val _myStatus = MutableStateFlow(PartnerStatus(statusType = "Evde", statusEmoji = "🏡", statusNote = "Bağlı"))
    val myStatus: StateFlow<PartnerStatus> = _myStatus.asStateFlow()

    private val _partnerStatus = MutableStateFlow(PartnerStatus(statusType = "Aktif", statusEmoji = "✨", statusNote = "Çevrimiçi"))
    val partnerStatus: StateFlow<PartnerStatus> = _partnerStatus.asStateFlow()

    private val _memoryPins = MutableStateFlow<List<MemoryPin>>(emptyList())
    val memoryPins: StateFlow<List<MemoryPin>> = _memoryPins.asStateFlow()

    // DM / Realtime Database Chat
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    val unreadMessageCount: StateFlow<Int> = _chatMessages.map { msgs ->
        val currentUid = _currentUser.value?.userId ?: ""
        if (currentUid.isBlank()) 0
        else {
            msgs.count { msg ->
                !msg.isDeleted && !msg.isRead && (msg.receiverId == currentUid || (msg.senderId != currentUid && msg.senderId != "me"))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    // Couple Gallery & Memories
    private val _coupleMemories = MutableStateFlow<List<CoupleMemory>>(emptyList())
    val coupleMemories: StateFlow<List<CoupleMemory>> = _coupleMemories.asStateFlow()

    private var currentUserJob: Job? = null
    private var partnerJob: Job? = null
    private var liveDataJob: Job? = null

    init {
        checkExistingSession()
    }

    fun selectTab(tab: BottomNavTab) {
        if (_currentTab.value == BottomNavTab.CHAT && tab != BottomNavTab.CHAT) {
            setTyping(false)
        }
        _currentTab.value = tab
        if (tab == BottomNavTab.CHAT) {
            markChatAsRead()
        }
    }

    private fun checkExistingSession() {
        val authUser = authRepository.getCurrentFirebaseUser()
        if (authUser == null) {
            _currentUser.value = null
            _partnerUser.value = null
            return
        }

        val local = authRepository.getLocalProfile()
        if (local != null && local.userId == authUser.uid) {
            _currentUser.value = local
            listenToUserUpdates(authUser.uid)
            if (local.isPaired && !local.partnerId.isNullOrEmpty()) {
                listenToPartnerUpdates(local.partnerId)
                startObservingLiveData(local.userId, local.partnerId)
            }
        } else {
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

            val result = authRepository.signUp(
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

            val result = authRepository.signIn(email, pass)
            _isAuthLoading.value = false

            result.onSuccess { profile ->
                _currentUser.value = profile
                listenToUserUpdates(profile.userId)
                if (profile.isPaired && !profile.partnerId.isNullOrEmpty()) {
                    listenToPartnerUpdates(profile.partnerId)
                    startObservingLiveData(profile.userId, profile.partnerId)
                }
            }.onFailure { error ->
                _authError.value = error.message ?: "Giriş yapılamadı."
            }
        }
    }

    private fun listenToUserUpdates(userId: String) {
        currentUserJob?.cancel()
        currentUserJob = viewModelScope.launch {
            authRepository.observeCurrentUser(userId).collectLatest { updated ->
                if (updated != null) {
                    val wasPaired = _currentUser.value?.isPaired == true
                    _currentUser.value = updated

                    if (updated.isPaired && !updated.partnerId.isNullOrEmpty()) {
                        listenToPartnerUpdates(updated.partnerId)
                        startObservingLiveData(updated.userId, updated.partnerId)
                    } else if (wasPaired && !updated.isPaired) {
                        partnerJob?.cancel()
                        liveDataJob?.cancel()
                        _partnerUser.value = null
                    }
                }
            }
        }
    }

    private fun listenToPartnerUpdates(partnerId: String) {
        partnerJob?.cancel()
        partnerJob = viewModelScope.launch {
            authRepository.observePartner(partnerId).collectLatest { partner ->
                _partnerUser.value = partner
            }
        }
    }

    private fun getDeletedMessageIds(): Set<String> {
        return prefs.getStringSet("deleted_msg_ids_set", emptySet()) ?: emptySet()
    }

    private fun markMessageIdAsDeletedLocally(id: String) {
        val current = getDeletedMessageIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("deleted_msg_ids_set", current).apply()
    }

    private fun startObservingLiveData(uid1: String, uid2: String) {
        liveDataJob?.cancel()
        val coupleId = coupleRepository.getCoupleDocId(uid1, uid2)
        _isDataLoading.value = true

        // Start background synchronization service to ensure real-time notifications even when app is closed/minimized
        try {
            CoupleMessageForegroundService.start(getApplication(), uid1, uid2)
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "Could not start CoupleMessageForegroundService: ${e.message}")
        }

        liveDataJob = viewModelScope.launch {
            // 1. Live Chat Messages (Firestore + Realtime DB)
            launch {
                coupleRepository.observeChatMessages(coupleId).collectLatest { msgs ->
                    val locallyDeleted = getDeletedMessageIds()
                    val processed = msgs.map { m ->
                        if (m.isDeleted || locallyDeleted.contains(m.id)) {
                            m.copy(isDeleted = true, text = "", imageUrl = null, reactionEmoji = null)
                        } else {
                            m
                        }
                    }

                    // Background & Out-of-chat notification for incoming messages from partner
                    if (isFirstChatLoad) {
                        processed.forEach { m -> notifiedMessageIds.add(m.id) }
                        isFirstChatLoad = false
                    } else {
                        processed.forEach { m ->
                            if ((m.senderId == uid2 || (m.senderId != uid1 && m.senderId != "me")) &&
                                !notifiedMessageIds.contains(m.id) &&
                                !m.isDeleted
                            ) {
                                notifiedMessageIds.add(m.id)
                                if (_currentTab.value != BottomNavTab.CHAT) {
                                    NotificationHelper.showChatNotification(
                                        context = getApplication(),
                                        senderName = _partnerUser.value?.displayName ?: "Sevgilin",
                                        messageText = m.text,
                                        messageId = m.id,
                                        imageUrl = m.imageUrl
                                    )
                                }
                            }
                        }
                    }

                    _chatMessages.value = processed
                    _isDataLoading.value = false

                    // If currently on Chat tab and there are incoming unread messages, mark them as read
                    if (_currentTab.value == BottomNavTab.CHAT && processed.any { it.receiverId == uid1 && !it.isRead }) {
                        coupleRepository.markMessagesAsRead(coupleId, uid1)
                    }
                }
            }
            // 1.1 Partner Typing Status (/chats/{coupleId}/typing/{partnerId})
            launch {
                coupleRepository.observePartnerTyping(coupleId, uid2).collectLatest { typing ->
                    _isPartnerTyping.value = typing
                }
            }
            // 2. Bucket List
            launch {
                coupleRepository.observeBucketList(coupleId).collectLatest { items ->
                    _bucketList.value = items
                }
            }
            // 3. Secret Notes
            launch {
                coupleRepository.observeSecretNotes(coupleId).collectLatest { notes ->
                    _secretNotes.value = notes
                }
            }
            // 4. Memory Pins & Map
            launch {
                coupleRepository.observeMemoryPins(coupleId).collectLatest { pins ->
                    _memoryPins.value = pins
                }
            }
            // 5. Couple Memories & Photos
            launch {
                coupleRepository.observeCoupleMemories(coupleId).collectLatest { memories ->
                    _coupleMemories.value = memories
                }
            }
            // 6. Daily Questions
            launch {
                coupleRepository.observeDailyQuestions(coupleId).collectLatest { questions ->
                    _dailyQuestions.value = questions
                }
            }
            // 7. Partner Status
            launch {
                coupleRepository.observePartnerStatus(coupleId, uid2).collectLatest { status ->
                    if (status != null) {
                        _partnerStatus.value = status
                    }
                }
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

            val result = authRepository.pairWithCode(user.userId, targetCode)
            _isPairingInProgress.value = false

            when (result) {
                is PairingResult.Success -> {
                    _pairingSuccessMessage.value = "${result.partnerName} ile başarıyla eşleştiniz! 💖"
                    _pairingCodeInput.value = ""
                    val updated = authRepository.getLocalProfile()
                    if (updated != null) {
                        _currentUser.value = updated
                        if (updated.partnerId != null) {
                            listenToPartnerUpdates(updated.partnerId)
                            startObservingLiveData(updated.userId, updated.partnerId)
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

            val result = authRepository.unpair(user)
            _isPairingInProgress.value = false

            result.onSuccess {
                CoupleMessageForegroundService.stop(getApplication())
                partnerJob?.cancel()
                liveDataJob?.cancel()
                _partnerUser.value = null
                val updated = authRepository.getLocalProfile()
                _currentUser.value = updated
                _currentTab.value = BottomNavTab.HOME
            }.onFailure { error ->
                _pairingError.value = error.message
            }
        }
    }

    // ----------------------------------------------------
    // TAB ACTIONS (LIVE DATABASE WRITES)
    // ----------------------------------------------------

    fun toggleBucketItem(id: String) {
        val item = _bucketList.value.find { it.id == id } ?: return
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)

        val newStatus = !item.isCompleted
        val updated = item.copy(
            isCompleted = newStatus,
            completedAt = if (newStatus) System.currentTimeMillis() else null
        )

        viewModelScope.launch {
            coupleRepository.saveBucketItem(coupleId, updated)
        }
    }

    fun addBucketItem(title: String, category: String) {
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)

        val newItem = BucketItem(
            id = UUID.randomUUID().toString(),
            title = title,
            category = category,
            isCompleted = false,
            addedByName = user.displayName,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            coupleRepository.saveBucketItem(coupleId, newItem)
        }
    }

    fun unlockSecretNote(id: String) {
        val note = _secretNotes.value.find { it.id == id } ?: return
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)

        val updated = note.copy(isUnlocked = true)
        viewModelScope.launch {
            coupleRepository.saveSecretNote(coupleId, updated)
        }
    }

    fun addSecretNote(title: String, content: String, unlockCondition: String) {
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)

        val newNote = SecretLoveNote(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            unlockCondition = unlockCondition,
            isUnlocked = false,
            authorName = user.displayName,
            iconEmoji = "💌",
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            coupleRepository.saveSecretNote(coupleId, newNote)
        }
    }

    fun answerDailyQuestion(questionId: String, answer: String) {
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)

        val existing = _dailyQuestions.value.find { it.id == questionId }
        val updated = existing?.copy(myAnswer = answer) ?: DailyCoupleQuestion(
            id = questionId,
            question = "Günün Sorusu ✨",
            myAnswer = answer,
            date = "Bugün"
        )

        viewModelScope.launch {
            coupleRepository.saveDailyQuestionAnswer(coupleId, updated)
        }
    }

    fun updateMyStatus(type: String, emoji: String, note: String) {
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)

        val newStatus = PartnerStatus(
            userId = user.userId,
            statusType = type,
            statusEmoji = emoji,
            statusNote = note,
            updatedAt = System.currentTimeMillis()
        )
        _myStatus.value = newStatus

        viewModelScope.launch {
            coupleRepository.updateMyStatus(coupleId, user.userId, newStatus)
        }
    }

    fun addMemoryPin(title: String, locationName: String, category: String, date: String, note: String) {
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)

        val randomX = (0.2f + Math.random().toFloat() * 0.6f)
        val randomY = (0.2f + Math.random().toFloat() * 0.6f)
        val emoji = when (category) {
            "Kafe", "Kafe ☕" -> "☕"
            "Doğa", "Doğa 🌊" -> "🌊"
            "Restoran", "Restoran 🍕" -> "🍕"
            else -> "✨"
        }
        val newPin = MemoryPin(
            id = UUID.randomUUID().toString(),
            title = title,
            locationName = locationName,
            category = category,
            date = date,
            note = note,
            iconEmoji = emoji,
            posX = randomX,
            posY = randomY,
            addedByName = user.displayName,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            coupleRepository.saveMemoryPin(coupleId, newPin)
        }
    }

    fun sendChatMessage(text: String, imageUri: Uri? = null, replyToMessage: ChatMessage? = null) {
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: "partner"
        val coupleId = if (user.partnerId != null) coupleRepository.getCoupleDocId(user.userId, user.partnerId!!) else "couple_${user.userId}"

        val newMsgId = UUID.randomUUID().toString()
        val base64Compressed = if (imageUri != null) r2StorageRepository.compressUriToBase64(imageUri, 600, 600, 75) else null
        val localPreview = base64Compressed ?: imageUri?.toString()
        val hasPhoto = (imageUri != null)

        val tempMsg = ChatMessage(
            id = newMsgId,
            senderId = user.userId,
            receiverId = partnerId,
            senderName = user.displayName,
            text = text,
            isPhoto = hasPhoto,
            mediaUrl = localPreview,
            imageUrl = localPreview,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            replyToText = if (replyToMessage?.isDeleted == true) "🚫 Bu mesaj silindi" else replyToMessage?.text?.take(80),
            replyToSenderName = replyToMessage?.senderName,
            replyToId = replyToMessage?.id
        )

        // Optimistic append - instant rendering in chat
        _chatMessages.value = _chatMessages.value + tempMsg

        viewModelScope.launch(Dispatchers.IO) {
            var mediaUrl: String? = base64Compressed
            if (imageUri != null) {
                // If base64 wasn't generated synchronously, generate it now
                if (mediaUrl.isNullOrBlank()) {
                    mediaUrl = r2StorageRepository.compressUriToBase64(imageUri, 600, 600, 75)
                }
                
                // Optional R2 cloud backup in background if configured
                try {
                    val uploadRes = r2StorageRepository.uploadImageUri(imageUri, "ikimiz-media/chat_photos")
                    val uploaded = uploadRes.getOrNull()
                    // If cloud storage returned a permanent public or valid URL, prioritize it if not expiring
                    if (!uploaded.isNullOrBlank() && !uploaded.startsWith("data:image") && !uploaded.contains("X-Amz-Expires")) {
                        mediaUrl = uploaded
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "Optional R2 upload skipped/failed: ${e.message}")
                }
            }

            val finalMsg = tempMsg.copy(
                isPhoto = (mediaUrl != null),
                mediaUrl = mediaUrl,
                imageUrl = mediaUrl
            )
            // Update in local state with persistent media URL
            _chatMessages.value = _chatMessages.value.map { if (it.id == newMsgId) finalMsg else it }
            coupleRepository.sendChatMessage(coupleId, finalMsg)
            setTyping(false)
        }
    }

    fun setTyping(isTyping: Boolean) {
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)
        viewModelScope.launch {
            coupleRepository.setTypingStatus(coupleId, user.userId, isTyping)
        }
    }

    fun markChatAsRead() {
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)
        viewModelScope.launch {
            coupleRepository.markMessagesAsRead(coupleId, user.userId)
        }
    }

    fun deleteChatMessage(messageId: String) {
        markMessageIdAsDeletedLocally(messageId)

        // Optimistic update immediately in local state
        _chatMessages.value = _chatMessages.value.map { msg ->
            if (msg.id == messageId) {
                msg.copy(
                    isDeleted = true,
                    text = "",
                    imageUrl = null,
                    reactionEmoji = null
                )
            } else {
                msg
            }
        }

        val user = _currentUser.value ?: return
        val partnerId = user.partnerId
        val coupleId = if (partnerId != null) coupleRepository.getCoupleDocId(user.userId, partnerId) else "couple_${user.userId}"

        viewModelScope.launch {
            coupleRepository.deleteChatMessage(coupleId, messageId)
        }
    }

    fun editChatMessage(messageId: String, newText: String) {
        _chatMessages.value = _chatMessages.value.map { msg ->
            if (msg.id == messageId) {
                msg.copy(text = newText, isEdited = true)
            } else {
                msg
            }
        }

        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)

        viewModelScope.launch {
            coupleRepository.editChatMessage(coupleId, messageId, newText)
        }
    }

    fun reactToChatMessage(messageId: String, emoji: String?) {
        val currentMsg = _chatMessages.value.find { it.id == messageId }
        val finalEmoji = if (currentMsg?.reactionEmoji == emoji && emoji != null) null else emoji

        _chatMessages.value = _chatMessages.value.map { msg ->
            if (msg.id == messageId) {
                msg.copy(reactionEmoji = finalEmoji)
            } else {
                msg
            }
        }

        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)

        viewModelScope.launch {
            coupleRepository.reactToChatMessage(coupleId, messageId, finalEmoji)
        }
    }

    fun updateProfilePhoto(imageUri: Uri) {
        val user = _currentUser.value ?: return

        // 1. INSTANT ZERO-LATENCY UPDATE: Compress to base64 immediately for 0ms visual update
        val compressedBase64 = r2StorageRepository.compressUriToBase64(imageUri, 500, 500, 80)
        val localPreview = compressedBase64 ?: imageUri.toString()
        val optimisticUser = user.copy(
            avatarBase64 = localPreview,
            profileImageUrl = localPreview,
            avatarPreset = "custom"
        )
        _currentUser.value = optimisticUser
        authRepository.saveLocalProfile(optimisticUser)

        viewModelScope.launch(Dispatchers.IO) {
            _isDataLoading.value = true
            val result = profileRepository.updateProfilePhoto(user.userId, imageUri)
            result.onSuccess { publicUrl ->
                val updatedUser = optimisticUser.copy(
                    avatarBase64 = publicUrl,
                    profileImageUrl = publicUrl,
                    avatarPreset = "custom"
                )
                _currentUser.value = updatedUser
                authRepository.saveLocalProfile(updatedUser)
            }
            _isDataLoading.value = false
        }
    }

    fun updateProfilePreset(preset: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            profileRepository.updateProfilePreset(user.userId, preset)
            _currentUser.value = _currentUser.value?.copy(
                avatarPreset = preset,
                avatarBase64 = null,
                profileImageUrl = null
            )
        }
    }

    fun addCoupleMemory(
        title: String,
        caption: String,
        location: String,
        date: String,
        preset: String,
        imageBase64: String?,
        imageUri: Uri? = null
    ) {
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)

        viewModelScope.launch {
            var publicUrl: String? = null
            if (imageUri != null) {
                val uploadRes = r2StorageRepository.uploadImageUri(imageUri, "memories")
                publicUrl = uploadRes.getOrNull()
            }

            val newMemory = CoupleMemory(
                id = UUID.randomUUID().toString(),
                title = title,
                caption = caption,
                location = location,
                date = date,
                imagePreset = preset,
                imageBase64 = imageBase64,
                imageUrl = publicUrl,
                likesCount = 1,
                isLikedByMe = true,
                authorName = user.displayName,
                createdAt = System.currentTimeMillis()
            )

            coupleRepository.saveCoupleMemory(coupleId, newMemory)
        }
    }

    fun toggleLikeMemory(memoryId: String) {
        val memory = _coupleMemories.value.find { it.id == memoryId } ?: return
        val user = _currentUser.value ?: return
        val partnerId = user.partnerId ?: return
        val coupleId = coupleRepository.getCoupleDocId(user.userId, partnerId)

        val newLiked = !memory.isLikedByMe
        val updated = memory.copy(
            isLikedByMe = newLiked,
            likesCount = if (newLiked) memory.likesCount + 1 else (memory.likesCount - 1).coerceAtLeast(0)
        )

        viewModelScope.launch {
            coupleRepository.saveCoupleMemory(coupleId, updated)
        }
    }

    // Dialog Controls
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
        CoupleMessageForegroundService.stop(getApplication())
        currentUserJob?.cancel()
        partnerJob?.cancel()
        liveDataJob?.cancel()
        authRepository.signOut()
        _currentUser.value = null
        _partnerUser.value = null
        _showSettingsDialog.value = false
        _showUnpairConfirmDialog.value = false
        _currentTab.value = BottomNavTab.HOME
    }
}
