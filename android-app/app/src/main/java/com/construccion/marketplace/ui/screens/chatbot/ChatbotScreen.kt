/**
 * Pantalla del chatbot ConstruBot.
 *
 * Interfaz de chat con un modelo LLM local (Ollama). El usuario puede
 * hacer preguntas sobre materiales de construcción, cálculos, uso de la app, etc.
 * Muestra indicador de conexión (conectado/sin modelos/offline),
 * historial de mensajes con burbujas diferenciadas, y sugerencias rápidas.
 * Usa [OllamaRepository] para comunicarse con el servidor Ollama local.
 */
package com.construccion.marketplace.ui.screens.chatbot

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.construccion.marketplace.data.repository.OllamaEstado
import com.construccion.marketplace.data.repository.OllamaMessage
import com.construccion.marketplace.data.repository.OllamaRepository
import com.construccion.marketplace.data.repository.OllamaResult
import com.construccion.marketplace.ui.theme.OrangeConstruction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ─── Colores ──────────────────────────────────────────────────── */
private val BotBubble  = Color(0xFFF5F5F5)
private val UserBubble = OrangeConstruction

/* ─── Modelos de UI ────────────────────────────────────────────── */
enum class TipoMensaje { USUARIO, BOT }

data class ProductoSugerido(
    val nombre: String,
    val precio: Double,
    val unidad: String
)

data class Mensaje(
    val id: String = System.currentTimeMillis().toString() + Math.random(),
    val tipo: TipoMensaje,
    val texto: String,
    val productoSugerido: ProductoSugerido? = null
)

/* ─── Sugerencias rápidas ──────────────────────────────────────── */
val sugerenciasRapidas = listOf(
    "¿Qué necesito para un baño?",
    "Precio del cemento",
    "Materiales para solera",
    "¿Cuántos ladrillos necesito?",
    "Comparar morteros"
)

/* ─── Fallback por keywords (sin Ollama) ───────────────────────── */
private val respuestasKeyword = mapOf(
    "baño" to Pair(
        "Para reformar un baño estándar de 5m², necesitarás:\n• Azulejos: ~20m² (paredes + suelo)\n• Adhesivo para azulejo: 5 sacos\n• Mortero de rejuntado: 2 sacos\n• Impermeabilizante: 2L\n\n¿Quieres que te añada estos materiales al carrito?",
        ProductoSugerido("Kit Reforma Baño Completo", 189.90, "kit")
    ),
    "cemento" to Pair(
        "Tenemos varias opciones de cemento:\n• CEM II 25kg — 6,50 €/saco\n• CEM I Portland 42,5N — 8,20 €/saco\n• Cemento rápido 5kg — 4,90 €/bote\n\nEl más vendido para obra general es el CEM II 25kg.",
        ProductoSugerido("Cemento CEM II 25kg", 6.50, "saco")
    ),
    "solera" to Pair(
        "Para una solera de hormigón necesitas:\n• Cemento CEM II: 350 kg/m³\n• Arena: 700 kg/m³\n• Grava: 1.000 kg/m³\n• Agua: 175 L/m³\n\n¿Cuántos m² va a tener tu solera? Puedo calcular la cantidad exacta.",
        ProductoSugerido("Arena lavada m³", 28.00, "m³")
    )
)

private fun respuestaKeyword(texto: String): Pair<String, ProductoSugerido?> {
    val lower = texto.lowercase()
    return when {
        lower.contains("baño")   -> respuestasKeyword["baño"]!!
        lower.contains("cemento") || lower.contains("precio") -> respuestasKeyword["cemento"]!!
        lower.contains("solera") -> respuestasKeyword["solera"]!!
        else -> Pair(
            "Entendido. Puedo ayudarte con materiales de construcción, calcular cantidades o resolver dudas técnicas. ¿En qué más puedo ayudarte?",
            null
        )
    }
}

