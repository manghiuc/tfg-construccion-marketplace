package com.construccion.marketplace.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.construccion.marketplace.data.api.ProductImageHelper

// ─── Modelos de datos ────────────────────────────────────────────────────────

data class Category(val name: String)

/**
 * Modelo de producto para la capa UI de la home.
 * Se mapea desde [com.construccion.marketplace.data.model.Product] en [HomeViewModel].
 * CatalogScreen también usa este tipo a través del import de ProductCard.
 */
data class HomeProduct(
    val id: Int,
    val name: String,
    val price: Double,
    val unit: String,
    val category: String,
    val inStock: Boolean = true
)

data class ActiveWork(val id: Int, val name: String)

data class KpiCard(val title: String, val value: String, val icon: ImageVector, val color: Color)

// ─── Datos de ejemplo (fallback mientras carga o sin conexión) ───────────────

private val sampleCategories = listOf(
    Category("Cemento"),
    Category("Hierro"),
    Category("Madera"),
    Category("Pintura"),
    Category("Fontanería"),
    Category("Electricidad"),
    Category("Herramientas"),
    Category("Aislamiento")
)

private val sampleProducts = listOf(
    HomeProduct(1, "Cemento Portland CEM I 52.5", 7.50, "saco 25kg", "Cemento"),
    HomeProduct(2, "Varilla corrugada B500S Ø12", 3.20, "ud", "Hierro"),
    HomeProduct(3, "Tablero OSB 18mm 244x122", 22.90, "ud", "Madera"),
    HomeProduct(4, "Pintura plástica blanca 15L", 38.00, "bote", "Pintura"),
    HomeProduct(5, "Tubo PVC evacuación 110mm", 4.80, "m", "Fontanería"),
    HomeProduct(6, "Cable eléctrico 2.5mm² (100m)", 45.00, "rollo", "Electricidad"),
    HomeProduct(7, "Ladrillo caravista 24x11.5x7", 0.45, "ud", "Albañilería"),
    HomeProduct(8, "Mortero cola flexible C2", 12.30, "saco 25kg", "Adhesivos")
)

private val sampleActiveWorks = listOf(
    ActiveWork(1, "Obra calle Mayor 12"),
    ActiveWork(2, "Reforma local comercial"),
    ActiveWork(3, "Vivienda unifamiliar"),
    ActiveWork(4, "Naves industriales Pol. Norte")
)

