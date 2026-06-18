/**
 * Pantalla de detalle de un pedido.
 *
 * Muestra el estado con timeline visual (borrador → confirmado → preparación →
 * reparto → entregado), líneas de producto, desglose de costes,
 * información de tracking (transportista, nº seguimiento, ubicación),
 * y datos de la obra destino.
 */
package com.construccion.marketplace.ui.screens.orders

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.construccion.marketplace.data.model.MaterialRequest
import com.construccion.marketplace.data.model.MaterialRequestState

private val PrimaryOrange = Color(0xFFE65100)

// Nivel de progreso por estado (para colorear el timeline)
private fun stateProgress(state: MaterialRequestState): Int = when (state) {
    MaterialRequestState.DRAFT -> 0
    MaterialRequestState.CONFIRMED -> 1
    MaterialRequestState.APPROVED,
    MaterialRequestState.EN_PREPARACION,
    MaterialRequestState.IN_PROGRESS -> 2
    MaterialRequestState.EN_REPARTO,
    MaterialRequestState.SHIPPED -> 3
    MaterialRequestState.DELIVERED -> 4
    MaterialRequestState.CANCELLED -> -1
}

private data class TimelineStep(
    val nivel: Int,
    val titulo: String,
    val descripcion: String,
    val icono: ImageVector
)

