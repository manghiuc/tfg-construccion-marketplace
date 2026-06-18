/**
 * Pantalla del programa de fidelización.
 *
 * Muestra el nivel actual (Bronce/Plata/Oro/Platino), puntos acumulados,
 * barra de progreso hacia el siguiente nivel, porcentaje de descuento activo,
 * formulario para canjear puntos, e historial de transacciones de puntos.
 * Datos cargados vía [LoyaltyViewModel].
 */
package com.construccion.marketplace.ui.screens.loyalty

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.construccion.marketplace.data.model.LoyaltyLevel
import com.construccion.marketplace.data.model.LoyaltyStatus
import com.construccion.marketplace.data.model.LoyaltyTransaction
import com.construccion.marketplace.ui.theme.OrangeConstruction
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Colores y constantes
// ---------------------------------------------------------------------------

private val BronzeColor = Color(0xFFCD7F32)
private val SilverColor = Color(0xFF9E9E9E)
private val GoldColor = Color(0xFFFFD700)

private const val POINTS_PER_EURO = 100

// ---------------------------------------------------------------------------
// Datos mock (fallback cuando no hay conexión)
// ---------------------------------------------------------------------------

private val mockLoyaltyStatus = LoyaltyStatus(
    pointsBalance = 720,
    loyaltyLevel = LoyaltyLevel.PLATA,
    nextLevel = LoyaltyLevel.ORO,
    pointsToNextLevel = 280,
    discountPercentage = 5.0,
    history = listOf(
        LoyaltyTransaction("19/05/2026", "earn", +150, "Compra #4821 – Materiales de construcción"),
        LoyaltyTransaction("12/05/2026", "earn", +90, "Compra #4806 – Herramientas eléctricas"),
        LoyaltyTransaction("03/05/2026", "redeem", -100, "Canje de descuento en pedido #4790"),
        LoyaltyTransaction("25/04/2026", "earn", +580, "Bienvenida al programa de fidelidad")
    )
)

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun levelColor(level: LoyaltyLevel): Color = when (level) {
    LoyaltyLevel.BRONCE -> BronzeColor
    LoyaltyLevel.PLATA -> SilverColor
    LoyaltyLevel.ORO, LoyaltyLevel.PLATINO -> GoldColor
}

private fun levelMaxPoints(level: LoyaltyLevel): Int = when (level) {
    LoyaltyLevel.BRONCE -> 500
    LoyaltyLevel.PLATA -> 1000
    else -> Int.MAX_VALUE
}

private fun prevLevelPoints(level: LoyaltyLevel): Int = when (level) {
    LoyaltyLevel.PLATA -> 500
    LoyaltyLevel.ORO -> 1000
    LoyaltyLevel.PLATINO -> 2000
    else -> 0
}

