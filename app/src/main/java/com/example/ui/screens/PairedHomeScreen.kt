package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BottomNavTab
import com.example.model.BucketItem
import com.example.model.UserProfile
import com.example.ui.components.AvatarImage
import com.example.ui.theme.*

@Composable
fun PairedHomeScreen(
    currentUser: UserProfile,
    partnerUser: UserProfile?,
    bucketList: List<BucketItem>,
    onOpenSettings: () -> Unit,
    onNavigateToTab: ((BottomNavTab) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val pairedAt = currentUser.pairedAt ?: now
    val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays((now - pairedAt).coerceAtLeast(0))
    val openGoals = bucketList.count { !it.isCompleted }
    val doneGoals = bucketList.count { it.isCompleted }
    val partnerName = partnerUser?.displayName ?: currentUser.partnerName ?: "Sevgilin"
    val pulse by rememberInfiniteTransition(label = "home_pulse").animateFloat(
        1f, 1.08f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize().background(WarmCreamBackground).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Biz", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DeepCharcoal)
                    Text("İkinizin gerçekten kullanacağı alan.", fontSize = 12.sp, color = SlateNavy)
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("open_settings_btn")) {
                    Icon(Icons.Default.Settings, "Ayarlar", tint = DeepCharcoal)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderSoft, RoundedCornerShape(24.dp))
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                        AvatarImage(currentUser.avatarPreset, currentUser.displayPhotoUrl, 68.dp)
                        Box(
                            Modifier.padding(horizontal = 14.dp).size(42.dp).scale(pulse).background(
                                Brush.linearGradient(listOf(SoftCoralPrimary, SoftCoralDark)), CircleShape
                            ),
                            Alignment.Center
                        ) { Icon(Icons.Default.Favorite, null, tint = Color.White) }
                        AvatarImage(partnerUser?.avatarPreset ?: "heart_rose", partnerUser?.displayPhotoUrl, 68.dp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("$days gündür birlikte", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = SoftCoralDark)
                    Text("${currentUser.displayName} & $partnerName", fontSize = 13.sp, color = SlateNavy)
                }
            }
        }

        item {
            SectionTitle("Bugün ne yapmak istiyorsunuz?", "Hızlı erişim")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile("Mesajlaş", Icons.Default.Chat, Modifier.weight(1f)) { onNavigateToTab?.invoke(BottomNavTab.CHAT) }
                ActionTile("Oyun oyna", Icons.Default.SportsEsports, Modifier.weight(1f)) { onNavigateToTab?.invoke(BottomNavTab.GAMES) }
                ActionTile("Anılara bak", Icons.Default.PhotoLibrary, Modifier.weight(1f)) { onNavigateToTab?.invoke(BottomNavTab.PROFILE) }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SoftCoralContainer), shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column {
                            Text("Ortak hedefler", fontWeight = FontWeight.Bold, color = SoftCoralDark)
                            Text("$openGoals açık • $doneGoals tamamlandı", fontSize = 12.sp, color = DeepCharcoal)
                        }
                        Icon(Icons.Default.AddTask, null, tint = SoftCoralPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    val next = bucketList.firstOrNull { !it.isCompleted }
                    Text(next?.let { "Sıradaki: ${it.title}" } ?: "Henüz açık hedef yok. Eğlence bölümünden ilk hedefinizi ekleyin.",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DeepCharcoal,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (next != null) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onNavigateToTab?.invoke(BottomNavTab.GAMES) }, shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)) { Text("Hedeflere git") }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = WarmCreamSurface), shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderSoft, RoundedCornerShape(20.dp))) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = SoftCoralPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("İlişki özeti", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepCharcoal)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                        Stat("💖", "$days", "Gün")
                        Stat("🎯", "$doneGoals", "Hedef")
                        Stat("✨", "$openGoals", "Plan")
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = WarmCreamSurface), shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToTab?.invoke(BottomNavTab.CHAT) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Forum, null, tint = SoftCoralPrimary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Birbirinize bir şey bırakın", fontWeight = FontWeight.Bold, color = DeepCharcoal)
                        Text("Mesajlar bölümünden fotoğraf, ses veya düşüncenizi paylaşın.", fontSize = 12.sp, color = SlateNavy)
                    }
                    Text("→", fontSize = 22.sp, color = SoftCoralPrimary)
                }
            }
        }
    }
}

@Composable private fun SectionTitle(title: String, subtitle: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepCharcoal)
    Text(subtitle, fontSize = 11.sp, color = SlateNavy)
}
@Composable private fun ActionTile(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(92.dp), colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
        shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = SoftCoralPrimary, modifier = Modifier.size(25.dp))
            Spacer(Modifier.height(7.dp)); Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
        }
    }
}
@Composable private fun Stat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 19.sp); Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = DeepCharcoal)
        Text(label, fontSize = 10.sp, color = SlateNavy)
    }
}
