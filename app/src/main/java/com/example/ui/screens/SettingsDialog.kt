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
import androidx.compose.material.icons.filled.BatterySaver
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
import com.example.ui.theme.WineTertiary
import com.example.util.BatteryOptimizationHelper
import com.example.util.NotificationHelper

enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ACCOUNT("Hesap & Profil", "Profilin, hesabın ve kişisel bilgiler", Icons.Default.Person),
    APPEARANCE("Görünüm", "Tema, renkler ve arayüz tercihleri", Icons.Default.Palette),
    CHAT("Sohbet", "Mesajlaşma ve konuşma davranışları", Icons.Default.Chat),
    NOTIFICATIONS("Bildirimler", "Mesaj ve uygulama bildirimleri", Icons.Default.Notifications),
    PRIVACY("Gizlilik", "Görünürlük ve etkileşim tercihleri", Icons.Default.Lock),
    MEDIA("Medya & Depolama", "Fotoğraf, video ve indirme tercihleri", Icons.Default.PhotoLibrary),
    COUPLE("Biz", "Çift deneyimi ve ortak alanlar", Icons.Default.Favorite),
    APP("Uygulama", "Dil, performans ve genel davranış", Icons.Default.SettingsSuggest),
    ABOUT("Hakkında", "İkimiz hakkında ve destek", Icons.Default.Info)
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
                Text("Ayarlar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WineTertiary)
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
                    Text(currentUser.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WineTertiary)
                    Text(currentUser.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (currentUser.isPaired) "💞 ${currentUser.partnerName ?: "Sevgilin"} ile eşleşti" else "Henüz eşleşme yok",
                        fontSize = 11.sp,
                        color = RoseDark,
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
                Text(category.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WineTertiary)
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
    onOpenUnpairConfirm: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(Modifier.padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text(category.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = WineTertiary)
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
                SettingsCategory.APPEARANCE -> AppearanceSettings(themeMode, store, onThemeModeChanged)
                SettingsCategory.CHAT -> ChatSettings(doubleTapEmoji, onSetDoubleTapEmoji, store)
                SettingsCategory.NOTIFICATIONS -> NotificationSettings(isNotificationsEnabled, onNotificationPermission, refreshNotifications, store)
                SettingsCategory.PRIVACY -> PrivacySettings(store)
                SettingsCategory.MEDIA -> MediaSettings(store)
                SettingsCategory.COUPLE -> CoupleSettings(currentUser, store)
                SettingsCategory.APP -> AppSettings(store)
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
                if (currentUser.birthDate.isNotBlank()) Text("Doğum tarihi: ${currentUser.birthDate}", fontSize = 11.sp, color = RoseDark)
            }
        }
        OutlinedButton(onClick = onChangeProfilePhoto, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(11.dp)) {
            Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(17.dp), tint = RosePrimary)
            Spacer(Modifier.width(6.dp)); Text("Profil fotoğrafını değiştir")
        }
        Text("Avatar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RoseDark)
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
private fun AppearanceSettings(themeMode: String, store: SettingsStore, onThemeModeChanged: (String) -> Unit) {
    SettingsGroup("Tema") {
        ChoiceRow("Sistem", "Telefonun temasını takip eder", themeMode == "system") { onThemeModeChanged("system") }
        ChoiceRow("Açık", "Açık ve sıcak İkimiz görünümü", themeMode == "light") { onThemeModeChanged("light") }
        ChoiceRow("Koyu", "Gece kullanımı için koyu tema", themeMode == "dark") { onThemeModeChanged("dark") }
    }
    SettingsGroup("Arayüz") {
        SettingSwitch("Animasyonlar", "Geçiş ve mikro animasyonları kullan", store, "animations", true)
        SettingSwitch("Dinamik renkler", "Sistem renklerini kullan", store, "dynamic_colors", false)
        SettingSwitch("Kompakt görünüm", "Listelerde daha az boşluk kullan", store, "compact_ui", false)
    }
}

@Composable
private fun ChatSettings(doubleTapEmoji: String, onSetDoubleTapEmoji: (String) -> Unit, store: SettingsStore) {
    SettingsGroup("Mesaj davranışı") {
        SettingSwitch("Yazıyor göstergesi", "Yazarken karşı tarafa bilgi gönder", store, "typing_indicator", true)
        SettingSwitch("Okundu bilgisi", "Mesajların okundu durumunu göster", store, "read_receipts", true)
        SettingSwitch("Enter ile gönder", "Enter tuşunu gönderme için kullan", store, "enter_to_send", false)
        SettingSwitch("Bağlantı önizlemeleri", "Mesajlardaki bağlantılara önizleme ekle", store, "link_previews", true)
    }
    SettingsGroup("Çift dokunma tepkisi") {
        Text("Mesaja iki kez dokununca gönderilecek tepki", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val emojis = listOf("🤍", "❤️", "💖", "🥰", "✨", "🔥", "🌸", "🧸", "🐾", "😘", "😍", "😂")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(emojis) { emoji ->
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(if (emoji == doubleTapEmoji) RosePrimary else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, if (emoji == doubleTapEmoji) RoseDark else BorderLight, CircleShape).clickable { onSetDoubleTapEmoji(emoji) },
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 19.sp) }
            }
        }
    }
    SettingsGroup("Sohbet görünümü") {
        SettingSwitch("Mesaj saatini göster", "Mesajların saat bilgisini göster", store, "show_message_time", true)
        SettingSwitch("Avatarları göster", "Sohbette profil avatarlarını göster", store, "show_chat_avatars", true)
        SettingSwitch("Titreşimli tepki", "Tepki gönderirken kısa titreşim", store, "reaction_haptic", true)
    }
}

