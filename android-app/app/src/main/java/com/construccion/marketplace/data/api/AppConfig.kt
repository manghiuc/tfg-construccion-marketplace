package com.construccion.marketplace.data.api

/**
 * Configuración central de la aplicación.
 * Cambia BASE_URL para apuntar a tu instancia de Odoo 17.
 */
object AppConfig {
    /** URL base del backend Odoo 17. No incluir barra final.
     *  Emulador Android: http://10.0.2.2:8069
     *  Dispositivo físico en la misma red WiFi: http://<IP_LOCAL_PC>:8069
     */
    const val BASE_URL = "http://10.0.2.2:8069"

    /** Prefijo de la API REST — rutas Odoo: /api/construction/... */
    const val API_PREFIX = "/api/construction"

    /** Timeout de conexión en segundos */
    const val CONNECT_TIMEOUT_SECONDS = 30L

    /** Timeout de lectura en segundos */
    const val READ_TIMEOUT_SECONDS = 60L

    /** Timeout de escritura en segundos */
    const val WRITE_TIMEOUT_SECONDS = 30L

    /** Activar logs de red en builds debug */
    const val ENABLE_HTTP_LOGS = true

    /** URL base de Ollama (LLM local).
     *  Emulador Android: http://10.0.2.2:11434
     *  Dispositivo físico: http://<IP_LOCAL_PC>:11434
     *  Requiere: OLLAMA_HOST=0.0.0.0 ollama serve
     */
    const val OLLAMA_URL = "http://10.0.2.2:11434"
}
