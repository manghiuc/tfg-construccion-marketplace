package com.construccion.marketplace.data.api

/**
 * Configuración central de la aplicación ConstruApp.
 *
 * Este objeto almacena todas las constantes de configuración que usa la app
 * para conectarse al backend Odoo 17 y al servicio de IA Ollama.
 * Al ser un `object`, actúa como un singleton accesible desde cualquier parte del código.
 *
 * Para cambiar de entorno (emulador → dispositivo físico → producción),
 * solo hay que modificar BASE_URL y OLLAMA_URL aquí.
 */
object AppConfig {

    /**
     * URL base del servidor Odoo 17 donde está instalado el módulo
     * `construction_marketplace`. Todas las llamadas de Retrofit se
     * construyen sobre esta URL.
     *
     * - Emulador Android: 10.0.2.2 es el alias que Android da al localhost del PC host.
     * - Dispositivo físico: usar la IP local del PC (ej. 192.168.1.X).
     * - Producción: usar el dominio real (ej. https://odoo.miempresa.com).
     */
    const val BASE_URL = "http://10.0.2.2:8069"

    /**
     * Prefijo común de todos los endpoints REST del módulo Odoo.
     * Se concatena antes de cada ruta en OdooApiService.
     * Ejemplo: POST /api/construction/auth/login
     */
    const val API_PREFIX = "/api/construction"

    /** Tiempo máximo en segundos para establecer la conexión TCP con el servidor. */
    const val CONNECT_TIMEOUT_SECONDS = 30L

    /** Tiempo máximo en segundos esperando a que el servidor envíe datos de respuesta. */
    const val READ_TIMEOUT_SECONDS = 60L

    /** Tiempo máximo en segundos para enviar el cuerpo de la petición al servidor. */
    const val WRITE_TIMEOUT_SECONDS = 30L

    /**
     * Si es true, OkHttp imprime en Logcat el cuerpo completo de cada petición
     * y respuesta HTTP (headers + body). Útil para depurar, pero debe ser false
     * en producción para no exponer datos sensibles.
     */
    const val ENABLE_HTTP_LOGS = true

    /**
     * URL base del servidor Ollama (modelo de lenguaje local) que alimenta
     * el chatbot de asistencia de la app.
     *
     * Ollama debe estar corriendo con: OLLAMA_HOST=0.0.0.0 ollama serve
     * para aceptar conexiones desde el emulador/dispositivo.
     */
    const val OLLAMA_URL = "http://10.0.2.2:11434"
}
