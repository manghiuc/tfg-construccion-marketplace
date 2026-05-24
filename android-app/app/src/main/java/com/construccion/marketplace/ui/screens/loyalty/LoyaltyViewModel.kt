package com.construccion.marketplace.ui.screens.loyalty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccion.marketplace.data.api.OdooApiService
import com.construccion.marketplace.data.model.LoyaltyStatus
import com.construccion.marketplace.data.model.RedeemRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoyaltyViewModel @Inject constructor(
    private val apiService: OdooApiService
) : ViewModel() {

    private val _status = MutableStateFlow<LoyaltyStatus?>(null)
    val status: StateFlow<LoyaltyStatus?> = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _redeemSuccess = MutableStateFlow(false)
    val redeemSuccess: StateFlow<Boolean> = _redeemSuccess.asStateFlow()

    init { loadStatus() }

    fun loadStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getLoyaltyStatus()
                if (response.isSuccessful && response.body()?.success == true) {
                    _status.value = response.body()?.data
                }
            } catch (_: Exception) {
                // Sin conexión: la pantalla usará datos mock
            }
            _isLoading.value = false
        }
    }

    fun redeemPoints(points: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.redeemLoyaltyPoints(RedeemRequest(points))
                if (response.isSuccessful && response.body()?.success == true) {
                    _status.value = response.body()?.data
                    _redeemSuccess.value = true
                }
            } catch (_: Exception) {}
        }
    }

    fun clearRedeemSuccess() { _redeemSuccess.value = false }
}
