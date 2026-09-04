package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.model.CoupleMemory
import com.example.model.UserProfile
import com.example.ui.components.AvatarImage
import com.example.ui.components.MemoryPresetImage
import com.example.ui.theme.*

@Composable
fun ProfileGalleryScreen(
    currentUser: UserProfile,
    partnerUser: UserProfile?,
    memories: List<CoupleMemory>,
    completedGoalsCount: Int,
    onOpenSettings: () -> Unit,
    onChangeProfilePhoto: (Uri) -> Unit,
    onChangeAvatarPreset: (String) -> Unit,
    onAddMemory: (String, String, String, String, String, String?, Uri?) -> Unit,
    onToggleLikeMemory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdd by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<CoupleMemory?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onChangeProfilePhoto(uri)
    }
    val partnerName = partnerUser?.displayName ?: currentUser.partnerName ?: "Sevgilin"

    Column(modifier.fillMaxSize().background(WarmCreamBackground)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("Profil & Anılar", fontSize=27.sp, fontWeight=FontWeight.ExtraBold, color=DeepCharcoal)
                Text("İkinize ait gerçek içerikler burada.", fontSize=12.sp, color=SlateNavy) }
            IconButton(onClick=onOpenSettings) { Icon(Icons.Default.Settings, "Ayarlar") }
        }
        LazyVerticalGrid(columns=GridCells.Fixed(2), modifier=Modifier.fillMaxSize(),
            contentPadding=PaddingValues(horizontal=16.dp, vertical=4.dp), horizontalArrangement=Arrangement.spacedBy(10.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(2)}) {
                Card(colors=CardDefaults.cardColors(containerColor=WarmCreamSurface),shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                            Box(contentAlignment=Alignment.BottomEnd) {
                                AvatarImage(currentUser.avatarPreset,currentUser.displayPhotoUrl,88.dp)
                                FilledTonalIconButton(onClick={picker.launch("image/*")},modifier=Modifier.size(34.dp)) { Icon(Icons.Default.AddAPhoto,null,Modifier.size(17.dp)) }
                            }
                            Spacer(Modifier.width(18.dp))
                            Text("♡",fontSize=30.sp,color=SoftCoralPrimary)
                            Spacer(Modifier.width(18.dp))
                            AvatarImage(partnerUser?.avatarPreset?:"heart_rose",partnerUser?.displayPhotoUrl,88.dp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("${currentUser.displayName} & $partnerName",fontSize=18.sp,fontWeight=FontWeight.ExtraBold,color=DeepCharcoal)
                        Text("Birlikte oluşturduğunuz alan",fontSize=11.sp,color=SlateNavy)
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(),Arrangement.SpaceEvenly) {
                            ProfileStat("${memories.size}","Anı"); ProfileStat("$completedGoalsCount","Hedef"); ProfileStat("${memories.count{it.isFavorite}}","Favori")
                        }
                    }
                }
            }
            item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(2)}) {
                Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically) {
                    Column { Text("Ortak anılar",fontWeight=FontWeight.ExtraBold,fontSize=18.sp,color=DeepCharcoal); Text("Fotoğraf veya hazır anı kartı ekleyin.",fontSize=11.sp,color=SlateNavy) }
                    FilledIconButton(onClick={showAdd=true}) { Icon(Icons.Default.AddAPhoto,null) }
                }
            }
            if (memories.isEmpty()) {
                item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(2)}) {
                    Card(colors=CardDefaults.cardColors(containerColor=WarmCreamContainer),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text("📸",fontSize=34.sp); Text("Henüz anı yok",fontWeight=FontWeight.Bold,color=DeepCharcoal)
                            Text("İlk ortak anınızı şimdi ekleyin.",fontSize=12.sp,color=SlateNavy)
                            Spacer(Modifier.height(8.dp)); Button({showAdd=true}){Text("İlk anıyı ekle")}
                        }
                    }
                }
            } else {
                items(memories,key={it.id}) { memory ->
                    Card(onClick={selected=memory},colors=CardDefaults.cardColors(containerColor=WarmCreamSurface),shape=RoundedCornerShape(18.dp)) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(topStart=18.dp,topEnd=18.dp))) {
                                MemoryPresetImage(memory.imagePreset,memory.imageBase64,memory.imageUrl)
                                IconButton(onClick={onToggleLikeMemory.bind(memory.id)},modifier=Modifier.align(Alignment.TopEnd)) {
                                    Icon(Icons.Default.Favorite,null,tint=if(memory.isLikedByMe) SoftCoralPrimary else Color.White)
                                }
                            }
                            Column(Modifier.padding(12.dp)) {
                                Text(memory.title.ifBlank{"Adsız anı"},fontWeight=FontWeight.Bold,color=DeepCharcoal,maxLines=1,overflow=TextOverflow.Ellipsis)
                                Text(listOf(memory.date,memory.location).filter{it.isNotBlank()}.joinToString(" • ").ifBlank{"Tarih eklenmedi"},fontSize=10.sp,color=SlateNavy)
                            }
                        }
                    }
                }
            }
        }
    }
    if(showAdd) AddMemoryDialog({showAdd=false}) { t,c,l,d,p,u -> onAddMemory(t,c,l,d,p,null,u); showAdd=false }
    selected?.let { MemoryDetailDialogSimple(it){selected=null} }
}

