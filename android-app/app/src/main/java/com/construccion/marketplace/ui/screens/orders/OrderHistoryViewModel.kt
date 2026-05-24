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

/** Agrupación de pedidos pertenecientes a una misma obra */
data class ObraOrderGroup(
    val obraName: String,   // "" = sin obra asignada
    val obraId: Int,        // 0 = sin obra
    val orders: List<MaterialRequest>,
    val totalAmount: Double
)

sealed class OrderHistoryUiState {
    object Loading : OrderHistoryUiState()
    data class Success(val groups: List<ObraOrderGroup>) : OrderHistoryUiState()
    data class Error(val message: String) : OrderHistoryUiState()
    object Empty : OrderHistoryUiState()
}

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderHistoryUiState>(OrderHistoryUiState.Loading)
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    // Cache de todos los pedidos (sin filtro aplicado)
    private val _allOrders = MutableStateFlow<List<MaterialRequest>>(emptyList())

    private val _activeFilter = MutableStateFlow<String?>(null)
    val activeFilter: StateFlow<String?> = _activeFilter.asStateFlow()

    init { loadOrders() }

    /** Recarga pedidos desde la API */
    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = OrderHistoryUiState.Loading
            orderRepository.getOrders().fold(
                onSuccess = { orders ->
                    _allOrders.value = orders
                    applyFilter(_activeFilter.value)
                },
                onFailure = { e ->
                    _uiState.value = OrderHistoryUiState.Error(
                        e.message ?: "Error al cargar los pedidos"
                    )
                }
            )
        }
    }

    /** Aplica filtro por estado de pedido */
    fun setFilter(stateKey: String?) {
        _activeFilter.value = stateKey
        applyFilter(stateKey)
    }

    private fun applyFilter(stateKey: String?) {
        val orders = if (stateKey == null) {
            _allOrders.value
        } else {
            _allOrders.value.filter {
                it.state.name.lowercase() == stateKey.lowercase()
            }
        }

        if (orders.isEmpty()) {
            _uiState.value = OrderHistoryUiState.Empty
            return
        }

        // Agrupar por obra — obras sin nombre al final
        val groups = orders
            .groupBy { Pair(it.obraId, it.obraName) }
            .map { (pair, pedidos) ->
                ObraOrderGroup(
                    obraName = pair.second,
                    obraId = pair.first,
                    orders = pedidos,
                    totalAmount = pedidos.sumOf { it.totalAmount }
                )
            }
            .sortedWith(
                compareBy(
                    { it.obraName.isBlank() }, // sin obra va al final
                    { it.obraName }
                )
            )

        _uiState.value = OrderHistoryUiState.Success(groups)
    }
}
