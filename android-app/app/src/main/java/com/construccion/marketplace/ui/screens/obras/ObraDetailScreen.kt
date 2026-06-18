/**
 * Pantalla de detalle de una obra.
 *
 * Muestra información completa: nombre, código, dirección, estado,
 * fechas de inicio/fin, descripción, coordenadas GPS (si tiene),
 * y acceso a pedir materiales para esa obra.
 * Usa [ObraDetailViewModel] con fallback a datos mock sin conexión.
 */
package com.construccion.marketplace.ui.screens.obras

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.construccion.marketplace.data.model.Obra
import com.construccion.marketplace.data.model.ObraState

private val PrimaryOrange = Color(0xFFE65100)

// --- Pantalla principal ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObraDetailScreen(
    obraId: Int,
    onNavigateBack: () -> Unit = {},
    onPedirMateriales: (Int) -> Unit = {},
    viewModel: ObraDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var retryCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(obraId, retryCount) {
        viewModel.loadObra(obraId)
    }

    val titleText = when (val s = uiState) {
        is ObraDetailUiState.Success -> s.obra.name
        else -> "Detalle de obra"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = titleText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        if (uiState is ObraDetailUiState.Success) {
                            Text(
                                text = (uiState as ObraDetailUiState.Success).obra.code,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
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
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ObraDetailUiState.Loading -> LoadingContent(paddingValues)
            is ObraDetailUiState.Error -> ErrorContent(
                message = state.message,
                paddingValues = paddingValues,
                onRetry = {
                    retryCount++
                }
            )
            is ObraDetailUiState.Success -> ObraDetailContent(
                obra = state.obra,
                paddingValues = paddingValues,
                onPedirMateriales = { onPedirMateriales(state.obra.id) }
            )
        }
    }
}

// --- Contenido: cargando ---

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = PrimaryOrange, strokeWidth = 3.dp)
            Text(
                "Cargando obra...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

// --- Contenido: error ---

@Composable
private fun ErrorContent(
    message: String,
    paddingValues: PaddingValues,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFD32F2F)
            )
            Text(
                "No se pudo cargar la obra",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reintentar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Contenido: detalle de la obra ---

@Composable
private fun ObraDetailContent(
    obra: Obra,
    paddingValues: PaddingValues,
    onPedirMateriales: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera: icono + nombre + chip de estado
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF3E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Construction,
                                contentDescription = null,
                                tint = PrimaryOrange,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                obra.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ObraStateChip(state = obra.state)
                        }
                    }
                }
            }
        }

        // Información general
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                SectionTitle("Información general")
                Spacer(modifier = Modifier.height(12.dp))

                DetalleRow(
                    icon = Icons.Default.Tag,
                    label = "Código",
                    value = obra.code
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color(0xFFEEEEEE))

                DetalleRow(
                    icon = Icons.Default.LocationOn,
                    label = "Dirección",
                    value = obra.address
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color(0xFFEEEEEE))

                DetalleRow(
                    icon = Icons.Default.Business,
                    label = "Partner / Empresa",
                    value = obra.partnerName
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color(0xFFEEEEEE))

                DetalleRow(
                    icon = Icons.Default.Receipt,
                    label = "Solicitudes de material",
                    value = "${obra.materialRequestCount} solicitudes"
                )
            }
        }

        // Fechas (sólo si existen)
        if (obra.startDate != null || obra.endDate != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    SectionTitle("Planificación")
                    Spacer(modifier = Modifier.height(12.dp))

                    obra.startDate?.let { fecha ->
                        DetalleRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Fecha inicio",
                            value = fecha
                        )
                    }

                    if (obra.startDate != null && obra.endDate != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                    }

                    obra.endDate?.let { fecha ->
                        DetalleRow(
                            icon = Icons.Default.EventAvailable,
                            label = "Fecha fin prevista",
                            value = fecha
                        )
                    }
                }
            }
        }

        // Descripción (sólo si existe)
        if (!obra.description.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SectionTitle("Descripción")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = obra.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF424242),
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Botón "Pedir materiales" (sólo si la obra está ACTIVE)
        if (obra.state == ObraState.ACTIVE) {
            Button(
                onClick = onPedirMateriales,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Pedir materiales para esta obra",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// --- Chip de estado ---

@Composable
private fun ObraStateChip(state: ObraState) {
    val (bgColor, textColor, icon) = when (state) {
        ObraState.ACTIVE    -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.PlayCircle)
        ObraState.PAUSED    -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), Icons.Default.Pause)
        ObraState.DONE      -> Triple(Color(0xFFEEEEEE), Color(0xFF616161), Icons.Default.TaskAlt)
        ObraState.DRAFT     -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), Icons.Default.Numbers)
        ObraState.CANCELLED -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), Icons.Default.ErrorOutline)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                state.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

// --- Fila de detalle con icono ---

@Composable
private fun DetalleRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = PrimaryOrange,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121)
            )
        }
    }
}

// --- Título de sección ---

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF424242)
    )
}
