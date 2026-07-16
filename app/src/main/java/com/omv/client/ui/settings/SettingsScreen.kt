package com.omv.client.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omv.client.MainActivity
import com.omv.client.R
import com.omv.client.data.repository.OmvRepository
import com.omv.client.data.security.SecurePrefs
import com.omv.client.ui.theme.*
import com.omv.client.util.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: OmvRepository,
    private val securePrefs: SecurePrefs
) : ViewModel() {

    var themeMode: Int
        get() = securePrefs.themeMode
        set(value) { securePrefs.themeMode = value }

    var accentColor: Int
        get() = securePrefs.accentColor
        set(value) { securePrefs.accentColor = value }

    var language: String
        get() = securePrefs.language
        set(value) { securePrefs.language = value }

    var diskThreshold: Int
        get() = securePrefs.diskThreshold
        set(value) { securePrefs.diskThreshold = value }

    var notifyDiskLow: Boolean
        get() = securePrefs.notifyDiskLow
        set(value) { securePrefs.notifyDiskLow = value }

    var notifyContainers: Boolean
        get() = securePrefs.notifyContainers
        set(value) { securePrefs.notifyContainers = value }

    var notifyUpdates: Boolean
        get() = securePrefs.notifyUpdates
        set(value) { securePrefs.notifyUpdates = value }

    val serverUrl: String get() {
        val proto = if (securePrefs.useHttps) "https" else "http"
        return "$proto://${securePrefs.hostPort}"
    }
    val username: String get() = securePrefs.username

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            val savedTheme = securePrefs.themeMode
            val savedAccent = securePrefs.accentColor
            val savedLang = securePrefs.language
            val savedThreshold = securePrefs.diskThreshold
            val savedNotifyDisk = securePrefs.notifyDiskLow
            val savedNotifyContainers = securePrefs.notifyContainers
            val savedNotifyUpdates = securePrefs.notifyUpdates
            securePrefs.clearAll()
            securePrefs.themeMode = savedTheme
            securePrefs.accentColor = savedAccent
            securePrefs.language = savedLang
            securePrefs.diskThreshold = savedThreshold
            securePrefs.notifyDiskLow = savedNotifyDisk
            securePrefs.notifyContainers = savedNotifyContainers
            securePrefs.notifyUpdates = savedNotifyUpdates
            onComplete()
        }
    }
}

data class LanguageOption(val code: String, val name: String, val flag: String)

