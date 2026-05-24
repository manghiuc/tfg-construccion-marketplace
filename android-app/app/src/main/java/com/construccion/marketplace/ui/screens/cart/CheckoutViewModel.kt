package com.construccion.marketplace.ui.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccion.marketplace.data.api.OdooApiService
import com.construccion.marketplace.data.model.CartItem
import com.construccion.marketplace.data.model.Obra
import com.construccion.marketplace.data.model.ObraCreateRequest
import com.construccion.marketplace.data.repository.CreateRequestBody
import com.construccion.marketplace.data.repository.OrderLine
import com.construccion.marketplace.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutUiState(
    val obras: List<Obra> = emptyList(),
    val isLoadingObras: Boolean = false,
    val obrasError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submittedOrderId: Int? = null,
    val isCreatingObra: Boolean = false,
    val newlyCreatedObraId: Int? = null,
    val obraCreateError: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val apiService: OdooApiService,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        loadObras()
    }

    fun loadObras() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingObras = true, obrasError = null)
            try {
                val response = apiService.getObras()
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        obras = response.body()!!.data ?: emptyList(),
                        isLoadingObras = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingObras = false,
                        obrasError = response.body()?.message ?: "Error cargando obras"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingObras = false,
                    obrasError = "Error de conexión: ${e.localizedMessage}"
                )
            }
        }
    }

    fun createOrder(
        cartItems: List<CartItem>,
        obraId: Int,
        deliveryAddress: String,
        notes: String,
        isUrgent: Boolean = false
    ) {
        if (cartItems.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            val result = orderRepository.createOrder(
                CreateRequestBody(
                    obraId = obraId,
                    lines = cartItems.map { item ->
                        OrderLine(
                            productId = item.productId,
                            qty = item.qty,
                            priceUnit = item.priceUnit
                        )
                    },
                    deliveryAddress = deliveryAddress,
                    isUrgent = isUrgent,
                    notes = notes.ifBlank { null }
                )
            )
            result.fold(
                onSuccess = { order ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submittedOrderId = order.id
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitError = e.message ?: "Error al crear el pedido"
                    )
                }
            )
        }
    }

    fun createObra(name: String, address: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingObra = true, obraCreateError = null)
            try {
                val response = apiService.createObra(
                    ObraCreateRequest(name = name.trim(), address = address?.takeIf { it.isNotBlank() })
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val obra = response.body()!!.data!!
                    _uiState.value = _uiState.value.copy(
                        obras = _uiState.value.obras + obra,
                        isCreatingObra = false,
                        newlyCreatedObraId = obra.id
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isCreatingObra = false,
                        obraCreateError = response.body()?.message ?: "Error al crear la obra"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreatingObra = false,
                    obraCreateError = "Error de conexión: ${e.localizedMessage}"
                )
            }
        }
    }

    fun consumeNewlyCreatedObraId() {
        _uiState.value = _uiState.value.copy(newlyCreatedObraId = null)
    }

    fun clearObraCreateError() {
        _uiState.value = _uiState.value.copy(obraCreateError = null)
    }

    fun clearSubmitError() {
        _uiState.value = _uiState.value.copy(submitError = null)
    }
}
