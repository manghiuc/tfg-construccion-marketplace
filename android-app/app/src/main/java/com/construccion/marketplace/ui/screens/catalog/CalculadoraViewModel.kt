package com.construccion.marketplace.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccion.marketplace.data.api.OdooApiService
import com.construccion.marketplace.data.model.CalculatorResultOdoo
import com.construccion.marketplace.data.model.ObraMaterial
import com.construccion.marketplace.data.model.Product
import com.construccion.marketplace.data.model.ProductCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estados posibles de la pantalla de la calculadora de materiales. */
sealed class CalculadoraUiState {
    /** Estado inicial: esperando que el usuario introduzca datos. */
    object Idle : CalculadoraUiState()
    /** Calculando en el backend. */
    object Loading : CalculadoraUiState()
    /** Cálculo completado con éxito. */
    data class Success(val resultado: CalculatorResultOdoo) : CalculadoraUiState()
    /** Error durante el cálculo. */
    data class Error(val message: String) : CalculadoraUiState()
}

/** Cada material del resultado con su producto de catálogo (si se encontró) */
data class CartMaterial(
    val material: ObraMaterial,
    val product: Product?,
    val qty: Double = material.quantity
)

/** Estado del proceso de búsqueda de productos para añadir al carrito. */
sealed class CartAddState {
    object Idle : CartAddState()
    object Searching : CartAddState()
    data class Ready(val items: List<CartMaterial>) : CartAddState()
}

/**
 * ViewModel de la calculadora de materiales.
 *
 * Permite al usuario seleccionar un tipo de obra y una superficie (m²),
 * calcula los materiales necesarios vía el backend de Odoo, y ofrece
 * la opción de añadir todos los materiales al carrito con un clic.
 */
@HiltViewModel
class CalculadoraViewModel @Inject constructor(
    private val apiService: OdooApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<CalculadoraUiState>(CalculadoraUiState.Idle)
    val uiState: StateFlow<CalculadoraUiState> = _uiState.asStateFlow()

    private val _cartAddState = MutableStateFlow<CartAddState>(CartAddState.Idle)
    val cartAddState: StateFlow<CartAddState> = _cartAddState.asStateFlow()

    fun calcular(type: String, m2: Double) {
        viewModelScope.launch {
            _uiState.value = CalculadoraUiState.Loading
            _cartAddState.value = CartAddState.Idle
            try {
                val response = apiService.calculateMaterials(type, m2)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    _uiState.value = if (data != null) {
                        CalculadoraUiState.Success(data)
                    } else {
                        CalculadoraUiState.Error("Sin resultados del servidor")
                    }
                } else {
                    _uiState.value = CalculadoraUiState.Error("Error ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = CalculadoraUiState.Error(
                    e.localizedMessage ?: "Error de conexión"
                )
            }
        }
    }

    /** Busca en el catálogo el producto que mejor coincide con cada material calculado.
     *  Si el endpoint ya devolvió product_id embebido, lo usa directamente sin llamada extra. */
    fun buscarProductosParaCarrito(materials: List<ObraMaterial>) {
        if (_cartAddState.value is CartAddState.Searching) return
        viewModelScope.launch {
            _cartAddState.value = CartAddState.Searching
            val items = materials.map { mat ->
                // Caso 1: el endpoint ya resolvió el producto → producto sintético directo
                if (mat.productId != null && mat.productId > 0 && mat.priceUnit > 0.0) {
                    val syntheticProduct = Product(
                        id = mat.productId,
                        name = mat.productName ?: mat.material,
                        defaultCode = "",
                        category = ProductCategory(id = 0, name = ""),
                        uom = mat.productUom ?: mat.unit,
                        price = mat.priceUnit
                    )
                    CartMaterial(material = mat, product = syntheticProduct, qty = mat.quantity)
                } else {
                    // Caso 2: fallback — buscar por PRIMERA palabra (búsqueda más amplia)
                    try {
                        val query = mat.material.split(" ").first()
                        val resp = apiService.getProducts(search = query, pageSize = 1)
                        val product = if (resp.isSuccessful && resp.body()?.success == true) {
                            resp.body()?.data?.firstOrNull()
                        } else null
                        CartMaterial(material = mat, product = product, qty = mat.quantity)
                    } catch (_: Exception) {
                        CartMaterial(material = mat, product = null, qty = mat.quantity)
                    }
                }
            }
            _cartAddState.value = CartAddState.Ready(items)
        }
    }

    /** Ajusta la cantidad de un material antes de añadir al carrito */
    fun updateCartQty(index: Int, qty: Double) {
        val current = _cartAddState.value
        if (current is CartAddState.Ready) {
            val updated = current.items.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(qty = qty.coerceAtLeast(0.1))
                _cartAddState.value = CartAddState.Ready(updated)
            }
        }
    }

    fun reset() {
        _uiState.value = CalculadoraUiState.Idle
        _cartAddState.value = CartAddState.Idle
    }
}
