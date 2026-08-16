package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.components.AvatarImage
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralLight
import com.example.ui.theme.RoseDark
import com.example.ui.theme.RoseLight
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSoft
import com.example.ui.theme.WineLight
import com.example.ui.theme.WineTertiary
import java.util.concurrent.TimeUnit

@Composable
fun PairedHomeScreen(
    currentUser: UserProfile,
    partnerUser: UserProfile?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Calculate days together
    val daysTogether = remember(currentUser.pairedAt) {
        val pairedTime = currentUser.pairedAt ?: System.currentTimeMillis()
        val diff = System.currentTimeMillis() - pairedTime
        val days = TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(1)
        days
    }

    val partnerDisplayName = partnerUser?.displayName ?: currentUser.partnerName ?: "Sevgilin"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar with discreet Settings Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = RosePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "İkimiz",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = WineTertiary
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("open_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ayarlar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connected Lovers Hero Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatars Connected by Glowing Heart
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // You
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AvatarImage(
                            preset = currentUser.avatarPreset,
                            base64 = currentUser.avatarBase64,
                            size = 72.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentUser.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = WineTertiary
                        )
                        Text(
                            text = "Sen",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Connected Romantic Bridge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(RoseLight, RosePrimary)
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
                        Text(
                            text = "💖",
                            fontSize = 12.sp
                        )
                    }

                    // Partner
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AvatarImage(
                            preset = partnerUser?.avatarPreset ?: "flower_pink",
                            base64 = partnerUser?.avatarBase64,
                            size = 72.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = partnerDisplayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = WineTertiary
                        )
                        Text(
                            text = "Sevgilin",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Love Duration Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(RoseSoft)
                        .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$daysTogether. Günümüz",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RoseDark
                        )
                        Text(
                            text = "Birlikte Aşkla Geçen Zaman",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Romantic Status Quote Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CoralContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderLight, RoundedCornerShape(18.dp))
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "✨", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Kalplerimiz Başarıyla Birleşti",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = WineTertiary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Artık ikinizin özel alanı kuruldu. Birlikte yeni anılar ve özellikler eklemeye hazırız!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section Title: Profil Detayları
        Text(
            text = "Profil Detayları",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = WineTertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // User 1 & User 2 Info Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Current User Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarImage(
                        preset = currentUser.avatarPreset,
                        base64 = currentUser.avatarBase64,
                        size = 48.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentUser.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = WineTertiary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (currentUser.birthDate.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = RosePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentUser.birthDate,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Right: Partner Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarImage(
                        preset = partnerUser?.avatarPreset ?: "flower_pink",
                        base64 = partnerUser?.avatarBase64,
                        size = 48.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = partnerDisplayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = WineTertiary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (!partnerUser?.birthDate.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = RosePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = partnerUser!!.birthDate,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Bağlı ve Aktif 💖",
                            fontSize = 11.sp,
                            color = RoseDark
                        )
                    }
                }
            }
        }
    }
}
