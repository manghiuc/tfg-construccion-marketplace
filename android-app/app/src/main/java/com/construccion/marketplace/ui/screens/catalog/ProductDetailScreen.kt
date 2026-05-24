package com.construccion.marketplace.ui.screens.catalog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.construccion.marketplace.data.api.ProductImageHelper

// ─── Modelo detalle ──────────────────────────────────────────────────────────

enum class StockStatus(val label: String, val color: Color) {
    AVAILABLE("Disponible", Color(0xFF2E7D32)),
    LOW_STOCK("Poco stock", Color(0xFFF57F17)),
    OUT_OF_STOCK("Sin stock", Color(0xFFC62828))
}

data class ProductDetail(
    val id: Int,
    val name: String,
    val category: String,
    val price: Double,
    val unit: String,
    val description: String,
    val stockStatus: StockStatus,
    val images: List<String>, // URLs o identificadores
    val relatedProducts: List<RelatedProduct>
)

data class RelatedProduct(
    val id: Int,
    val name: String,
    val price: Double,
    val unit: String
)

// ─── Datos de ejemplo ────────────────────────────────────────────────────────

private fun getSampleProduct(productId: Int) = ProductDetail(
    id = productId,
    name = "Cemento Portland CEM I 52.5 N",
    category = "Cemento",
    price = 7.50,
    unit = "saco 25kg",
    description = "Cemento Portland de alta resistencia inicial (52.5 N) ideal para hormigones " +
        "estructurales, prefabricados y cualquier aplicación que requiera alta resistencia en " +
        "edades tempranas. Cumple la norma UNE-EN 197-1. Presenta excelente trabajabilidad " +
        "y durabilidad frente a agentes agresivos del suelo y agua. \n\n" +
        "Rendimiento aproximado: 1 saco = 13 litros de hormigón a relación agua/cemento 0.5. " +
        "Conservar en lugar seco y protegido de la humedad. Vida útil: 3 meses desde fabricación.",
    stockStatus = StockStatus.AVAILABLE,
    images = listOf("img_cemento_1", "img_cemento_2", "img_cemento_3"),
    relatedProducts = listOf(
        RelatedProduct(2, "Cemento blanco CEM II 32.5", 9.20, "saco 25kg"),
        RelatedProduct(5, "Arena de río lavada", 3.80, "saco 25kg"),
        RelatedProduct(6, "Grava 12-20mm", 4.50, "saco 25kg"),
        RelatedProduct(7, "Plastificante hormigón 1L", 12.00, "bote"),
        RelatedProduct(8, "Fibra de polipropileno", 5.90, "bolsa")
    )
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    cartItemCount: Int = 0,
    onNavigateBack: () -> Unit = {},
    onAddToCart: (ProductDetail, Int) -> Unit = { _, _ -> },
    onViewCart: () -> Unit = {},
    onRelatedProductClick: (Int) -> Unit = {}
) {
    // En producción se obtendría del ViewModel
    val product = remember { getSampleProduct(productId) }

    var quantity by remember { mutableIntStateOf(1) }
    var descriptionExpanded by remember { mutableStateOf(false) }
    var addedToCart by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { product.images.size.coerceAtLeast(1) })
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(addedToCart) {
        if (addedToCart) {
            snackbarHostState.showSnackbar(
                message = "${product.name} añadido al carrito",
                actionLabel = "Ver carrito",
                duration = SnackbarDuration.Short
            ).also {
                if (it == SnackbarResult.ActionPerformed) onViewCart()
                addedToCart = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = product.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) Badge { Text(cartItemCount.toString()) }
                        }
                    ) {
                        IconButton(onClick = onViewCart) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Carrito")
                        }
                    }
                    IconButton(onClick = { /* Compartir */ }) {
                        Icon(Icons.Filled.Share, contentDescription = "Compartir")
                    }
                }
            )
        },
        bottomBar = {
            ProductBottomBar(
                product = product,
                quantity = quantity,
                addedToCart = addedToCart,
                onAddToCart = {
                    onAddToCart(product, quantity)
                    addedToCart = true
                },
                onViewCart = onViewCart
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Galería de imágenes ──
            ImageGallery(
                productName = product.name,
                images = product.images,
                pagerState = pagerState
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // ── Chip categoría ──
                AssistChip(
                    onClick = {},
                    label = { Text(product.category) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Category,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Nombre ──
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Precio ──
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.2f€".format(product.price),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "/ ${product.unit}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Indicador stock ──
                StockIndicator(status = product.stockStatus)

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // ── Selector cantidad ──
                QuantitySelector(
                    quantity = quantity,
                    onDecrease = { if (quantity > 1) quantity-- },
                    onIncrease = { quantity++ }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // ── Descripción expandible ──
                ExpandableDescription(
                    description = product.description,
                    isExpanded = descriptionExpanded,
                    onToggle = { descriptionExpanded = !descriptionExpanded }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // ── Productos relacionados ──
                RelatedProductsSection(
                    products = product.relatedProducts,
                    onProductClick = onRelatedProductClick
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ─── ImageGallery ────────────────────────────────────────────────────────────

@Composable
private fun ImageGallery(
    productName: String,
    images: List<String>,
    pagerState: PagerState
) {
    val imageUrl = remember(productName) { ProductImageHelper.getImageUrl(productName) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(imageUrl).build(),
                        contentDescription = productName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(96.dp)
                    )
                }
            }
        }

        // Indicadores de página
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(images.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

// ─── StockIndicator ──────────────────────────────────────────────────────────

@Composable
private fun StockIndicator(status: StockStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(status.color)
        )
        Text(
            text = status.label,
            style = MaterialTheme.typography.bodyMedium,
            color = status.color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── QuantitySelector ────────────────────────────────────────────────────────

@Composable
private fun QuantitySelector(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cantidad",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledIconButton(
                onClick = onDecrease,
                enabled = quantity > 1,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = "Reducir cantidad",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 32.dp),
                textAlign = TextAlign.Center
            )

            FilledIconButton(
                onClick = onIncrease,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Aumentar cantidad",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ─── Descripción expandible ──────────────────────────────────────────────────

@Composable
private fun ExpandableDescription(
    description: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(modifier = Modifier.animateContentSize()) {
        Text(
            text = "Descripción",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
            overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = onToggle,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text = if (isExpanded) "Ver menos" else "Ver más",
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Productos relacionados ──────────────────────────────────────────────────

@Composable
private fun RelatedProductsSection(
    products: List<RelatedProduct>,
    onProductClick: (Int) -> Unit
) {
    Text(
        text = "Productos relacionados",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(12.dp))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.id }) { related ->
            RelatedProductCard(product = related, onClick = { onProductClick(related.id) })
        }
    }
}

@Composable
private fun RelatedProductCard(
    product: RelatedProduct,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.width(130.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%.2f€".format(product.price),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/ ${product.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Bottom Bar sticky ────────────────────────────────────────────────────────

@Composable
private fun ProductBottomBar(
    product: ProductDetail,
    quantity: Int,
    addedToCart: Boolean,
    onAddToCart: () -> Unit,
    onViewCart: () -> Unit
) {
    val totalPrice = product.price * quantity

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$quantity ud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "%.2f€ total".format(totalPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (addedToCart) {
                OutlinedButton(
                    onClick = onViewCart,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver carrito")
                }
            } else {
                Button(
                    onClick = onAddToCart,
                    modifier = Modifier.height(48.dp),
                    enabled = product.stockStatus != StockStatus.OUT_OF_STOCK
                ) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (product.stockStatus == StockStatus.OUT_OF_STOCK) "Sin stock"
                               else "Añadir al carrito",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
