package com.example.ui.screens

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsSuggest
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
import androidx.compose.material3.Switch
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
import com.example.util.NotificationHelper

enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ACCOUNT("Hesap & Profil", "Profilin ve hesabın", Icons.Default.Person),
    APPEARANCE("Görünüm", "Tema ve akıcılık", Icons.Default.Palette),
    CHAT("Sohbet", "Mesaj davranışları", Icons.Default.Chat),
    NOTIFICATIONS("Bildirimler", "Mesaj bildirimleri", Icons.Default.Notifications),
    COUPLE("Biz", "Tanışma tarihi ve ortak alan", Icons.Default.Favorite),
    ABOUT("Hakkında", "İkimiz hakkında", Icons.Default.Info)
}

private class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("ikimiz_settings", Context.MODE_PRIVATE)

    fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    fun setBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun getString(key: String, default: String) = prefs.getString(key, default) ?: default
    fun setString(key: String, value: String) = prefs.edit().putString(key, value).apply()
}

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
    onThemeModeChanged: (String) -> Unit = {},
    onSetRelationshipStartedAt: (Long) -> Unit = {},
    relationshipStartedAt: Long? = null,
    onSetMessageNotificationsEnabled: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    var isNotificationsEnabled by remember { mutableStateOf(NotificationHelper.areNotificationsEnabled(context)) }
    var themeMode by remember { mutableStateOf(store.getString("theme_mode", "system")) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isNotificationsEnabled = NotificationHelper.areNotificationsEnabled(context) }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) onChangeProfilePhoto(uri)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = modifier
                .fillMaxWidth(0.94f)
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
        ) {
            if (selectedCategory == null) {
                SettingsHome(
                    currentUser = currentUser,
                    categories = SettingsCategory.entries,
                    onCategoryClick = { selectedCategory = it },
                    onDismiss = onDismiss
                )
            } else {
                SettingsCategoryDetail(
                    category = selectedCategory!!,
                    currentUser = currentUser,
                    doubleTapEmoji = doubleTapEmoji,
                    isNotificationsEnabled = isNotificationsEnabled,
                    themeMode = themeMode,
                    store = store,
                    onBack = { selectedCategory = null },
                    onDismiss = onDismiss,
                    onSetDoubleTapEmoji = onSetDoubleTapEmoji,
                    onChangeProfilePhoto = { photoPickerLauncher.launch("image/*") },
                    onChangeAvatarPreset = onChangeAvatarPreset,
                    onNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            NotificationHelper.openNotificationSettings(context)
                        }
                    },
                    refreshNotifications = { isNotificationsEnabled = NotificationHelper.areNotificationsEnabled(context) },
                    onThemeModeChanged = {
                        themeMode = it
                        store.setString("theme_mode", it)
                        onThemeModeChanged(it)
                    },
                    onSetRelationshipStartedAt = onSetRelationshipStartedAt,
                    relationshipStartedAt = relationshipStartedAt,
                    onSetMessageNotificationsEnabled = onSetMessageNotificationsEnabled,
                    onOpenUnpairConfirm = onOpenUnpairConfirm,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun SettingsHome(
    currentUser: UserProfile,
    categories: List<SettingsCategory>,
    onCategoryClick: (SettingsCategory) -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Ayarlar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("İkimiz'i kendinize göre şekillendirin", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Kapat")
            }
        }

        Spacer(Modifier.height(14.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = RoseSoft),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(currentUser.avatarPreset, currentUser.avatarBase64, 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(currentUser.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(currentUser.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (currentUser.isPaired) "💞 ${currentUser.partnerName ?: "Sevgilin"} ile eşleşti" else "Henüz eşleşme yok",
                        fontSize = 11.sp,
                        color = RosePrimary,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(560.dp)
        ) {
            items(categories) { category ->
                SettingsCategoryRow(category, onClick = { onCategoryClick(category) })
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(category: SettingsCategory, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(RoseSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon, null, tint = RosePrimary, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(category.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsCategoryDetail(
    category: SettingsCategory,
    currentUser: UserProfile,
    doubleTapEmoji: String,
    isNotificationsEnabled: Boolean,
    themeMode: String,
    store: SettingsStore,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onSetDoubleTapEmoji: (String) -> Unit,
    onChangeProfilePhoto: () -> Unit,
    onChangeAvatarPreset: (String) -> Unit,
    onNotificationPermission: () -> Unit,
    refreshNotifications: () -> Unit,
    onThemeModeChanged: (String) -> Unit,
    onSetRelationshipStartedAt: (Long) -> Unit,
    relationshipStartedAt: Long?,
    onSetMessageNotificationsEnabled: (Boolean) -> Unit,
    onOpenUnpairConfirm: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(Modifier.padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text(category.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(category.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Kapat") }
        }
        Spacer(Modifier.height(10.dp))

        Column(
            Modifier.fillMaxWidth().height(590.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (category) {
                SettingsCategory.ACCOUNT -> AccountSettings(currentUser, onChangeProfilePhoto, onChangeAvatarPreset, onOpenUnpairConfirm, onSignOut)
                SettingsCategory.APPEARANCE -> AppearanceSettings(themeMode, onThemeModeChanged)
                SettingsCategory.CHAT -> ChatSettings(doubleTapEmoji, onSetDoubleTapEmoji)
                SettingsCategory.NOTIFICATIONS -> NotificationSettings(isNotificationsEnabled, currentUser.notificationsEnabled, onNotificationPermission, refreshNotifications, onSetMessageNotificationsEnabled)
                SettingsCategory.COUPLE -> CoupleSettings(currentUser, relationshipStartedAt, onSetRelationshipStartedAt)
                SettingsCategory.ABOUT -> AboutSettings()
            }
        }
    }
}

@Composable
private fun AccountSettings(
    currentUser: UserProfile,
    onChangeProfilePhoto: () -> Unit,
    onChangeAvatarPreset: (String) -> Unit,
    onOpenUnpairConfirm: () -> Unit,
    onSignOut: () -> Unit
) {
    SettingsGroup("Profil") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable { onChangeProfilePhoto() }) {
                AvatarImage(currentUser.avatarPreset, currentUser.avatarBase64, 64.dp)
                Box(Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape).background(RosePrimary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(currentUser.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(currentUser.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (currentUser.birthDate.isNotBlank()) Text("Doğum tarihi: ${currentUser.birthDate}", fontSize = 11.sp, color = RosePrimary)
            }
        }
        OutlinedButton(onClick = onChangeProfilePhoto, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(11.dp)) {
            Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(17.dp), tint = RosePrimary)
            Spacer(Modifier.width(6.dp)); Text("Profil fotoğrafını değiştir")
        }
        Text("Avatar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RosePrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AVATAR_PRESETS) { item ->
                Box(
                    Modifier.size(43.dp).clip(CircleShape).background(Brush.linearGradient(listOf(item.bgColor1, item.bgColor2)))
                        .border(if (currentUser.avatarPreset == item.id && currentUser.avatarBase64 == null) 2.5.dp else 1.dp, if (currentUser.avatarPreset == item.id && currentUser.avatarBase64 == null) RosePrimary else Color.White, CircleShape)
                        .clickable { onChangeAvatarPreset(item.id) },
                    contentAlignment = Alignment.Center
                ) { Text(item.emoji, fontSize = 18.sp) }
            }
        }
    }
    SettingsGroup("Hesap") {
        InfoRow("E-posta", currentUser.email, Icons.Default.Person)
        InfoRow("Kullanıcı kimliği", if (currentUser.userId.isNotBlank()) currentUser.userId.take(18) + "…" else "—", Icons.Default.Fingerprint)
        InfoRow("Eşleşme", if (currentUser.isPaired) (currentUser.partnerName ?: "Aktif") else "Yok", Icons.Default.Favorite)
    }
    SettingsGroup("Hesap işlemleri") {
        SettingsAction("Eşleşmeden Ayrıl", "Çift bağlantısını kaldır", Icons.Default.Warning, onOpenUnpairConfirm)
        SettingsAction("Hesaptan Çıkış Yap", "Bu cihazdaki oturumu kapat", Icons.Default.Logout, onSignOut)
    }
}

@Composable
private fun AppearanceSettings(themeMode: String, onThemeModeChanged: (String) -> Unit) {
    SettingsGroup("Tema") {
        ChoiceRow("Sistem", "Telefonun temasını takip eder", themeMode == "system") { onThemeModeChanged("system") }
        ChoiceRow("Açık", "Açık ve sıcak görünüm", themeMode == "light") { onThemeModeChanged("light") }
        ChoiceRow("Koyu", "Gece kullanımı için koyu tema", themeMode == "dark") { onThemeModeChanged("dark") }
    }
    Text("Tüm ana metinler sistem uyumlu ve okunabilir tipografi kullanır.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ChatSettings(doubleTapEmoji: String, onSetDoubleTapEmoji: (String) -> Unit) {
    SettingsGroup("Çift dokunma tepkisi") {
        Text("Mesaja iki kez dokununca kullanılacak kalp/emojiyi seç.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val emojis = listOf("🤍", "❤️", "💖", "🥰", "✨", "🔥", "🌸", "🧸", "😘", "😍", "😂")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(emojis) { emoji ->
                Box(
                    Modifier.size(42.dp).clip(CircleShape)
                        .background(if (emoji == doubleTapEmoji) RosePrimary else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, if (emoji == doubleTapEmoji) RoseDark else BorderLight, CircleShape)
                        .clickable { onSetDoubleTapEmoji(emoji) },
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun NotificationSettings(
    isSystemEnabled: Boolean,
    messagesEnabled: Boolean,
    onPermission: () -> Unit,
    refresh: () -> Unit,
    onSetMessageNotificationsEnabled: (Boolean) -> Unit
) {
    SettingsGroup("Mesaj bildirimleri") {
        SettingSwitch(
            title = "Mesaj bildirimleri",
            subtitle = "Partnerinden yeni mesaj geldiğinde bildirim al",
            checked = messagesEnabled,
            onCheckedChange = onSetMessageNotificationsEnabled
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (isSystemEnabled) "Android bildirimleri açık." else "Android bildirimleri kapalı.",
            fontSize = 11.sp,
            color = if (isSystemEnabled) SageGreen else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onPermission(); refresh() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
        ) {
            Icon(Icons.Default.Notifications, null)
            Spacer(Modifier.width(6.dp))
            Text("Bildirimleri aç / yönet")
        }
    }
}

@Composable
private fun CoupleSettings(
    currentUser: UserProfile,
    relationshipStartedAt: Long?,
    onSetRelationshipStartedAt: (Long) -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember(relationshipStartedAt) { mutableStateOf(relationshipStartedAt) }

    SettingsGroup("Bizim alanımız") {
        InfoRow("Partner", currentUser.partnerName ?: "Henüz eşleşmedin", Icons.Default.Favorite)
        InfoRow("Eşleşme tarihi",
            currentUser.pairedAt?.let { java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale("tr", "TR")).format(java.util.Date(it)) } ?: "—",
            Icons.Default.Favorite
        )
    }

    SettingsGroup("Tanıştığımız tarih") {
        Text(
            "Tarihi ve saati bir kez ayarla. Bu değer iki telefonda Firebase üzerinden aynı kalır.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val dateCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                        }
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                dateCal.set(Calendar.HOUR_OF_DAY, hour)
                                dateCal.set(Calendar.MINUTE, minute)
                                dateCal.set(Calendar.SECOND, 0)
                                dateCal.set(Calendar.MILLISECOND, 0)
                                selectedDate = dateCal.timeInMillis
                                onSetRelationshipStartedAt(dateCal.timeInMillis)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
        ) {
            Text("Tarih + saat seç")
        }
        selectedDate?.let {
            Text(
                "Seçilen: " + java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale("tr", "TR")).format(java.util.Date(it)),
                fontSize = 11.sp,
                color = RosePrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AboutSettings() {
    SettingsGroup("İkimiz") {
        InfoRow("Sürüm", "1.2 • Firebase + gerçek zamanlı sohbet", Icons.Default.Info)
        Text(
            "İkimiz, iki kişinin mesajlarını, anılarını ve ortak küçük anlarını tek bir yerde tutması için tasarlanıyor.",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    SettingsGroup("Destek") {
        Text("Yardım ve diğer bölümler sade tutuldu; kullanılmayan ayar bırakılmadı.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RosePrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        Spacer(Modifier.height(6.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(13.dp), content = content)
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChoiceRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Default.Check, "Seçili", tint = RosePrimary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun InfoRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = RosePrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SettingsAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = RosePrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
