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

/**
 * Metodo utilizado para calcular el coste de transporte.
 */
enum class TransportMethod {
    /** Calculo basado en GPS (coordenadas enviadas al backend). */
    GPS,
    /** Fallback: calculo local por tramos de peso. */
    WEIGHT_BASED
}

data class CheckoutUiState(
    val obras: List<Obra> = emptyList(),
    val isLoadingObras: Boolean = false,
    val obrasError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submittedOrderId: Int? = null,
    val isCreatingObra: Boolean = false,
    val newlyCreatedObraId: Int? = null,
    val obraCreateError: String? = null,
    /** Metodo de transporte activo (GPS o por peso). */
    val transportMethod: TransportMethod = TransportMethod.WEIGHT_BASED,
    /** Latitud de entrega (solo cuando GPS disponible). */
    val deliveryLat: Double? = null,
    /** Longitud de entrega (solo cuando GPS disponible). */
    val deliveryLon: Double? = null
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

    // -------------------------------------------------------------------------
    // Calculo de transporte por peso (fallback sin GPS)
    // -------------------------------------------------------------------------

    /**
     * Calcula el coste de transporte basado en tramos de peso,
     * con las mismas tarifas que el portal web:
     * - <=50kg  -> 15EUR
     * - <=200kg -> 22EUR
     * - <=500kg -> 35EUR
     * - >500kg  -> 55EUR
     * Si el pedido es urgente, se aplica multiplicador x1.5.
     */
    fun calculateWeightBasedTransport(totalWeightKg: Double, isUrgent: Boolean): Double {
        val baseCost = when {
            totalWeightKg <= 50.0 -> 15.0
            totalWeightKg <= 200.0 -> 22.0
            totalWeightKg <= 500.0 -> 35.0
            else -> 55.0
        }
        return if (isUrgent) baseCost * 1.5 else baseCost
    }

    /**
     * Establece la ubicacion GPS del punto de entrega.
     * Cambia el metodo de transporte a GPS.
     */
    fun setDeliveryLocation(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(
            deliveryLat = lat,
            deliveryLon = lon,
            transportMethod = TransportMethod.GPS
        )
    }

    /**
     * Indica que el GPS no esta disponible y se usara calculo por peso.
     */
    fun setGpsUnavailable() {
        _uiState.value = _uiState.value.copy(
            deliveryLat = null,
            deliveryLon = null,
            transportMethod = TransportMethod.WEIGHT_BASED
        )
    }

    fun createOrder(
        cartItems: List<CartItem>,
        obraId: Int?,
        deliveryAddress: String,
        notes: String,
        isUrgent: Boolean = false,
        transportCost: Double? = null
    ) {
        if (cartItems.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            val state = _uiState.value

            // Si usamos GPS, enviamos lat/lon y el backend calcula.
            // Si usamos peso, enviamos transport_cost calculado localmente.
            val useGps = state.transportMethod == TransportMethod.GPS
                    && state.deliveryLat != null
                    && state.deliveryLon != null

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
                    deliveryLat = if (useGps) state.deliveryLat else null,
                    deliveryLon = if (useGps) state.deliveryLon else null,
                    transportCost = if (!useGps) transportCost else null,
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
