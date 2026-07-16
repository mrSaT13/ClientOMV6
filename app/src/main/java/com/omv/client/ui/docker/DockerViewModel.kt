package com.omv.client.ui.docker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omv.client.data.model.DockerContainer
import com.omv.client.data.repository.OmvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DockerViewModel @Inject constructor(
    private val repository: OmvRepository
) : ViewModel() {

    private val _containers = MutableStateFlow<List<DockerContainer>>(emptyList())
    val containers: StateFlow<List<DockerContainer>> = _containers

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage

    private val _apiInfo = MutableStateFlow("")
    val apiInfo: StateFlow<String> = _apiInfo

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getDockerContainers()
                .onSuccess { _containers.value = it }
                .onFailure { _error.value = it.message }
            _apiInfo.value = repository.getDockerApiInfo()
            _isLoading.value = false
        }
    }

    fun startContainer(name: String) {
        viewModelScope.launch {
            _actionMessage.value = null
            repository.dockerAction(name, "start")
                .onSuccess {
                    _actionMessage.value = "$name запущен"
                    kotlinx.coroutines.delay(1000)
                    refresh()
                }
                .onFailure { _actionMessage.value = "Ошибка: ${it.message}" }
        }
    }

    fun stopContainer(name: String) {
        viewModelScope.launch {
            _actionMessage.value = null
            repository.dockerAction(name, "stop")
                .onSuccess {
                    _actionMessage.value = "$name остановлен"
                    kotlinx.coroutines.delay(1000)
                    refresh()
                }
                .onFailure { _actionMessage.value = "Ошибка: ${it.message}" }
        }
    }

    fun restartContainer(name: String) {
        viewModelScope.launch {
            _actionMessage.value = null
            repository.dockerAction(name, "restart")
                .onSuccess {
                    _actionMessage.value = "$name перезапущен"
                    kotlinx.coroutines.delay(1000)
                    refresh()
                }
                .onFailure { _actionMessage.value = "Ошибка: ${it.message}" }
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }
}
