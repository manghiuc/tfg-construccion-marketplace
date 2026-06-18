package com.construccion.marketplace.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccion.marketplace.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la pantalla de inicio (Home).
 *
 * Carga los 8 primeros productos del catálogo de Odoo para mostrarlos
 * como destacados. Si la carga falla, HomeScreen usa productos de ejemplo.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    // Lista de productos destacados mapeados al modelo simplificado de la UI
    private val _products = MutableStateFlow<List<HomeProduct>>(emptyList())
    val products: StateFlow<List<HomeProduct>> = _products.asStateFlow()

    // Indicador de carga para mostrar un shimmer/skeleton en la UI
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadProducts()
    }

    /** Carga los productos destacados desde el repositorio. */
    private fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            productRepository.getProducts(limit = 8).fold(
                onSuccess = { list ->
                    _products.value = list.map { it.toHomeProduct() }
                },
                onFailure = {
                    // Mantener lista vacía — HomeScreen usa sampleProducts como fallback
                }
            )
            _isLoading.value = false
        }
    }

    // ── Mapeo dominio → UI: convierte Product (modelo de red) a HomeProduct (modelo de UI)

    private fun com.construccion.marketplace.data.model.Product.toHomeProduct() = HomeProduct(
        id = id,
        name = name,
        price = price,
        unit = uom,
        category = category.name,
        inStock = stockQty > 0.0
    )
}
