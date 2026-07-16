# OpenMediaVault 6 — Android Client

Нативный Android-клиент для управления серверами OpenMediaVault 6.

## Возможности

- **Авторизация** — вход по логину/паролю, сохранение учётных данных в зашифрованном хранилище (EncryptedSharedPreferences)
- **Главный экран** — информация о системе (hostname, версия, uptime), CPU, RAM, температура, графики нагрузки, быстрые действия (перезагрузка/выключение)
- **Сервисы** — список/включение/отключение сервисов OMV
- **Мониторинг** — CPU, RAM, диски, файловые системы, сеть (авто-обновление каждые 3 сек)
- **Docker** — список контейнеров, запуск/остановка/перезапуск, анимированный поиск с фильтрацией
- **S.M.A.R.T.** — мониторинг состояния дисков, атрибуты, температура, тесты
- **Терминал** — удалённое выполнение команд на сервере
- **Бэкап настроек** — экспорт/импорт конфигурации OMV в JSON
- **Уведомления** — фоновые проверки дисков, контейнеров, обновлений (WorkManager, каждые 15 мин)
- **Настройки** — тема (светлая/тёмная/системная), 8 цветов акцента, 4 языка (EN/RU/UK/DE), порог диска
- **Виджет** — домашний виджет с CPU/RAM/Дисками, тап открывает приложение
- **Свайпы** — переключение между вкладками жестом влево/вправо
- **Keep-alive** — автоматическое поддержание сессии, перелогин при истечении

## Технологии

- Kotlin + Jetpack Compose (Material 3)
- Hilt (Dependency Injection)
- Retrofit + OkHttp (JSON-RPC API)
- EncryptedSharedPreferences (безопасное хранение учётных данных)
- WorkManager (фоновые задачи, мониторинг)
- Navigation Compose + анимации
- AppWidgetProvider (домашний виджет)

## Требования

- Android 8.0+ (API 26)
- OpenMediaVault 6 на сервере
- Доступ к серверу по HTTP/HTTPS

## Сборка

```bash
cd OmvClient
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Структура проекта

```
OmvClient/
├── app/
│   ├── src/main/
│   │   ├── java/com/omv/client/
│   │   │   ├── data/
│   │   │   │   ├── model/OmvModels.kt        # Data-классы
│   │   │   │   ├── repository/OmvRepository.kt # API репозиторий
│   │   │   │   └── security/SecurePrefs.kt     # Зашифрованные настройки
│   │   │   ├── ui/
│   │   │   │   ├── components/                 # LoadingOverlay, ErrorMessage
│   │   │   │   ├── dashboard/DashboardScreen.kt # Главный экран
│   │   │   │   ├── docker/DockerScreen.kt      # Docker управление
│   │   │   │   ├── login/LoginScreen.kt        # Авторизация
│   │   │   │   ├── monitoring/MonitoringScreen.kt # Мониторинг
│   │   │   │   ├── navigation/NavHost.kt       # Навигация + свайпы
│   │   │   │   ├── notifications/              # Уведомления
│   │   │   │   ├── onboarding/                 # Онбординг
│   │   │   │   ├── services/ServicesScreen.kt  # Сервисы OMV
│   │   │   │   ├── settings/SettingsScreen.kt  # Настройки
│   │   │   │   ├── smart/SmartScreen.kt        # S.M.A.R.T.
│   │   │   │   ├── terminal/TerminalScreen.kt  # Терминал
│   │   │   │   ├── backup/BackupScreen.kt      # Бэкап настроек
│   │   │   │   ├── plugins/PluginScreen.kt     # Плагины
│   │   │   │   ├── splash/SplashScreen.kt      # Заставка
│   │   │   │   └── theme/Theme.kt              # Темы и цвета
│   │   │   ├── util/
│   │   │   │   ├── NotificationHelper.kt       # Каналы уведомлений
│   │   │   │   └── LocaleHelper.kt             # Локализация
│   │   │   ├── worker/OmvMonitorWorker.kt      # Фоновый мониторинг
│   │   │   ├── widget/
│   │   │   │   ├── OmvWidget.kt                # Домашний виджет
│   │   │   │   └── widget_layout.xml           # Макет виджета
│   │   │   ├── MainActivity.kt
│   │   │   └── OmvApp.kt
│   │   ├── res/
│   │   │   ├── layout/widget_layout.xml        # Макет виджета
│   │   │   ├── xml/widget_info.xml             # Конфиг виджета
│   │   │   ├── values/strings.xml              # Английский
│   │   │   ├── values-ru/strings.xml           # Русский
│   │   │   ├── values-uk/strings.xml           # Украинский
│   │   │   └── values-de/strings.xml           # Немецкий
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/wrapper/
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── LICENSE
└── README.md
```

## Загрузка на GitHub

### Загружать (всё включено в .gitignore):

| Папка/файл | Описание |
|------------|----------|
| `app/src/` | Весь исходный код |
| `app/build.gradle.kts` | Зависимости |
| `gradle/` | Gradle wrapper |
| `build.gradle.kts` | Root build file |
| `settings.gradle.kts` | Настройки Gradle |
| `gradle.properties` | Свойства Gradle |
| `gradlew`, `gradlew.bat` | Gradle wrapper scripts |
| `README.md` | Описание проекта |
| `LICENSE` | Лицензия |
| `.gitignore` | Правила игнорирования |

### НЕ загружать (исключены .gitignore):

| Папка/файл | Почему |
|------------|--------|
| `app/build/` | Собранные файлы (Gradle сгенерирует заново) |
| `.gradle/` | Кэш Gradle |
| `.idea/` | Файлы Android Studio |
| `local.properties` | Локальные пути к SDK |
| `*.apk`, `*.aab` | Бинарники |
| `*.jks`, `*.keystore` | Ключи подписи |
| `*.hprof`, `*.log` | Отладочные файлы |
| `hs_err_pid*` | JVM crash логи |
| `google-services.json` | Google Services конфиг |

### Быстрая загрузка:

```bash
# 1. Инициализировать git (если ещё не)
cd OmvClient
git init

# 2. Добавить всё
git add .

# 3. Проверить что будет добавлено (не должно быть build/, .gradle/, etc.)
git status

# 4. Коммит
git commit -m "Initial commit: OMV6 Android Client v1.1.0"

# 5. Подключить репозиторий
git remote add origin https://github.com/mrSaT13/ClientOMV6.git

# 6. Загрузить
git push -u origin main
```

## Проверка безопасности

- Нет хардкоженных паролей или ключей
- Учётные данные хранятся в EncryptedSharedPreferences (AES-256)
- TrustManager для self-signed сертификатов (только для dev, не для прода)
- Crash-логи и debug-файлы исключены из .gitignore

## Лицензия

MIT License

## Автор

**mrSaT13** — разработчик приложения

GitHub: [github.com/mrSaT13/ClientOMV6](https://github.com/mrSaT13/ClientOMV6)
