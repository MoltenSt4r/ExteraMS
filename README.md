<div align="center">

<img src="assets/logo.png" width="128" alt="MoltenGram">

# MoltenGram

Экспериментальный форк exteraless — новые фишки раньше всех

[![Исходный код](https://img.shields.io/badge/GitHub-MoltenSt4r%2FExteraMS-993c38?logo=github)](https://github.com/MoltenSt4r/ExteraMS)
[![Upstream](https://img.shields.io/badge/Upstream-exteraless-blue?logo=github)](https://github.com/exteraless/exteraless)

</div>

---

## Что это

**MoltenGram** — экспериментальный форк [exteraless](https://github.com/exteraless/exteraless) (идейного наследника [exteraGram](https://github.com/exteraSquad/exteraGram) и [NagramX](https://github.com/risin42/NagramX)).

**Суть и концепция проекта:**
- **Ранний доступ к новым возможностям:** внедрение и тестирование свежих функций, интерфейсных улучшений и оптимизаций раньше, чем они попадут в основной репозиторий exteraless.
- **Bleeding-edge:** сборки носят статус экспериментальных и созданы для активного тестирования.
- **Дружественность к апстриму:** проект **не является конкурентом** exteraless или другим клиентам Telegram. Это площадка для обкатки идей, лучшие из которых могут быть предложены в upstream.
- **Безопасность плагинов:** сохраняется изоляция и строгая модель разрешений для плагинов из exteraless.
- **Фирменный дизайн и иконки:** эксклюзивные иконки лаунчера (Основная, Точки, Неон, Лава, Кибер, Космос, Мокко, Ледник, Лаванда), плавный экран выбора в стиле Nothing OS.

* **Имя пакета:** `com.moltengram.app` (позволяет устанавливать приложение параллельно с exteraless и официальным Telegram)
* **Что под капотом:** модули оформления и параметров чатов, иконпаки, полоса пилюль (Pill Stack), расширенное боковое меню, движок Python-плагинов и экспериментальные доработки MoltenGram.

### Ссылки

* Исходный код MoltenGram: [github.com/MoltenSt4r/ExteraMS](https://github.com/MoltenSt4r/ExteraMS)
* Исходный код апстрима (exteraless): [github.com/exteraless/exteraless](https://github.com/exteraless/exteraless)
* Канал апстрима: [@exteraless](https://t.me/exteraless)

### Сборка

1. Склонировать репозиторий вместе с подмодулями:

    ```bash
    git clone --recursive --shallow-submodules git@github.com:MoltenSt4r/ExteraMS.git MoltenGram
    ```

    Если репозиторий уже склонирован без подмодулей:

    ```bash
    git submodule update --init --recursive --depth=1
    ```

2. Получить `TELEGRAM_APP_ID` и `TELEGRAM_APP_HASH` на [my.telegram.org](https://my.telegram.org/auth)
   и создать `local.properties` в корне проекта:

   ```properties
   TELEGRAM_APP_ID=<ваш_app_id>
   TELEGRAM_APP_HASH=<ваш_app_hash>
   ```

3. Для подписи APK положить свой `TMessagesProj/release.keystore` и дописать в `local.properties`:

   ```properties
   KEYSTORE_PASS=<пароль_хранилища>
   ALIAS_NAME=<имя_ключа>
   ALIAS_PASS=<пароль_ключа>
   ```

   Ключа в репозитории нет намеренно. Без него сборка не падает — APK подписывается
   отладочным ключом Android.

4. Для push-уведомлений положить свой `TMessagesProj/google-services.json`
   (Firebase, имя пакета `com.moltengram.app`).

5. Заменить метаданные проекта:

    - ключ Google Maps в записи `com.google.android.maps.v2.API_KEY` в `TMessagesProj/src/main/AndroidManifest.xml`;
    - `BaseRemoteHelper.CHANNEL_METADATA_ID` — числовой id вашего канала метаданных, без префикса `-100`.

6. Собрать: `./gradlew :TMessagesProj:assembleDebug` или открыть проект в Android Studio.

**Про ABI.** Собираются только 64-битные `arm64-v8a` и `x86_64`: Chaquopy собирает
Python 3.12 лишь под них, и на `armeabi-v7a` конфигурация обрывается. Переменная
`NATIVE_TARGET` задаёт цель: `arm64-v8a` (один ABI, быстрее), `universal` (оба),
`SKIP` (без нативной части — только Java и ресурсы).

### Сборка через GitHub Actions

Нужны два секрета репозитория:

* `LOCAL_PROPERTIES` — содержимое `local.properties` в base64:

  ```bash
  base64 -w0 local.properties
  ```

* `RELEASE_KEYSTORE` — файл ключа в base64:

  ```bash
  base64 -w0 TMessagesProj/release.keystore
  ```

Дальше запустить workflow **Release Build** или **Canary Build**. Готовый APK лежит в артефактах и релизах.

### Авторы дизайна и благодарности

- Иконки и айдентика MoltenGram: [@moltenst4r](https://github.com/MoltenSt4r)
- Дизайн и иконки exteraGram: [@the8055u](https://t.me/the8055u) и студия [@BlueprintDsgn](https://t.me/BlueprintDsgn).
- [exteraless](https://github.com/exteraless/exteraless)
- [AyuGram](https://github.com/AyuGram/AyuGram4A)
- [Cherrygram](https://github.com/arsLan4k1390/Cherrygram)
- [Dr4iv3rNope](https://github.com/Dr4iv3rNope/NotSoAndroidAyuGram)
- [exteraGram](https://github.com/exteraSquad/exteraGram)
- [Nagram](https://github.com/NextAlone/Nagram)
- [NagramX](https://github.com/risin42/NagramX)
- [Nekogram](https://github.com/Nekogram/Nekogram)
- [OctoGram](https://github.com/OctoGramApp/OctoGram)

---

## English

### What this is

**MoltenGram** is an experimental fork of [exteraless](https://github.com/exteraless/exteraless) (and spiritual successor to [exteraGram](https://github.com/exteraSquad/exteraGram) & [NagramX](https://github.com/risin42/NagramX)).

**Core concept:**
- **Early access to new features:** Bringing and testing new features, UI tweaks, and optimizations before they land in upstream exteraless.
- **Bleeding-edge:** Builds are experimental and crafted for rapid innovation.
- **Not a competitor:** MoltenGram is **not** a competitor to exteraless or any other client. It serves as a testing ground for experimental ideas that can later be upstreamed.
- **Plugin isolation:** Retains exteraless's secure plugin permission model.
- **Distinctive identity & icons:** MoltenGram, Dotted, Neon, Lava, Cyber, Cosmic, Mocha, Glacier, Lavender launcher styles with smooth Nothing OS-inspired picker.

* **Package name:** `com.moltengram.app` (allows co-existence alongside exteraless and official Telegram on the same device)

### Links

* MoltenGram repository: [github.com/MoltenSt4r/ExteraMS](https://github.com/MoltenSt4r/ExteraMS)
* Upstream repository: [github.com/exteraless/exteraless](https://github.com/exteraless/exteraless)
* Upstream channel: [@exteraless](https://t.me/exteraless)

### Building

1. Clone with submodules:

    ```bash
    git clone --recursive --shallow-submodules git@github.com:MoltenSt4r/ExteraMS.git MoltenGram
    ```

2. Get `TELEGRAM_APP_ID` and `TELEGRAM_APP_HASH` from [my.telegram.org](https://my.telegram.org/auth)
   and put them into `local.properties` in the project root.

3. For release signing, drop your own `TMessagesProj/release.keystore` and add
   `KEYSTORE_PASS`, `ALIAS_NAME`, `ALIAS_PASS` to `local.properties`. No keystore is
   shipped with the repository; without one the build is signed with the Android debug key.

4. For push notifications, replace `TMessagesProj/google-services.json` with your own
   Firebase config for `com.moltengram.app`.

5. Build with `./gradlew :TMessagesProj:assembleDebug` or from Android Studio.

Only 64-bit ABIs are built (`arm64-v8a`, `x86_64`): Chaquopy ships Python 3.12 for those
only, and `armeabi-v7a` fails at configuration time. `NATIVE_TARGET` selects the target:
`arm64-v8a`, `universal` (both) or `SKIP` (no native part).

For CI, set two repository secrets — `LOCAL_PROPERTIES` and `RELEASE_KEYSTORE`, both
base64-encoded — and run the **Release Build** or **Canary Build** workflow.
