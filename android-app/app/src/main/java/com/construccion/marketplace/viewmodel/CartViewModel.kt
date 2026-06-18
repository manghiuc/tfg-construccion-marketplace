/*
 * CartViewModel.kt
 * Este archivo gestiona el carrito de la compra de la aplicacion.
 * Controla los productos que el usuario quiere comprar, calcula precios,
 * aplica descuentos por fidelidad y gestiona las opciones de envio.
 */
package com.construccion.marketplace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccion.marketplace.data.api.OdooApiService
import com.construccion.marketplace.data.model.CartItem
import com.construccion.marketplace.data.model.Product
import com.construccion.marketplace.data.model.TransportCalc
import com.construccion.marketplace.data.model.TransportRequest
import com.construccion.marketplace.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val transport: TransportCalc? = null,
    val isCalculatingTransport: Boolean = false,
    val transportError: String? = null,
    val loyaltyDiscountApplied: Boolean = false,
    val loyaltyPointsToUse: Int = 0,
    val loyaltyDiscountAmount: Double = 0.0,
    val isUrgent: Boolean = false,
    val deliveryAddress: String = "",
    val deliveryLat: Double? = null,
    val deliveryLon: Double? = null,
    val obraId: Int? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submitSuccess: Boolean = false
) {
    val materialTotal: Double get() = items.sumOf { it.subtotal }
    val transportTotal: Double get() = transport?.total ?: 0.0
    /** Tarifa plana: 15€ si total ≤ 1000€, gratis si > 1000€ */
    val flatRateTransportBase: Double get() = if (materialTotal > 1000.0) 0.0 else 15.0
    /** Tarifa plana con recargo urgente ×1.5 */
    val flatRateTransport: Double get() = if (isUrgent && flatRateTransportBase > 0.0) flatRateTransportBase * 1.5 else flatRateTransportBase
    val grossTotal: Double get() = materialTotal + flatRateTransport
    val netTotal: Double get() = grossTotal - loyaltyDiscountAmount
    val totalWeightKg: Double get() = items.sumOf { it.weightKg * it.qty }
    val isEmpty: Boolean get() = items.isEmpty()
    val itemCount: Int get() = items.size
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val apiService: OdooApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Gestión de items del carrito
    // -------------------------------------------------------------------------

    /** Añade un producto al carrito usando datos primitivos (desde HomeScreen/CatalogScreen). */
    fun addItem(id: Int, name: String, price: Double, unit: String, qty: Double = 1.0) {
        _uiState.update { state ->
            val existingIndex = state.items.indexOfFirst { it.productId == id }
            val updatedItems = if (existingIndex >= 0) {
                state.items.toMutableList().also { list ->
                    val existing = list[existingIndex]
                    list[existingIndex] = existing.withQty(existing.qty + qty)
                }
            } else {
                state.items + CartItem(
                    productId = id,
                    productName = name,
                    qty = qty,
                    priceUnit = price,
                    uom = unit
                )
            }
            state.copy(items = updatedItems)
        }
    }

    fun addProduct(product: Product, qty: Double = 1.0) {
        _uiState.update { state ->
            val existingIndex = state.items.indexOfFirst { it.productId == product.id }
            val updatedItems = if (existingIndex >= 0) {
                state.items.toMutableList().also { list ->
                    val existing = list[existingIndex]
                    list[existingIndex] = existing.withQty(existing.qty + qty)
                }
            } else {
                state.items + CartItem(
                    productId = product.id,
                    productName = product.name,
                    qty = qty,
                    priceUnit = product.price,
                    subtotal = qty * product.price,
                    imageUrl = product.imageUrl,
                    uom = product.uom,
                    weightKg = product.weightKg
                )
            }
            state.copy(items = updatedItems)
        }
    }

    fun removeProduct(productId: Int) {
        _uiState.update { state ->
            state.copy(
                items = state.items.filter { it.productId != productId },
                transport = if (state.items.size <= 1) null else state.transport
            )
        }
    }

    fun updateQuantity(productId: Int, newQty: Double) {
        if (newQty <= 0) {
            removeProduct(productId)
            return
        }
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.productId == productId) item.withQty(newQty) else item
                }
            )
        }
    }

    fun clearCart() {
        _uiState.update {
            CartUiState()
        }
    }

    // -------------------------------------------------------------------------
    // Configuración de entrega
    // -------------------------------------------------------------------------

    fun setDeliveryAddress(address: String) {
        _uiState.update { it.copy(deliveryAddress = address) }
    }

    fun setDeliveryLocation(lat: Double, lon: Double) {
        _uiState.update { it.copy(deliveryLat = lat, deliveryLon = lon) }
    }

    fun setUrgent(urgent: Boolean) {
        _uiState.update { it.copy(isUrgent = urgent) }
        // Recalcular transporte automáticamente si ya tenemos ubicación
        val state = _uiState.value
        if (state.deliveryLat != null && state.deliveryLon != null) {
            calculateTransport(
                lat = state.deliveryLat,
                lon = state.deliveryLon,
                weightKg = state.totalWeightKg,
                isUrgent = urgent
            )
        }
    }

    fun setObra(obraId: Int) {
        _uiState.update { it.copy(obraId = obraId) }
    }

    // -------------------------------------------------------------------------
    // Cálculo de transporte
    // -------------------------------------------------------------------------

    fun calculateTransport(
        lat: Double,
        lon: Double,
        weightKg: Double,
        isUrgent: Boolean = _uiState.value.isUrgent
    ) {
        if (_uiState.value.isEmpty) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCalculatingTransport = true, transportError = null) }

            try {
                val response = apiService.calculateTransport(
                    TransportRequest(
                        lat = lat,
                        lon = lon,
                        weightKg = weightKg,
                        isUrgent = isUrgent,
                        obraId = _uiState.value.obraId
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val transportCalc = response.body()!!.data
                    _uiState.update { state ->
                        state.copy(
                            transport = transportCalc,
                            deliveryLat = lat,
                            deliveryLon = lon,
                            isUrgent = isUrgent,
                            isCalculatingTransport = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCalculatingTransport = false,
                            transportError = response.body()?.message ?: "Error calculando transporte"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCalculatingTransport = false,
                        transportError = e.localizedMessage ?: "Error de conexión"
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Descuentos por fidelización
    // -------------------------------------------------------------------------

    /**
     * Aplica un descuento por puntos de fidelidad.
     * @param pointsToUse puntos que el usuario quiere canjear
     * @param pointValueEuros valor en euros de cada punto (configurable desde backend)
     */
    fun applyLoyaltyDiscount(pointsToUse: Int, pointValueEuros: Double = 0.01) {
        val availablePoints = sessionManager.getLoyaltyPoints()
        val effectivePoints = minOf(pointsToUse, availablePoints)
        val discountAmount = effectivePoints * pointValueEuros

        // No aplicar descuento mayor que el total
        val cappedDiscount = minOf(discountAmount, _uiState.value.grossTotal)

        _uiState.update {
            it.copy(
                loyaltyDiscountApplied = effectivePoints > 0,
                loyaltyPointsToUse = effectivePoints,
                loyaltyDiscountAmount = cappedDiscount
            )
        }
    }

    fun removeLoyaltyDiscount() {
        _uiState.update {
            it.copy(
                loyaltyDiscountApplied = false,
                loyaltyPointsToUse = 0,
                loyaltyDiscountAmount = 0.0
            )
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    fun getAvailableLoyaltyPoints(): Int = sessionManager.getLoyaltyPoints()

    fun getUserType() = sessionManager.getUserType()

    fun clearSubmitState() {
        _uiState.update { it.copy(submitError = null, submitSuccess = false) }
    }
}
