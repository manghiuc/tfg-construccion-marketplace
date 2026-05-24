package com.construccion.marketplace.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.construccion.marketplace.data.model.CalculatorResultOdoo
import com.construccion.marketplace.data.model.ObraMaterial
import kotlinx.coroutines.launch

// ─── Constantes visuales ────────────────────────────────────────────────────

private val PrimaryOrange = Color(0xFFE65100)
private val OrangeSurface = Color(0xFFFFF8F5)
private val OrangeBorder = Color(0xFFFFCCBC)
private val HeaderBg = Color(0xFFFFE0B2)

// ─── Tipos de obra ───────────────────────────────────────────────────────────

data class TipoObra(
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val icono: ImageVector
)

private val tiposObra = listOf(
    TipoObra(
        codigo = "banyo",
        nombre = "Baño",
        descripcion = "Reforma completa de baño: alicatado de paredes y suelo con azulejo, adhesivo y lechada.",
        icono = Icons.Default.Bathtub
    ),
    TipoObra(
        codigo = "solera",
        nombre = "Solera",
        descripcion = "Solera de hormigón en masa: cemento Portland, arena de río y agua en proporciones estándar.",
        icono = Icons.Default.Layers
    ),
    TipoObra(
        codigo = "tabique_ladrillo",
        nombre = "Tabique",
        descripcion = "Tabique de ladrillo hueco: ladrillo cerámico, cemento cola y arena para juntas.",
        icono = Icons.Default.ViewColumn
    ),
    TipoObra(
        codigo = "pintura",
        nombre = "Pintura",
        descripcion = "Pintura de paredes interiores: dos manos de pintura plástica más imprimación previa.",
        icono = Icons.Default.FormatPaint
    )
)

// ─── Pantalla principal ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculadoraScreen(
    onNavigateBack: () -> Unit = {},
    onAddToCart: (id: Int, name: String, price: Double, unit: String, qty: Double) -> Unit = { _, _, _, _, _ -> },
    viewModel: CalculadoraViewModel = hiltViewModel()
) {
    var tipoSeleccionado by remember { mutableStateOf(tiposObra[0]) }
    var m2Input by remember { mutableStateOf("") }
    val calcState by viewModel.uiState.collectAsStateWithLifecycle()
    val cartAddState by viewModel.cartAddState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isLoading = calcState is CalculadoraUiState.Loading
    val errorMessage: String? = (calcState as? CalculadoraUiState.Error)?.message
    val resultado: CalculatorResultOdoo? = (calcState as? CalculadoraUiState.Success)?.resultado

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Calculate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Calculadora", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Estima materiales por m²", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryOrange,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Selector tipo de obra ──────────────────────────────────────
            Text(
                text = "Tipo de obra",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF212121)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tiposObra.forEach { tipo ->
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = tipoSeleccionado.codigo == tipo.codigo,
                        onClick = {
                            tipoSeleccionado = tipo
                            viewModel.reset()
                        },
                        label = {
                            Text(
                                text = tipo.nombre,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = tipo.icono,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            // ── Descripción del tipo (solo sin resultado) ──────────────────
            if (resultado == null) {
                TipoObraDescripcionCard(tipo = tipoSeleccionado)
            }

            // ── Campo m² ───────────────────────────────────────────────────
            Text(
                text = "Superficie a tratar",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF212121)
            )

            OutlinedTextField(
                value = m2Input,
                onValueChange = { value ->
                    // Permitir solo dígitos y un punto decimal
                    if (value.isEmpty() || value.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?\$"))) {
                        m2Input = value
                        viewModel.reset()
                    }
                },
                label = { Text("Metros cuadrados (m²)") },
                placeholder = { Text("Ej: 15.0") },
                suffix = { Text("m²") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    focusedLabelColor = PrimaryOrange,
                    cursorColor = PrimaryOrange
                )
            )

            // ── Botón Calcular ─────────────────────────────────────────────
            Button(
                onClick = {
                    val m2Value = m2Input.toDoubleOrNull()
                    if (m2Value != null && m2Value > 0) {
                        viewModel.calcular(tipoSeleccionado.codigo, m2Value)
                    }
                },
                enabled = m2Input.toDoubleOrNull()?.let { it > 0 } == true && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange,
                    disabledContainerColor = Color(0xFFBCAAA4)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Calculando...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        Icons.Default.Calculate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calcular materiales", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            // ── Error ─────────────────────────────────────────────────────
            errorMessage?.let { msg ->
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = Color(0xFFB71C1C),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Error: $msg",
                            fontSize = 13.sp,
                            color = Color(0xFFB71C1C)
                        )
                    }
                }
            }

            // ── Resultado ─────────────────────────────────────────────────
            resultado?.let { res ->
                ResultadoCard(resultado = res)

                // ── Sección añadir al carrito ──────────────────────────────
                CartAddSection(
                    cartAddState = cartAddState,
                    onBuscar = { viewModel.buscarProductosParaCarrito(res.materials) },
                    onQtyChange = { idx, qty -> viewModel.updateCartQty(idx, qty) },
                    onAddOne = { item ->
                        item.product?.let { prod ->
                            onAddToCart(prod.id, prod.name, prod.price, prod.uom, item.qty)
                            scope.launch {
                                snackbarHostState.showSnackbar("✓ ${prod.name} añadido al carrito")
                            }
                        }
                    },
                    onAddAll = { items ->
                        var added = 0
                        items.forEach { item ->
                            item.product?.let { prod ->
                                onAddToCart(prod.id, prod.name, prod.price, prod.uom, item.qty)
                                added++
                            }
                        }
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (added > 0) "✓ $added producto${if (added != 1) "s" else ""} añadido${if (added != 1) "s" else ""} al carrito"
                                else "No se encontraron productos en el catálogo"
                            )
                        }
                    }
                )
            }

            // Espacio inferior para scroll cómodo
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Tarjeta descripción tipo de obra ────────────────────────────────────────

