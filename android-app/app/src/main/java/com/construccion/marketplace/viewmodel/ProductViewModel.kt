package com.construccion.marketplace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccion.marketplace.data.model.Product
import com.construccion.marketplace.data.repository.CalculatedMaterial
import com.construccion.marketplace.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// =============================================================================
// Estados de UI
// =============================================================================

data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategoryId: Int? = null
)

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CalculatorUiState(
    val materials: List<CalculatedMaterial> = emptyList(),
    val totalEstimate: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val obraType: String = "",
    val m2: Float = 0f
)

// =============================================================================
// ViewModel
// =============================================================================

/**
 * ViewModel del catálogo de productos y calculadora de materiales.
 *
 * Gestiona tres flujos de estado independientes:
 * - [listUiState]: lista del catálogo con búsqueda y filtros
 * - [detailUiState]: detalle de un producto seleccionado
 * - [calculatorUiState]: resultados de la calculadora de obra
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    // -------------------------------------------------------------------------
    // Estado: lista de productos
    // -------------------------------------------------------------------------

    private val _listUiState = MutableStateFlow(ProductListUiState())
    val listUiState: StateFlow<ProductListUiState> = _listUiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Estado: detalle de producto seleccionado
    // -------------------------------------------------------------------------

    private val _detailUiState = MutableStateFlow(ProductDetailUiState())
    val detailUiState: StateFlow<ProductDetailUiState> = _detailUiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Estado: calculadora de materiales
    // -------------------------------------------------------------------------

    private val _calculatorUiState = MutableStateFlow(CalculatorUiState())
    val calculatorUiState: StateFlow<CalculatorUiState> = _calculatorUiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Búsqueda con debounce
    // -------------------------------------------------------------------------

    /** Flujo interno para disparar búsquedas con debounce de 400 ms */
    private val _searchQuery = MutableStateFlow("")

    init {
        // Escuchar cambios en la búsqueda con debounce para evitar llamadas
        // excesivas a la API mientras el usuario escribe
        _searchQuery
            .debounce(400L)
            .distinctUntilChanged()
            .onEach { query ->
                loadProducts(query, _listUiState.value.selectedCategoryId)
            }
            .launchIn(viewModelScope)

        // Carga inicial del catálogo
        loadProducts()
    }

    // -------------------------------------------------------------------------
    // Catálogo
    // -------------------------------------------------------------------------

    /**
     * Actualiza el texto de búsqueda. La carga se dispara automáticamente
     * tras el debounce de 400 ms para no saturar la API.
     *
     * @param query texto libre de búsqueda
     */
    fun searchProducts(query: String) {
        _listUiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    /**
     * Aplica un filtro de categoría y recarga el catálogo.
     *
     * @param categoryId ID de la categoría, o null para mostrar todas
     */
    fun filterByCategory(categoryId: Int?) {
        _listUiState.update { it.copy(selectedCategoryId = categoryId) }
        loadProducts(_listUiState.value.searchQuery, categoryId)
    }

    /** Recarga el catálogo con los parámetros actuales. */
    fun refresh() {
        val state = _listUiState.value
        loadProducts(state.searchQuery, state.selectedCategoryId)
    }

    private fun loadProducts(search: String = "", categoryId: Int? = null) {
        viewModelScope.launch {
            _listUiState.update { it.copy(isLoading = true, error = null) }

            val result = productRepository.getProducts(
                search = search,
                categoryId = categoryId
            )

            _listUiState.update { state ->
                result.fold(
                    onSuccess = { products ->
                        state.copy(products = products, isLoading = false, error = null)
                    },
                    onFailure = { error ->
                        state.copy(isLoading = false, error = error.message)
                    }
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Detalle de producto
    // -------------------------------------------------------------------------

    /**
     * Carga el detalle de un producto por su ID.
     *
     * @param productId identificador del producto en Odoo
     */
    fun loadProductDetail(productId: Int) {
        viewModelScope.launch {
            _detailUiState.update { it.copy(isLoading = true, error = null) }

            val result = productRepository.getProductById(productId)

            _detailUiState.update {
                result.fold(
                    onSuccess = { product ->
                        it.copy(product = product, isLoading = false, error = null)
                    },
                    onFailure = { error ->
                        it.copy(isLoading = false, error = error.message)
                    }
                )
            }
        }
    }

    /** Limpia el estado del detalle al salir de la pantalla. */
    fun clearProductDetail() {
        _detailUiState.value = ProductDetailUiState()
    }

    // -------------------------------------------------------------------------
    // Calculadora de materiales
    // -------------------------------------------------------------------------

    /**
     * Calcula los materiales necesarios para una obra según su tipo y superficie.
     *
     * @param type tipo de construcción (ej. "tabique", "solera", "cubierta")
     * @param m2 superficie en metros cuadrados
     */
    fun loadCalculator(type: String, m2: Float) {
        if (type.isBlank()) {
            _calculatorUiState.update {
                it.copy(error = "Selecciona un tipo de construcción")
            }
            return
        }
        if (m2 <= 0f) {
            _calculatorUiState.update {
                it.copy(error = "Introduce una superficie válida mayor que 0")
            }
            return
        }

        viewModelScope.launch {
            _calculatorUiState.update {
                it.copy(isLoading = true, error = null, obraType = type, m2 = m2)
            }

            val result = productRepository.calculateMaterials(type, m2)

            _calculatorUiState.update { state ->
                result.fold(
                    onSuccess = { materials ->
                        val total = materials.sumOf { it.subtotal }
                        state.copy(
                            materials = materials,
                            totalEstimate = total,
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        state.copy(isLoading = false, error = error.message)
                    }
                )
            }
        }
    }

    /** Limpia los resultados de la calculadora. */
    fun clearCalculator() {
        _calculatorUiState.value = CalculatorUiState()
    }

    /** Limpia el error de la calculadora sin perder los resultados. */
    fun clearCalculatorError() {
        _calculatorUiState.update { it.copy(error = null) }
    }
}
