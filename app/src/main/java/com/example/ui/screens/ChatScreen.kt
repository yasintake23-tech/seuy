package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.CircularProgressIndicator
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.model.ChatMessage
import com.example.model.UserProfile
import com.example.ui.components.AvatarImage
import com.example.ui.theme.BorderSoft
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SlateNavy
import com.example.ui.theme.SoftCoralContainer
import com.example.ui.theme.SoftCoralDark
import com.example.ui.theme.SoftCoralPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarmCreamBackground
import com.example.ui.theme.WarmCreamContainer
import com.example.ui.theme.WarmCreamSurface
import com.example.ui.theme.WarmGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentUser: UserProfile,
    partnerUser: UserProfile?,
    messages: List<ChatMessage>,
    doubleTapEmoji: String = "🤍",
    isPartnerTyping: Boolean = false,
    onTypingChanged: (Boolean) -> Unit = {},
    onMarkAsRead: () -> Unit = {},
    onSetDoubleTapEmoji: (String) -> Unit = {},
    onSendMessage: (String, Uri?, ChatMessage?) -> Unit,
    onDeleteMessage: (String) -> Unit = {},
    onEditMessage: (String, String) -> Unit = { _, _ -> },
    onReactMessage: (String, String?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var typingDebounceJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var menuMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    var fullPhotoPreviewUrl by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val partnerName = partnerUser?.displayName ?: currentUser.partnerName ?: "Sevgilin"

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val quickReactions = listOf(
        "❤️ Seni seviyorum",
        "🌹 Çok güzelsin",
        "💌 Çok özledim",
        "☕ Kahve içelim mi?",
        "🌙 İyi geceler biriciğim",
        "🤗 Sarılalım mı?"
    )

    val doubleTapEmojiOptions = listOf("🤍", "❤️", "💖", "🥰", "✨", "🔥", "🌸", "🧸", "🐾")

    val showScrollToBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            messages.size > 4 && lastVisible < messages.size - 2
        }
    }

    // Mark messages as read on entry
    LaunchedEffect(Unit) {
        onMarkAsRead()
    }

    // Auto scroll to bottom and mark read on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            onMarkAsRead()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(WarmCreamBackground)
    ) {
        // Chat Header
        Card(
            colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
            shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSoft, RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarImage(
                    preset = partnerUser?.avatarPreset ?: "flower_pink",
                    base64 = partnerUser?.avatarBase64,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = partnerName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = DeepCharcoal
                    )
                    if (isPartnerTyping) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SoftCoralPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Yazıyor...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftCoralPrimary
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SageGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Özel Çift Alanı • Çift Tıklama: $doubleTapEmoji",
                                fontSize = 11.sp,
                                color = SlateNavy
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = SoftCoralPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Messages List Container with Scroll to Bottom Button
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SoftCoralContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "💌", fontSize = 32.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Henüz mesaj yok",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepCharcoal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sevgiline ilk tatlı mesajını veya fotoğrafını göndererek sohbeti başlat 💖\n(İki kez dokunarak $doubleTapEmoji atabilirsin)",
                            fontSize = 13.sp,
                            color = SlateNavy,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == "me" || msg.senderId == currentUser.userId

                        SwipeableMessageItem(
                            msg = msg,
                            isMe = isMe,
                            isHighlighted = (msg.id == highlightedMessageId),
                            doubleTapEmoji = doubleTapEmoji,
                            partnerPreset = partnerUser?.avatarPreset ?: "flower_pink",
                            partnerBase64 = partnerUser?.avatarBase64,
                            onDoubleTap = {
                                if (!msg.isDeleted) {
                                    val newReaction = if (msg.reactionEmoji == doubleTapEmoji) null else doubleTapEmoji
                                    onReactMessage(msg.id, newReaction)
                                }
                            },
                            onReply = {
                                if (!msg.isDeleted) {
                                    replyingToMessage = msg
                                    editingMessage = null
                                }
                            },
                            onReplyClick = { replyId ->
                                val targetIndex = messages.indexOfFirst { it.id == replyId }
                                if (targetIndex != -1) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(targetIndex)
                                        highlightedMessageId = replyId
                                        kotlinx.coroutines.delay(2000)
                                        if (highlightedMessageId == replyId) {
                                            highlightedMessageId = null
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Orijinal mesaja ulaşılamadı", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onPhotoClick = { url ->
                                fullPhotoPreviewUrl = url
                            },
                            onReactionPillClick = {
                                if (!msg.isDeleted) {
                                    onReactMessage(msg.id, null)
                                    Toast.makeText(context, "Tepki kaldırıldı", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onLongClick = {
                                menuMessage = msg
                            }
                        )
                    }
                }

                // Scroll To Bottom Floating Action Button
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        containerColor = SoftCoralPrimary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Aşağı Kaydır",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Live Partner Typing Bubble at Bottom
        AnimatedVisibility(
            visible = isPartnerTyping,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarImage(
                    preset = partnerUser?.avatarPreset ?: "flower_pink",
                    base64 = partnerUser?.avatarBase64,
                    size = 26.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                TypingDotsIndicator()
            }
        }

        // Selected Image Preview Thumbnail
        if (selectedImageUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmCreamSurface)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Seçilen fotoğraf",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Fotoğraf eklendi ✨",
                    fontSize = 12.sp,
                    color = SoftCoralDark,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Kaldır", tint = SlateNavy, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Replying To Message Banner
        AnimatedVisibility(
            visible = replyingToMessage != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            replyingToMessage?.let { replyMsg ->
                val senderTitle = if (replyMsg.senderId == currentUser.userId || replyMsg.senderId == "me") "Sen" else partnerName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmCreamContainer)
                        .border(1.dp, SoftCoralPrimary.copy(alpha = 0.3f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(32.dp)
                            .background(SoftCoralPrimary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Yanıtlanıyor: $senderTitle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftCoralDark
                        )
                        Text(
                            text = if (replyMsg.isDeleted) "🚫 Bu mesaj silindi" else replyMsg.text.ifBlank { "📷 Fotoğraf" },
                            fontSize = 12.sp,
                            color = DeepCharcoal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { replyingToMessage = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "İptal", tint = SlateNavy, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Editing Message Banner
        AnimatedVisibility(
            visible = editingMessage != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            editingMessage?.let { editMsg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmGold.copy(alpha = 0.15f))
                        .border(1.dp, WarmGold.copy(alpha = 0.4f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = DeepCharcoal, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mesajı Düzenliyorsun",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepCharcoal
                        )
                        Text(
                            text = editMsg.text,
                            fontSize = 12.sp,
                            color = SlateNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            editingMessage = null
                            inputText = ""
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "İptal", tint = SlateNavy, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Selected Photo Preview Banner
        androidx.compose.animation.AnimatedVisibility(
            visible = selectedImageUri != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            selectedImageUri?.let { uri ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmCreamSurface)
                        .border(1.dp, BorderSoft)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Seçilen Fotoğraf",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, SoftCoralPrimary, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📸 Fotoğraf Eklendi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftCoralDark
                        )
                        Text(
                            text = "Gönder butonuna basarak sevgiline iletebilirsin",
                            fontSize = 11.sp,
                            color = SlateNavy
                        )
                    }
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kaldır", tint = SlateNavy, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Quick Love Prompts Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickReactions) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarmCreamSurface)
                        .border(1.dp, BorderSoft, RoundedCornerShape(12.dp))
                        .clickable { onSendMessage(prompt, null, replyingToMessage) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = DeepCharcoal
                    )
                }
            }
        }

        // Input Field Bar (Comfortable padding and rounded card design)
        Card(
            colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSoft, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Fotoğraf Gönder",
                        tint = SoftCoralPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        val typing = it.isNotBlank()
                        onTypingChanged(typing)
                        typingDebounceJob?.cancel()
                        if (typing) {
                            typingDebounceJob = coroutineScope.launch {
                                delay(2500)
                                onTypingChanged(false)
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = if (editingMessage != null) "Düzenlenmiş mesajı yaz..." else "Sevgiline tatlı bir mesaj yaz...",
                            fontSize = 13.sp,
                            color = SlateNavy.copy(alpha = 0.6f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DeepCharcoal,
                        unfocusedTextColor = DeepCharcoal,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = SoftCoralPrimary,
                        focusedBorderColor = SoftCoralPrimary,
                        unfocusedBorderColor = BorderSoft,
                        focusedPlaceholderColor = SlateNavy.copy(alpha = 0.6f),
                        unfocusedPlaceholderColor = SlateNavy.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() || selectedImageUri != null) {
                            typingDebounceJob?.cancel()
                            onTypingChanged(false)
                            if (editingMessage != null) {
                                onEditMessage(editingMessage!!.id, inputText.trim())
                                editingMessage = null
                            } else {
                                onSendMessage(inputText.trim(), selectedImageUri, replyingToMessage)
                                replyingToMessage = null
                            }
                            inputText = ""
                            selectedImageUri = null
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SoftCoralPrimary)
                        .testTag("send_chat_msg_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gönder",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Long Press Action Sheet Modal
    if (menuMessage != null) {
        val targetMsg = menuMessage!!
        val isSenderMe = targetMsg.senderId == "me" || targetMsg.senderId == currentUser.userId

        ModalBottomSheet(
            onDismissRequest = { menuMessage = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = WarmCreamSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Mesaj Seçenekleri",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepCharcoal
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (!targetMsg.isDeleted) {
                    // Quick Emojis Reaction Row
                    Text(
                        text = "Tepki Bırak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val emojis = listOf("❤️", "😍", "😂", "🥺", "👏", "🔥")
                        emojis.forEach { em ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (targetMsg.reactionEmoji == em) SoftCoralContainer else WarmCreamContainer)
                                    .border(
                                        width = if (targetMsg.reactionEmoji == em) 1.5.dp else 0.dp,
                                        color = if (targetMsg.reactionEmoji == em) SoftCoralPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onReactMessage(targetMsg.id, if (targetMsg.reactionEmoji == em) null else em)
                                        menuMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = em, fontSize = 22.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Quick Double-Tap Emoji Picker Row
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarmCreamContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = SoftCoralPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Çift Tıklama Emojisi (Şu an: $doubleTapEmoji)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepCharcoal
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(doubleTapEmojiOptions) { emoji ->
                                val isSelected = emoji == doubleTapEmoji
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) SoftCoralPrimary else WarmCreamSurface)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) SoftCoralDark else BorderSoft,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onSetDoubleTapEmoji(emoji)
                                            Toast.makeText(context, "Çift dokunma emojisi $emoji olarak ayarlandı", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 17.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!targetMsg.isDeleted) {
                    // Option: Reply
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                replyingToMessage = targetMsg
                                editingMessage = null
                                menuMessage = null
                            }
                            .padding(vertical = 10.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = SoftCoralPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Cevapla", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DeepCharcoal)
                    }

                    // Option: Copy
                    if (targetMsg.text.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("chat_message", targetMsg.text)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Mesaj kopyalandı", Toast.LENGTH_SHORT).show()
                                    menuMessage = null
                                }
                                .padding(vertical = 10.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = SlateNavy)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Metni Kopyala", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DeepCharcoal)
                        }
                    }

                    // Option: Edit (Only if sender is me and text is present)
                    if (isSenderMe && targetMsg.text.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                editingMessage = targetMsg
                                inputText = targetMsg.text
                                replyingToMessage = null
                                menuMessage = null
                            }
                            .padding(vertical = 10.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = SageGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Mesajı Düzenle", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DeepCharcoal)
                        }
                    }

                // Option: Delete for Everyone
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onDeleteMessage(targetMsg.id)
                            menuMessage = null
                            Toast.makeText(context, "Mesaj herkes için silindi", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 10.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Herkes İçin Sil", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFD32F2F))
                        Text("Mesaj her iki tarafın sohbetinden de silinir", fontSize = 11.sp, color = SlateNavy.copy(alpha = 0.7f))
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = TextMuted)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Bu mesaj daha önce silindi.",
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextMuted
                    )
                }
            }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // Full Screen Photo Preview Dialog
    if (fullPhotoPreviewUrl != null) {
        Dialog(
            onDismissRequest = { fullPhotoPreviewUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { fullPhotoPreviewUrl = null },
                contentAlignment = Alignment.Center
            ) {
                SmartChatMessageImage(
                    mediaUrl = fullPhotoPreviewUrl!!,
                    contentDescription = "Büyük Fotoğraf Önizlemesi",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit,
                    isMe = true
                )
                IconButton(
                    onClick = { fullPhotoPreviewUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun SwipeableMessageItem(
    msg: ChatMessage,
    isMe: Boolean,
    isHighlighted: Boolean = false,
    doubleTapEmoji: String,
    partnerPreset: String,
    partnerBase64: String?,
    onDoubleTap: () -> Unit,
    onReply: () -> Unit,
    onReplyClick: ((String) -> Unit)? = null,
    onPhotoClick: ((String) -> Unit)? = null,
    onReactionPillClick: (() -> Unit)? = null,
    onLongClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(msg.timestamp) { timeFormatter.format(Date(msg.timestamp)) }

    // Heart pop animation state on double tap
    var showPopHeart by remember { mutableStateOf(false) }
    val popScale by animateFloatAsState(
        targetValue = if (showPopHeart) 1.25f else 0.3f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "pop_scale"
    )
    val popAlpha by animateFloatAsState(
        targetValue = if (showPopHeart) 1f else 0f,
        animationSpec = tween(if (showPopHeart) 150 else 300),
        label = "pop_alpha"
    )

    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            val newOffset = (offsetX.value + delta).coerceIn(0f, 100f)
            offsetX.snapTo(newOffset)
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Swipe Reveal Icon (Reply Indicator on Left)
        if (offsetX.value > 10f && !msg.isDeleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SoftCoralContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Cevapla",
                    tint = SoftCoralPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = !msg.isDeleted,
                    onDragStopped = {
                        if (offsetX.value > 65f) {
                            onReply()
                        }
                        coroutineScope.launch {
                            offsetX.animateTo(0f, spring())
                        }
                    }
                ),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            if (!isMe) {
                AvatarImage(
                    preset = partnerPreset,
                    base64 = partnerBase64,
                    size = 28.dp,
                    modifier = Modifier.align(Alignment.Bottom)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            )
                        )
                        .background(
                            when {
                                isHighlighted -> if (isMe) SoftCoralPrimary else SoftCoralContainer
                                msg.isDeleted -> if (isMe) Color(0xFFFDF2F2) else Color(0xFFF3F4F6)
                                isMe -> SoftCoralPrimary
                                else -> WarmCreamSurface
                            }
                        )
                        .border(
                            width = if (isHighlighted) 2.5.dp else 1.dp,
                            color = when {
                                isHighlighted -> SoftCoralDark
                                msg.isDeleted -> if (isMe) Color(0xFFFBD5D5) else Color(0xFFE5E7EB)
                                isMe -> SoftCoralPrimary
                                else -> BorderSoft
                            },
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            )
                        )
                        .pointerInput(msg.id, msg.isDeleted) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (!msg.isDeleted) {
                                        showPopHeart = true
                                        onDoubleTap()
                                        coroutineScope.launch {
                                            delay(650)
                                            showPopHeart = false
                                        }
                                    }
                                },
                                onLongPress = {
                                    onLongClick()
                                }
                            )
                        }
                        .padding(10.dp)
                ) {
                    if (msg.isDeleted) {
                        // Deleted Message Display
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Silindi",
                                    tint = if (isMe) Color(0xFFE02424) else Color(0xFF6B7280),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🚫 Bu mesaj silindi",
                                    fontStyle = FontStyle.Italic,
                                    color = if (isMe) Color(0xFF9B1C1C) else Color(0xFF4B5563),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = timeString,
                                fontSize = 9.sp,
                                color = TextMuted,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    } else {
                        Column {
                            // Replying To Quoted Bubble (Clickable to jump to original message)
                            if (!msg.replyToText.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isMe) Color.White.copy(alpha = 0.2f) else WarmCreamContainer
                                        )
                                        .clickable(enabled = !msg.replyToId.isNullOrBlank()) {
                                            msg.replyToId?.let { onReplyClick?.invoke(it) }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.5.dp)
                                            .height(26.dp)
                                            .background(if (isMe) Color.White else SoftCoralPrimary, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (!msg.replyToSenderName.isNullOrBlank()) {
                                            Text(
                                                text = msg.replyToSenderName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMe) Color.White else SoftCoralDark
                                            )
                                        }
                                        Text(
                                            text = msg.replyToText,
                                            fontSize = 11.sp,
                                            color = if (isMe) Color.White.copy(alpha = 0.9f) else DeepCharcoal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Photo attachment with SmartChatMessageImage (supports Base64, Uri, S3/R2 presigned URLs)
                            val mediaPhotoUrl = msg.effectiveMediaUrl
                            if (mediaPhotoUrl != null) {
                                SmartChatMessageImage(
                                    mediaUrl = mediaPhotoUrl,
                                    contentDescription = "Fotoğraf",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                    isMe = isMe,
                                    onPhotoClick = onPhotoClick
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Text content
                            if (msg.text.isNotBlank()) {
                                Text(
                                    text = msg.text,
                                    color = if (isMe) Color.White else DeepCharcoal,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Time & Status indicator
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (msg.isEdited) {
                                    Text(
                                        text = "düzenlendi ",
                                        fontSize = 9.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = if (isMe) Color.White.copy(alpha = 0.75f) else TextMuted
                                    )
                                }
                                Text(
                                    text = timeString,
                                    fontSize = 10.sp,
                                    color = if (isMe) Color.White.copy(alpha = 0.85f) else TextMuted
                                )
                                if (isMe && !msg.isDeleted) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    if (msg.isRead) {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = "Okundu",
                                            tint = Color(0xFFBAE6FD),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "İletildi",
                                            tint = Color.White.copy(alpha = 0.75f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Animated Popping Double-Tap Heart over the bubble
                    if (popAlpha > 0.05f) {
                        Text(
                            text = doubleTapEmoji,
                            fontSize = 42.sp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .scale(popScale)
                                .alpha(popAlpha)
                        )
                    }
                }

                // Reaction Emoji Pill on Bottom Corner (Clickable to remove reaction)
                if (!msg.reactionEmoji.isNullOrBlank() && !msg.isDeleted) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-8).dp, x = if (isMe) (-6).dp else 6.dp)
                            .clip(CircleShape)
                            .background(WarmCreamSurface)
                            .border(1.dp, BorderSoft, CircleShape)
                            .clickable { onReactionPillClick?.invoke() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = msg.reactionEmoji, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TypingDotsIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots_trans")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1_alpha"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2_alpha"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3_alpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(WarmCreamSurface)
            .border(1.dp, BorderSoft, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Yazıyor",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = SoftCoralPrimary
        )
        Spacer(modifier = Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(dot1Alpha)
                .clip(CircleShape)
                .background(SoftCoralPrimary)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(dot2Alpha)
                .clip(CircleShape)
                .background(SoftCoralPrimary)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(dot3Alpha)
                .clip(CircleShape)
                .background(SoftCoralPrimary)
        )
    }
}

@Composable
fun SmartChatMessageImage(
    mediaUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    isMe: Boolean = true,
    onPhotoClick: ((String) -> Unit)? = null
) {
    val decodedBitmap = remember(mediaUrl) {
        if (mediaUrl.startsWith("data:image") || (!mediaUrl.startsWith("http://") && !mediaUrl.startsWith("https://") && !mediaUrl.startsWith("content://") && !mediaUrl.startsWith("file://") && mediaUrl.length > 30)) {
            try {
                val raw = if (mediaUrl.startsWith("data:image")) mediaUrl.substringAfter(",") else mediaUrl
                val cleanRaw = raw.replace("\n", "").replace("\r", "").trim()
                val decoded = Base64.decode(cleanRaw, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier
                .clickable { onPhotoClick?.invoke(mediaUrl) },
            contentScale = contentScale
        )
    } else {
        SubcomposeAsyncImage(
            model = mediaUrl,
            contentDescription = contentDescription,
            modifier = modifier
                .clickable { onPhotoClick?.invoke(mediaUrl) },
            contentScale = contentScale,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isMe) Color.White.copy(alpha = 0.15f) else WarmCreamContainer),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = if (isMe) Color.White else SoftCoralPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            error = {
                val fallbackBitmap = remember(mediaUrl) {
                    try {
                        val raw = if (mediaUrl.startsWith("data:image")) mediaUrl.substringAfter(",") else mediaUrl
                        val cleanRaw = raw.replace("\n", "").replace("\r", "").trim()
                        val decoded = Base64.decode(cleanRaw, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (fallbackBitmap != null) {
                    Image(
                        bitmap = fallbackBitmap.asImageBitmap(),
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Görsel yüklenemedi",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        )
    }
}