/* ─── Screen principal ─────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatbotScreen(
    onNavigateBack: () -> Unit = {},
    onAnadirAlCarrito: (ProductoSugerido) -> Unit = {}
) {
    /* Estado de mensajes */
    val mensajes = remember {
        mutableStateListOf(
            Mensaje(
                tipo  = TipoMensaje.BOT,
                texto = "¡Hola! Soy ConstruBot, tu asistente de materiales de construcción. ¿En qué puedo ayudarte hoy?"
            )
        )
    }
    /* Historial para Ollama (user + assistant, sin system) */
    val historialOllama = remember { mutableStateListOf<OllamaMessage>() }

    /* Estado Ollama */
    val ollamaRepo  = remember { OllamaRepository() }
    var modeloActivo by remember { mutableStateOf<String?>(null) }
    var ollamaEstado by remember { mutableStateOf(OllamaEstado.CONECTANDO) }

    /* UI */
    var textoInput    by remember { mutableStateOf("") }
    var botEscribiendo by remember { mutableStateOf(false) }
    val listState      = rememberLazyListState()
    val scope          = rememberCoroutineScope()

    /* ── Detectar Ollama al arrancar ── */
    LaunchedEffect(Unit) {
        val (estado, modelo) = ollamaRepo.detectarModelo()
        ollamaEstado = estado
        modeloActivo = modelo

        if (estado == OllamaEstado.CONECTADO && modelo != null) {
            // Añadir mensaje informativo del modelo
            val modeloCorto = modelo.substringBefore(":").replaceFirstChar { it.uppercase() }
            mensajes.add(
                Mensaje(
                    tipo  = TipoMensaje.BOT,
                    texto = "✓ Usando $modeloCorto. ¡Pregúntame lo que necesites!"
                )
            )
        }
    }

    /* ── Scroll al último mensaje ── */
    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            delay(80)
            listState.animateScrollToItem(mensajes.size - 1)
        }
    }

    /* ── Función de envío ── */
    fun enviarMensaje(texto: String) {
        if (texto.isBlank() || botEscribiendo) return
        val textoCleaned = texto.trim()
        mensajes.add(Mensaje(tipo = TipoMensaje.USUARIO, texto = textoCleaned))
        textoInput = ""
        botEscribiendo = true

        scope.launch {
            val modelo = modeloActivo
            if (modelo != null) {
                /* ── Llamada a Ollama ── */
                val result = ollamaRepo.chat(
                    modelo   = modelo,
                    historial = historialOllama.toList(),
                    pregunta  = textoCleaned
                )
                botEscribiendo = false

                when (result) {
                    is OllamaResult.Success -> {
                        // Actualizar historial para mantener contexto
                        historialOllama.add(OllamaMessage("user",      textoCleaned))
                        historialOllama.add(OllamaMessage("assistant", result.text))
                        // Limitar historial a últimas 10 rondas para no desbordar el contexto
                        while (historialOllama.size > 20) historialOllama.removeAt(0)

                        mensajes.add(Mensaje(tipo = TipoMensaje.BOT, texto = result.text))
                    }
                    is OllamaResult.Error -> {
                        // Si Ollama falla en tiempo de ejecución, usamos keyword como fallback
                        val (respFallback, productoFallback) = respuestaKeyword(textoCleaned)
                        mensajes.add(
                            Mensaje(
                                tipo             = TipoMensaje.BOT,
                                texto            = respFallback,
                                productoSugerido = productoFallback
                            )
                        )
                    }
                }
            } else {
                /* ── Sin Ollama: modo keywords ── */
                delay(900)
                botEscribiendo = false
                val (respFallback, productoFallback) = respuestaKeyword(textoCleaned)
                mensajes.add(
                    Mensaje(
                        tipo             = TipoMensaje.BOT,
                        texto            = respFallback,
                        productoSugerido = productoFallback
                    )
                )
            }
        }
    }

    /* ─── UI ─────────────────────────────────────────────────────── */
    Scaffold(
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
                                Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("ConstruBot", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            // Subtítulo dinámico según estado Ollama
                            val subtitulo = when (ollamaEstado) {
                                OllamaEstado.CONECTANDO -> "Conectando con IA..."
                                OllamaEstado.CONECTADO  -> "● IA activa — ${modeloActivo?.substringBefore(":")}"
                                OllamaEstado.SIN_MODELOS -> "Ollama sin modelos"
                                OllamaEstado.CORS       -> "Inicia Ollama con CORS"
                                OllamaEstado.OFFLINE    -> "Modo offline"
                            }
                            Text(
                                subtitulo,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontStyle = if (ollamaEstado == OllamaEstado.CONECTADO) FontStyle.Normal else FontStyle.Italic
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
                    containerColor         = OrangeConstruction,
                    titleContentColor      = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            /* ── Lista de mensajes ── */
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mensajes, key = { it.id }) { mensaje ->
                    BurbujaMensaje(
                        mensaje            = mensaje,
                        onAnadirAlCarrito  = onAnadirAlCarrito
                    )
                }
                if (botEscribiendo) {
                    item { IndicadorEscribiendo() }
                }
            }

            /* ── Sugerencias rápidas ── */
            LazyRow(
                contentPadding       = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sugerenciasRapidas) { sugerencia ->
                    AssistChip(
                        onClick = { enviarMensaje(sugerencia) },
                        label   = { Text(sugerencia, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            /* ── Campo de texto + botón enviar ── */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value       = textoInput,
                    onValueChange = { textoInput = it },
                    placeholder = { Text("Escribe tu pregunta...") },
                    modifier    = Modifier.weight(1f),
                    shape       = RoundedCornerShape(24.dp),
                    maxLines    = 3,
                    enabled     = !botEscribiendo
                )
                val puedeEnviar = textoInput.isNotBlank() && !botEscribiendo
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (puedeEnviar) OrangeConstruction else Color.LightGray)
                        .clickable(enabled = puedeEnviar) { enviarMensaje(textoInput) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/* ─── Burbuja de mensaje ───────────────────────────────────────── */

@Composable
private fun BurbujaMensaje(mensaje: Mensaje, onAnadirAlCarrito: (ProductoSugerido) -> Unit) {
    val esUsuario = mensaje.tipo == TipoMensaje.USUARIO

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (esUsuario) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart    = 16.dp,
                        topEnd      = 16.dp,
                        bottomStart = if (esUsuario) 16.dp else 4.dp,
                        bottomEnd   = if (esUsuario) 4.dp  else 16.dp
                    )
                )
                .background(if (esUsuario) UserBubble else BotBubble)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text       = mensaje.texto,
                color      = if (esUsuario) Color.White else Color(0xFF212121),
                fontSize   = 14.sp,
                lineHeight = 20.sp
            )
        }

        if (!esUsuario && mensaje.productoSugerido != null) {
            Spacer(modifier = Modifier.height(6.dp))
            ProductoSugeridoCard(
                producto = mensaje.productoSugerido,
                onAnadir = { onAnadirAlCarrito(mensaje.productoSugerido) }
            )
        }
    }
}

/* ─── Tarjeta de producto sugerido ────────────────────────────── */

@Composable
private fun ProductoSugeridoCard(producto: ProductoSugerido, onAnadir: () -> Unit) {
    Card(
        modifier = Modifier.widthIn(max = 300.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F5)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCCBC))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFE0B2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint     = OrangeConstruction,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    producto.nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp
                )
                Text(
                    "${String.format("%.2f", producto.precio)} €/${producto.unidad}",
                    color      = OrangeConstruction,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick          = onAnadir,
                colors           = ButtonDefaults.buttonColors(containerColor = OrangeConstruction),
                contentPadding   = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier         = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir", fontSize = 12.sp)
            }
        }
    }
}

/* ─── Indicador de escritura animado ──────────────────────────── */

@Composable
private fun IndicadorEscribiendo() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
            .background(BotBubble)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text("Escribiendo", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.width(4.dp))
        repeat(3) { index ->
            val animOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue  = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400,
                        delayMillis    = index * 150,
                        easing         = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { translationY = animOffset }
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }
    }
}
