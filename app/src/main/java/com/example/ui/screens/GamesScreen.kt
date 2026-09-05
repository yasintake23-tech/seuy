package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.SoftCoralPrimary
import com.example.ui.theme.WarmCreamBackground
import com.example.ui.theme.WarmCreamSurface

@Composable
fun GamesScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(WarmCreamBackground),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Favorite, null, tint = SoftCoralPrimary)
                Text("Oyunlar", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = DeepCharcoal)
                Text("Burayı sade tuttuk. Oyunları birlikte daha sonra ekleyeceğiz.", fontSize = 13.sp, color = DeepCharcoal)
            }
        }
    }
}
