package com.example.service

import com.example.ui.viewmodel.VpnStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AlphaVpnState {
    private val _status = MutableStateFlow(VpnStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val _downloadSpeed = MutableStateFlow(0f)
    val downloadSpeed = _downloadSpeed.asStateFlow()

    private val _uploadSpeed = MutableStateFlow(0f)
    val uploadSpeed = _uploadSpeed.asStateFlow()

    private val _sessionBytes = MutableStateFlow(0L)
    val sessionBytes = _sessionBytes.asStateFlow()

    fun updateStatus(newStatus: VpnStatus) {
        _status.value = newStatus
    }

    fun updateSpeeds(down: Float, up: Float, bytes: Long) {
        _downloadSpeed.value = down
        _uploadSpeed.value = up
        _sessionBytes.value = bytes
    }

    fun reset() {
        _status.value = VpnStatus.DISCONNECTED
        _downloadSpeed.value = 0f
        _uploadSpeed.value = 0f
        _sessionBytes.value = 0L
    }
}
