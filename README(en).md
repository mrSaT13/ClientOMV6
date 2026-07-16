# OpenMediaVault 6 — Android Client

Native Android client for managing OpenMediaVault 6 servers.

## Features

- **Authentication** — login/password authentication, saving credentials in encrypted storage (EncryptedSharedPreferences)
- **Main screen** — system information (hostname, version, uptime), CPU, RAM, temperature, load graphs, quick actions (reboot/shutdown)
- **Services** — list, enable, and disable OMV services
- **Monitoring** — CPU, RAM, disks, file systems, network (auto-update every 3 sec)
- **Docker** — list of containers, start/stop/restart, animated search with filtering
- **S.M.A.R.T.** — disk health monitoring, attributes, temperature, tests
- **Terminal** — remote command execution on the server
- **Settings backup** — export/import OMV configuration in JSON
- **Notifications** — background checks for disks, containers, and updates (WorkManager, every 15 min)
- **Settings** — theme (light/dark/system), 8 accent colors, 4 languages (EN/RU/UK/DE), disk usage threshold
- **Widget** — home screen widget with CPU/RAM/Disks info, tap to open the app
- **Swipe gestures** — switching between tabs with left/right swipe gestures
- **Keep-alive** — automatic session maintenance, re-login upon expiration

<img width="571" height="933" alt="image" src="https://github.com/user-attachments/assets/7ea13df6-f9d0-42d1-9fee-cd787de31203" />

## Technologies

- Kotlin + Jetpack Compose (Material 3)
- Hilt (Dependency Injection)
- Retrofit + OkHttp (JSON-RPC API)
- EncryptedSharedPreferences (secure credential storage)
- WorkManager (background tasks, monitoring)
- Navigation Compose + animations
- AppWidgetProvider (home screen widget)

## Requirements

- Android 8.0+ (API 26)
- OpenMediaVault 6 running on the server
- Access to the server via HTTP/HTTPS

## Build

```bash
cd OmvClient
./gradlew assembleDebug
```

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## Author

**mrSaT13** — application developer

GitHub: [github.com/mrSaT13/ClientOMV6](https://github.com/mrSaT13/ClientOMV6)