@Composable
private fun NotificationSettings(isEnabled: Boolean, onPermission: () -> Unit, refresh: () -> Unit, store: SettingsStore) {
    SettingsGroup("Bildirim durumu") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff, null, tint = if (isEnabled) RosePrimary else MaterialTheme.colorScheme.error, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (isEnabled) "Bildirimler açık" else "Bildirimler kapalı", fontWeight = FontWeight.Bold)
                Text(if (isEnabled) "Yeni mesajları ve uygulama bildirimlerini alabilirsin." else "Bildirimleri açarak yeni mesajları kaçırma.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = { onPermission(); refresh() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(11.dp), colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)) {
            Icon(Icons.Default.Notifications, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(if (isEnabled) "Sistem bildirim ayarlarını aç" else "Bildirimleri etkinleştir")
        }
    }
    SettingsGroup("Bildirim türleri") {
        SettingSwitch("Mesaj bildirimleri", "Yeni sohbet mesajları", store, "notif_messages", true)
        SettingSwitch("Mesaj önizlemesi", "Bildirimde mesaj metnini göster", store, "notif_preview", true)
        SettingSwitch("Bildirim sesi", "Mesaj geldiğinde ses çal", store, "notif_sound", true)
        SettingSwitch("Titreşim", "Mesaj bildirimlerinde titreşim", store, "notif_vibration", true)
        SettingSwitch("Partner etkinliği", "Yazıyor / çevrimiçi gibi etkinlikler", store, "notif_activity", true)
    }
    SettingsGroup("Arka plan") {
        SettingSwitch("Arka planda mesaj takibi", "Uygulama arka plandayken mesaj akışını destekle", store, "background_messages", true)
        SettingsAction("Pil optimizasyonunu yönet", "Android pil ayarlarını aç", Icons.Default.BatterySaver) { }
    }
}

