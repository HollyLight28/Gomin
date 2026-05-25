# 🔊 Gomin — The First Telegram Client with a Ukrainian Soul and Character

[![GitHub Stars](https://img.shields.io/github/stars/HollyLight28/Gomin?style=for-the-badge&color=2AABEE&logo=github)](https://github.com/HollyLight28/Gomin)
[![Platform](https://img.shields.io/badge/Platform-Android-007ACC?style=for-the-badge&logo=android)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-GPL%20v2-orange?style=for-the-badge)](LICENSE)

---

## 🏛️ The Origin Story: How "Gomin" Was Born

This entire journey didn't start with corporate decks, marketing budgets, or technical specifications. It began with a single, beautiful word that was impossible not to fall in love with — **«Gomin» (Гомін)**. 

In Ukrainian, it stands for the distant, warm murmur of voices, the echo of free people gathering together. Once this name was born, it became clear: it was simply too good to let fade away or collect dust in some graveyard of abandoned side-projects. To save this name and breathe life into it, it was worth going through the absolute hell of Android development, fighting with millions of lines of complex legacy code, and building something real.

Until now, Ukraine didn't have its own custom Telegram client with a distinct national character. Most third-party clients out there are either commercial monsters bloated with sketchy ads, or childish apps covered in useless sparkles and laggy transitions.

**Gomin** is an honest attempt to make Telegram clean, incredibly fast, and truly ours. It's an app created not for commercial profit, but on raw enthusiasm and love for the craft. Crafted for people who value speed, privacy, and want to feel a genuine local vibe in every single swipe.

---

## ⚡ The Speed Engine: When Performance Actually Matters

We won't bore you with corporate talk about "cloud innovations". Let's be completely honest: under the hood, Gomin features a **hardware-accelerated network engine** designed for real-world scenarios.

| Network Mode | What's Happening Under the Hood? | Primary Use Case |
| :--- | :--- | :--- |
| **Gomin Boost Extreme** 🚀 | Splits media downloads into massive **1 MB chunks** and downloads them simultaneously over **12 parallel threads** (compared to standard Telegram's 4 streams of 128 KB). | To extract every single drop of speed from your high-speed 5G or gigabit fiber connection. High-res videos and files download 2-3x faster. |
| **Shelter Mode (Slow Network)** 🛡️ | Forces a highly stable, single-threaded connection using micro-packets of **32 KB**. | The harsh reality of modern Ukraine. When you are in a bomb shelter, subway, or basement where cell coverage is close to dead — this mode holds onto the connection with its teeth, preventing timeouts and packet drops. |

On the upload side, we expanded the outgoing buffer to **512 KB** (up from 128 KB) to ensure your media and heavy documents fly into the chat without wasted seconds.

---

## ✨ Signature Features (What's Actually Inside)

Zero marketing fluff. Here is the exact set of features built into Gomin that are fully functional right now:

### 👻 Gomin Ghost Mode (Stealth Security)
Your digital presence is your personal space. We've built 4 straightforward, rock-solid stealth features:
* **Unread Messages Guard**: Read incoming chats in full secrecy. Messages remain marked as "unread" on the server until you reply or manually sync.
* **Typing Indicator Mask**: Completely hides your "typing..." or "recording voice..." status. Take your time composing a reply without pressure.
* **Invisible Stories Viewer**: Watch public and personal Stories without leaving a single trace in the viewers list.
* **Offline Presence Lock**: Chat with others while your global status remains permanently "offline".

### 🧠 Gemini Voice-to-Text Bypass (Free Transcription)
Standard Telegram demands a paid Telegram Premium subscription to transcribe voice notes to text. 
* Gomin completely bypasses this artificial wall. By entering your personal, free **Gemini API Key** (which Google provides for free in just 1 minute) in Gomin settings, the app transcribes any voice message to text absolutely free, powered by Google's state-of-the-art AI.

### 🎨 Clean Aesthetics Without Visual Junk
* **True OLED Black**: A gorgeous, high-contrast black interface that looks incredibly premium and saves precious battery life on OLED screens.
* **Zero Ads & Trackers**: We completely stripped sponsored proxy ads and visual trackers. Only clean code, maximum speed, and total data privacy.
* **Zero Visual Clutter**: We removed broken layout options (such as the non-functional "folders at bottom" toggle), annoying snowflakes, and confusing experimental menus. Instead, you get a clean, single-screen flat settings layout.
* **Crafted Typography**: Instead of generic system fonts, Gomin utilizes hand-picked typography (such as Manrope assets) to make reading long messages smooth and comfortable on your eyes.

---

## 🗺️ Development Roadmap: What's Next?

Gomin is just getting started. Here is our honest development plan, which we will implement step-by-step:

- [x] Full removal of old legacy references, interface clean-up, and flat Gomin settings.
- [x] Implementation of the 12-thread network booster and Shelter-Mode.
- [x] Rock-solid 4-layer Ghost Mode.
- [ ] **Gomin Drive**: A native, ultra-clean personal cloud workspace inside "Saved Messages" using the `MediaActivity` layout to manage files without hacks or hashtags.
- [ ] **Gomin AI**: An interactive, localized Gemini assistant chat running on your local SQLite database to summarize conversations on the fly.

---

## ☕ Support the Artisan

Gomin is developed and maintained by a single developer who spends his evenings, nights, and weekends building this code — simply because he wanted to create a beautiful, high-quality Ukrainian product.

If Gomin made your communication faster, cleaner, and more enjoyable, consider supporting the author with a coffee or helping pay for update servers:

👉 **[Treat the Author to a Coffee (Monobank Jar) ☕](https://send.monobank.ua/jar/4ecLBi7WaZ)**

---

## 🛠️ Compilation & Local Build

To compile Gomin locally on your machine:

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/HollyLight28/Gomin.git
   ```
2. **Open in Android Studio** and let Gradle sync.
3. **Build the Standalone Version** by running the following command in the terminal:
   ```bash
   ./gradlew :TMessagesProj_AppStandalone:assembleAfatStandaloneDebug
   ```

---

*Engineered with the vibrations of a free Ukraine and a passionate heart.*
