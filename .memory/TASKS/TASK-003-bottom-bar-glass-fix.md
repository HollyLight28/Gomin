# TASK-003: Bottom Bar (MainTabs) — чистий glass border + видимий selected tab

**Статус:** COMPLETED
**Дата:** 2026-06-03
**Залежності:** немає

## Опис

Виправити подвійний border навколо bottom bar (MainTabs) в Black Edition dark theme. Прибрати артефакти, зробити чистий скляний ефект з одним border. Підняти видимість вибраного таба.

## Чому це важливо

На dark theme (особливо Black Edition AMOLED) bottom bar має 3 лінії border: outer stroke (15% білий roundRect) + inner top stroke (20% білий) + inner bottom stroke (13% білий). Вони накладаються одна на одну і створюють "брудний" контур, який виглядає як некрасиве коло навколо пігулки.

## Файли, які треба змінити

- `TMessagesProj/src/main/java/org/telegram/ui/Components/blur3/drawable/color/impl/BlurredBackgroundProviderImpl.java:27-28` — занулити top/bottom strokes
- `TMessagesProj/src/main/java/org/telegram/ui/Components/glass/GlassTabView.java:152` — підняти opacity selected pill

## Детальний план

### 1. Прибрати подвійний border (inner strokes)

**Файл:** `BlurredBackgroundProviderImpl.java:27-28`

**Що зараз:**
```java
.setStrokeColorTop(0x22000000, 0x33FFFFFF)    // Top: light=13% black, dark=20% white
.setStrokeColorBottom(0x33000000, 0x22FFFFFF)  // Bottom: light=20% black, dark=13% white
```

**Що треба зробити:**
Занулити обидва stroke — вони створюють подвійний border з outer stroke в MainTabsActivity.

```java
.setStrokeColorTop(0x00000000, 0x00000000)     // transparent
.setStrokeColorBottom(0x00000000, 0x00000000)  // transparent
```

**Чому:** Outer stroke (MainTabsActivity.java:274-286) вже малює чистий roundRect навколо всієї пігулки. Inner strokes додають зайві лінії зверху/знизу, які створюють артефакт "коло навколо контура".

### 2. Підняти видимість вибраного таба

**Файл:** `GlassTabView.java:152`

**Що зараз:**
```java
paintCounterBackground.setColor(Theme.multAlpha(colorSelected, 0.09f * alpha));
```

**Що треба зробити:**
Підняти opacity з 0.09f (9%) до 0.17f (17%) — достатньо, щоб чітко бачити вибраний таб, але без втрати мінімалістичного стилю.

```java
paintCounterBackground.setColor(Theme.multAlpha(colorSelected, 0.17f * alpha));
```

## Code Review: 3 знайдені баги у змінах

### БАГ 1 (FIXED): Light theme — повністю без border
- **Де:** `BlurredBackgroundProviderImpl.java:27-28` + `MainTabsActivity.java:274`
- **Причина:** Outer stroke малюється тільки на dark theme (`if (Theme.isCurrentThemeDark())`). Inner strokes були занулені для обох режимів.
- **Наслідок:** На light theme bottom bar не мав жодної лінії.
- **Фікс:** Залишити inner strokes для light mode. `setStrokeColorTop(0x22000000, 0x00000000)` — тільки dark = transparent.

### БАГ 2 (FIXED): Search button на light theme — без border
- **Де:** `MainTabsActivity.java:380` + `GlassTabView.java:216`
- **Причина:** Search button використовує `BlurredBackgroundProviderImpl.mainTabs()` для фону. А `drawGlassBorder` малюється тільки `if (Theme.isCurrentThemeDark())`.
- **Наслідок:** Search button на light theme не мав border.
- **Фікс:** Той самий, що і для Баг 1 — light theme inner strokes повернуті.

### БАГ 3 (UNRESOLVED): Outer stroke 15% без inner strokes на dark theme
- **Де:** `MainTabsActivity.java:279` + `BlurredBackgroundProviderImpl.java:27-28`
- **Причина:** Outer stroke (15% білий) був оверлеєм поверх inner strokes (20% top + 13% bottom). Ефективна яскравість верхнього краю була ~32%, тепер тільки 15%.
- **Наслідок:** Border на dark theme може виглядати тьмяніше, ніж задумано.
- **Статус:** Не виправлено. Якщо visually border занадто тьмяний — підняти `0x26FFFFFF` → `0x33FFFFFF` в `MainTabsActivity.java:279`.

## Acceptance Criteria

- [ ] На dark theme bottom bar має рівно один clean border (15% білий roundRect)
- [ ] Немає артефактів/подвійних ліній навколо пігулки
- [ ] Selected tab pill чітко видно (не зливається з фоном)
- [ ] Light theme не зламана (inner strokes змінені на transparent для обох режимів)
- [ ] Build проходить

## Known Risks

- **Ризик:** На light theme outer stroke не малюється (тільки на dark). Якщо inner strokes прибрані, на light theme не буде жодного border на барі.
  - **Рішення:** На light theme inner strokes тепер transparent, але outer stroke не активний. Це ок — на light theme фон білий, і бар і так видно. Якщо треба буде border — можна активувати outer stroke для light теж.
