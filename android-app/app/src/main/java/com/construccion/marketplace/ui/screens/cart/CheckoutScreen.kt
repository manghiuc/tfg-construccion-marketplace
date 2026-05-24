package com.construccion.marketplace.ui.screens.cart

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.construccion.marketplace.data.model.CartItem
import com.construccion.marketplace.data.model.Obra
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PrimaryOrange = Color(0xFFE65100)
private val LightOrange = Color(0xFFFFF3E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    isUrgent: Boolean = false,
    checkoutViewModel: CheckoutViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onOrderConfirmed: (orderId: String) -> Unit = {}
) {
    val checkoutState by checkoutViewModel.uiState.collectAsStateWithLifecycle()
    var currentStep by remember { mutableIntStateOf(0) }

    // Paso 1 - Dirección
    var calle by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var ciudad by remember { mutableStateOf("") }
    var codigoPostal by remember { mutableStateOf("") }
    var obraSeleccionada by remember { mutableStateOf<Obra?>(null) }

    // Paso 2 - Fecha y notas
    var fechaEntregaMs by remember { mutableStateOf<Long?>(null) }
    var mostrarDatePicker by remember { mutableStateOf(false) }
    var notasEntrega by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState()

    val costoMateriales = cartItems.sumOf { it.priceUnit * it.qty }
    val costoTransporteBase = if (costoMateriales > 1000) 0.0 else 15.0
    val costoTransporte = if (isUrgent && costoTransporteBase > 0.0) costoTransporteBase * 1.5 else costoTransporteBase
    val total = costoMateriales + costoTransporte

    // Auto-seleccionar obra recién creada
    LaunchedEffect(checkoutState.newlyCreatedObraId) {
        checkoutState.newlyCreatedObraId?.let { newId ->
            val obra = checkoutState.obras.find { it.id == newId }
            if (obra != null) {
                obraSeleccionada = obra
                checkoutViewModel.consumeNewlyCreatedObraId()
            }
        }
    }

    // Navegar al detalle cuando el pedido se crea exitosamente
    LaunchedEffect(checkoutState.submittedOrderId) {
        checkoutState.submittedOrderId?.let { orderId ->
            onOrderConfirmed(orderId.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
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
        ) {
            CheckoutStepper(currentStep = currentStep)

            // Aviso de error al enviar
            checkoutState.submitError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            error,
                            color = Color(0xFFD32F2F),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { checkoutViewModel.clearSubmitError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "checkout_step"
            ) { step ->
                when (step) {
                    0 -> StepDireccion(
                        calle = calle,
                        onCalleChange = { calle = it },
                        numero = numero,
                        onNumeroChange = { numero = it },
                        ciudad = ciudad,
                        onCiudadChange = { ciudad = it },
                        codigoPostal = codigoPostal,
                        onCodigoPostalChange = { codigoPostal = it },
                        obras = checkoutState.obras,
                        isLoadingObras = checkoutState.isLoadingObras,
                        onRetryObras = { checkoutViewModel.loadObras() },
                        obraSeleccionada = obraSeleccionada,
                        onObraSeleccionada = { obraSeleccionada = it },
                        isCreatingObra = checkoutState.isCreatingObra,
                        obraCreateError = checkoutState.obraCreateError,
                        onCreateObra = { name, address ->
                            checkoutViewModel.createObra(name, address)
                        },
                        onClearObraCreateError = { checkoutViewModel.clearObraCreateError() },
                        onNext = { currentStep = 1 }
                    )

                    1 -> StepFechaEntrega(
                        fechaEntregaMs = fechaEntregaMs,
                        onMostrarDatePicker = { mostrarDatePicker = true },
                        notasEntrega = notasEntrega,
                        onNotasChange = { notasEntrega = it },
                        onNext = { currentStep = 2 }
                    )

                    2 -> StepResumen(
                        cartItems = cartItems,
                        calle = calle,
                        numero = numero,
                        ciudad = ciudad,
                        codigoPostal = codigoPostal,
                        obraSeleccionada = obraSeleccionada,
                        fechaEntregaMs = fechaEntregaMs,
                        notasEntrega = notasEntrega,
                        costoMateriales = costoMateriales,
                        costoTransporte = costoTransporte,
                        total = total,
                        isUrgent = isUrgent,
                        isSubmitting = checkoutState.isSubmitting,
                        onConfirmar = {
                            checkoutViewModel.createOrder(
                                cartItems = cartItems,
                                obraId = obraSeleccionada?.id ?: 0,
                                deliveryAddress = "$calle $numero, $ciudad $codigoPostal".trim(),
                                notes = notasEntrega,
                                isUrgent = isUrgent
                            )
                        }
                    )
                }
            }
        }
    }

    if (mostrarDatePicker) {
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    fechaEntregaMs = datePickerState.selectedDateMillis
                    mostrarDatePicker = false
                }) {
                    Text("Confirmar", color = PrimaryOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun CheckoutStepper(currentStep: Int) {
    val pasos = listOf("Dirección", "Entrega", "Resumen")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pasos.forEachIndexed { index, label ->
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep

            val circleColor by animateColorAsState(
                targetValue = when {
                    isCompleted -> PrimaryOrange
                    isCurrent -> PrimaryOrange
                    else -> Color.LightGray
                },
                animationSpec = tween(300),
                label = "stepColor"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(circleColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = if (isCurrent || isCompleted) PrimaryOrange else Color.Gray,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (index < pasos.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(2.dp)
                        .background(if (index < currentStep) PrimaryOrange else Color.LightGray)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun StepDireccion(
    calle: String,
    onCalleChange: (String) -> Unit,
    numero: String,
    onNumeroChange: (String) -> Unit,
    ciudad: String,
    onCiudadChange: (String) -> Unit,
    codigoPostal: String,
    onCodigoPostalChange: (String) -> Unit,
    obras: List<Obra>,
    isLoadingObras: Boolean,
    onRetryObras: () -> Unit,
    obraSeleccionada: Obra?,
    onObraSeleccionada: (Obra) -> Unit,
    isCreatingObra: Boolean = false,
    obraCreateError: String? = null,
    onCreateObra: (name: String, address: String?) -> Unit = { _, _ -> },
    onClearObraCreateError: () -> Unit = {},
    onNext: () -> Unit
) {
    var showCreateObraDialog by remember { mutableStateOf(false) }
    var newObraName by remember { mutableStateOf("") }
    var newObraAddress by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryOrange)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Dirección de entrega",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = calle,
                onValueChange = onCalleChange,
                label = { Text("Calle / Avenida") },
                modifier = Modifier.weight(2f),
                singleLine = true
            )
            OutlinedTextField(
                value = numero,
                onValueChange = onNumeroChange,
                label = { Text("Nº") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ciudad,
                onValueChange = onCiudadChange,
                label = { Text("Ciudad") },
                modifier = Modifier.weight(2f),
                singleLine = true
            )
            OutlinedTextField(
                value = codigoPostal,
                onValueChange = onCodigoPostalChange,
                label = { Text("CP") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = PrimaryOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Asociar a una obra",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoadingObras || isCreatingObra) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PrimaryOrange,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (obras.isEmpty()) {
                    IconButton(onClick = onRetryObras, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reintentar",
                            tint = PrimaryOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                TextButton(
                    onClick = {
                        newObraName = ""
                        newObraAddress = ""
                        showCreateObraDialog = true
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = PrimaryOrange
                    )
                ) {
                    Text("+ Nueva obra", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        obraCreateError?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(err, color = Color(0xFFD32F2F), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClearObraCreateError, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (!isLoadingObras && !isCreatingObra && obras.isEmpty()) {
            Text(
                "No tienes obras registradas. Crea una con el botón '+ Nueva obra'.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        } else if (!isLoadingObras) {
            Text(
                "Selecciona la obra a la que se enviará el pedido",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            obras.forEach { obra ->
                ObraSelector(
                    obra = obra,
                    isSelected = obraSeleccionada?.id == obra.id,
                    onClick = { onObraSeleccionada(obra) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            enabled = calle.isNotBlank() && ciudad.isNotBlank() && codigoPostal.isNotBlank()
        ) {
            Text("Continuar →", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }

    if (showCreateObraDialog) {
        AlertDialog(
            onDismissRequest = { showCreateObraDialog = false },
            title = { Text("Nueva obra", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newObraName,
                        onValueChange = { newObraName = it },
                        label = { Text("Nombre de la obra *") },
                        placeholder = { Text("Ej: Reforma local C/ Mayor 12") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newObraAddress,
                        onValueChange = { newObraAddress = it },
                        label = { Text("Dirección (opcional)") },
                        placeholder = { Text("Ej: C/ Mayor 12, Madrid") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newObraName.isNotBlank()) {
                            onCreateObra(newObraName, newObraAddress.takeIf { it.isNotBlank() })
                            showCreateObraDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    enabled = newObraName.isNotBlank()
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateObraDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ObraSelector(obra: Obra, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PrimaryOrange else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) LightOrange else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                tint = if (isSelected) PrimaryOrange else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(obra.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (obra.address.isNotBlank()) {
                    Text(obra.address, fontSize = 12.sp, color = Color.Gray)
                }
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryOrange)
            }
        }
    }
}

@Composable
private fun StepFechaEntrega(
    fechaEntregaMs: Long?,
    onMostrarDatePicker: () -> Unit,
    notasEntrega: String,
    onNotasChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val fechaFormateada = fechaEntregaMs?.let {
        SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(Date(it))
    } ?: "Seleccionar fecha"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = PrimaryOrange)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Fecha de entrega",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMostrarDatePicker() },
            colors = CardDefaults.cardColors(containerColor = LightOrange),
            border = androidx.compose.foundation.BorderStroke(
                width = if (fechaEntregaMs != null) 2.dp else 1.dp,
                color = if (fechaEntregaMs != null) PrimaryOrange else Color.LightGray
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Fecha preferida", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        fechaFormateada,
                        fontWeight = if (fechaEntregaMs != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (fechaEntregaMs != null) Color(0xFF212121) else Color.Gray
                    )
                }
            }
        }

        Text(
            "El horario exacto de entrega se coordinará con el transportista.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        HorizontalDivider()

        Text(
            "Preferencias de entrega / notas para el repartidor",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = notasEntrega,
            onValueChange = onNotasChange,
            placeholder = {
                Text(
                    "Ej: Dejar en portería, llamar antes de llegar, acceso por calle lateral...",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            enabled = fechaEntregaMs != null
        ) {
            Text("Continuar →", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun StepResumen(
    cartItems: List<CartItem>,
    calle: String,
    numero: String,
    ciudad: String,
    codigoPostal: String,
    obraSeleccionada: Obra?,
    fechaEntregaMs: Long?,
    notasEntrega: String,
    costoMateriales: Double,
    costoTransporte: Double,
    total: Double,
    isUrgent: Boolean = false,
    isSubmitting: Boolean,
    onConfirmar: () -> Unit
) {
    val fechaFormateada = fechaEntregaMs?.let {
        SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(Date(it))
    } ?: "-"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Resumen del pedido",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (isUrgent) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Pedido urgente",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange,
                            fontSize = 14.sp
                        )
                        Text(
                            "Entrega en 24h · Recargo +50% en transporte",
                            fontSize = 11.sp,
                            color = Color(0xFF795548)
                        )
                    }
                }
            }
        }

        // Productos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Productos", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                HorizontalDivider()
                if (cartItems.isEmpty()) {
                    Text(
                        "Carrito vacío",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    cartItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontSize = 14.sp)
                                Text(
                                    "x${
                                        if (item.qty == item.qty.toLong().toDouble())
                                            item.qty.toLong().toString()
                                        else
                                            String.format("%.2f", item.qty)
                                    } ${item.uom}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                "${String.format("%.2f", item.priceUnit * item.qty)} €",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Dirección y obra
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Dirección de entrega",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                HorizontalDivider()
                Text("$calle $numero, $ciudad $codigoPostal", fontSize = 14.sp)
                if (obraSeleccionada != null) {
                    Text(
                        "Obra: ${obraSeleccionada.name}",
                        fontSize = 13.sp,
                        color = PrimaryOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        "ℹ Sin obra — el pedido se vinculará a tu perfil",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Text("Fecha: $fechaFormateada", fontSize = 13.sp)
                if (notasEntrega.isNotBlank()) {
                    Text("Notas: $notasEntrega", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        // Desglose precios
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Desglose de precios",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                HorizontalDivider()
                LineaPrecio("Materiales", costoMateriales)
                LineaPrecio(
                    when {
                        costoTransporte == 0.0 -> "Transporte (gratuito)"
                        isUrgent -> "Transporte urgente (+50%)"
                        else -> "Transporte"
                    },
                    costoTransporte
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(
                        "${String.format("%.2f", total)} €",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = PrimaryOrange
                    )
                }
                if (costoTransporte == 0.0) {
                    Text(
                        "Envío gratuito en pedidos superiores a 1.000 €",
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onConfirmar,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSubmitting && cartItems.isNotEmpty()
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviando pedido...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar pedido", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun LineaPrecio(label: String, valor: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.DarkGray)
        Text("${String.format("%.2f", valor)} €", fontSize = 14.sp)
    }
}