@Composable private fun ProfileStat(value:String,label:String) {
    Column(horizontalAlignment=Alignment.CenterHorizontally){Text(value,fontWeight=FontWeight.ExtraBold,fontSize=18.sp,color=DeepCharcoal);Text(label,fontSize=10.sp,color=SlateNavy)}
}
@Composable private fun AddMemoryDialog(onDismiss:()->Unit,onAdd:(String,String,String,String,String,Uri?)->Unit) {
    var title by remember{mutableStateOf("")}; var caption by remember{mutableStateOf("")}; var location by remember{mutableStateOf("")}; var date by remember{mutableStateOf("")}
    var uri by remember{mutableStateOf<Uri?>(null)}; var preset by remember{mutableStateOf("romantic_sunset")}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri=it}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Yeni ortak anı")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("Başlık")})
            OutlinedTextField(caption,{caption=it},Modifier.fillMaxWidth(),label={Text("Ne oldu?")},minLines=2)
            OutlinedTextField(location,{location=it},Modifier.fillMaxWidth(),label={Text("Yer (opsiyonel)")})
            OutlinedTextField(date,{date=it},Modifier.fillMaxWidth(),label={Text("Tarih (opsiyonel)")})
            Row(verticalAlignment=Alignment.CenterVertically) { Button({picker.launch("image/*")}){Text(if(uri==null)"Fotoğraf seç" else "Fotoğraf seçildi")}
                Spacer(Modifier.width(8.dp)); Text("veya hazır kart",fontSize=11.sp,color=SlateNavy) }
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) { listOf("romantic_sunset","coffee_date","walk_park","candle_dinner","stargazing","beach_trip").take(4).forEach { id -> FilterChip(preset==id,{preset=id},{Text(if(id=="romantic_sunset")"🌅" else if(id=="coffee_date")"☕" else if(id=="walk_park")"🍃" else "🕯️")}) } }
        }},confirmButton={Button({if(title.isNotBlank())onAdd(title.trim(),caption.trim(),location.trim(),date.trim(),preset,uri)}){Text("Kaydet")}},dismissButton={TextButton(onDismiss){Text("Vazgeç")}})
}
@Composable private fun MemoryDetailDialogSimple(memory:CoupleMemory,onDismiss:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text(memory.title.ifBlank{"Anı"})},text={
        Column { Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp))){MemoryPresetImage(memory.imagePreset,memory.imageBase64,memory.imageUrl)}
            Spacer(Modifier.height(10.dp)); if(memory.caption.isNotBlank())Text(memory.caption,color=DeepCharcoal)
            Text(listOf(memory.date,memory.location).filter{it.isNotBlank()}.joinToString(" • "),fontSize=11.sp,color=SlateNavy)
        }},confirmButton={TextButton(onDismiss){Text("Kapat")}})
}
private fun ((String)->Unit).bind(value:String):()->Unit = { this(value) }
