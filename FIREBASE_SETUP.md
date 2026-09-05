# HeartBond Firebase altyapısı

Bu sürümde sohbet bildirimleri uygulama içi eski mesaj taramasından değil, tek bir FCM + Cloud Function akışından gelir.

## Bir kez yapılacak

```bash
npm install -g firebase-tools
firebase login
firebase use ikimiz-7306c
cd functions
npm install
cd ..
firebase deploy --only functions,firestore:rules
```

Cloud Functions için Firebase projesinin Blaze planında olması gerekir.

## Bildirim akışı

```text
Android → Firestore /couples/{coupleId}/messages/{messageId}
      ↓
Cloud Function: sendChatNotification
      ↓
users/{receiverId}.fcmToken
      ↓
Firebase Cloud Messaging
      ↓
Android sistem bildirimi / foreground bildirimi
```

`users/{uid}.notificationsEnabled=false` ise Function bildirim göndermez.
