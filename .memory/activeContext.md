# Gomin Project State - 2026-05-20

## [CURRENT MISSION]: Commit and push the true bold and regular Nunito fonts, trigger GitHub CI/CD, and run a local Standalone build for verification.

## [COMPLETED ATOMIC STEPS]
- Проаналізовано директорії збірки на наявність APK-файлів.
- Виявлено та задокументовано відмінності між Standalone, Debug та Release збірками.
- Завантажено оригінальні файли `PlusJakartaSans-Regular.ttf` та `PlusJakartaSans-SemiBold.ttf` з репозиторію Tokotype.
- Зроблено бекапи (`geist.ttf.bak` / `geist_medium.ttf.bak`) та проведено "гарячу заміну" шрифтів в ассетах `TMessagesProj/src/main/assets/fonts/` (записано Plus Jakarta Sans поверх geist). Це виключає зміни коду Java/Kotlin та забезпечує максимальну стабільність.
- Отримано повне підтвердження від користувача на стратегію "гарячої заміни" шрифтів (підміна бінарників).
- Успішно завершено Gradle-збірку Standalone версії (`task-761`) за 5 хвилин 2 секунди.
- **Нове дослідження**: За допомогою скрипта інспекції `inspect_font.py` підтверджено, що поточні активні файли `geist.ttf` та `geist_medium.ttf` (які є Plus Jakarta Sans) містять рівно **0 кириличних гліфів** в таблиці `cmap`. Це повністю пояснює тихий фолбек (silent fallback) на Roboto для українського тексту на фізичному пристрої.
- **Верифікація Nunito**: Перевірено та підключено якісні статичні файли шрифту Nunito (інтегровано як `geist.ttf` та `geist_medium.ttf` в ассети). Вони містять повну підтримку українських літер (`і`, `ї`, `є`, `І`, `Ї`, `Є`) з приємними округлими закінченнями літер, які неможливо сплутати з квадратними Roboto.
- **Діагностика товщини шрифтів**: Виявлено, що попередні файли `geist.ttf` та `geist_medium.ttf` в ассетах проекту мали практично однаковий розмір (~132 KB), оскільки обидва містили Regular-вагу шрифту. Це викликало надмірно тонкі заголовки чатів та вкладок.
- **Завантаження офіційних шрифтів**: За допомогою Google Fonts API завантажено справжні оригінальні TTF-файли:
  - `Nunito-Regular` (125 504 байт)
  - `Nunito-Bold` (125 440 байт)
- **Верифікація кирилиці**: Скриптом `check_glyphs.py` математично підтверджено наявність усіх українських символів (`і`, `ї`, `є` тощо) в обох файлах.
- **Успішна гаряча заміна**: Нові верифіковані Regular та Bold шрифти успішно скопійовані замість `geist.ttf` та `geist_medium.ttf` в ассети.
- **Ініціалізація Git операцій**: Отримано запит від користувача зробити коміт та пуш для автоматичної збірки на GitHub.

## [MODIFIED FILES]
- `TMessagesProj/src/main/assets/fonts/geist.ttf` -> Замінено на офіційний Nunito Regular з повною підтримкою української кирилиці та округлими гліфами.
- `TMessagesProj/src/main/assets/fonts/geist_medium.ttf` -> Замінено на офіційний Nunito Bold з повною підтримкою української кирилиці для жирних заголовків та табів.
- `.memory/activeContext.md` -> Оновлення контексту поточної місії та списку модифікованих файлів.

## [OPEN PROBLEMS]
- Жодних критичних проблем не виявлено.

## [GIT COMMIT MESSAGE]
```
rebrand: integrate true bold and regular Nunito static fonts
```