// ---------------------------------------------------------------------------
// Pantalla principal
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: LoyaltyViewModel = hiltViewModel()
) {
    val apiStatus by viewModel.status.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val redeemSuccess by viewModel.redeemSuccess.collectAsStateWithLifecycle()

    // Usar datos reales si llegan, fallback a mock si no hay conexión
    val status = apiStatus ?: mockLoyaltyStatus

    var sliderValue by remember { mutableFloatStateOf(0f) }
    var inputText by remember { mutableStateOf("0") }
    var canjeExitoso by remember { mutableStateOf(false) }

    LaunchedEffect(redeemSuccess) {
        if (redeemSuccess) {
            canjeExitoso = true
            sliderValue = 0f
            inputText = "0"
            viewModel.clearRedeemSuccess()
        }
    }

    val maxCanjeable = status.pointsBalance
    val puntosAcanjear = sliderValue.roundToInt().coerceIn(0, maxCanjeable)
    val euroEquivalente = puntosAcanjear.toDouble() / POINTS_PER_EURO

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Programa de Fidelidad", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
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

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = OrangeConstruction)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cargando puntos...", color = Color.Gray)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item { LoyaltyHeader(status = status) }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                SeccionTitulo(
                    icono = {
                        Icon(
                            Icons.Default.Redeem,
                            contentDescription = null,
                            tint = OrangeConstruction,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    titulo = "Canjear puntos"
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Puntos disponibles", fontSize = 13.sp, color = Color.Gray)
                            Text(
                                "${status.pointsBalance} pts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = OrangeConstruction
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Selecciona puntos a canjear",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = sliderValue,
                            onValueChange = { newVal ->
                                sliderValue = newVal
                                inputText = newVal.roundToInt().toString()
                                canjeExitoso = false
                            },
                            valueRange = 0f..maxCanjeable.toFloat().coerceAtLeast(1f),
                            steps = if (maxCanjeable > 1) (maxCanjeable / 10) - 1 else 0,
                            colors = SliderDefaults.colors(
                                thumbColor = OrangeConstruction,
                                activeTrackColor = OrangeConstruction,
                                inactiveTrackColor = Color(0xFFFFCCBC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { texto ->
                                val parsed = texto.filter { it.isDigit() }
                                inputText = parsed
                                val num = parsed.toIntOrNull() ?: 0
                                sliderValue = num.coerceIn(0, maxCanjeable).toFloat()
                                canjeExitoso = false
                            },
                            label = { Text("Puntos a canjear") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeConstruction,
                                focusedLabelColor = OrangeConstruction,
                                cursorColor = OrangeConstruction
                            ),
                            trailingIcon = {
                                Text("pts", color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF3E0))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$puntosAcanjear puntos equivalen a:", fontSize = 13.sp, color = Color.DarkGray)
                                Text(
                                    "%.2f €".format(euroEquivalente),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = OrangeConstruction
                                )
                            }
                        }
                        if (canjeExitoso) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Canje realizado correctamente",
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (puntosAcanjear > 0) {
                                    viewModel.redeemPoints(puntosAcanjear)
                                }
                            },
                            enabled = puntosAcanjear > 0,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeConstruction,
                                disabledContainerColor = Color(0xFFBDBDBD)
                            )
                        ) {
                            Icon(Icons.Default.Redeem, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (puntosAcanjear > 0) "Canjear $puntosAcanjear pts" else "Selecciona puntos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                SeccionTitulo(
                    icono = {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = OrangeConstruction,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    titulo = "Historial de transacciones"
                )
            }

            if (status.history.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aún no hay transacciones", color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateContentSize(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column {
                            status.history.forEachIndexed { index, transaction ->
                                TransaccionItem(transaction = transaction)
                                if (index < status.history.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = Color(0xFFF0F0F0)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "100 puntos = 1 € de descuento en tu próximo pedido",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header de fidelidad
// ---------------------------------------------------------------------------

@Composable
private fun LoyaltyHeader(status: LoyaltyStatus) {
    val lColor = levelColor(status.loyaltyLevel)
    val isMaxLevel = status.nextLevel == null

    val currentMax = levelMaxPoints(status.loyaltyLevel)
    val prevMax = prevLevelPoints(status.loyaltyLevel)
    val progressInLevel = if (!isMaxLevel && currentMax > prevMax) {
        (status.pointsBalance - prevMax).toFloat().coerceAtLeast(0f) /
                (currentMax - prevMax).toFloat()
    } else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrangeConstruction)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isMaxLevel) Icons.Default.Star else Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = lColor,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            "${status.pointsBalance}",
            fontSize = 52.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            lineHeight = 52.sp
        )
        Text("puntos disponibles", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(lColor.copy(alpha = 0.25f))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = lColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    "Nivel ${status.loyaltyLevel.label}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        if (status.discountPercentage > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Descuento activo: ${status.discountPercentage.toInt()}% en todos tus pedidos",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold
            )
        }
        val nextLvl = status.nextLevel
        if (!isMaxLevel && nextLvl != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progreso a ${nextLvl.label}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                Text("${status.pointsToNextLevel} pts restantes", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progressInLevel.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = levelColor(nextLvl),
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        } else {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = GoldColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nivel máximo alcanzado", fontSize = 13.sp, color = GoldColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Fila de transacción
// ---------------------------------------------------------------------------

@Composable
private fun TransaccionItem(transaction: LoyaltyTransaction) {
    val isEarn = transaction.points > 0
    val pointsColor = if (isEarn) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    val pointsPrefix = if (isEarn) "+" else ""

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isEarn) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isEarn) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = pointsColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.description, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(transaction.date, fontSize = 11.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("$pointsPrefix${transaction.points} pts", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = pointsColor)
    }
}

// ---------------------------------------------------------------------------
// Cabecera de sección
// ---------------------------------------------------------------------------

@Composable
private fun SeccionTitulo(icono: @Composable () -> Unit, titulo: String) {
    Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        icono()
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            titulo.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp
        )
    }
}
