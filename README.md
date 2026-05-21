# 🔊 Gomin — The Executive Telegram Suite for Ukraine

**Gomin** is a high-end, distraction-free Telegram client designed for those who value premium aesthetics, ultimate performance, and absolute privacy. Built on top of a highly optimized Telegram core, Gomin redesigns the messaging experience into a sleek, professional workspace with a deeply embedded Ukrainian soul.

---

## 🏛️ The Vision

Gomin is crafted to transition away from the chaotic hyper-customization of common forks toward an elegant, **WhatsApp-like minimalism**. We believe that a premium tool should work flawlessly out of the box. By hardcoding high-performance network settings, fluid spring animations, and strict privacy defaults, Gomin frees you from system clutter, letting you focus entirely on what matters: secure and elite communication.

---

## ✨ Signature Killer Features

### ☁️ Gomin Drive (100% Native Personal Cloud)
Telegram has always been an outstanding file storage platform, but its interface hides this potential. **Gomin Drive** unleashes it without clunky hacks or hashtags:
* **One-Tap Access**: A dedicated Cloud icon inside your "Saved Messages" chat takes you instantly to your drive.
* **Zero Overhead**: Fully leverages Telegram's native `MediaActivity` (Shared Media) framework.
* **Automatic Categorization**: Your files, music, links, and voice notes are automatically sorted into clean tabs with native search, running on Telegram's high-speed database.
* **Beautiful & Fast**: Fully respects your active theme, with ultra-smooth native transitions.

### 🧠 Gomin AI Assistant (Seamless Gemini Integration)
Skip low-quality third-party chat overlays. Gomin AI brings Gemini directly into Telegram's native, ultra-responsive `ChatActivity`:
* **Native Interface**: Operates as a local virtual chat right in your dialog list, supporting Telegram's fluid bubble animations, markdown rendering, text selection, and rich media.
* **Contextual Intelligence**: Tap the attachment clip 📎 or the top menu to "Connect Chat Context." Select any personal or group dialogue, and Gomin AI will analyze the last 150 messages to generate instant summaries, action items, or tone suggestions.
* **100% Local database**: Conversations are saved securely in your local SQLite app database, never exposed to external tracking.

### ⚡ Gomin Speed Boost (Extreme Multi-Connection Engine)
Enjoy extreme media transfer speeds without configuration anxiety:
* **Extreme Downloads**: Features a built-in multi-connection download engine (`BOOST_EXTREME` by default) that maximizes bandwidth utility for rapid media acquisition.
* **Accelerated Uploads**: Optimizes socket buffer allocations and stream piping to upload documents and high-resolution videos in seconds.
* **Battery-Optimized**: Hardcoded to work in harmony with your device's network chip to avoid thermal throttling.

### ✍️ Executive Typography & Clean Slate UX
* **Custom Typography**: Features pre-loaded, highly legible branded typography specifically optimized for long-form reading comfort on mobile screens.
* **Decluttered Settings**: Replaces hundreds of confusing, unstable toggle switches with a clean, handpicked layout that ensures system stability and aesthetic harmony.

---

## 🛠️ Technical Architecture

* **Base Core**: Telegram for Android (Custom Optimized Core)
* **Understream Engine**: Cherrygram Framework
* **Programming Languages**: Kotlin (54%), Java (42%), C++ (JNI for cryptography and high-speed core tasks)
* **AI Architecture**: Google Gemini API Integration via secure local endpoints
* **Security & Privacy**: Zero analytics/trackers, forced TLS verification, automated proxy promotion blocking.

---

## 🚀 Development & Build

To compile Gomin locally on your machine:

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/HollyLight28/Gomin.git
   ```
2. **Open in Android Studio**:
   Import the project root. Let the Gradle sync complete.
3. **Build the Standalone Version**:
   Use the terminal to compile the standalone package:
   ```bash
   ./gradlew :TMessagesProj_AppStandalone:assembleAfatStandaloneDebug
   ```

---

## 📜 License

Gomin is free software distributed under the **GNU GPL v2** license. We express our utmost respect and gratitude to the original developers of Telegram and Cherrygram, upon whose robust foundations this project proudly stands.

---

*Engineered with the vibrations of a free Ukraine.*
