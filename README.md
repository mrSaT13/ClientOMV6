# OMV Client — Android клиент для OpenMediaVault 6

Нативное Android-приложение для управления OMV6 сервером с телефона или планшета.

## Возможности

- **Dashboard** — обзор системы (CPU, RAM, диски, сервисы)
- **Управление сервисами** — включение/выключение SMB, NFS, Docker, SSH, FTP и др.
- **Мониторинг** — файловые системы, диски, сеть, температура CPU
- **Уведомления** — просмотр системных событий
- **Безопасность** — пароли зашифрованы AES-256 (EncryptedSharedPreferences)
- **Темы** — светлая, тёмная, системная
- **Onboarding** — гайд при первом запуске

## Требования

- Android 8.0+ (API 26)
- OpenMediaVault 6 с включённым web-интерфейсом
- Доступ к серверу по HTTP/HTTPS

## Сборка

```bash
# Клонируйте проект
cd OmvClient

# Соберите APK
./gradlew assembleRelease

# APK будет в app/build/outputs/apk/release/
```

## Как подключить

1. Убедитесь, что OMV6 доступен по сети (http://IP:80)
2. Откройте приложение
3. Пройдите onboarding (3 экрана с описанием возможностей)
4. Введите адрес сервера, порт, логин и пароль
5. Нажмите "Подключиться"

## API

Приложение использует JSON-RPC API OMV6 через `/rpc.php`:

```json
POST /rpc.php
{
  "service": "Session",
  "method": "login",
  "params": {
    "username": "admin",
    "password": "your_password"
  }
}
```

### Доступные методы

| Сервис | Методы |
|--------|--------|
| Session | login, logout, verify |
| Sysinfo | get |
| DiskMgmt | enumerateDevices |
| FileSystemMgmt | enumerate |
| Services | enumerate, enable, disable, start, stop |
| NetworkMgmt | enumerateIface |
| UserMgmt | enumerateUsers |
| Notification | enumerate |

## Стек технологий

- **Kotlin** — основной язык
- **Jetpack Compose** — UI фреймворк
- **Material 3** — дизайн-система
- **Retrofit + OkHttp** — HTTP клиент
- **Hilt** — dependency injection
- **EncryptedSharedPreferences** — безопасное хранение
- **Coroutines + Flow** — асинхронность
- **Navigation Compose** — навигация

## Структура проекта

```
app/src/main/java/com/omv/client/
├── data/
│   ├── api/        — Retrofit интерфейс
│   ├── model/      — Данные (Request/Response)
│   ├── repository/ — Бизнес-логика
│   └── security/   — Шифрование
├── ui/
│   ├── components/  — Переиспользуемые компоненты
│   ├── dashboard/   — Главный экран
│   ├── login/       — Авторизация
│   ├── monitoring/  — Мониторинг
│   ├── navigation/  — Навигация
│   ├── notifications/ — Уведомления
│   ├── onboarding/  — Гайд
│   ├── services/    — Управление сервисами
│   ├── settings/    — Настройки
│   ├── splash/      — Заставка
│   └── theme/       — Темы оформления
```


