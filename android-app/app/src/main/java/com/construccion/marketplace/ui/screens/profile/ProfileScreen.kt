package com.construccion.marketplace.ui.screens.profile

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.construccion.marketplace.data.model.LoyaltyLevel
import com.construccion.marketplace.data.model.PartnerType

private val PrimaryOrange = Color(0xFFE65100)

enum class NivelFidelidad(val label: String, val color: Color, val puntosParaSiguiente: Int) {
    BRONCE("Bronce", Color(0xFFCD7F32), 500),
    PLATA("Plata", Color(0xFF9E9E9E), 1000),
    ORO("Oro", Color(0xFFFFD700), 2000),
    PLATINO("Platino", Color(0xFFE5E4E2), Int.MAX_VALUE)
}

data class PerfilUsuario(
    val nombre: String,
    val email: String,
    val tipoCuenta: TipoCuentaPerfil,
    val puntosFidelidad: Int,
    val nivelFidelidad: NivelFidelidad
)

enum class TipoCuentaPerfil(val label: String, val color: Color) {
    PARTICULAR("Particular", Color(0xFF1565C0)),
    AUTONOMO("Autónomo", Color(0xFF2E7D32)),
    EMPRESA("Empresa", PrimaryOrange)
}

// Mapeos desde el modelo de datos Odoo
private fun PartnerType.toPerfil() = when (this) {
    PartnerType.AUTONOMO -> TipoCuentaPerfil.AUTONOMO
    PartnerType.EMPRESA  -> TipoCuentaPerfil.EMPRESA
    else                 -> TipoCuentaPerfil.PARTICULAR
}

private fun LoyaltyLevel.toNivel() = when (this) {
    LoyaltyLevel.PLATA   -> NivelFidelidad.PLATA
    LoyaltyLevel.ORO     -> NivelFidelidad.ORO
    LoyaltyLevel.PLATINO -> NivelFidelidad.PLATINO
    else                 -> NivelFidelidad.BRONCE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onEditarDatos: () -> Unit = {},
    onMisObras: () -> Unit = {},
    onCalculadora: () -> Unit = {},
    onLoyalty: () -> Unit = {},
    onCerrarSesion: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // Datos reales del usuario desde la sesión de Odoo
    val usuario = remember(viewModel.userName, viewModel.userLogin, viewModel.userType, viewModel.loyaltyPoints, viewModel.loyaltyLevel) {
        PerfilUsuario(
            nombre = viewModel.userName,
            email = viewModel.userLogin,
            tipoCuenta = viewModel.userType.toPerfil(),
            puntosFidelidad = viewModel.loyaltyPoints,
            nivelFidelidad = viewModel.loyaltyLevel.toNivel()
        )
    }

    var notifPedidos by remember { mutableStateOf(true) }
    var notifPromos by remember { mutableStateOf(false) }
    var notifReparto by remember { mutableStateOf(true) }
    var mostrarDialogoCerrarSesion by remember { mutableStateOf(false) }

    val esProfesional = usuario.tipoCuenta == TipoCuentaPerfil.EMPRESA ||
            usuario.tipoCuenta == TipoCuentaPerfil.AUTONOMO

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = onEditarDatos) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White)
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header avatar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryOrange)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            usuario.nombre.first().uppercase(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(usuario.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(usuario.email, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            usuario.tipoCuenta.label,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Mis datos
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SeccionHeader("Mis datos")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        FilaDato(icono = Icons.Default.Person, label = "Nombre", valor = usuario.nombre)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        FilaDato(icono = Icons.Default.Email, label = "Correo", valor = usuario.email)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditarDatos() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Editar mis datos", color = PrimaryOrange, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Programa de fidelidad
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SeccionHeader("Programa de fidelidad")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = usuario.nivelFidelidad.color,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Nivel ${usuario.nivelFidelidad.label}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = usuario.nivelFidelidad.color
                                )
                                Text("${usuario.puntosFidelidad} puntos acumulados", fontSize = 13.sp, color = Color.DarkGray)
                            }
                        }

                        if (usuario.nivelFidelidad != NivelFidelidad.ORO) {
                            val siguiente = when (usuario.nivelFidelidad) {
                                NivelFidelidad.BRONCE -> NivelFidelidad.PLATA
                                NivelFidelidad.PLATA -> NivelFidelidad.ORO
                                else -> NivelFidelidad.ORO
                            }
                            val puntosSiguiente = siguiente.puntosParaSiguiente
                            val progreso = usuario.puntosFidelidad.toFloat() / puntosSiguiente.toFloat()

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Progreso a ${siguiente.label}", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    "${puntosSiguiente - usuario.puntosFidelidad} pts restantes",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progreso.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                color = usuario.nivelFidelidad.color,
                                trackColor = Color(0xFFE0E0E0)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nivel máximo alcanzado", fontSize = 13.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Mis obras (solo profesionales)
            if (esProfesional) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SeccionHeader("Mis obras")
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMisObras() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Gestionar mis obras", fontWeight = FontWeight.SemiBold)
                                    Text("3 obras activas", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Herramientas
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SeccionHeader("Herramientas")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCalculadora() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Calculadora de materiales", fontWeight = FontWeight.SemiBold)
                                    Text("Estima cantidades por m²", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLoyalty() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Programa de fidelidad", fontWeight = FontWeight.SemiBold)
                                    Text("Ver puntos y canjear descuentos", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Notificaciones
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SeccionHeader("Notificaciones")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        SwitchNotificacion(
                            icono = Icons.Default.CheckCircle,
                            titulo = "Estado de pedidos",
                            subtitulo = "Actualizaciones cuando cambie el estado",
                            valor = notifPedidos,
                            onCambio = { notifPedidos = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwitchNotificacion(
                            icono = Icons.Default.LocalShipping,
                            titulo = "Seguimiento de reparto",
                            subtitulo = "Notificaciones cuando el pedido esté en camino",
                            valor = notifReparto,
                            onCambio = { notifReparto = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwitchNotificacion(
                            icono = Icons.Default.Notifications,
                            titulo = "Ofertas y promociones",
                            subtitulo = "Descuentos y novedades del catálogo",
                            valor = notifPromos,
                            onCambio = { notifPromos = it }
                        )
                    }
                }
            }

            // Cerrar sesión
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { mostrarDialogoCerrarSesion = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (mostrarDialogoCerrarSesion) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCerrarSesion = false },
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que quieres cerrar sesión? Tendrás que volver a iniciar sesión para acceder a tu cuenta.") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoCerrarSesion = false
                        viewModel.logout { onCerrarSesion() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCerrarSesion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SeccionHeader(titulo: String) {
    Text(
        titulo,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        fontSize = 12.sp,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun FilaDato(icono: ImageVector, label: String, valor: String) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(valor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SwitchNotificacion(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    valor: Boolean,
    onCambio: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitulo, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(
            checked = valor,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryOrange)
        )
    }
}
