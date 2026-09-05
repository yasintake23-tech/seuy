const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2/options");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { initializeApp } = require("firebase-admin/app");

initializeApp();

setGlobalOptions({
  region: "europe-west1",
  maxInstances: 10,
});

exports.sendChatNotification = onDocumentCreated(
  "couples/{coupleId}/messages/{messageId}",
  async (event) => {
    const message = event.data?.data();
    if (!message) return;

    const receiverId = message.receiverId;
    const senderId = message.senderId;
    if (!receiverId || !senderId || receiverId === senderId) return;
    if (message.isDeleted === true) return;

    const db = getFirestore();
    const receiverDoc = await db.collection("users").doc(receiverId).get();
    if (!receiverDoc.exists) return;

    const receiver = receiverDoc.data() || {};
    if (receiver.notificationsEnabled === false) return;

    const token = receiver.fcmToken;
    if (typeof token !== "string" || token.trim().length === 0) return;

    const senderName = String(message.senderName || "Sevgilin");
    const rawText = String(message.text || "").trim();
    const hasPhoto = Boolean(message.imageUrl || message.mediaUrl || message.isPhoto === true);
    const body = rawText || (hasPhoto ? "📸 Sana bir fotoğraf gönderdi" : "Sana yeni bir mesaj gönderdi ❤️");
    const notificationText = rawText.length > 900 ? `${rawText.slice(0, 897)}...` : rawText;

    try {
      await getMessaging().send({
        token,
        notification: {
          title: `${senderName} ❤️`,
          body,
        },
        data: {
          type: "chat_message",
          messageId: String(message.id || event.params.messageId),
          senderId: String(senderId),
          senderName,
          text: notificationText,
          imageUrl: String(message.imageUrl || message.mediaUrl || ""),
          coupleId: String(event.params.coupleId),
        },
        android: {
          priority: "high",
          notification: {
            channelId: "ikimiz_chat_notifications",
            tag: String(message.id || event.params.messageId),
          },
        },
      });
    } catch (error) {
      const code = error?.errorInfo?.code || error?.code || "";
      if (String(code).includes("registration-token-not-registered") ||
          String(code).includes("invalid-registration-token")) {
        await receiverDoc.ref.set(
          { fcmToken: null, fcmTokenUpdatedAt: Date.now() },
          { merge: true }
        );
        return;
      }
      console.error("FCM send failed:", error);
    }
  }
);