// ─── HomeScreen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userType: String, // "particular" | "autonomo" | "empresa" | "admin"
    cartItemCount: Int = 0,
    notificationCount: Int = 3,
    onCartClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onProductClick: (Int) -> Unit = {},
    onAddToCart: (HomeProduct) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val apiProducts by viewModel.products.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Usa los datos reales si ya llegaron; si no, muestra el fallback sample
    val allProducts = if (!isLoading && apiProducts.isNotEmpty()) apiProducts else sampleProducts
    // Filtra por categoría seleccionada (si hay alguna)
    val displayProducts = if (selectedCategory != null) {
        allProducts.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    } else {
        allProducts
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ConstruApp",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    // Notificaciones con badge
                    BadgedBox(
                        badge = {
                            if (notificationCount > 0) {
                                Badge { Text(notificationCount.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = onNotificationsClick) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones")
                        }
                    }
                    // Carrito con badge
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge { Text(cartItemCount.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = onCartClick) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Carrito")
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
                .verticalScroll(rememberScrollState())
        ) {
            // ── SearchBar ──
            SearchBarSection(onClick = onSearchClick)

            // ── KPIs (solo admin) ──
            if (userType == "admin") {
                AdminKpiSection()
            }

            // ── Obras activas (empresa / autónomo) ──
            if (userType == "empresa" || userType == "autonomo") {
                ActiveWorksSection(works = sampleActiveWorks)
            }

            // ── Categorías ──
            CategorySection(
                categories = sampleCategories,
                selectedCategory = selectedCategory,
                onCategoryClick = { name ->
                    selectedCategory = if (selectedCategory == name) null else name
                    onCategoryClick(name)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Productos populares ──
            Text(
                text = if (selectedCategory != null) selectedCategory!! else "Productos populares",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Indicador de carga mientras llegan los datos reales
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            ProductsGrid(
                products = displayProducts,
                onProductClick = onProductClick,
                onAddToCart = onAddToCart
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── SearchBar ──────────────────────────────────────────────────────────────

@Composable
private fun SearchBarSection(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Buscar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Buscar materiales...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// ─── KPIs admin ─────────────────────────────────────────────────────────────

@Composable
private fun AdminKpiSection() {
    val primary = MaterialTheme.colorScheme.primary
    val kpis = listOf(
        KpiCard("Pedidos hoy", "34", Icons.Filled.ShoppingBag, primary),
        KpiCard("Importe hoy", "8.240€", Icons.Filled.Euro, Color(0xFF1B5E20)),
        KpiCard("Pendientes", "12", Icons.Filled.HourglassEmpty, Color(0xFFF57F17)),
        KpiCard("Clientes nuevos", "7", Icons.Filled.PersonAdd, Color(0xFF1A237E))
    )

    Text(
        text = "Resumen del día",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(kpis) { kpi ->
            KpiCardItem(kpi)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun KpiCardItem(kpi: KpiCard) {
    ElevatedCard(modifier = Modifier.width(140.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = kpi.icon,
                contentDescription = kpi.title,
                tint = kpi.color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = kpi.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = kpi.color
            )
            Text(
                text = kpi.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Obras activas ──────────────────────────────────────────────────────────

@Composable
private fun ActiveWorksSection(works: List<ActiveWork>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mis obras activas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = {}) {
                Text("Ver todas")
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(works) { work ->
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text(work.name) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Construction,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─── Categorías ─────────────────────────────────────────────────────────────

@Composable
private fun CategorySection(
    categories: List<Category>,
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit
) {
    Text(
        text = "Categorías",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category.name,
                onClick = { onCategoryClick(category.name) },
                label = { Text(category.name) }
            )
        }
    }
}

// ─── Grid de productos ───────────────────────────────────────────────────────

@Composable
private fun ProductsGrid(
    products: List<HomeProduct>,
    onProductClick: (Int) -> Unit,
    onAddToCart: (HomeProduct) -> Unit
) {
    // LazyVerticalGrid dentro de un Column con scroll externo: usamos altura fija calculada
    val rows = (products.size + 1) / 2
    val itemHeight = 220
    val gridHeight = (rows * itemHeight).dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false // El scroll lo gestiona el Column exterior
    ) {
        items(products) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product.id) },
                onAddToCart = { onAddToCart(product) }
            )
        }
    }
}

// ─── ProductCard ─────────────────────────────────────────────────────────────

@Composable
fun ProductCard(
    product: HomeProduct,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Imagen del producto (keyword → URL estática Odoo, fallback icono)
            val imageUrl = remember(product.name) {
                ProductImageHelper.getImageUrl(product.name)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .build(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                } else {
                    Icon(
                        imageVector = when (product.category.lowercase()) {
                            "cemento", "materiales de construcción" -> Icons.Filled.Layers
                            "hierro" -> Icons.Filled.Build
                            "madera" -> Icons.Filled.Forest
                            "pintura" -> Icons.Filled.Brush
                            "fontanería", "fontaneria" -> Icons.Filled.Plumbing
                            "electricidad", "herramientas y equipos" -> Icons.Filled.Bolt
                            "herramientas" -> Icons.Filled.Construction
                            "aislamiento" -> Icons.Filled.Shield
                            else -> Icons.Filled.Inventory
                        },
                        contentDescription = product.category,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Badge de stock
                val stockColor = if (product.inStock) Color(0xFF2E7D32) else Color(0xFFC62828)
                val stockText = if (product.inStock) "Disponible" else "Sin stock"
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = stockColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = stockText,
                        style = MaterialTheme.typography.labelSmall,
                        color = stockColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "%.2f€".format(product.price),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "/ ${product.unit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = { if (product.inStock) onAddToCart() },
                        modifier = Modifier.size(32.dp),
                        containerColor = if (product.inStock)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Añadir al carrito",
                            tint = if (product.inStock)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