private val TIMELINE_STEPS = listOf(
    TimelineStep(0, "Pedido recibido", "Hemos recibido tu pedido correctamente", Icons.Default.ReceiptLong),
    TimelineStep(1, "Confirmado", "Tu pedido ha sido confirmado por el proveedor", Icons.Default.CheckCircle),
    TimelineStep(2, "En preparación", "Estamos preparando tu pedido en almacén", Icons.Default.Inventory),
    TimelineStep(3, "En camino", "Tu pedido está de camino a la dirección indicada", Icons.Default.LocalShipping),
    TimelineStep(4, "Entregado", "Pedido entregado en destino", Icons.Default.TaskAlt)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    pedidoId: String = "0",
    onNavigateBack: () -> Unit = {},
    onCancelarPedido: () -> Unit = {},
    onVolverAPedir: () -> Unit = {},
    onDescargarFactura: () -> Unit = {},
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Cargar el pedido al entrar en la pantalla
    LaunchedEffect(pedidoId) {
        val id = pedidoId.toIntOrNull() ?: 0
        if (id > 0) {
            viewModel.loadOrder(id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        when (val state = uiState) {
                            is OrderDetailUiState.Success -> {
                                Text(
                                    state.order.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                state.order.createDate?.let { date ->
                                    Text(
                                        date.take(10),
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            else -> Text("Detalle del pedido", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (uiState is OrderDetailUiState.Error || uiState is OrderDetailUiState.Loading) {
                        // Mostrar botón de reintento si hay error
                    } else {
                        IconButton(onClick = {
                            val id = pedidoId.toIntOrNull() ?: 0
                            if (id > 0) viewModel.retry(id)
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Actualizar",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryOrange,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is OrderDetailUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryOrange)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando pedido...", color = Color.Gray)
                    }
                }

                is OrderDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            state.message,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val id = pedidoId.toIntOrNull() ?: 0
                                if (id > 0) viewModel.retry(id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reintentar")
                        }
                    }
                }

                is OrderDetailUiState.Success -> {
                    OrderDetailContent(
                        order = state.order,
                        onCancelarPedido = onCancelarPedido,
                        onVolverAPedir = onVolverAPedir,
                        onDescargarFactura = onDescargarFactura
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: MaterialRequest,
    onCancelarPedido: () -> Unit,
    onVolverAPedir: () -> Unit,
    onDescargarFactura: () -> Unit
) {
    val progress = stateProgress(order.state)
    val isCancelled = order.state == MaterialRequestState.CANCELLED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chip de estado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isCancelled) Color(0xFFFFEBEE)
                        else if (progress >= 4) Color(0xFFE8F5E9)
                        else Color(0xFFFFF3E0)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    order.state.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelled) Color(0xFFD32F2F)
                    else if (progress >= 4) Color(0xFF2E7D32)
                    else PrimaryOrange
                )
            }
        }

        // Obra asociada
        if (order.obraName.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏗", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Obra", fontSize = 11.sp, color = Color.Gray)
                        Text(order.obraName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Timeline de seguimiento (solo si no cancelado)
        if (!isCancelled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Seguimiento del pedido",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TIMELINE_STEPS.forEachIndexed { index, step ->
                        val completado = step.nivel <= progress
                        val actual = step.nivel == progress
                        TimelineItem(
                            titulo = step.titulo,
                            descripcion = step.descripcion,
                            icono = step.icono,
                            fechaHora = if (step.nivel == 0) order.createDate?.take(16)?.replace("T", " ") else null,
                            completado = completado,
                            actual = actual,
                            isLast = index == TIMELINE_STEPS.lastIndex
                        )
                    }
                }
            }
        } else {
            // Estado cancelado
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Pedido cancelado",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Text(
                            "Este pedido ha sido cancelado.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Productos del pedido
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Productos",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()
                if (order.lines.isEmpty()) {
                    Text(
                        "Sin detalle de líneas disponible",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    order.lines.forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    line.productName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${
                                        if (line.qty == line.qty.toLong().toDouble())
                                            line.qty.toLong().toString()
                                        else
                                            String.format("%.2f", line.qty)
                                    } ${line.uom} × ${String.format("%.2f", line.priceUnit)} €",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                "${String.format("%.2f", line.subtotal)} €",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Resumen económico
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Resumen económico",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()
                FilaResumen("Materiales", order.totalAmount)
                if (order.transportCost > 0) {
                    FilaResumen("Transporte", order.transportCost)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Transporte", fontSize = 14.sp, color = Color.DarkGray)
                        Text(
                            "Gratuito",
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (order.loyaltyDiscount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Descuento fidelidad", fontSize = 14.sp, color = Color(0xFF4CAF50))
                        Text(
                            "-${String.format("%.2f", order.loyaltyDiscount)} €",
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(
                        "${String.format("%.2f", order.totalWithTransport)} €",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = PrimaryOrange
                    )
                }
            }
        }

        // Botones de acción
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (order.state) {
                MaterialRequestState.DRAFT,
                MaterialRequestState.CONFIRMED -> {
                    Button(
                        onClick = onCancelarPedido,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancelar pedido", fontWeight = FontWeight.Bold)
                    }
                }

                MaterialRequestState.DELIVERED -> {
                    Button(
                        onClick = onVolverAPedir,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Volver a pedir", fontWeight = FontWeight.Bold)
                    }
                }

                else -> {}
            }

            OutlinedButton(
                onClick = onDescargarFactura,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Descargar factura")
            }
        }
    }
}

@Composable
private fun TimelineItem(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    fechaHora: String?,
    completado: Boolean,
    actual: Boolean,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            val circleColor by animateColorAsState(
                targetValue = when {
                    completado -> PrimaryOrange
                    actual -> Color(0xFF1565C0)
                    else -> Color(0xFFBDBDBD)
                },
                animationSpec = tween(400),
                label = "timelineColor"
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(circleColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icono,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(
                            if (completado) PrimaryOrange else Color(0xFFE0E0E0)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titulo,
                    fontWeight = if (actual || completado) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = when {
                        actual -> Color(0xFF1565C0)
                        completado -> Color(0xFF212121)
                        else -> Color.LightGray
                    }
                )
                if (actual) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE3F2FD))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Ahora",
                            fontSize = 10.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                descripcion,
                fontSize = 12.sp,
                color = if (completado || actual) Color.Gray else Color.LightGray
            )
            if (fechaHora != null) {
                Text(
                    fechaHora,
                    fontSize = 11.sp,
                    color = if (actual) Color(0xFF1565C0) else Color(0xFFBDBDBD)
                )
            }
        }
    }
}

@Composable
private fun FilaResumen(label: String, valor: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.DarkGray)
        Text("${String.format("%.2f", valor)} €", fontSize = 14.sp)
    }
}
