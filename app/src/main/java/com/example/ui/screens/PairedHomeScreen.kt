package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BucketItem
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
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun PairedHomeScreen(
    currentUser: UserProfile,
    partnerUser: UserProfile?,
    bucketList: List<BucketItem> = emptyList(),
    onOpenSettings: () -> Unit,
    onNavigateToTab: ((com.example.model.BottomNavTab) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sentHeartsCount by remember { mutableIntStateOf(0) }
    var showHeartBurst by remember { mutableStateOf(false) }

    // Live clock ticker
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val pairedTime = currentUser.pairedAt ?: currentTime
    val diff = (currentTime - pairedTime).coerceAtLeast(0)

    val days = TimeUnit.MILLISECONDS.toDays(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

    val partnerDisplayName = partnerUser?.displayName ?: currentUser.partnerName ?: "Sevgilin"

    // Infinite heartbeat animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_pulse"
    )

    val nextMilestoneDays = remember(days) {
        val daysToNext30 = (30 - (days % 30)).coerceAtLeast(1)
        daysToNext30
    }
    val nextMilestoneMonth = remember(days) {
        (days / 30) + 1
    }
    val nextBucketGoal = remember(bucketList) {
        bucketList.firstOrNull { !it.isCompleted }
    }

    val dailyLoveQuotes = remember {
        listOf(
            "\"Seninle geçen her gün, kalbime yazılan en güzel şiirdir.\" 💖",
            "\"Gözlerinin içine baktığım her an dünyam güzelleşiyor.\" ✨",
            "\"Mesafeler ne olursa olsun kalbim hep senin yanında atıyor.\" 🌸",
            "\"Birlikte kurduğumuz hayaller, hayatımın en tatlı gerçeği.\" 💫",
            "\"Gülüşün, en yorgun anlarımda bile içimi aydınlatan güneşim.\" ☀️",
            "\"İki kalp bir olunca her an unutulmaz bir masala dönüşür.\" 🌹",
            "\"Seni sevmek, hayatın bana sunduğu en güzel mucize.\" 💌"
        )
    }
    val dayOfYear = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }
    val todayQuote = remember(dayOfYear) { dailyLoveQuotes[dayOfYear % dailyLoveQuotes.size] }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SoftCoralContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = SoftCoralPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "İkimiz",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepCharcoal
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("open_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ayarlar",
                        tint = DeepCharcoal,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 1. Connected Lovers Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSoft, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dual Avatars & Romantic Heart Bridge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // User 1 (Me)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AvatarImage(
                                preset = currentUser.avatarPreset,
                                base64 = currentUser.avatarBase64,
                                size = 70.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentUser.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepCharcoal
                            )
                            Text(
                                text = "Sen",
                                fontSize = 11.sp,
                                color = SlateNavy
                            )
                        }

                        // Pulse Glowing Heart Bridge
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .scale(pulseScale)
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SoftCoralPrimary, SoftCoralDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Aşk Bağı",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "💖", fontSize = 12.sp)
                        }

                        // User 2 (Partner)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AvatarImage(
                                preset = partnerUser?.avatarPreset ?: "flower_pink",
                                base64 = partnerUser?.avatarBase64,
                                size = 70.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = partnerDisplayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepCharcoal
                            )
                            Text(
                                text = "Sevgilin",
                                fontSize = 11.sp,
                                color = SlateNavy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Live Elapsed Time Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SoftCoralContainer)
                            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${days + 1}. Günümüz",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SoftCoralDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$days gün • $hours saat • $minutes dk • $seconds sn",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepCharcoal
                            )
                            Text(
                                text = "Birlikte Aşkla Geçen Her An",
                                fontSize = 10.sp,
                                color = SlateNavy
                            )
                        }
                    }
                }
            }
        }

        // 2. Interactive "Seni Düşünüyorum / Kalp Gönder" Button
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sevgiline Sevgi Dokunuşu Gönder 💌",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepCharcoal
                    )
                    Text(
                        text = "Bir dokunuşla sevgiline kalbini hissettir",
                        fontSize = 11.sp,
                        color = SlateNavy
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            sentHeartsCount++
                            showHeartBurst = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftCoralPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp)
                            .testTag("send_love_touch_btn")
                    ) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seni Düşünüyorum (Kalp Gönder 💖)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (sentHeartsCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✨ Bugün sevgiline $sentHeartsCount kalp gönderdin!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SoftCoralDark
                        )
                    }
                }
            }
        }

        // 3. Special Day Countdown & Milestones Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Özel Günler & Ortak Planlar 🎂",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DeepCharcoal
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = SoftCoralPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Anniversary Countdown
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(WarmCreamContainer)
                                .border(1.dp, BorderSoft, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (days < 365) "$nextMilestoneMonth. Ay Dönümümüz" else "Yıldönümümüz",
                                    fontSize = 12.sp,
                                    color = SlateNavy
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (days < 365) "$nextMilestoneDays Gün Kaldı" else "${365 - (days % 365)} Gün Kaldı",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DeepCharcoal
                                )
                                Text(
                                    text = "🎉 Özel kutlama",
                                    fontSize = 10.sp,
                                    color = SoftCoralPrimary
                                )
                            }
                        }

                        // Next Date Night / Bucket Goal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SoftCoralContainer)
                                .border(1.dp, BorderSoft, RoundedCornerShape(14.dp))
                                .clickable {
                                    onNavigateToTab?.invoke(com.example.model.BottomNavTab.GAMES)
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Sıradaki Planımız",
                                    fontSize = 12.sp,
                                    color = SlateNavy
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (nextBucketGoal != null) {
                                    Text(
                                        text = nextBucketGoal.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SoftCoralDark,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "🎯 ${nextBucketGoal.category}",
                                        fontSize = 10.sp,
                                        color = DeepCharcoal
                                    )
                                } else {
                                    Text(
                                        text = "Plan Belirle",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SoftCoralDark
                                    )
                                    Text(
                                        text = "✨ Hedef eklemek için dokun",
                                        fontSize = 10.sp,
                                        color = DeepCharcoal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Daily Love Affirmation & Quote
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmCreamContainer.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSoft, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "✨", fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Günün Romantik Sözü",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SoftCoralDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = todayQuote,
                            fontSize = 12.sp,
                            color = DeepCharcoal,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(50.dp)) }
    }
}