val languages = listOf(
    LanguageOption("", "System", "🌐"),
    LanguageOption("en", "English", "🇬🇧"),
    LanguageOption("ru", "Русский", "🇷🇺"),
    LanguageOption("uk", "Українська", "🇺🇦"),
    LanguageOption("de", "Deutsch", "🇩🇪")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var currentTheme by remember { mutableStateOf(viewModel.themeMode) }
    var currentAccent by remember { mutableStateOf(viewModel.accentColor) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // User info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                viewModel.username.ifEmpty { "admin" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Text(
                                viewModel.serverUrl,
                                fontSize = 12.sp,
                                color = Grey600
                            )
                        }
                    }
                }
            }

            // Theme
            item {
                SectionHeader(stringResource(R.string.settings_theme))
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column {
                            ThemeOption(
                                title = stringResource(R.string.settings_system),
                                icon = Icons.Default.PhoneAndroid,
                                selected = currentTheme == 0,
                                onClick = {
                                    currentTheme = 0
                                    viewModel.themeMode = 0
                                    (context as? MainActivity)?.updateTheme(0)
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ThemeOption(
                                title = stringResource(R.string.settings_light),
                                icon = Icons.Default.LightMode,
                                selected = currentTheme == 1,
                                onClick = {
                                    currentTheme = 1
                                    viewModel.themeMode = 1
                                    (context as? MainActivity)?.updateTheme(1)
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ThemeOption(
                                title = stringResource(R.string.settings_dark),
                                icon = Icons.Default.DarkMode,
                                selected = currentTheme == 2,
                                onClick = {
                                    currentTheme = 2
                                    viewModel.themeMode = 2
                                    (context as? MainActivity)?.updateTheme(2)
                                }
                            )
                    }
                }
            }

            // Accent color
            item {
                SectionHeader("Accent Color")
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        for (row in accentColors.chunked(4)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                row.forEach { accent ->
                                    val index = accentColors.indexOf(accent)
                                    val selected = currentAccent == index
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(accent.primary)
                                            .then(
                                                if (selected) Modifier.border(3.dp, Color.White, CircleShape)
                                                else Modifier
                                            )
                                            .clickable {
                                                currentAccent = index
                                                viewModel.accentColor = index
                                                (context as? MainActivity)?.updateAccent(index)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                // Fill remaining space in row
                                repeat(4 - row.size) {
                                    Spacer(modifier = Modifier.size(48.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Language
            item {
                SectionHeader(stringResource(R.string.settings_language))
            }

            item {
                val currentLangName = languages.find { it.code == viewModel.language }?.name ?: "System"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    onClick = { showLanguageDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = Grey600,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            currentLangName,
                            modifier = Modifier.weight(1f),
                            fontSize = 15.sp
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Grey600)
                    }
                }
            }

            // Notifications
            item {
                SectionHeader("Notifications")
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column {
                        SwitchItem(
                            title = "Low disk space",
                            subtitle = "Alert when disk usage exceeds threshold",
                            icon = Icons.Default.Warning,
                            checked = viewModel.notifyDiskLow,
                            onCheckedChange = { viewModel.notifyDiskLow = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwitchItem(
                            title = "Container status",
                            subtitle = "Alert on container stop/error",
                            icon = Icons.Default.Inventory2,
                            checked = viewModel.notifyContainers,
                            onCheckedChange = { viewModel.notifyContainers = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwitchItem(
                            title = "System updates",
                            subtitle = "Alert on available updates",
                            icon = Icons.Default.SystemUpdate,
                            checked = viewModel.notifyUpdates,
                            onCheckedChange = { viewModel.notifyUpdates = it }
                        )
                    }
                }
            }

            // Disk threshold
            if (viewModel.notifyDiskLow) {
                item {
                    val threshold = viewModel.diskThreshold
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        onClick = { showThresholdDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                tint = Grey600,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Disk threshold", fontSize = 15.sp)
                                Text(
                                    "Alert when free space is below $threshold%",
                                    fontSize = 12.sp,
                                    color = Grey600
                                )
                            }
                            Text(
                                "$threshold%",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // App info
            item {
                SectionHeader(stringResource(R.string.settings_app))
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column {
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.settings_version),
                            subtitle = "1.0.0"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(
                            icon = Icons.Default.Security,
                            title = stringResource(R.string.settings_security),
                            subtitle = "AES-256"
                        )
                    }
                }
            }

            // About section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(stringResource(R.string.settings_about))
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("OpenMediaVault Client", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("v1.0.0", fontSize = 12.sp, color = Grey600)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.settings_developer),
                            fontSize = 13.sp,
                            color = Grey600
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "mrSaT13",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mrSaT13/ClientOMV6"))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    stringResource(R.string.settings_open_github),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Logout
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Red500.copy(alpha = 0.1f),
                        contentColor = Red500
                    )
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_logout), fontWeight = FontWeight.SemiBold)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Language dialog
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text(stringResource(R.string.settings_language)) },
                text = {
                    Column {
                        languages.forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.language = lang.code
                                        showLanguageDialog = false
                                        // Restart activity to apply language
                                        val intent = Intent(context, MainActivity::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        (context as? Activity)?.finish()
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang.flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(lang.name, fontSize = 15.sp)
                                if (lang.code == viewModel.language) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }

        // Threshold dialog
        if (showThresholdDialog) {
            val options = listOf(5, 10, 15, 20)
            AlertDialog(
                onDismissRequest = { showThresholdDialog = false },
                title = { Text("Disk threshold") },
                text = {
                    Column {
                        Text(
                            "Alert when free disk space is below:",
                            fontSize = 14.sp,
                            color = Grey600
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        options.forEach { value ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.diskThreshold = value
                                        showThresholdDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = viewModel.diskThreshold == value,
                                    onClick = {
                                        viewModel.diskThreshold = value
                                        showThresholdDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("$value%", fontSize = 16.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThresholdDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }

        // Logout dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(stringResource(R.string.settings_logout)) },
                text = { Text(stringResource(R.string.settings_logout_confirm)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout(onLogout)
                        }
                    ) {
                        Text(stringResource(R.string.common_ok), color = Red500)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = Grey600,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else Grey600,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Grey600, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = Grey600)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Grey600, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = Grey600)
        }
    }
}