@Composable
private fun TipoObraDescripcionCard(tipo: TipoObra) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = OrangeSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HeaderBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tipo.icono,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column {
                Text(
                    text = tipo.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF212121)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tipo.descripcion,
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    lineHeight = 19.sp
                )
            }
        }
    }
}

// ─── Tarjeta de resultado ────────────────────────────────────────────────────

@Composable
private fun ResultadoCard(resultado: CalculatorResultOdoo) {
    val tipoNombre = tiposObra.find { it.codigo == resultado.obraType }?.nombre ?: resultado.obraType

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera del resultado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HeaderBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Calculate,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Resultado para $tipoNombre",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${resultado.m2} m² calculados",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Encabezado de la tabla
            TablaEncabezado()

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // Filas de materiales
            resultado.materials.forEachIndexed { index, material ->
                TablaFila(material = material, esPar = index % 2 == 0)
                if (index < resultado.materials.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "* Estimación orientativa. Los precios pueden variar según proveedor y zona.",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                lineHeight = 16.sp
            )
        }
    }
}

// ─── Encabezado de la tabla ───────────────────────────────────────────────────

@Composable
private fun TablaEncabezado() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Material",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color(0xFF616161),
            modifier = Modifier.weight(2.5f)
        )
        Text(
            text = "Cant.",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color(0xFF616161),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Ud.",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color(0xFF616161),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.8f)
        )
    }
}

// ─── Fila de material ─────────────────────────────────────────────────────────

@Composable
private fun TablaFila(material: ObraMaterial, esPar: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (esPar) Color.White else Color(0xFFFAFAFA))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = material.material,
            fontSize = 13.sp,
            color = Color(0xFF212121),
            modifier = Modifier.weight(2.5f),
            lineHeight = 17.sp
        )
        Text(
            text = formatQuantity(material.quantity),
            fontSize = 13.sp,
            color = Color(0xFF424242),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = material.unit,
            fontSize = 12.sp,
            color = Color(0xFF757575),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.8f)
        )
    }
}

// ─── Sección añadir al carrito ────────────────────────────────────────────────

@Composable
private fun CartAddSection(
    cartAddState: CartAddState,
    onBuscar: () -> Unit,
    onQtyChange: (Int, Double) -> Unit,
    onAddOne: (CartMaterial) -> Unit,
    onAddAll: (List<CartMaterial>) -> Unit
) {
    when (cartAddState) {
        is CartAddState.Idle -> {
            Button(
                onClick = onBuscar,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Añadir al carrito", fontWeight = FontWeight.SemiBold)
            }
        }

        is CartAddState.Searching -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryOrange, strokeWidth = 2.dp)
                    Text("Buscando precios en catálogo...", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }

        is CartAddState.Ready -> {
            val items = cartAddState.items
            val hayAlgunProducto = items.any { it.product != null }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir al carrito", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    items.forEachIndexed { idx, item ->
                        if (idx > 0) HorizontalDivider(color = Color(0xFFF0F0F0))
                        CartMaterialRow(
                            item = item,
                            onQtyChange = { newQty -> onQtyChange(idx, newQty) },
                            onAdd = { onAddOne(item) }
                        )
                    }

                    if (hayAlgunProducto) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onAddAll(items) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir todo al carrito", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No se encontraron productos en el catálogo para estos materiales.",
                            fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartMaterialRow(
    item: CartMaterial,
    onQtyChange: (Double) -> Unit,
    onAdd: () -> Unit
) {
    val product = item.product
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product?.name ?: item.material.material,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (product != null) Color(0xFF212121) else Color.Gray,
                lineHeight = 17.sp
            )
            if (product != null) {
                Text(
                    text = "${String.format("%.2f", product.price)} € / ${product.uom}",
                    fontSize = 12.sp, color = Color(0xFF757575)
                )
            } else {
                Text("No encontrado en catálogo", fontSize = 11.sp, color = Color(0xFFBDBDBD))
            }
        }

        if (product != null) {
            // Stepper de cantidad
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onQtyChange((item.qty - 1.0).coerceAtLeast(1.0)) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(14.dp), tint = Color.Gray)
                }
                Text(
                    text = formatQuantity(item.qty),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = { onQtyChange(item.qty + 1.0) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(14.dp), tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .size(36.dp)
                        .background(PrimaryOrange, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Añadir", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ─── Helpers de formato ───────────────────────────────────────────────────────

private fun formatQuantity(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format("%.2f", value)
