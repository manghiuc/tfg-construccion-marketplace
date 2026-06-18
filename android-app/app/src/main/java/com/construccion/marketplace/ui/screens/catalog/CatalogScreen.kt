/**
 * Pantalla del catálogo de productos.
 *
 * Permite buscar productos con un campo de texto con debounce,
 * filtrar por categoría mediante chips horizontales, y muestra
 * los resultados en una cuadrícula con imagen, precio y botón
 * de añadir al carrito. Incluye skeleton loading animado.
 */
package com.construccion.marketplace.ui.screens.catalog

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccion.marketplace.ui.screens.home.HomeProduct
import com.construccion.marketplace.ui.screens.home.ProductCard
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─── Modelos ─────────────────────────────────────────────────────────────────

data class CatalogFilters(
    val minPrice: Float = 0f,
    val maxPrice: Float = 1000f,
    val sortBy: SortOption = SortOption.RELEVANCE,
    val onlyInStock: Boolean = false
)

enum class SortOption(val label: String) {
    RELEVANCE("Relevancia"),
    PRICE_ASC("Precio: menor a mayor"),
    PRICE_DESC("Precio: mayor a menor"),
    NAME_ASC("Nombre A-Z")
}

sealed class CatalogUiState {
    object Loading : CatalogUiState()
    data class Success(val products: List<HomeProduct>) : CatalogUiState()
    object Empty : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

private val allProducts = listOf(
    HomeProduct(1, "Cemento Portland CEM I 52.5", 7.50, "saco 25kg", "Cemento"),
    HomeProduct(2, "Cemento blanco CEM II 32.5", 9.20, "saco 25kg", "Cemento"),
    HomeProduct(3, "Varilla corrugada B500S Ø12", 3.20, "ud", "Hierro"),
    HomeProduct(4, "Malla electrosoldada 15x15", 12.80, "m²", "Hierro"),
    HomeProduct(5, "Tablero OSB 18mm 244x122", 22.90, "ud", "Madera"),
    HomeProduct(6, "Viga madera pino 10x10x300", 18.50, "ud", "Madera"),
    HomeProduct(7, "Pintura plástica blanca 15L", 38.00, "bote", "Pintura"),
    HomeProduct(8, "Esmalte sintético gris 4L", 24.50, "bote", "Pintura"),
    HomeProduct(9, "Tubo PVC evacuación 110mm", 4.80, "m", "Fontanería"),
    HomeProduct(10, "Grifo monomando cocina", 45.00, "ud", "Fontanería"),
    HomeProduct(11, "Cable eléctrico 2.5mm² (100m)", 45.00, "rollo", "Electricidad"),
    HomeProduct(12, "Caja de empotrar 65x65mm", 0.85, "ud", "Electricidad", false)
)

private val categories = listOf("Cemento", "Hierro", "Madera", "Pintura", "Fontanería", "Electricidad")

class CatalogViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _filters = MutableStateFlow(CatalogFilters())
    val filters: StateFlow<CatalogFilters> = _filters.asStateFlow()

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        observeFilters()
    }

    @OptIn(FlowPreview::class)
    private fun observeFilters() {
        viewModelScope.launch {
            combine(
                _searchQuery.debounce(300L),
                _selectedCategory,
                _filters
            ) { query, category, filters ->
                Triple(query, category, filters)
            }.collect { (query, category, filters) ->
                applyFilters(query, category, filters)
            }
        }
    }

    private suspend fun applyFilters(
        query: String,
        category: String?,
        filters: CatalogFilters
    ) {
        _uiState.value = CatalogUiState.Loading
        delay(300L) // Simula latencia de red

        var result = allProducts.filter { product ->
            val matchesQuery = query.isEmpty() || product.name.contains(query, ignoreCase = true)
            val matchesCategory = category == null || product.category == category
            val matchesPrice = product.price.toFloat() in filters.minPrice..filters.maxPrice
            val matchesStock = !filters.onlyInStock || product.inStock
            matchesQuery && matchesCategory && matchesPrice && matchesStock
        }

        result = when (filters.sortBy) {
            SortOption.PRICE_ASC -> result.sortedBy { it.price }
            SortOption.PRICE_DESC -> result.sortedByDescending { it.price }
            SortOption.NAME_ASC -> result.sortedBy { it.name }
            SortOption.RELEVANCE -> result
        }

        _uiState.value = if (result.isEmpty()) CatalogUiState.Empty
                         else CatalogUiState.Success(result)
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onCategorySelect(category: String?) { _selectedCategory.value = category }
    fun onFiltersChange(newFilters: CatalogFilters) { _filters.value = newFilters }
}

