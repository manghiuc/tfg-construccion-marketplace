package com.construccion.marketplace.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.construccion.marketplace.data.model.MaterialRequest
import com.construccion.marketplace.data.model.MaterialRequestState
import kotlinx.coroutines.launch

private val PrimaryOrange = Color(0xFFE65100)
private val OrangeLight   = Color(0xFFFFF3E0)

// ─── Filtros de estado ────────────────────────────────────────────────────────

private data class FiltroEstado(val label: String, val stateKey: String?)

private val FILTROS = listOf(
    FiltroEstado("Todos",          null),
    FiltroEstado("Tramitando",     "confirmed"),
    FiltroEstado("En Preparación", "en_preparacion"),
    FiltroEstado("En Reparto",     "en_reparto"),
    FiltroEstado("Entregado",      "delivered"),
    FiltroEstado("Cancelado",      "cancelled"),
)

// ─── Helpers de estado ────────────────────────────────────────────────────────

private fun MaterialRequestState.toLabel(): String = when (this) {
    MaterialRequestState.DRAFT          -> "Borrador"
    MaterialRequestState.CONFIRMED      -> "Tramitando"
    MaterialRequestState.APPROVED       -> "Aprobado"
    MaterialRequestState.EN_PREPARACION,
    MaterialRequestState.IN_PROGRESS    -> "En preparación"
    MaterialRequestState.EN_REPARTO,
    MaterialRequestState.SHIPPED        -> "En reparto"
    MaterialRequestState.DELIVERED      -> "Entregado"
    MaterialRequestState.CANCELLED      -> "Cancelado"
}

private fun MaterialRequestState.toColor(): Color = when (this) {
    MaterialRequestState.DRAFT          -> Color(0xFF9E9E9E)
    MaterialRequestState.CONFIRMED      -> Color(0xFFF57F17)
    MaterialRequestState.APPROVED       -> Color(0xFF1565C0)
    MaterialRequestState.EN_PREPARACION,
    MaterialRequestState.IN_PROGRESS    -> Color(0xFF1565C0)
    MaterialRequestState.EN_REPARTO,
    MaterialRequestState.SHIPPED        -> Color(0xFF2E7D32)
    MaterialRequestState.DELIVERED      -> Color(0xFF388E3C)
    MaterialRequestState.CANCELLED      -> Color(0xFFC62828)
}

// ─── Pantalla principal ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onNavigateBack: () -> Unit = {},
    onPedidoClick: (String) -> Unit = {},
    viewModel: OrderHistoryViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis pedidos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
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
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    viewModel.loadOrders()
                    isRefreshing = false
                }
            },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Filtros por estado ──
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FILTROS) { filtro ->
                        FilterChip(
                            selected = activeFilter == filtro.stateKey,
                            onClick  = { viewModel.setFilter(filtro.stateKey) },
                            label    = { Text(filtro.label, fontSize = 13.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryOrange,
                                selectedLabelColor     = Color.White
                            )
                        )
                    }
                }

                // ── Contenido ──
                when (val state = uiState) {
                    is OrderHistoryUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryOrange)
                        }
                    }

                    is OrderHistoryUiState.Error -> {
                        EstadoError(mensaje = state.message, onRetry = { viewModel.loadOrders() })
                    }

                    is OrderHistoryUiState.Empty -> {
                        EstadoVacio(filtroLabel = FILTROS.firstOrNull { it.stateKey == activeFilter }?.label ?: "Todos")
                    }

                    is OrderHistoryUiState.Success -> {
                        val soloUnGrupo = state.groups.size == 1 && state.groups.first().obraName.isBlank()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            state.groups.forEach { grupo ->
                                // Cabecera de obra (solo si hay más de un grupo o tiene nombre)
                                if (!soloUnGrupo) {
                                    item(key = "header_${grupo.obraId}") {
                                        ObraCabeceraCard(grupo)
                                    }
                                }
                                // Pedidos de la obra
                                items(grupo.orders, key = { it.id }) { pedido ->
                                    PedidoCard(
                                        pedido = pedido,
                                        mostrarObra = false,
                                        onClick = { onPedidoClick(pedido.id.toString()) }
                                    )
                                }
                                // Espacio entre grupos
                                if (!soloUnGrupo) {
                                    item(key = "spacer_${grupo.obraId}") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Cabecera de obra ─────────────────────────────────────────────────────────

@Composable
private fun ObraCabeceraCard(grupo: ObraOrderGroup) {
    val sinObra = grupo.obraName.isBlank()
    Surface(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        shape     = RoundedCornerShape(10.dp),
        color     = if (sinObra) Color(0xFFF5F5F5) else OrangeLight,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (sinObra) Icons.Default.Receipt else Icons.Default.Construction,
                contentDescription = null,
                tint   = if (sinObra) Color.Gray else PrimaryOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = if (sinObra) "Sin obra asignada" else grupo.obraName,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = if (sinObra) Color.Gray else PrimaryOrange
                )
                Text(
                    text     = "${grupo.orders.size} pedido${if (grupo.orders.size != 1) "s" else ""}",
                    fontSize = 11.sp,
                    color    = Color.Gray
                )
            }
            Text(
                text       = "${"%.2f".format(grupo.totalAmount)} €",
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = if (sinObra) Color.DarkGray else PrimaryOrange
            )
        }
    }
}

// ─── Tarjeta de pedido ────────────────────────────────────────────────────────

@Composable
private fun PedidoCard(
    pedido: MaterialRequest,
    mostrarObra: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Cabecera: número de pedido + badge estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, null, tint = PrimaryOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(pedido.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                EstadoBadge(pedido.state)
            }

            Spacer(Modifier.height(6.dp))

            // Fecha + importe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = (pedido.createDate ?: "").take(10),
                    fontSize = 12.sp,
                    color    = Color.Gray
                )
                Text(
                    text       = "${"%.2f".format(pedido.totalAmount)} €",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 15.sp,
                    color      = PrimaryOrange
                )
            }

            // Obra (solo si se muestra fuera de grupo)
            if (mostrarObra && pedido.obraName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Construction, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(pedido.obraName, fontSize = 12.sp, color = Color.Gray)
                }
            }

            // Líneas de producto
            if (pedido.lines.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Inventory2, null, tint = Color.LightGray, modifier = Modifier.size(13.dp))
                    pedido.lines.take(3).forEach { line ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF5F5F5))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${line.productName.take(12)} x${line.qty.toInt()}",
                                fontSize = 11.sp,
                                color    = Color.DarkGray
                            )
                        }
                    }
                    if (pedido.lines.size > 3) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEEEEEE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("+${pedido.lines.size - 3}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Flecha derecha
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(Icons.Default.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(13.dp))
            }
        }
    }
}

// ─── Badge de estado ──────────────────────────────────────────────────────────

@Composable
private fun EstadoBadge(state: MaterialRequestState) {
    val color = state.toColor()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text       = state.toLabel(),
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = color
        )
    }
}

// ─── Estados de pantalla ──────────────────────────────────────────────────────

@Composable
private fun EstadoVacio(filtroLabel: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Receipt, null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
            Text(
                "No hay pedidos${if (filtroLabel != "Todos") " con estado \"$filtroLabel\"" else ""}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Text("Tus pedidos aparecerán aquí", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
        }
    }
}

@Composable
private fun EstadoError(mensaje: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(64.dp), tint = Color(0xFFC62828))
            Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)) {
                Text("Reintentar")
            }
        }
    }
}
