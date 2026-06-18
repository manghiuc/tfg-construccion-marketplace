/**
 * Pantalla del carrito de compras.
 *
 * Muestra la lista de artículos añadidos con controles para cambiar
 * cantidad (+/-), eliminar artículos, y un resumen con subtotal,
 * transporte estimado y total. Botón "Tramitar pedido" lleva al Checkout.
 */
package com.construccion.marketplace.ui.screens.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.construccion.marketplace.viewmodel.CartUiState
import com.construccion.marketplace.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateBack: () -> Unit,
    onCheckout: () -> Unit,
    viewModel: CartViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.transportError) {
        uiState.transportError?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Carrito (${uiState.itemCount})",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!uiState.isEmpty) {
                CartBottomBar(
                    total = uiState.netTotal,
                    isUrgent = uiState.isUrgent,
                    onCheckout = onCheckout
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isEmpty) {
            EmptyCartContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Lista de productos
                items(
                    items = uiState.items,
                    key = { it.productId }
                ) { item ->
                    CartItemRow(
                        item = item,
                        onRemove = { viewModel.removeProduct(item.productId) },
                        onIncrement = {
                            viewModel.updateQuantity(item.productId, item.qty + 1)
                        },
                        onDecrement = {
                            viewModel.updateQuantity(item.productId, item.qty - 1)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                // Separador
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Card de transporte y resumen
                item {
                    TransportSummaryCard(
                        uiState = uiState,
                        onUrgentChange = { viewModel.setUrgent(it) },
                        onLoyaltyToggle = { usePoints, points ->
                            if (usePoints) {
                                viewModel.applyLoyaltyDiscount(points)
                            } else {
                                viewModel.removeLoyaltyDiscount()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Fila de producto en el carrito con SwipeToDismiss
// -------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartItemRow(
    item: com.construccion.marketplace.data.model.CartItem,
    onRemove: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Imagen del producto
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.productName,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Nombre y precio unitario
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatEuros(item.priceUnit) + " / ${item.uom}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Subtotal: ${formatEuros(item.subtotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Control de cantidad
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = onDecrement,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Reducir cantidad",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = if (item.qty % 1 == 0.0) item.qty.toInt().toString() else item.qty.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    FilledIconButton(
                        onClick = onIncrement,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aumentar cantidad",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Card de resumen: transporte + fidelización + totales
// -------------------------------------------------------------------------

@Composable
private fun TransportSummaryCard(
    uiState: CartUiState,
    onUrgentChange: (Boolean) -> Unit,
    onLoyaltyToggle: (Boolean, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var useLoyaltyPoints by remember { mutableStateOf(false) }
    val availablePoints = uiState.loyaltyPointsToUse.coerceAtLeast(0)

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Entrega y transporte",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Switch de pedido urgente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pedido urgente",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Entrega en 24h (+50%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isUrgent,
                    onCheckedChange = onUrgentChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Desglose de costes
            Text(
                text = "Resumen del pedido",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SummaryRow(label = "Materiales", amount = uiState.materialTotal)

            val transportBase = uiState.weightBasedTransportBase
            val transportFinal = uiState.weightBasedTransport
            val urgentColor = MaterialTheme.colorScheme.primary
            if (transportBase > 0.0) {
                // Mostrar peso total y tramo aplicado
                Text(
                    text = "Peso total: ${String.format(Locale("es", "ES"), "%.1f", uiState.totalWeightKg)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SummaryRow(label = "Transporte (por peso)", amount = transportBase)
                if (uiState.isUrgent) {
                    SummaryRow(
                        label = "⚡ Recargo urgente (+50%)",
                        amount = transportFinal - transportBase,
                        color = urgentColor
                    )
                }
                SummaryRow(
                    label = if (uiState.isUrgent) "⚡ Total transporte" else "Total transporte",
                    amount = transportFinal,
                    bold = true,
                    color = if (uiState.isUrgent) urgentColor else MaterialTheme.colorScheme.onSurface
                )
            } else {
                SummaryRow(label = "Transporte (gratuito)", amount = 0.0)
            }

            // Fidelización (si tiene puntos disponibles)
            val sessionPoints = uiState.loyaltyPointsToUse
            AnimatedVisibility(
                visible = sessionPoints > 0 || availablePoints > 0,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Usar puntos (${uiState.loyaltyPointsToUse} disp.)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Descuento: ${formatEuros(uiState.loyaltyDiscountAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Checkbox(
                            checked = useLoyaltyPoints,
                            onCheckedChange = { checked ->
                                useLoyaltyPoints = checked
                                onLoyaltyToggle(checked, uiState.loyaltyPointsToUse)
                            }
                        )
                    }

                    if (useLoyaltyPoints && uiState.loyaltyDiscountAmount > 0) {
                        SummaryRow(
                            label = "Descuento fidelización",
                            amount = -uiState.loyaltyDiscountAmount,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Total final
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isUrgent) "⚡ TOTAL URGENTE" else "TOTAL",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (uiState.isUrgent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatEuros(uiState.netTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: Double,
    bold: Boolean = false,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
        Text(
            text = formatEuros(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

// -------------------------------------------------------------------------
// Bottom bar con total y botón de checkout
// -------------------------------------------------------------------------

@Composable
private fun CartBottomBar(
    total: Double,
    isUrgent: Boolean,
    onCheckout: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isUrgent) "⚡ Total urgente (24h)" else "Total a pagar",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUrgent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatEuros(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onCheckout,
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isUrgent) "⚡ Checkout urgente" else "Ir al checkout",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Estado vacío
// -------------------------------------------------------------------------

@Composable
private fun EmptyCartContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tu carrito está vacío",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Añade materiales desde el catálogo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------------------------------------
// Utilidad de formato
// -------------------------------------------------------------------------

private fun formatEuros(amount: Double): String =
    String.format(Locale("es", "ES"), "%.2f €", amount)
