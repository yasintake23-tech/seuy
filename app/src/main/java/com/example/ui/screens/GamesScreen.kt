package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BucketItem
import com.example.model.DailyCoupleQuestion
import com.example.model.SecretLoveNote
import com.example.ui.theme.*

private data class ChoiceQuestion(val question: String, val a: String, val b: String)
private val choices = listOf(
    ChoiceQuestion("Birlikte ideal akşam?", "Evde film gecesi", "Dışarıda spontane gezi"),
    ChoiceQuestion("Hangisi daha tatlı?", "Sürpriz mesaj", "Beklenmedik sarılma"),
    ChoiceQuestion("Birlikte kaçamak?", "Deniz kenarı", "Dağ evi"),
    ChoiceQuestion("Randevu yemeği?", "Pizza & film", "Şık bir akşam yemeği"),
    ChoiceQuestion("Hafta sonu?", "Yeni bir yer keşfet", "Tüm gün birlikte dinlen")
)

@Composable
fun GamesScreen(
    bucketList: List<BucketItem>,
    secretNotes: List<SecretLoveNote>,
    dailyQuestions: List<DailyCoupleQuestion>,
    onToggleBucketItem: (String) -> Unit,
    onAddBucketItem: (String, String) -> Unit,
    onUnlockNote: (String) -> Unit,
    onAddSecretNote: (String, String, String) -> Unit,
    onAnswerDailyQuestion: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf(0) }
    Column(modifier.fillMaxSize().background(WarmCreamBackground)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Eğlence", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DeepCharcoal)
            Text("Birlikte oynayın, plan yapın, küçük anlar biriktirin.", fontSize = 12.sp, color = SlateNavy)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Oyunlar", "Planlarımız", "Sorular").forEachIndexed { i, label ->
                    SegmentedButton(selected = section == i, onClick = { section = i }, shape = SegmentedButtonDefaults.itemShape(i, 3)) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }
        }
        when(section) {
            0 -> MiniGames()
            1 -> PlansSection(bucketList, secretNotes, onToggleBucketItem, onAddBucketItem, onUnlockNote, onAddSecretNote)
            else -> QuestionsSection(dailyQuestions, onAnswerDailyQuestion)
        }
    }
}

@Composable private fun MiniGames() {
    var index by remember { mutableIntStateOf(0) }
    var aVotes by remember { mutableIntStateOf(0) }
    var bVotes by remember { mutableIntStateOf(0) }
    var truthMode by remember { mutableStateOf(false) }
    val q = choices[index]
    val truth = listOf("Partnerinde en sevdiğin küçük alışkanlık ne?", "İlk tanıştığınızda aklından ne geçti?", "Birlikte yapmak istediğin bir şey ne?", "Onun hangi özelliği seni hep gülümsetiyor?")
    val dare = listOf("Sevgiline 10 saniyelik sesli mesaj bırak.", "Son fotoğraflardan en tatlı olanı gönder.", "Ona üç kelimelik bir iltifat yaz.", "Bir sonraki randevuyu onun seçmesine izin ver.")
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            GameCard("Bu mu, şu mu?", "Aynı soruya ikiniz de cevap verin.", "💞") {
                Text(q.question, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DeepCharcoal)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { aVotes++ }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)) { Text(q.a) }
                    OutlinedButton(onClick = { bVotes++ }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text(q.b) }
                }
                Spacer(Modifier.height(10.dp))
                Text("Senin seçimlerin: ${aVotes + bVotes} • ${q.a}: $aVotes / ${q.b}: $bVotes", fontSize = 11.sp, color = SlateNavy)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { index = (index + 1) % choices.size; aVotes = 0; bVotes = 0 }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Yeni soru")
                }
            }
        }
        item {
            GameCard("Doğruluk mu Cesaret mi?", "Birbirinize sırayla seçim yaptırın.", "🎲") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = truthMode, onClick = { truthMode = true }, label = { Text("Doğruluk") })
                    FilterChip(selected = !truthMode, onClick = { truthMode = false }, label = { Text("Cesaret") })
                }
                Spacer(Modifier.height(12.dp))
                Text(if (truthMode) truth[index % truth.size] else dare[index % dare.size],
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { index = (index + 1) % choices.size }, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)) { Text("Yeni görev") }
            }
        }
        item {
            GameCard("Kalp sayacı", "Kendi aranızda günlük mini hedef belirleyin.", "❤️") {
                var count by remember { mutableIntStateOf(0) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("$count", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = SoftCoralDark, Modifier.weight(1f))
                    FilledIconButton(onClick = { count++ }) { Icon(Icons.Default.Favorite, null) }
                }
                Text("Bu sayaç cihazda tutulur; sonraki sürümde ortak sayaç olarak Firebase'e bağlanabilir.", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

@Composable private fun GameCard(title: String, subtitle: String, emoji: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = WarmCreamSurface), shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 26.sp); Spacer(Modifier.width(10.dp))
                Column { Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DeepCharcoal); Text(subtitle, fontSize = 11.sp, color = SlateNavy) }
            }
            Spacer(Modifier.height(14.dp)); content()
        }
    }
}

