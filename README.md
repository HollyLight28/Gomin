# 🔊 Gomin — The Executive Telegram Suite with a Ukrainian Soul

[![GitHub Stars](https://img.shields.io/github/stars/HollyLight28/Gomin?style=for-the-badge&color=2AABEE&logo=github)](https://github.com/HollyLight28/Gomin)
[![Platform](https://img.shields.io/badge/Platform-Android-007ACC?style=for-the-badge&logo=android)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-GPL%20v2-orange?style=for-the-badge)](LICENSE)

**Gomin** is not just another custom Telegram fork. It is a premium, high-performance executive communication tool engineered specifically for those who demand elite aesthetics, extreme speed, and absolute privacy. 

Built on top of a deeply optimized Telegram core, Gomin strips away the chaotic, visual clutter of generic forks and replaces it with an elegant, **minimalist layout, premium typography, and military-grade stealth features**. 

---

## 🏛️ The Origin & Philosophy: Why Gomin?

> *"It all started with the name: **Gomin** (Гомін) — the distant, vibrant murmur of voices, the echo of free people. It carried a unique Ukrainian vibe, a code that simply couldn't be allowed to fade away."*

In a landscape filled with hyper-customized forks that look like neon toys, Gomin stands apart as a monument of executive restraint. We believe that a high-end tool should work flawlessly out of the box. 

Our mission is to build the **first premium Telegram client with true Ukrainian roots**—crafted for resilience, high performance, and visual excellence. Gomin bridges the gap between raw messaging and professional productivity, all while retaining a clean, distraction-free atmosphere.

---

## ⚡ The Speed Engine: Real High-Performance Stats

Standard Telegram clients are built for baseline network conditions. Gomin is built to extract every single drop of bandwidth from your Gigabit Wi-Fi or 5G connection.

| Network Metric | Standard Telegram | Gomin (Boost Extreme) | Gomin (Slow Network Mode) |
| :--- | :--- | :--- | :--- |
| **Download Threads** | 4 parallel streams | **12 parallel streams** 🔥 | 1 stable stream |
| **Download Buffer** | 128 KB chunks | **1 MB chunks** 🚀 | 32 KB micro-packets |
| **Upload Buffer** | 128 KB chunks | **512 KB chunks** ⚡ | 32 KB micro-packets |
| **Target Network** | Basic Mobile Data | 5G / High-Speed Fiber | Bomb Shelters / Basements 🛡️ |

### 🚀 BOOST_EXTREME
By default, Gomin splits your media downloads into massive **1MB chunks** and processes them through **12 parallel TCP connections**. This yields up to a **2x–3x actual speed increase** when downloading high-resolution videos, large archives, or uncompressed media.

### 🛡️ Shelter-Ready Defensive Mode (Slow Network)
An engineering fallback designed for the reality of modern Ukraine—bomb shelters, basements, or rural areas with close to zero cell reception. When activated, it forces a ultra-stable, single-threaded connection using tiny **32KB packets**, preventing connection dropouts and socket timeouts when every kilobyte counts.

---

## ✨ Signature Features (What's Actually Inside)

We don't sell vaporware. Here is the exact suite of premium features engineered into Gomin right now:

### 👻 Gomin Ghost Mode (Military-Grade Stealth)
Take absolute control over your digital presence with our quad-layer private stealth engine:
* **Unread Messages Guard**: Read incoming chats in full secrecy. Messages remain marked as "unread" on the server until you reply or manually sync.
* **Typing Indicator Mask**: Completely hides your "typing..." or "recording voice..." status.
* **Invisible Stories Viewer**: Watch public and personal Stories without leaving a trace in the viewers list.
* **Offline Presence Lock**: Appear offline or hidden while staying fully active.

### 🧠 Gemini Voice-to-Text Bypass (Free Transcription)
Telegram's native voice-to-text transcription is locked behind the paid Telegram Premium subscription. Gomin bypasses this restriction entirely. 
* By entering your personal, free **Gemini API Key** in Gomin settings, you can transcribe any number of voice messages to text absolutely free, powered by Google's state-of-the-art AI.

### 🎨 Executive Aesthetics & Monet Engine
* **Material You (Monet)**: Fully adapts to your system palette (Android 12+), gracefully shifting colors to match your wallpaper.
* **Branded Typography**: Pre-loaded with Vercel's **Geist** font for messages (ultimate legibility) and **Playfair Display** for headers (classic elegance).
* **True Black Edition**: High-contrast, pure OLED black UI that looks premium and saves battery.
* **Decluttered Settings**: We completely removed experimental bugs, ugly "snowflakes," and visual trash, consolidating everything into a clean, flat settings screen.

### 🚫 Anti-Tracker & Ad-Block Core
* Fully stripped of all analytical trackers, crash report telemetry (except local ones), and sponsored proxy ads. No commercial bloat.

---

## 🗺️ The Vision & Roadmap

We are constantly pushing Gomin forward. Here is our direct development path:

- [x] Flat, premium settings layout & Monobank integration.
- [x] Custom executive typography and ActionBar scaling fixes.
- [x] Extreme 12-thread network booster and Shelter-Mode.
- [x] Quad-layer Gomin Ghost Mode.
- [ ] **Gomin Drive**: A 100% native, ultra-clean personal cloud workspace inside "Saved Messages" using the `MediaActivity` layout.
- [ ] **Gomin AI**: An interactive, localized Gemini virtual assistant chat that runs on your local SQLite database and analyzes custom chat contexts.

---

## ☕ Support the Artisan

Gomin is built by a solo developer who puts his heart, soul, and endless sleepless nights into this code. There are no corporate investors or paid subscriptions. 

If Gomin has made your communication faster, cleaner, and more elite, consider treating the author to a coffee or helping pay for the update servers:

👉 **[Treat the Author to a Coffee (Monobank Jar) ☕](https://send.monobank.ua/jar/4ecLBi7WaZ)**

---

## 🛠️ Compilation & Local Build

To compile Gomin locally on your machine:

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/HollyLight28/Gomin.git
   ```
2. **Open in Android Studio**:
   Import the project root and let Gradle sync.
3. **Build the Standalone Version**:
   Run the following command in the terminal to compile the standalone debug build:
   ```bash
   ./gradlew :TMessagesProj_AppStandalone:assembleAfatStandaloneDebug
   ```

---

*Engineered with the vibrations of a free Ukraine.*
