package com.omv.client.ui.smart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omv.client.data.model.SmartAttribute
import com.omv.client.data.model.SmartDevice
import com.omv.client.data.model.SmartInfo
import com.omv.client.data.repository.OmvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartViewModel @Inject constructor(
    private val repository: OmvRepository
) : ViewModel() {

    private val _devices = MutableStateFlow<List<SmartDevice>>(emptyList())
    val devices: StateFlow<List<SmartDevice>> = _devices

    private val _selectedDevice = MutableStateFlow<SmartDevice?>(null)
    val selectedDevice: StateFlow<SmartDevice?> = _selectedDevice

    private val _smartInfo = MutableStateFlow<SmartInfo?>(null)
    val smartInfo: StateFlow<SmartInfo?> = _smartInfo

    private val _attributes = MutableStateFlow<List<SmartAttribute>>(emptyList())
    val attributes: StateFlow<List<SmartAttribute>> = _attributes

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getSmartDevices()
                .onSuccess { _devices.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun selectDevice(device: SmartDevice) {
        _selectedDevice.value = device
        viewModelScope.launch {
            _isLoading.value = true
            repository.getSmartInfo(device.deviceFile)
                .onSuccess { _smartInfo.value = it }
            repository.getSmartAttributes(device.deviceFile)
                .onSuccess { _attributes.value = it }
            _isLoading.value = false
        }
    }

    fun clearSelection() {
        _selectedDevice.value = null
        _smartInfo.value = null
        _attributes.value = emptyList()
    }
}
