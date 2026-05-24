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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<HomeProduct>>(emptyList())
    val products: StateFlow<List<HomeProduct>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadProducts()
    }

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

    // ── Mapeo dominio → UI ────────────────────────────────────────────────────

    private fun com.construccion.marketplace.data.model.Product.toHomeProduct() = HomeProduct(
        id = id,
        name = name,
        price = price,
        unit = uom,
        category = category.name,
        inStock = stockQty > 0.0
    )
}
