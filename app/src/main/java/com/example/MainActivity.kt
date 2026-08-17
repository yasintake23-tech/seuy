package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.BottomNavTab
import com.example.ui.components.CoupleBottomNavigationBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CoupleMapScreen
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.PairedHomeScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.screens.ProfileGalleryScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.UnpairConfirmationDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainApp()
      }
    }
  }
}

@Composable
fun MainApp(
  modifier: Modifier = Modifier,
  viewModel: MainViewModel = viewModel()
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val partnerUser by viewModel.partnerUser.collectAsState()
  val isSignUpMode by viewModel.isSignUpMode.collectAsState()
  val isAuthLoading by viewModel.isAuthLoading.collectAsState()
  val authError by viewModel.authError.collectAsState()
  val pairingInput by viewModel.pairingCodeInput.collectAsState()
  val isPairingInProgress by viewModel.isPairingInProgress.collectAsState()
  val pairingError by viewModel.pairingError.collectAsState()
  val pairingSuccessMessage by viewModel.pairingSuccessMessage.collectAsState()
  val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
  val showUnpairConfirmDialog by viewModel.showUnpairConfirmDialog.collectAsState()

  // 5 Tab Data
  val currentTab by viewModel.currentTab.collectAsState()
  val bucketList by viewModel.bucketList.collectAsState()
  val secretNotes by viewModel.secretNotes.collectAsState()
  val dailyQuestions by viewModel.dailyQuestions.collectAsState()
  val myStatus by viewModel.myStatus.collectAsState()
  val partnerStatus by viewModel.partnerStatus.collectAsState()
  val memoryPins by viewModel.memoryPins.collectAsState()
  val chatMessages by viewModel.chatMessages.collectAsState()
  val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
  val coupleMemories by viewModel.coupleMemories.collectAsState()
  val doubleTapEmoji by viewModel.doubleTapEmoji.collectAsState()

  Crossfade(
    targetState = currentUser,
    label = "main_screen_crossfade",
    modifier = modifier.fillMaxSize()
  ) { user ->
    when {
      user == null -> {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          AuthScreen(
            isSignUp = isSignUpMode,
            isLoading = isAuthLoading,
            errorMessage = authError,
            onSignUp = { email, pass, confirmPass, name, birthDate, avatarPreset, avatarBase64 ->
              viewModel.signUp(email, pass, confirmPass, name, birthDate, avatarPreset, avatarBase64)
            },
            onSignIn = { email, pass ->
              viewModel.signIn(email, pass)
            },
            onToggleMode = { isSignUp ->
              viewModel.setSignUpMode(isSignUp)
            },
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
      !user.isPaired -> {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          PairingScreen(
            user = user,
            pairingInput = pairingInput,
            isPairingInProgress = isPairingInProgress,
            pairingError = pairingError,
            pairingSuccessMessage = pairingSuccessMessage,
            onPairingInputChange = { viewModel.setPairingInput(it) },
            onPairClick = { viewModel.pairWithPartner() },
            onSignOutClick = { viewModel.signOut() },
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
      else -> {
        // 5-Tab Paired Couple Space
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          bottomBar = {
            CoupleBottomNavigationBar(
              currentTab = currentTab,
              onTabSelected = { viewModel.selectTab(it) }
            )
          }
        ) { innerPadding ->
          Crossfade(
            targetState = currentTab,
            label = "tab_crossfade",
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) { tab ->
            when (tab) {
              BottomNavTab.GAMES -> {
                GamesScreen(
                  bucketList = bucketList,
                  secretNotes = secretNotes,
                  dailyQuestions = dailyQuestions,
                  onToggleBucketItem = { viewModel.toggleBucketItem(it) },
                  onAddBucketItem = { title, cat -> viewModel.addBucketItem(title, cat) },
                  onUnlockNote = { viewModel.unlockSecretNote(it) },
                  onAddSecretNote = { title, content, cond -> viewModel.addSecretNote(title, content, cond) },
                  onAnswerDailyQuestion = { qId, ans -> viewModel.answerDailyQuestion(qId, ans) }
                )
              }
              BottomNavTab.MAP -> {
                CoupleMapScreen(
                  currentUser = user,
                  partnerUser = partnerUser,
                  myStatus = myStatus,
                  partnerStatus = partnerStatus,
                  memoryPins = memoryPins,
                  onUpdateMyStatus = { type, emoji, note -> viewModel.updateMyStatus(type, emoji, note) },
                  onAddMemoryPin = { title, loc, cat, date, note -> viewModel.addMemoryPin(title, loc, cat, date, note) }
                )
              }
              BottomNavTab.HOME -> {
                PairedHomeScreen(
                  currentUser = user,
                  partnerUser = partnerUser,
                  bucketList = bucketList,
                  onOpenSettings = { viewModel.openSettings() },
                  onNavigateToTab = { viewModel.selectTab(it) }
                )
              }
              BottomNavTab.CHAT -> {
                ChatScreen(
                  currentUser = user,
                  partnerUser = partnerUser,
                  messages = chatMessages,
                  doubleTapEmoji = doubleTapEmoji,
                  isPartnerTyping = isPartnerTyping,
                  onTypingChanged = { viewModel.setTyping(it) },
                  onMarkAsRead = { viewModel.markChatAsRead() },
                  onSetDoubleTapEmoji = { viewModel.setDoubleTapEmoji(it) },
                  onSendMessage = { text, imageUri, replyMsg -> viewModel.sendChatMessage(text, imageUri, replyMsg) },
                  onDeleteMessage = { id -> viewModel.deleteChatMessage(id) },
                  onEditMessage = { id, newText -> viewModel.editChatMessage(id, newText) },
                  onReactMessage = { id, emoji -> viewModel.reactToChatMessage(id, emoji) }
                )
              }
              BottomNavTab.PROFILE -> {
                val completedGoals = bucketList.count { it.isCompleted }
                ProfileGalleryScreen(
                  currentUser = user,
                  partnerUser = partnerUser,
                  memories = coupleMemories,
                  completedGoalsCount = completedGoals,
                  onOpenSettings = { viewModel.openSettings() },
                  onAddMemory = { title, caption, loc, date, preset, base64, imageUri ->
                    viewModel.addCoupleMemory(title, caption, loc, date, preset, base64, imageUri)
                  },
                  onToggleLikeMemory = { viewModel.toggleLikeMemory(it) }
                )
              }
            }
          }
        }
      }
    }
  }

  // Settings Modal Dialog
  if (showSettingsDialog && currentUser != null) {
    SettingsDialog(
      currentUser = currentUser!!,
      doubleTapEmoji = doubleTapEmoji,
      onSetDoubleTapEmoji = { viewModel.setDoubleTapEmoji(it) },
      onDismiss = { viewModel.closeSettings() },
      onOpenUnpairConfirm = { viewModel.openUnpairConfirm() },
      onSignOut = { viewModel.signOut() }
    )
  }

  // Unpair Confirmation Dialog
  if (showUnpairConfirmDialog) {
    UnpairConfirmationDialog(
      isUnpairing = isPairingInProgress,
      onConfirm = { viewModel.unpair() },
      onDismiss = { viewModel.closeUnpairConfirm() }
    )
  }
}
