const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

exports.sendChatNotification = onDocumentCreated(
  "Chats/{chatId}/Messages/{messageId}", // <-- এখানে সাবকলেকশন নাম 'Messages' capital M
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const messageData = snapshot.data();
    const senderId = messageData.senderId;
    const receiverId = messageData.receiverId;
    const messageText = messageData.message || "📩 New Message";

    const receiverDoc = await db.collection("users").doc(receiverId).get();
    const receiverToken = receiverDoc.exists ? receiverDoc.data().fcmToken : null;

    if (!receiverToken) return;

    await getMessaging().send({
      notification: { title: `New message`, body: messageText },
      token: receiverToken,
    });

    console.log(`Notification sent to ${receiverId}`);
  }
);
