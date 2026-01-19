package com.gamebiller.tvlock.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamebiller.tvlock.domain.LockRepository
import com.gamebiller.tvlock.domain.model.DeviceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for device pairing screen
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    private val repository: LockRepository
) : ViewModel() {
    
    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()
    
    /**
     * Pair device with station using station code
     */
    fun pairDevice(stationCode: String) {
        if (stationCode.isBlank()) {
            _pairingState.value = PairingState.Error("Please enter a station code")
            return
        }
        
        _pairingState.value = PairingState.Loading
        
        viewModelScope.launch {
            val result = repository.pairDevice(stationCode.trim())
            
            _pairingState.value = if (result.isSuccess) {
                Timber.d("Pairing successful")
                PairingState.Success(result.getOrNull()!!)
            } else {
                Timber.e("Pairing failed: ${result.exceptionOrNull()?.message}")
                PairingState.Error(
                    result.exceptionOrNull()?.message ?: "Pairing failed. Please check the code and try again."
                )
            }
        }
    }
    
    /**
     * Reset pairing state to idle
     */
    fun resetState() {
        _pairingState.value = PairingState.Idle
    }
}

/**
 * Pairing UI state
 */
sealed class PairingState {
    data object Idle : PairingState()
    data object Loading : PairingState()
    data class Success(val deviceInfo: DeviceInfo) : PairingState()
    data class Error(val message: String) : PairingState()
}
