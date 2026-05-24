package com.construccion.marketplace.ui.screens.obras

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val PrimaryOrange = Color(0xFFE65100)

enum class EstadoObra(val label: String, val color: Color, val textColor: Color, val icono: ImageVector) {
    ACTIVA("Activa", Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.PlayCircle),
    EN_PAUSA("En pausa", Color(0xFFFFF8E1), Color(0xFFF57F17), Icons.Default.PauseCircle),
    FINALIZADA("Finalizada", Color(0xFFEEEEEE), Color(0xFF616161), Icons.Default.TaskAlt)
}

data class Obra(
    val id: String,
    val nombre: String,
    val direccion: String,
    val estado: EstadoObra,
    val numeroPedidos: Int,
    val descripcion: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProjectsScreen(
    onNavigateBack: () -> Unit = {},
    onObraDetalle: (String) -> Unit = {},
    onPedirParaObra: (String) -> Unit = {}
) {
    val obras = remember {
        mutableStateListOf(
            Obra("1", "Reforma Vivienda Principal", "C/ Mayor 15, Madrid", EstadoObra.ACTIVA, 5, "Reforma integral de vivienda 90m²"),
            Obra("2", "Nave Industrial Getafe", "Polígono Sur, Nave 7, Getafe", EstadoObra.ACTIVA, 12, "Construcción nave logística 500m²"),
            Obra("3", "Bloque Apartamentos Vallecas", "Av. Constitución 88, Madrid", EstadoObra.EN_PAUSA, 3, "Bloque 12 viviendas"),
            Obra("4", "Piscina Chalet Sierra", "Urb. La Presa s/n, Cercedilla", EstadoObra.FINALIZADA, 8, "Piscina + terraza exterior")
        )
    }

    var filtroActivo by remember { mutableStateOf<EstadoObra?>(null) }
    var mostrarBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val obrasFiltradas = if (filtroActivo == null) {
        obras.toList()
    } else {
        obras.filter { it.estado == filtroActivo }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis obras", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryOrange,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarBottomSheet = true },
                containerColor = PrimaryOrange,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva obra", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Contador y filtros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${obras.size} obras", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filtroActivo == null,
                        onClick = { filtroActivo = null },
                        label = { Text("Todas", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(EstadoObra.entries) { estado ->
                    FilterChip(
                        selected = filtroActivo == estado,
                        onClick = { filtroActivo = if (filtroActivo == estado) null else estado },
                        label = { Text(estado.label, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(estado.icono, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (obrasFiltradas.isEmpty()) {
                EstadoVacioObras()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(obrasFiltradas, key = { it.id }) { obra ->
                        ObraCard(
                            obra = obra,
                            onClick = { onObraDetalle(obra.id) },
                            onPedir = { onPedirParaObra(obra.id) }
                        )
                    }
                }
            }
        }
    }

    if (mostrarBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { mostrarBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            NuevaObraBottomSheet(
                onGuardar = { nombre, direccion, descripcion ->
                    obras.add(
                        Obra(
                            id = System.currentTimeMillis().toString(),
                            nombre = nombre,
                            direccion = direccion,
                            estado = EstadoObra.ACTIVA,
                            numeroPedidos = 0,
                            descripcion = descripcion
                        )
                    )
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        mostrarBottomSheet = false
                    }
                },
                onCancelar = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        mostrarBottomSheet = false
                    }
                }
            )
        }
    }
}

@Composable
private fun ObraCard(obra: Obra, onClick: () -> Unit, onPedir: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Construction,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(obra.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (obra.descripcion.isNotBlank()) {
                            Text(obra.descripcion, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                        }
                    }
                }
                EstadoObraChip(estado = obra.estado)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(obra.direccion, fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${obra.numeroPedidos} pedidos", fontSize = 12.sp, color = Color.Gray)
                }

                if (obra.estado == EstadoObra.ACTIVA) {
                    Button(
                        onClick = onPedir,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Pedir aquí", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Ver detalle", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EstadoObraChip(estado: EstadoObra) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(estado.color)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(estado.icono, contentDescription = null, tint = estado.textColor, modifier = Modifier.size(12.dp))
            Text(estado.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = estado.textColor)
        }
    }
}

@Composable
private fun NuevaObraBottomSheet(
    onGuardar: (nombre: String, direccion: String, descripcion: String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombreObra by remember { mutableStateOf("") }
    var calleObra by remember { mutableStateOf("") }
    var ciudadObra by remember { mutableStateOf("") }
    var descripcionObra by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Construction, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Nueva obra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider()

        OutlinedTextField(
            value = nombreObra,
            onValueChange = { nombreObra = it },
            label = { Text("Nombre de la obra *") },
            placeholder = { Text("Ej: Reforma piso Madrid") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = PrimaryOrange) }
        )

        OutlinedTextField(
            value = calleObra,
            onValueChange = { calleObra = it },
            label = { Text("Dirección (calle y número) *") },
            placeholder = { Text("Ej: C/ Gran Vía 42") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryOrange) }
        )

        OutlinedTextField(
            value = ciudadObra,
            onValueChange = { ciudadObra = it },
            label = { Text("Ciudad / Municipio *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = descripcionObra,
            onValueChange = { descripcionObra = it },
            label = { Text("Descripción (opcional)") },
            placeholder = { Text("Tipo de obra, detalles relevantes...") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    if (nombreObra.isNotBlank() && calleObra.isNotBlank() && ciudadObra.isNotBlank()) {
                        onGuardar(
                            nombreObra,
                            "$calleObra, $ciudadObra",
                            descripcionObra
                        )
                    }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(10.dp),
                enabled = nombreObra.isNotBlank() && calleObra.isNotBlank() && ciudadObra.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Crear obra", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EstadoVacioObras() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Construction,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color.LightGray
            )
            Text(
                "No hay obras registradas",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Text(
                "Pulsa el botón + para añadir tu primera obra",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
        }
    }
}
