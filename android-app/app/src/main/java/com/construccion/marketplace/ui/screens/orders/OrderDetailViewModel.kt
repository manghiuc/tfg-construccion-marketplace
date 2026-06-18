package com.construccion.marketplace.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccion.marketplace.data.model.MaterialRequest
import com.construccion.marketplace.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estados posibles de la pantalla de detalle de pedido. */
sealed class OrderDetailUiState {
    object Loading : OrderDetailUiState()
    data class Success(val order: MaterialRequest) : OrderDetailUiState()
    data class Error(val message: String) : OrderDetailUiState()
}

/**
 * ViewModel del detalle de un pedido.
 *
 * Carga el estado actualizado y la información de seguimiento
 * de un pedido específico por su ID.
 */
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Loading)
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    fun loadOrder(orderId: Int) {
        viewModelScope.launch {
            _uiState.value = OrderDetailUiState.Loading
            orderRepository.getOrderStatus(orderId).fold(
                onSuccess = { order ->
                    _uiState.value = OrderDetailUiState.Success(order)
                },
                onFailure = { e ->
                    _uiState.value = OrderDetailUiState.Error(
                        e.message ?: "Error al cargar el pedido"
                    )
                }
            )
        }
    }

    fun retry(orderId: Int) = loadOrder(orderId)
}
