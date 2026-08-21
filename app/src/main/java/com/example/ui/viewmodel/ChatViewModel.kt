package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CoupleDataRepository
import com.example.data.R2StorageRepository
import com.example.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val coupleRepository = CoupleDataRepository(application.applicationContext)
    private val r2StorageRepository = R2StorageRepository(application.applicationContext)

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    private val _coupleId = MutableStateFlow("")

    val unreadCount: StateFlow<Int> = _chatMessages.map { msgs ->
        val uid = _currentUserId.value
        if (uid.isBlank()) 0
        else msgs.count { !it.isDeleted && !it.isRead && (it.receiverId == uid || (it.senderId != uid && it.senderId != "me")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun initializeChat(userId: String, partnerId: String) {
        _currentUserId.value = userId
        val cid = coupleRepository.getCoupleDocId(userId, partnerId)
        _coupleId.value = cid

        viewModelScope.launch {
            coupleRepository.observeChatMessages(cid).collectLatest { msgs ->
                _chatMessages.value = msgs
            }
        }
    }

    fun sendMessage(text: String, senderName: String, imageUri: Uri? = null, replyTo: ChatMessage? = null) {
        val cid = _coupleId.value
        val uid = _currentUserId.value
        if (cid.isBlank() || uid.isBlank()) return

        val msgId = UUID.randomUUID().toString()
        val localPreview = imageUri?.toString()
        val hasPhoto = (imageUri != null)
        val tempMsg = ChatMessage(
            id = msgId,
            senderId = uid,
            senderName = senderName,
            text = text,
            isPhoto = hasPhoto,
            mediaUrl = localPreview,
            imageUrl = localPreview,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            replyToText = replyTo?.text?.take(80),
            replyToSenderName = replyTo?.senderName,
            replyToId = replyTo?.id
        )

        _chatMessages.value = _chatMessages.value + tempMsg

        viewModelScope.launch {
            var mediaUrl: String? = null
            if (imageUri != null) {
                val uploadRes = r2StorageRepository.uploadImageUri(imageUri, "ikimiz-media/chat_photos")
                mediaUrl = uploadRes.getOrNull() ?: localPreview
            }
            val finalMsg = tempMsg.copy(isPhoto = (mediaUrl != null), mediaUrl = mediaUrl, imageUrl = mediaUrl)
            _chatMessages.value = _chatMessages.value.map { if (it.id == msgId) finalMsg else it }
            coupleRepository.sendChatMessage(cid, finalMsg)
        }
    }

    fun deleteMessage(messageId: String) {
        val cid = _coupleId.value
        _chatMessages.value = _chatMessages.value.map { msg ->
            if (msg.id == messageId) msg.copy(isDeleted = true, text = "", mediaUrl = null, imageUrl = null)
            else msg
        }
        if (cid.isNotBlank()) {
            viewModelScope.launch {
                coupleRepository.deleteChatMessage(cid, messageId)
            }
        }
    }

    fun markAsRead() {
        val cid = _coupleId.value
        val uid = _currentUserId.value
        if (cid.isNotBlank() && uid.isNotBlank()) {
            viewModelScope.launch {
                coupleRepository.markMessagesAsRead(cid, uid)
            }
        }
    }
}
