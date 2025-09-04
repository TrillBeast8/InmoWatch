package com.example.inmocontrol_v2.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Searching : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class ConnectionStateViewModel : ViewModel() {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun setIdle() {
        _connectionState.value = ConnectionState.Idle
    }

    fun setSearching() {
        _connectionState.value = ConnectionState.Searching
    }

    fun setConnecting() {
        _connectionState.value = ConnectionState.Connecting
    }

    fun setConnected(deviceName: String) {
        _connectionState.value = ConnectionState.Connected(deviceName)
    }

    fun setError(message: String) {
        _connectionState.value = ConnectionState.Error(message)
    }

    override fun onCleared() {
        super.onCleared()
        setIdle()
    }
}
