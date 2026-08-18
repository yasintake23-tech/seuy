package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.UserProfile
import com.example.ui.components.AVATAR_PRESETS
import com.example.ui.components.AvatarImage
import com.example.ui.theme.BorderLight
import com.example.ui.theme.RoseDark
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSoft
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WineTertiary
import com.example.util.NotificationHelper

@Composable
fun SettingsDialog(
    currentUser: UserProfile,
    doubleTapEmoji: String = "🤍",
    onSetDoubleTapEmoji: (String) -> Unit = {},
    onChangeProfilePhoto: (Uri) -> Unit = {},
    onChangeAvatarPreset: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onOpenUnpairConfirm: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val doubleTapOptions = listOf("🤍", "❤️", "💖", "🥰", "✨", "🔥", "🌸", "🧸", "🐾")
    val scrollState = rememberScrollState()
    var isNotificationsEnabled by remember { mutableStateOf(NotificationHelper.areNotificationsEnabled(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationsEnabled = NotificationHelper.areNotificationsEnabled(context)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onChangeProfilePhoto(uri)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ayarlar & Profil",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WineTertiary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Info Card with Photo Change Trigger
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoseSoft),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
                            ) {
                                AvatarImage(
                                    preset = currentUser.avatarPreset,
                                    base64 = currentUser.avatarBase64,
                                    size = 56.dp
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(RosePrimary)
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Fotoğrafı Değiştir",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WineTertiary
                                )
                                Text(
                                    text = currentUser.email,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (currentUser.birthDate.isNotEmpty()) {
                                    Text(
                                        text = "Doğum Tarihi: ${currentUser.birthDate}",
                                        fontSize = 11.sp,
                                        color = RoseDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Button to pick photo from gallery
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp), tint = RosePrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galeriden Profil Resmi Seç", fontSize = 12.sp, color = RoseDark, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Romantik Avatar Seçimi:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RoseDark)
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(AVATAR_PRESETS) { item ->
                                val isSelected = currentUser.avatarPreset == item.id && currentUser.avatarBase64 == null
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(item.bgColor1, item.bgColor2)))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) RosePrimary else Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onChangeAvatarPreset(item.id)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = item.emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Double Tap Reaction Emoji Preference
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = doubleTapEmoji,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Çift Dokunma Tepki Emojisi",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WineTertiary
                                )
                                Text(
                                    text = "Mesaja iki kez tıklayınca gönderilecek emoji",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            doubleTapOptions.take(5).forEach { emoji ->
                                val isSelected = emoji == doubleTapEmoji
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) RosePrimary else MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) RoseDark else BorderLight,
                                            shape = CircleShape
                                        )
                                        .clickable { onSetDoubleTapEmoji(emoji) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            doubleTapOptions.drop(5).forEach { emoji ->
                                val isSelected = emoji == doubleTapEmoji
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) RosePrimary else MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) RoseDark else BorderLight,
                                            shape = CircleShape
                                        )
                                        .clickable { onSetDoubleTapEmoji(emoji) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notification Settings Card (Android 7+ compatible)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNotificationsEnabled) RoseSoft.copy(alpha = 0.6f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (isNotificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (isNotificationsEnabled) RosePrimary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Anlık Aşk Bildirimleri",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WineTertiary
                                )
                                Text(
                                    text = if (isNotificationsEnabled) "✅ Bildirimler Açık (Mesajlar anında iletilir)" else "⚠️ Bildirimler Kapalı (Mesajları kaçırabilirsiniz)",
                                    fontSize = 11.5.sp,
                                    color = if (isNotificationsEnabled) SageGreen else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (!isNotificationsEnabled) {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        NotificationHelper.openNotificationSettings(context)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bildirimleri Etkinleştir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        NotificationHelper.showChatNotification(
                                            context = context,
                                            senderName = currentUser.partnerName ?: "Sevgilin",
                                            messageText = "Seni çok seviyorum birtanem! ❤️✨"
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Test Bildirimi Gönder", fontSize = 11.5.sp, color = RoseDark, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        NotificationHelper.openNotificationSettings(context)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Sistem Ayarları", fontSize = 11.5.sp, color = WineTertiary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // App info item
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = RosePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "İkimiz Uygulaması v1.0",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sign out button
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onSignOut()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_sign_out_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = RoseDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hesaptan Çıkış Yap",
                        color = RoseDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = BorderLight.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))

                // Small, subtle, discreet unpair button at the very bottom
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = onOpenUnpairConfirm,
                        modifier = Modifier.testTag("unpair_discreet_btn")
                    ) {
                        Text(
                            text = "Eşleşmeden Ayrıl",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnpairConfirmationDialog(
    isUnpairing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isUnpairing) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Eşleşmeden ayrılmak istediğine emin misin?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = "Eşleşme kaldırılacak ancak hesabın ve profil bilgilerin silinmeyecektir. Dilediğin zaman yeni bir kod ile sevgilinle tekrar eşleşebilirsin.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isUnpairing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_unpair_dialog_btn")
            ) {
                if (isUnpairing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Evet, Ayrıl", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUnpairing,
                modifier = Modifier.testTag("cancel_unpair_dialog_btn")
            ) {
                Text("Vazgeç", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}