// ─── CatalogScreen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onProductClick: (Int) -> Unit = {},
    onAddToCart: (HomeProduct) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filters by viewModel.filters.collectAsState()

    var showFiltersSheet by remember { mutableStateOf(false) }
    var localFilters by remember { mutableStateOf(filters) }

    if (showFiltersSheet) {
        FiltersBottomSheet(
            filters = localFilters,
            onFiltersChange = { localFilters = it },
            onApply = {
                viewModel.onFiltersChange(localFilters)
                showFiltersSheet = false
            },
            onDismiss = { showFiltersSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showFiltersSheet = true }) {
                        BadgedBox(
                            badge = {
                                val activeFilters = (if (filters.onlyInStock) 1 else 0) +
                                    (if (filters.sortBy != SortOption.RELEVANCE) 1 else 0) +
                                    (if (filters.minPrice > 0f || filters.maxPrice < 1000f) 1 else 0)
                                if (activeFilters > 0) Badge { Text(activeFilters.toString()) }
                            }
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = "Filtros")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── SearchBar ──
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSearch = {},
                active = false,
                onActiveChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar materiales...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                        }
                    }
                }
            ) {}

            // ── Chips categorías ──
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.onCategorySelect(null) },
                        label = { Text("Todas") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            viewModel.onCategorySelect(if (selectedCategory == cat) null else cat)
                        },
                        label = { Text(cat) }
                    )
                }
            }

            // ── Contenido principal ──
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is CatalogUiState.Loading -> ShimmerGrid()
                    is CatalogUiState.Empty -> EmptyState(query = searchQuery)
                    is CatalogUiState.Success -> {
                        ProductsLazyGrid(
                            products = state.products,
                            onProductClick = onProductClick,
                            onAddToCart = onAddToCart
                        )
                    }
                    is CatalogUiState.Error -> ErrorState(message = state.message)
                }
            }
        }
    }
}

// ─── ProductsLazyGrid ────────────────────────────────────────────────────────

@Composable
private fun ProductsLazyGrid(
    products: List<HomeProduct>,
    onProductClick: (Int) -> Unit,
    onAddToCart: (HomeProduct) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product.id) },
                onAddToCart = { onAddToCart(product) }
            )
        }
    }
}

// ─── Shimmer ─────────────────────────────────────────────────────────────────

@Composable
private fun ShimmerGrid() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(8) {
            ShimmerCard(alpha = alpha)
        }
    }
}

@Composable
private fun ShimmerCard(alpha: Float) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                    )
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(14.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.2f),
                            shape = MaterialTheme.shapes.small
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.2f),
                            shape = MaterialTheme.shapes.small
                        )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(24.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.3f),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
    }
}

// ─── Estado vacío ────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (query.isNotEmpty()) "Sin resultados para \"$query\""
                   else "No hay productos con estos filtros",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Prueba con otros términos o ajusta los filtros",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Estado de error ─────────────────────────────────────────────────────────

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

// ─── FiltersBottomSheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersBottomSheet(
    filters: CatalogFilters,
    onFiltersChange: (CatalogFilters) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtros",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    onFiltersChange(CatalogFilters())
                }) {
                    Text("Resetear")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Rango de precio ──
            Text(
                text = "Rango de precio",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "%.0f€ — %.0f€".format(filters.minPrice, filters.maxPrice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RangeSlider(
                value = filters.minPrice..filters.maxPrice,
                onValueChange = { range ->
                    onFiltersChange(filters.copy(minPrice = range.start, maxPrice = range.endInclusive))
                },
                valueRange = 0f..1000f,
                steps = 19
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // ── Ordenar por ──
            Text(
                text = "Ordenar por",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            SortOption.values().forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = filters.sortBy == option,
                        onClick = { onFiltersChange(filters.copy(sortBy = option)) }
                    )
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // ── Solo en stock ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Solo productos en stock",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = filters.onlyInStock,
                    onCheckedChange = { onFiltersChange(filters.copy(onlyInStock = it)) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Aplicar filtros",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
