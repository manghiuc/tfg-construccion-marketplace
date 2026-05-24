package com.construccion.marketplace.data.repository

import com.construccion.marketplace.data.api.AppConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/* ─── Modelos de datos ─────────────────────────────────────────── */

data class OllamaMessage(
    val role: String,    // "system" | "user" | "assistant"
    val content: String
)

private data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = false,
    val options: OllamaOptions = OllamaOptions()
)

private data class OllamaOptions(
    val temperature: Double = 0.7,
    @SerializedName("num_predict") val numPredict: Int = 512
)

private data class OllamaChatResponse(
    val message: OllamaMessageRaw?,
    val error: String?
)

private data class OllamaMessageRaw(
    val role: String?,
    val content: String?
)

private data class OllamaTagsResponse(
    val models: List<OllamaModelInfo>?
)

private data class OllamaModelInfo(
    val name: String
)

/* ─── Resultado sellado ────────────────────────────────────────── */

sealed class OllamaResult {
    data class Success(val text: String) : OllamaResult()
    data class Error(val reason: String) : OllamaResult()
}

/* ─── Estado de conexión ───────────────────────────────────────── */

enum class OllamaEstado { CONECTANDO, CONECTADO, SIN_MODELOS, CORS, OFFLINE }

/* ─── Repositorio principal ────────────────────────────────────── */

class OllamaRepository {

    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Cliente con timeout largo para que el LLM tenga tiempo de generar */
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "http://10.0.2.2:11434"

    companion object {
        /** Prompt de sistema: define el rol de ConstruBot */
        const val SYSTEM_PROMPT = """Eres ConstruBot, el asistente de inteligencia artificial integrado en ConstruApp, un marketplace de materiales de construcción español.

Tu misión es ayudar a los usuarios con:
- Información técnica sobre materiales de construcción (cemento, ladrillos, áridos, mortero, azulejos, impermeabilizantes, tuberías, cable eléctrico, aislantes, etc.)
- Cálculo de cantidades necesarias para distintas obras (m², m³, kg, unidades)
- Comparativas entre productos y recomendaciones según el tipo de trabajo
- Precios orientativos del mercado español (indica siempre que son aproximados)
- Ayuda para usar la plataforma ConstruApp: crear solicitudes, gestionar obras, añadir al carrito

Normas de respuesta:
- Responde siempre en español claro y profesional
- Sé conciso y directo; evita rodeos innecesarios
- Si el usuario pregunta sobre algo fuera del sector construcción, redirige amablemente
- Nunca inventes especificaciones técnicas de las que no estés seguro; indica la incertidumbre
- Puedes usar listas con viñetas (•) para mayor claridad"""
    }

    /**
     * Detecta si Ollama está disponible y qué modelo tiene instalado.
     * Devuelve el nombre del modelo listo para usar, o null si falla.
     */
    suspend fun detectarModelo(): Pair<OllamaEstado, String?> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/api/tags")
                .get()
                .build()
            val resp = client.newCall(req).execute()

            if (!resp.isSuccessful) {
                return@withContext Pair(OllamaEstado.CORS, null)
            }

            val body = resp.body?.string() ?: return@withContext Pair(OllamaEstado.OFFLINE, null)
            val tags = gson.fromJson(body, OllamaTagsResponse::class.java)
            val models = tags.models

            if (models.isNullOrEmpty()) {
                return@withContext Pair(OllamaEstado.SIN_MODELOS, null)
            }

            // Preferencia de modelos por calidad/velocidad
            val preferencia = listOf("llama3", "phi3", "phi4", "mistral", "qwen", "gemma", "deepseek")
            val elegido = models.firstOrNull { m ->
                preferencia.any { m.name.contains(it, ignoreCase = true) }
            }?.name ?: models.first().name

            Pair(OllamaEstado.CONECTADO, elegido)

        } catch (e: Exception) {
            val estado = if (e.message?.contains("refused") == true || e.message?.contains("connect") == true)
                OllamaEstado.OFFLINE
            else
                OllamaEstado.CORS
            Pair(estado, null)
        }
    }

    /**
     * Envía un mensaje al modelo Ollama con todo el historial de conversación.
     * [historial] debe ser la lista de mensajes user/assistant anteriores (sin el system).
     */
    suspend fun chat(
        modelo: String,
        historial: List<OllamaMessage>,
        pregunta: String
    ): OllamaResult = withContext(Dispatchers.IO) {
        try {
            val mensajes = buildList {
                add(OllamaMessage("system", SYSTEM_PROMPT))
                addAll(historial)
                add(OllamaMessage("user", pregunta))
            }

            val bodyJson = gson.toJson(
                OllamaChatRequest(
                    model = modelo,
                    messages = mensajes,
                    stream = false
                )
            )

            val req = Request.Builder()
                .url("$baseUrl/api/chat")
                .post(bodyJson.toRequestBody(jsonType))
                .build()

            val resp = client.newCall(req).execute()
            val respBody = resp.body?.string()

            if (!resp.isSuccessful) {
                return@withContext OllamaResult.Error("HTTP ${resp.code}: ${respBody?.take(200)}")
            }

            if (respBody.isNullOrBlank()) {
                return@withContext OllamaResult.Error("Respuesta vacía del modelo")
            }

            val parsed = gson.fromJson(respBody, OllamaChatResponse::class.java)
            val content = parsed.message?.content?.trim()

            if (content.isNullOrBlank()) {
                OllamaResult.Error("El modelo devolvió contenido vacío")
            } else {
                OllamaResult.Success(content)
            }

        } catch (e: Exception) {
            OllamaResult.Error(e.message ?: "Error de red desconocido")
        }
    }
}
