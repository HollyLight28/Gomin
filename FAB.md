# FAB & Live API Integration Plan (Senior AI Engineer Edition)

## 1. Vision
Transform the Gomin AI entry point into a premium, dual-purpose interaction hub. Moving away from the "mini-sub-button" look to a full-sized, unified FAB (Floating Action Button) system that supports both standard chat and instant Live API activation.

## 2. UI/UX Overhaul: The 60dp FAB
### Changes to `FragmentFloatingButton.java`:
*   **Scale Increase**: Upgrade the base size from 48dp to **60dp** for both `floatingButton3` (New Message) and `floatingButtonAi`.
*   **Dynamic Sizing**: Implement a setter or layout parameter update to handle the 60dp dimensions programmatically.
*   **Shadow & Elevation**: Ensure shadows match the larger footprint (matching Material 3 standards for larger FABs).

### Changes to `DialogsActivity.java`:
*   **Style Alignment**: Change `floatingButtonAi` initialization from `isSubButton = true` to `false`. This forces a perfectly circular shape and the primary accent background color.
*   **Icon Update**: Replace the small PNG `gomin_bird` with the high-definition vector `@drawable/icon_foreground_gomin`.
*   **Positioning Logic**: Update `updateFloatingButtonOffset` and `createSubButtonLayoutParams` to accommodate the 60dp size. The vertical stack spacing will be adjusted (Stories -> AI FAB -> New Message).

---

## 3. The "Jarvis" Trigger (Long-Tap Logic)
### Implementation:
1.  **Haptic Feedback**: Trigger a "Heavy" vibration on `onLongClick`.
2.  **Activation**:
    *   **Single Tap**: Opens the `ChatActivity` with `Constants.GOMIN_AI_DIALOG_ID`.
    *   **Long Tap**: Directly triggers `GominAiChatHelper.INSTANCE.toggleLiveSession(this)`.
3.  **Cross-Fragment Support**: Ensure `GominAiChatHelper` can handle the `DialogsActivity` context for Live API overlays.

---

## 4. Why WhatsApp Design is Often Critiqued
*   **Inconsistency**: Until very recently, WhatsApp mixed old Material Design 2 with custom elements, leading to a "dated" feel.
*   **The "Squircle" Obsession**: Their square-ish FABs and inconsistent corner radii often clash with the native OS look (both on iOS and Android).
*   **Complexity**: The settings and navigation menus are often buried under multiple layers, unlike Telegram's flatter, more fluid structure.
*   **Color Palette**: The specific shade of "WhatsApp Green" is often seen as too jarring for OLED/Dark modes compared to Telegram's adaptive accent system.

---

## 5. Summary of Tasks for Next Session:
- [ ] Modify `FragmentFloatingButton.java` for 60dp support.
- [ ] Update `DialogsActivity.java` (Circle shape, Vector Icon, 60dp size).
- [ ] Implement `onLongClickListener` for Gomin AI FAB.
- [ ] Refactor `GominAiChatHelper` for universal Live Session launching.
- [ ] Final Architectural Review (Search for 3 bugs).

*Created by Senior AI Engineer - June 1, 2026*
