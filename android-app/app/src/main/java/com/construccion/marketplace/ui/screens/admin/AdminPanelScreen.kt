/**
 * Panel de administración (solo para cuentas de tipo EMPRESA).
 *
 * Muestra estadísticas del negocio: pedidos del mes, facturación,
 * obras activas, y accesos rápidos a la gestión de obras y pedidos.
 * Protegido en MainActivity: si el userType no es EMPRESA, se redirige.
 */
package com.construccion.marketplace.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.construccion.marketplace.ui.theme.OrangeConstruction
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class EstadoAdmin(val label: String) {
    PENDIENTE("Pendiente"),
    EN_PREPARACION("En preparación"),
    EN_REPARTO("En reparto"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado")
}

enum class TipoCliente(val label: String) {
    PARTICULAR("Particular"),
    AUTONOMO("Autónomo"),
    EMPRESA("Empresa")
}

data class PedidoAdmin(
    val id: String,
    val numero: String,
    val cliente: String,
    val fecha: String,
    val importe: Double,
    var estado: EstadoAdmin
)

data class ClienteAdmin(
    val id: String,
    val nombre: String,
    val email: String,
    val tipo: TipoCliente,
    val totalPedidos: Int
)

data class ProductoAdmin(
    val id: String,
    val nombre: String,
    val categoria: String,
    val precio: Double,
    val stock: Int,
    val stockMinimo: Int
)

data class MetricaMes(val titulo: String, val valor: String, val icono: ImageVector, val variacion: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onNavigateBack: () -> Unit = {}) {
    var tabSeleccionado by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pedidos", "Clientes", "Productos", "Informes")
    val tabIcons = listOf(Icons.Default.Receipt, Icons.Default.Group, Icons.Default.Inventory, Icons.Default.BarChart)

    val pedidos = remember {
        mutableStateListOf(
            PedidoAdmin("1", "#2025-001", "Carlos García", "12/05/2025", 245.80, EstadoAdmin.PENDIENTE),
            PedidoAdmin("2", "#2025-002", "Ana Martínez", "15/05/2025", 128.40, EstadoAdmin.EN_PREPARACION),
            PedidoAdmin("3", "#2025-003", "Construcciones López S.L.", "17/05/2025", 89.20, EstadoAdmin.EN_REPARTO),
            PedidoAdmin("4", "#2025-004", "Pedro Sánchez", "18/05/2025", 312.00, EstadoAdmin.PENDIENTE),
            PedidoAdmin("5", "#2025-005", "Reformas Norte S.L.", "18/05/2025", 567.00, EstadoAdmin.ENTREGADO)
        )
    }

    val clientes = remember {
        listOf(
            ClienteAdmin("1", "Carlos García", "carlos@email.com", TipoCliente.PARTICULAR, 5),
            ClienteAdmin("2", "Ana Martínez", "ana@empresa.com", TipoCliente.EMPRESA, 23),
            ClienteAdmin("3", "Pedro Sánchez", "pedro@auto.es", TipoCliente.AUTONOMO, 12),
            ClienteAdmin("4", "Construcciones López S.L.", "info@lopez.com", TipoCliente.EMPRESA, 47),
            ClienteAdmin("5", "María Torres", "maria@gmail.com", TipoCliente.PARTICULAR, 2)
        )
    }

    val productos = remember {
        listOf(
            ProductoAdmin("1", "Cemento CEM II 25kg", "Cemento", 6.50, 145, 50),
            ProductoAdmin("2", "Arena lavada m³", "Áridos", 28.00, 8, 10),
            ProductoAdmin("3", "Grava 6/12 m³", "Áridos", 32.00, 3, 5),
            ProductoAdmin("4", "Ladrillo cara vista", "Ladrillos", 0.45, 2500, 500),
            ProductoAdmin("5", "Yeso fino 20kg", "Yeso", 8.90, 12, 20),
            ProductoAdmin("6", "Pintura blanca 15L", "Pinturas", 45.00, 60, 15)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Administración", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangeConstruction,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            PrimaryTabRow(selectedTabIndex = tabSeleccionado) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tabSeleccionado == index,
                        onClick = { tabSeleccionado = index },
                        text = { Text(title, fontSize = 12.sp) },
                        icon = {
                            Icon(tabIcons[index], contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        selectedContentColor = OrangeConstruction,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            when (tabSeleccionado) {
                0 -> TabPedidosAdmin(pedidos = pedidos, onEstadoCambiado = { pedido, nuevoEstado ->
                    val index = pedidos.indexOf(pedido)
                    if (index >= 0) pedidos[index] = pedido.copy(estado = nuevoEstado)
                })
                1 -> TabClientesAdmin(clientes = clientes)
                2 -> TabProductosAdmin(productos = productos)
                3 -> TabInformesAdmin()
            }
        }
    }
}

@Composable
private fun TabPedidosAdmin(
    pedidos: List<PedidoAdmin>,
    onEstadoCambiado: (PedidoAdmin, EstadoAdmin) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "${pedidos.size} pedidos",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(pedidos, key = { it.id }) { pedido ->
            PedidoAdminCard(pedido = pedido, onEstadoCambiado = { nuevoEstado ->
                onEstadoCambiado(pedido, nuevoEstado)
            })
        }
    }
}

@Composable
private fun PedidoAdminCard(pedido: PedidoAdmin, onEstadoCambiado: (EstadoAdmin) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(pedido.numero, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(pedido.cliente, fontSize = 13.sp, color = Color.Gray)
                    Text(pedido.fecha, fontSize = 11.sp, color = Color.LightGray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${String.format("%.2f", pedido.importe)} €",
                        fontWeight = FontWeight.ExtraBold,
                        color = OrangeConstruction,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFFE0B2))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                pedido.estado.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrangeConstruction
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(
                                onClick = { expanded = true },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Cambiar estado",
                                    modifier = Modifier.size(12.dp),
                                    tint = OrangeConstruction
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            EstadoAdmin.entries.forEach { estado ->
                                DropdownMenuItem(
                                    text = { Text(estado.label) },
                                    onClick = {
                                        onEstadoCambiado(estado)
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        if (estado == pedido.estado) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(OrangeConstruction)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabClientesAdmin(clientes: List<ClienteAdmin>) {
    var busqueda by remember { mutableStateOf("") }
    var filtroTipo by remember { mutableStateOf<TipoCliente?>(null) }

    val clientesFiltrados = clientes.filter { cliente ->
        (busqueda.isBlank() || cliente.nombre.contains(busqueda, ignoreCase = true) ||
                cliente.email.contains(busqueda, ignoreCase = true)) &&
                (filtroTipo == null || cliente.tipo == filtroTipo)
    }

    Column {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text("Buscar clientes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filtroTipo == null,
                    onClick = { filtroTipo = null },
                    label = { Text("Todos", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangeConstruction,
                        selectedLabelColor = Color.White
                    )
                )
                TipoCliente.entries.forEach { tipo ->
                    FilterChip(
                        selected = filtroTipo == tipo,
                        onClick = { filtroTipo = if (filtroTipo == tipo) null else tipo },
                        label = { Text(tipo.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeConstruction,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(clientesFiltrados, key = { it.id }) { cliente ->
                ClienteCard(cliente = cliente)
            }
        }
    }
}

@Composable
private fun ClienteCard(cliente: ClienteAdmin) {
    val colorTipo = when (cliente.tipo) {
        TipoCliente.PARTICULAR -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        TipoCliente.AUTONOMO -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        TipoCliente.EMPRESA -> Color(0xFFFFF3E0) to OrangeConstruction
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(OrangeConstruction),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    cliente.nombre.first().uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(cliente.nombre, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(cliente.email, fontSize = 12.sp, color = Color.Gray)
                Text("${cliente.totalPedidos} pedidos", fontSize = 11.sp, color = Color.LightGray)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorTipo.first)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(cliente.tipo.label, fontSize = 11.sp, color = colorTipo.second, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TabProductosAdmin(productos: List<ProductoAdmin>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            val stockBajoCount = productos.count { it.stock <= it.stockMinimo }
            if (stockBajoCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "$stockBajoCount producto(s) con stock bajo",
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        items(productos, key = { it.id }) { producto ->
            ProductoAdminCard(producto = producto)
        }
    }
}

@Composable
private fun ProductoAdminCard(producto: ProductoAdmin) {
    val stockBajo = producto.stock <= producto.stockMinimo

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        border = if (stockBajo) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
        } else null
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(producto.nombre, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (stockBajo) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFC62828))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Stock bajo", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(producto.categoria, fontSize = 12.sp, color = Color.Gray)
                Text("Stock: ${producto.stock} ud (mín: ${producto.stockMinimo})", fontSize = 11.sp,
                    color = if (stockBajo) Color(0xFFC62828) else Color.LightGray)
            }
            Text(
                "${String.format("%.2f", producto.precio)} €",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = OrangeConstruction
            )
        }
    }
}

@Composable
private fun TabInformesAdmin() {
    val metricas = listOf(
        MetricaMes("Ventas del mes", "8.432 €", Icons.Default.TrendingUp, "+12%"),
        MetricaMes("Pedidos totales", "47", Icons.Default.Receipt, "+8%"),
        MetricaMes("Clientes activos", "23", Icons.Default.Group, "+5%"),
        MetricaMes("Producto más vendido", "Cemento CEM II", Icons.Default.Inventory, "10 ud/día")
    )

    val topProductos = listOf(
        "Cemento CEM II 25kg" to 124,
        "Ladrillo cara vista" to 98,
        "Arena lavada m³" to 67,
        "Grava 6/12" to 45,
        "Yeso fino 20kg" to 38
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Resumen de mayo 2025", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                metricas.take(2).forEach { metrica ->
                    MetricaCard(metrica = metrica, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                metricas.drop(2).forEach { metrica ->
                    MetricaCard(metrica = metrica, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Productos más pedidos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    HorizontalDivider()
                    topProductos.forEachIndexed { index, (nombre, cantidad) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (index == 0) OrangeConstruction else Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index == 0) Color.White else Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(nombre, fontSize = 13.sp)
                            }
                            Text("$cantidad ud", fontWeight = FontWeight.SemiBold, color = OrangeConstruction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricaCard(metrica: MetricaMes, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(metrica.icono, contentDescription = null, tint = OrangeConstruction, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(metrica.valor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF212121))
            Text(metrica.titulo, fontSize = 11.sp, color = Color.Gray)
            Text(metrica.variacion, fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
        }
    }
}