@Composable private fun PlansSection(
    bucket: List<BucketItem>, notes: List<SecretLoveNote>,
    toggle: (String) -> Unit, add: (String, String) -> Unit, unlock: (String) -> Unit,
    addNote: (String, String, String) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column { Text("Birlikte yapılacaklar", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DeepCharcoal)
                    Text("${bucket.count { it.isCompleted }} tamamlandı • ${bucket.count { !it.isCompleted }} bekliyor", fontSize = 11.sp, color = SlateNavy) }
                FilledIconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null) }
            }
        }
        if (bucket.isEmpty()) item { EmptyCard("Henüz ortak plan yok", "İlk hedefinizi ekleyin.") }
        items(bucket, key = { it.id }) { item ->
            Card(onClick = { toggle(item.id) }, colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.AddTask, null,
                        tint = if (item.isCompleted) SageGreen else SoftCoralPrimary)
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold, color = if (item.isCompleted) TextMuted else DeepCharcoal)
                        Text(item.category, fontSize = 10.sp, color = SlateNavy)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column { Text("Sevgi notları", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DeepCharcoal)
                    Text("Sonra açılmak üzere bırakın.", fontSize = 11.sp, color = SlateNavy) }
                FilledIconButton(onClick = { showNote = true }) { Icon(Icons.Default.Add, null) }
            }
        }
        if (notes.isEmpty()) item { EmptyCard("Henüz not yok", "Birbirinize küçük bir sürpriz bırakın.") }
        items(notes, key = { it.id }) { note ->
            Card(onClick = { if (!note.isUnlocked) unlock(note.id) }, colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (note.isUnlocked) "💌" else "🔒", fontSize = 24.sp); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(note.title, fontWeight = FontWeight.Bold, color = DeepCharcoal); Text(if (note.isUnlocked) note.content else "Kilidi açmak için dokun", fontSize = 11.sp, color = SlateNavy, maxLines = 2) }
                }
            }
        }
    }
    if (showAdd) AddPlanDialog({ showAdd = false }) { title, cat -> add(title, cat); showAdd = false }
    if (showNote) AddNoteDialog({ showNote = false }) { t,c,cond -> addNote(t,c,cond); showNote = false }
}

@Composable private fun QuestionsSection(questions: List<DailyCoupleQuestion>, answer: (String,String)->Unit) {
    var localAnswer by remember { mutableStateOf("") }
    val q = questions.firstOrNull()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = WarmCreamSurface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Günün sorusu", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DeepCharcoal)
                    Spacer(Modifier.height(10.dp))
                    Text(q?.question ?: "Bugün birbiriniz hakkında yeni ne öğrenmek isterdin?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(localAnswer, { localAnswer = it }, Modifier.fillMaxWidth(), label = { Text("Cevabın") }, minLines = 3)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { answer(q?.id ?: "daily_today", localAnswer); localAnswer = "" }, Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)) { Text("Cevabı kaydet") }
                    q?.partnerAnswer?.takeIf { it.isNotBlank() }?.let { p ->
                        Spacer(Modifier.height(12.dp)); Text("Sevgilinin cevabı", fontWeight = FontWeight.Bold, color = SoftCoralDark); Text(p, color = DeepCharcoal)
                    }
                }
            }
        }
    }
}
@Composable private fun EmptyCard(title: String, text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = WarmCreamContainer), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) { Text(title, fontWeight = FontWeight.Bold, color = DeepCharcoal); Text(text, fontSize = 12.sp, color = SlateNavy) }
    }
}
@Composable private fun AddPlanDialog(onDismiss: () -> Unit, onAdd: (String,String)->Unit) {
    var title by remember { mutableStateOf("") }; var cat by remember { mutableStateOf("Randevu") }
    AlertDialog(onDismissRequest=onDismiss, title={Text("Yeni ortak plan")}, text={
        Column { OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("Plan")}); Spacer(Modifier.height(8.dp))
            Text("Kategori"); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Randevu","Gezi","Yemek","Hayal").forEach { FilterChip(cat==it,{cat=it},{Text(it)}) }}
        }}, confirmButton={Button({if(title.isNotBlank())onAdd(title.trim(),cat)}){Text("Ekle")}}, dismissButton={TextButton(onDismiss){Text("Vazgeç")}})
}
@Composable private fun AddNoteDialog(onDismiss: () -> Unit, onAdd: (String,String,String)->Unit) {
    var title by remember { mutableStateOf("") }; var content by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Sevgi notu")},text={
        Column { OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("Başlık")}); Spacer(Modifier.height(8.dp))
            OutlinedTextField(content,{content=it},Modifier.fillMaxWidth(),label={Text("Not")},minLines=3)
        }},confirmButton={Button({if(title.isNotBlank()&&content.isNotBlank())onAdd(title.trim(),content.trim(),"Her Zaman")}){Text("Bırak")}},dismissButton={TextButton(onDismiss){Text("Vazgeç")}})
}