@Composable
private fun PrivacySettings(store: SettingsStore) {
    SettingsGroup("Görünürlük") {
        SettingSwitch("Çevrimiçi durum", "Çevrimiçi olduğunu partnerine göster", store, "online_status", true)
        SettingSwitch("Son görülme", "Son aktif olduğun zamanı göster", store, "last_seen", true)
        SettingSwitch("Yazıyor durumu", "Yazarken partnerine göster", store, "typing_visibility", true)
        SettingSwitch("Okundu bilgisi", "Mesajları okuduğunu göster", store, "privacy_read_receipts", true)
    }
    SettingsGroup("Profil") {
        ChoiceRow("Partnerim görebilir", "Profil bilgilerini partnerinle paylaş", store.getString("profile_visibility", "partner") == "partner") { store.setString("profile_visibility", "partner") }
        ChoiceRow("Sadece temel bilgiler", "Daha sınırlı profil görünürlüğü", store.getString("profile_visibility", "partner") == "basic") { store.setString("profile_visibility", "basic") }
    }
    SettingsGroup("Uygulama kilidi") {
        SettingSwitch("Uygulama kilidi", "İleride biyometrik/PIN kilidi için hazır ayar", store, "app_lock", false)
        Text("Kilit mekanizması bir sonraki geliştirme aşamasında bağlanacak.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MediaSettings(store: SettingsStore) {
    SettingsGroup("Otomatik indirme") {
        SettingSwitch("Wi‑Fi'da otomatik indir", "Medya dosyalarını Wi‑Fi bağlantısında indir", store, "auto_download_wifi", true)
        SettingSwitch("Mobil veride otomatik indir", "Mobil veride medya indirmeye izin ver", store, "auto_download_mobile", false)
        SettingSwitch("Videoları otomatik indir", "Videoları otomatik indirme listesine dahil et", store, "auto_download_video", false)
    }
    SettingsGroup("Gönderme") {
        SettingSwitch("Fotoğraf sıkıştır", "Daha az veri kullanmak için fotoğrafları küçült", store, "compress_photos", true)
        SettingSwitch("Yüksek kalite", "Uygun olduğunda daha yüksek medya kalitesi", store, "high_quality_media", true)
        SettingSwitch("Galerine kaydet", "Gönderilen medyaları cihaz galerisine kaydet", store, "save_to_gallery", false)
    }
    SettingsGroup("Depolama") {
        SettingsAction("Depolama kullanımını görüntüle", "Medya kullanımını ve önbelleği göster", Icons.Default.Folder) { }
        SettingsAction("Önbelleği temizle", "Yerel geçici dosyaları temizle", Icons.Default.Folder) { }
    }
}

@Composable
private fun CoupleSettings(currentUser: UserProfile, store: SettingsStore) {
    SettingsGroup("Bizim alanımız") {
        InfoRow("Partner", currentUser.partnerName ?: "Henüz eşleşmedin", Icons.Default.Favorite)
        InfoRow("Eşleşme tarihi", currentUser.pairedAt?.let { java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale("tr", "TR")).format(java.util.Date(it)) } ?: "—", Icons.Default.Favorite)
    }
    SettingsGroup("Çift deneyimi") {
        SettingSwitch("Kalp efektleri", "Özel romantik animasyonları kullan", store, "couple_heart_effects", true)
        SettingSwitch("Ortak etkinlik bildirimleri", "Ortak alanlardaki yeni etkinlikleri bildir", store, "couple_activity_notifications", true)
        SettingSwitch("Günlük soru hatırlatıcıları", "Günlük çift sorularını hatırlat", store, "daily_question_reminders", true)
        SettingSwitch("Anı ekleme önerileri", "Anı oluşturmak için küçük öneriler göster", store, "memory_suggestions", true)
    }
    SettingsGroup("İleride gelecek özellikler") {
        SettingsAction("Yıldönümü ve özel günler", "Özel tarihleri ve geri sayımları yönet", Icons.Default.Favorite) { }
        SettingsAction("Ortak hedefler", "Birlikte yapılacaklar ve hedefler", Icons.Default.Check) { }
        SettingsAction("Çift teması", "İkinize özel renk ve stil oluştur", Icons.Default.Palette) { }
    }
}

@Composable
private fun AppSettings(store: SettingsStore) {
    SettingsGroup("Genel") {
        ChoiceRow("Türkçe", "Uygulama dili", store.getString("language", "tr") == "tr") { store.setString("language", "tr") }
        SettingSwitch("Haptik geri bildirim", "Dokunmalarda hafif titreşim", store, "haptics", true)
        SettingSwitch("Yumuşak geçişler", "Ekran değişimlerinde geçiş animasyonları", store, "smooth_transitions", true)
        SettingSwitch("Otomatik yenileme", "Aktif ekranlarda veriyi yenile", store, "auto_refresh", true)
    }
    SettingsGroup("Başlangıç") {
        ChoiceRow("Ana sayfa", "Uygulama açıldığında ana sayfayı göster", store.getString("startup_tab", "home") == "home") { store.setString("startup_tab", "home") }
        ChoiceRow("Sohbet", "Uygulama açıldığında sohbeti göster", store.getString("startup_tab", "home") == "chat") { store.setString("startup_tab", "chat") }
    }
    SettingsGroup("Performans") {
        SettingSwitch("Düşük veri kullanımı", "Medya ve ağ işlemlerini daha tutumlu yap", store, "low_data_mode", false)
        SettingSwitch("Arka plan senkronizasyonu", "Uygulama arka plandayken senkronizasyonu sürdür", store, "background_sync", true)
    }
}

@Composable
private fun AboutSettings() {
    SettingsGroup("İkimiz") {
        InfoRow("Sürüm", "1.1 • Geniş Ayarlar", Icons.Default.Info)
        InfoRow("Platform", "Android • Jetpack Compose", Icons.Default.PlayArrow)
        InfoRow("Medya altyapısı", "Cloudflare R2", Icons.Default.PhotoLibrary)
    }
    SettingsGroup("Destek") {
        SettingsAction("Yardım ve SSS", "Uygulamanın kullanımını öğren", Icons.Default.Info) { }
        SettingsAction("Sorun bildir", "Bir problem veya öneri gönder", Icons.Default.Email) { }
        SettingsAction("Gizlilik politikası", "Gizlilik bilgilerini görüntüle", Icons.Default.Lock) { }
    }
    SettingsGroup("Teşekkürler") {
        Text("İkimiz, iki kişinin birlikte anılarını, mesajlarını ve küçük mutluluklarını tek bir yerde tutması için tasarlanıyor.", fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoseDark, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
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
private fun SettingSwitch(title: String, subtitle: String, store: SettingsStore, key: String, default: Boolean) {
    var checked by remember(key) { mutableStateOf(store.getBoolean(key, default)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
        }
        Switch(checked = checked, onCheckedChange = { checked = it; store.setBoolean(key, it) })
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
                Icon(Icons.Default.Warning, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Eşleşmeden ayrılmak istediğine emin misin?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = { Text("Eşleşme kaldırılacak ancak hesabın ve profil bilgilerin silinmeyecektir. Dilediğin zaman yeni bir kod ile tekrar eşleşebilirsin.", fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isUnpairing, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = RoundedCornerShape(10.dp), modifier = Modifier.testTag("confirm_unpair_dialog_btn")) {
                if (isUnpairing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Evet, Ayrıl", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isUnpairing, modifier = Modifier.testTag("cancel_unpair_dialog_btn")) { Text("Vazgeç") } },
        shape = RoundedCornerShape(18.dp)
    )
}
