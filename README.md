# 🔊 Gomin — The First Telegram Client with a Ukrainian Soul and Character

[![GitHub Stars](https://img.shields.io/github/stars/HollyLight28/Gomin?style=for-the-badge&color=2AABEE&logo=github)](https://github.com/HollyLight28/Gomin)
[![Platform](https://img.shields.io/badge/Platform-Android-007ACC?style=for-the-badge&logo=android)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-GPL%20v2-orange?style=for-the-badge)](LICENSE)

---

## 🏛️ The Origin Story: How "Gomin" Was Born

This entire journey didn't start in quiet corporate offices, from marketing decks, or business plans. It began with a single, beautiful word that was impossible not to fall in love with — **«Gomin» (Гомін)**. 

In Ukrainian, it stands for the distant, warm murmur of voices, the lively echo of free people gathering together. Once this name was born, it became clear: it was simply too good to let fade away or collect dust in some graveyard of abandoned side-projects. To save this name and breathe life into it, it was worth going through the absolute hell of Android development, fighting with millions of lines of complex legacy C++ and Java code from the original Telegram core, enduring weeks of sleepless nights, and building something real.

Until now, Ukraine didn't have its own custom Telegram client with a distinct national character and soul. Most third-party clients out there are either commercial monsters bloated with sketchy ads, or childish apps covered in useless sparkles and laggy transitions.

**Gomin** is an honest attempt to make Telegram clean, incredibly fast, and truly ours. It's an app created not for commercial profit, but on raw enthusiasm, passion, and love for the craft. Crafted for people who value speed, privacy, and want to feel a genuine local vibe in every single swipe.

---

## 💎 Our Core Killer Feature: The Speed Engine (Gomin Speed Boost)

This is the heart of our app. Standard Telegram is optimized for conservative global mobile network limits, but we have unleashed data transfer speeds to a whole new level.

* **Gomin Boost Extreme (Active by Default)** 🚀:
  When downloading high-res videos, large archives, or original photos, Gomin splits the file into massive **1 MB chunks** and processes them simultaneously over **12 parallel TCP connections** (compared to standard Telegram's conservative 4 streams of 128 KB). This delivers a **2x–3x actual speed increase** on high-speed Wi-Fi or 5G!
* **Extreme Upload Booster** ⚡:
  We expanded the outgoing transmission buffer to **512 KB** (up from 128 KB). Your media and heavy documents fly into the chat instantly, without wasted seconds waiting for socket headers.
* **Shelter Mode (Slow Network Defensive Mode)** 🛡️:
  Our harsh defense reality. When you are in a bomb shelter, subway, basement, or rural area with close to zero coverage where cell signal is barely alive — this mode saves the day. It forces the app to transmit media over a highly stable, single-threaded connection using micro-packets of **32 KB**, preventing connection drops and socket timeouts.

---

## ✨ Real Features & Premium Ergonomics (Only What's in the Code)

Zero marketing fluff. Here is the exact set of features built into Gomin that are fully functional right now:

### 🖤 Exclusive Gomin Black Edition Design
We wanted to make the user interface as clean, high-contrast, and elegant as possible:
* **Pure Black & White Contrast**: No gradients, transitions, or system palette adaptation. Only a deep, pure OLED black background and crystal-clear white accents. This is Black Edition — an uncompromising premium design that relieves eye strain during night reading and saves precious battery life on your device.

### 👻 Quad-Layer Gomin Ghost Mode (Stealth Security)
Your digital presence is your personal space. Stay in the shadows whenever you need to:
* **Unread Messages Guard**: Read incoming chats in full secrecy. Messages remain marked as "unread" on the server until you reply or manually sync.
* **Typing Indicator Mask**: Completely hides your "typing..." or "recording voice..." status. Take your time composing a reply without pressure.
* **Invisible Stories Viewer**: Watch public and personal Stories without leaving a single trace in the viewers list.
* **Offline Presence Lock**: Chat with others while your global status remains permanently "offline".

### 🚨 Gomin Air Alert (Integrated Civil Defense Siren)
Gomin is built with deep care for the safety of our users. We have integrated the official civil defense alert system directly into the core engine:
* **Ajax Systems API Engine**: Connects directly to the official `api.ukrainealarm.com` endpoint using a free developer API key.
* **Granular Location Selector**: Allows you to pick your exact region, city, or local territorial community in settings using interactive dynamic lists fetched in real time.
* **Intelligent Siren Engine**: Operates a lightweight background checking daemon. The moment an air alert is declared in your selected region, Gomin triggers a loud looping alarm siren for 15 seconds to warn you instantly. It plays a gentle notification chime once the threat is cleared.
* **Seamless Local Testing**: Features an instant "Test Alert" button to check sound volumes and verification states locally.

### 🧠 Integrated AI Features (Gomin AI & Gomin Shield)
We have integrated advanced Google Gemini LLM capabilities directly into the core application using native interfaces without exposing any data to third-party proxy servers:
* **🤖 Gomin AI Chat Assistant** 🌟:
  An elite virtual AI assistant chat (`dialog_id = 99999999L`) accessible directly from your main chat screen via a custom **Lucide Sparkles** Floating Action Button (FAB) or in settings.
  - **Dynamic Intercept Engine**: Emulates a 100% native Telegram chat using high-speed local JSON storage with a strict **1000-message buffer**. No network payload is sent to MTProto servers, preventing any account desync.
  - **Premium Polish**: Renders standard native conversation bubbles, displays "Gomin AI is typing..." real-time ActionBar subtitle states, supports clear history commands, and features a model selector (Flash/Pro) inside the three-dot header dropdown.
* **🛡️ Gomin Shield (Manipulation & Gaslighting Analyzer)** 🚨:
  A highly sophisticated native behavioral profiler integrated directly into private chats.
  - **One-Touch Scan**: Tap the thin custom Lucide outline Shield icon in any private dialogue header to pull the last **1000 text messages** and analyze them against professional psycholinguistic frameworks.
  - **Deep-Dive Diagnostic**: Instantly constructs a native bottom sheet containing a gorgeous custom markdown-rendered report outlining **Red Flags (🚩)**, **Hidden Intentions (🧩)**, **Defense Strategies (🛡️)**, and an **Overall Toxicity Meter (📊)**.
  - **Bridge to Assistant**: Tap the **"💬 Discuss in Gomin AI"** button at the bottom of the analysis sheet. It closes the sheet, opens your Gomin AI chat, and **pre-seeds** the entire conversation context dynamically! The AI assistant starts fully aware of the dialogue, ready to draft tactically solid replies or guide you on communication boundaries.

### 🎯 Usability Upgrades
* **Toggle #16: "Delete for All" by Default** 🗑️:
  No more annoying popups asking if you want to check that box every time you delete a message. We turned this on by default. Just hit delete — and sleep peacefully knowing it's gone for everyone.
* **Decluttered Lower Search Layout**:
  We moved the search bar from the top header down to the bottom navigation area and removed the extra visual input field, keeping the UI clean and minimalist. This is our signature upgrade for effortless one-handed control!
* **Crafted Typography**:
  Instead of default system fonts, Gomin utilizes hand-picked typography (such as **Manrope** assets) to make reading long messages smooth and comfortable on your eyes.
* **Zero Commercial Bloat**:
  We completely stripped sponsored proxy ads and visual trackers. Gomin operates faster and never collects your personal data.

---

## 🗺️ Development Roadmap: What's Next?

Gomin is just getting started. Here is our honest development plan, which we will implement step-by-step:

- [x] Full removal of old legacy references, interface clean-up, and flat Gomin settings.
- [x] Implementation of the 12-thread network booster and Shelter-Mode.
- [x] Rock-solid 4-layer Ghost Mode.
- [x] Automatic message deletion for all participants by default.
- [x] Integration of adaptive launcher icons (Adaptive Icons) with a gorgeous white bird on a blue background (#2AABEE) and perfect outline borders.
- [x] **Gomin AI & Gomin Shield**: Interactive virtual Gemini assistant chat, model settings selector, clear history actions, and premium gaslighting profiler with pre-seeded context bridging.
- [ ] **Gomin Drive**: A native, ultra-clean personal cloud workspace inside "Saved Messages" using the `MediaActivity` layout to manage files without hacks or hashtags.

---

## 🧪 How to Verify and Test Gomin AI, Shield & Air Alert

Follow these straightforward steps to experience the complete Gomin feature set on your local build:

1. **Set Up Gomin AI & Shield**:
   - Navigate to Gomin **Settings** -> **Gomin AI Settings**.
   - Enter your personal Google Gemini API Key.
   - Enter your preferred model (e.g., `gemini-1.5-flash` or `gemini-1.5-pro`).
2. **Launch Gomin AI Chat Assistant**:
   - Go to your main **Chats List** (`DialogsActivity`).
   - You will notice a gorgeous new thin **Lucide Sparkles FAB button** floating elegantly above the write/stories buttons on the right side.
   - Tap it to instantly launch the native Gomin AI private chat! Send some queries, test the response speeds, watch the "typing..." header state, and try selecting a model or clearing history via the top right corner three-dot menu.
3. **Run Gomin Shield (Behavioral Profiling)**:
   - Open any private dialogue with another user.
   - Tap the three-dot menu in the upper-right corner.
   - You will see a dedicated Lucide-style thin outline **Shield icon** (🛡️).
   - Tap it to initiate an automatic analysis. The app will extract the chat history, query the Gemini API, and present a breathtaking, structured bottom sheet with complete behavioral metrics, flags, and tactical defense recommendations.
4. **Test Context Pre-Seeding**:
   - In the resulting Gomin Shield report sheet, tap the **"💬 Discuss in Gomin AI"** button.
   - The sheet will dismiss and open the Gomin AI chat.
   - Type *"Give me a specific advice on how to respond to their last message"* or *"What is their primary gaslighting method here?"*.
   - Watch the assistant respond with absolute awareness of the chat context!
5. **Verify and Test Gomin Air Alert**:
   - Navigate to Gomin **Settings** -> **Gomin Settings**.
   - Scroll down to the **Air Alert 🚨** section.
   - Enable alerts via the main switch.
   - Enter your free developer API key from `api.ukrainealarm.com` (Ajax systems).
   - Tap **Your Region** to dynamically load oblasts/municipalities and select your exact region.
   - Tap **Test Alert** to instantly trigger a 15-second simulation check (it will play the loud air alert alarm sound and dismiss safely).

---

## ☕ Support the Artisan

Gomin is developed and maintained by a single developer who spends his evenings, nights, and weekends building this code — simply because he wanted to save a beautiful name and create a high-quality Ukrainian product for his people.

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
