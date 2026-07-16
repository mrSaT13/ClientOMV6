package com.omv.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.omv.client.data.security.SecurePrefs
import com.omv.client.ui.navigation.OmvNavHost
import com.omv.client.ui.theme.OmvClientTheme
import com.omv.client.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var securePrefs: SecurePrefs

    private val _themeMode = MutableStateFlow(0)
    val themeMode: StateFlow<Int> = _themeMode

    private val _accentColor = MutableStateFlow(0)
    val accentColor: StateFlow<Int> = _accentColor

    override fun attachBaseContext(newBase: android.content.Context?) {
        super.attachBaseContext(newBase?.let { LocaleHelper.onAttach(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _themeMode.value = securePrefs.themeMode
        _accentColor.value = securePrefs.accentColor

        setContent {
            val mode by themeMode.collectAsState()
            val accent by accentColor.collectAsState()
            OmvClientTheme(darkTheme = mode, accentIndex = accent) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OmvNavHost()
                }
            }
        }
    }

    fun updateTheme(mode: Int) {
        _themeMode.value = mode
    }

    fun updateAccent(index: Int) {
        _accentColor.value = index
    }
}
