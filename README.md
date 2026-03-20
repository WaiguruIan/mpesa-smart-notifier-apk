# M-ALERT 🚨

Have you ever gotten a notification from your phone hoping it was an MPESA message only to find out it is an advertising SMS? Or have you ever gotten an MPESA message but don't know if it is money you have received, sent, or just a balance message that came late? 

**Introducing M-ALERT.**

M-ALERT is a smart Android utility that intercepts M-PESA SMS notifications and replaces the default system sound with custom audio alerts based on the transaction type.

## ✨ Features
- **Custom Sounds:** Distinct alerts for Money Received, Money Sent/Paid, Balance Inquiries, and System Errors.
- **Smart Filtering:** Specifically targets messages from the "MPESA" sender ID.
- **Notification Management:** Silences the default system SMS "ding" for M-PESA and replaces it with a branded, silent notification in the tray.
- **Privacy Focused:** Runs locally on your device as a Background Service.

## 🛠 How It Works
The app utilizes the `NotificationListenerService` API to:
1. Detect an incoming notification from the "MPESA" sender.
2. Immediately cancel the original notification to stop the default sound.
3. Parse the message content for keywords like "received", "paid", or "balance".
4. Play the corresponding high-quality `.mp3` alert.
5. Re-post a silent notification with the **M-ALERT** logo to keep your records visible.

## 📥 Installation & Setup
1. Download the `M-ALERT.apk` from the [Releases](#) section.
2. Open the app and click **Grant Permission** to enable Notification Access.
3. **Important:** To ensure the app works in the background, go to:
   *Settings > Apps > M-ALERT > Battery* and select **Unrestricted**.

## 🚀 Development
Built with:
- **Kotlin**
- **Jetpack Compose** (UI)
- **NotificationListenerService** (Core Logic)
- **MediaPlayer API** (Audio)

## ⚖️ License
This project is for personal use and educational purposes.
