package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

/**
 * Envoltorio genérico para TODAS las respuestas de la API REST de Odoo.
 *
 * El módulo construction_marketplace siempre devuelve este formato:
 * { "success": true/false, "data": <T>, "message": "...", ... }
 *
 * El tipo genérico T varía según el endpoint:
 * - LoginResponse para auth/login
 * - List<Product> para products
 * - MaterialRequest para material_request
 * - etc.
 *
 * Los campos de paginación (total, page, pageSize) solo se rellenan
 * en endpoints que devuelven listas paginadas (ej. products, material_request).
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,            // true si la operación fue exitosa
    @SerializedName("data") val data: T? = null,                // Datos de la respuesta (tipado genérico)
    @SerializedName("message") val message: String? = null,     // Mensaje descriptivo (para errores)
    @SerializedName("error_code") val errorCode: String? = null,// Código de error interno (ej. "AUTH_FAILED")
    @SerializedName("total") val total: Int? = null,            // Total de registros (para paginación)
    @SerializedName("page") val page: Int? = null,              // Página actual (base 0)
    @SerializedName("page_size") val pageSize: Int? = null      // Tamaño de página
)

/**
 * Resultado del cálculo de materiales (formato antiguo / alternativo).
 *
 * Estructura con productos del catálogo y estimación total de coste.
 * Se usa como fallback si el endpoint devuelve este formato.
 */
data class CalculatorResult(
    @SerializedName("type") val type: String,                            // Tipo de obra (ej. "tabique")
    @SerializedName("m2") val m2: Double,                                // Metros cuadrados introducidos
    @SerializedName("products") val products: List<CalculatorProduct>,   // Lista de productos necesarios
    @SerializedName("total_estimate") val totalEstimate: Double          // Coste total estimado en €
)

/**
 * Producto individual dentro de un resultado de calculadora (formato antiguo).
 */
data class CalculatorProduct(
    @SerializedName("product_id") val productId: Int,       // ID del producto en Odoo
    @SerializedName("product_name") val productName: String,// Nombre del producto
    @SerializedName("qty_needed") val qtyNeeded: Double,    // Cantidad necesaria para los m²
    @SerializedName("uom") val uom: String,                 // Unidad de medida (kg, ud, m, litro)
    @SerializedName("price_unit") val priceUnit: Double,    // Precio unitario
    @SerializedName("subtotal") val subtotal: Double        // qtyNeeded × priceUnit
)

/**
 * Body JSON para POST /loyalty/redeem — canjear puntos por descuento.
 *
 * 100 puntos = 1€ de descuento. El requestId es opcional y permite
 * aplicar el descuento directamente a un pedido concreto.
 */
data class RedeemRequest(
    @SerializedName("points_to_redeem") val pointsToRedeem: Int, // Puntos a canjear
    @SerializedName("request_id") val requestId: Int? = null     // ID del pedido (opcional)
)

/**
 * Resultado de la calculadora de materiales — formato real de Odoo.
 *
 * El endpoint /catalog/calculator devuelve esta estructura con el tipo de obra,
 * los m² y la lista de materiales necesarios con cantidades calculadas.
 */
data class CalculatorResultOdoo(
    @SerializedName("obra_type") val obraType: String,              // Tipo de obra (tabique, solera, etc.)
    @SerializedName("m2") val m2: Double,                           // Superficie en m²
    @SerializedName("materials") val materials: List<ObraMaterial>   // Materiales calculados
)

/**
 * Material individual dentro del resultado de la calculadora.
 *
 * Contiene el nombre genérico del material y la cantidad necesaria.
 * Si el backend encuentra un producto del catálogo que corresponde,
 * también rellena los campos opcionales (productId, productName, priceUnit).
 */
data class ObraMaterial(
    @SerializedName("material") val material: String,                // Nombre genérico (ej. "Cemento")
    @SerializedName("unit") val unit: String,                        // Unidad (kg, m², litro)
    @SerializedName("quantity") val quantity: Double,                 // Cantidad necesaria
    @SerializedName("product_id") val productId: Int? = null,        // ID del producto del catálogo (si existe)
    @SerializedName("product_name") val productName: String? = null,  // Nombre del producto del catálogo
    @SerializedName("price_unit") val priceUnit: Double = 0.0,       // Precio unitario del producto
    @SerializedName("product_uom") val productUom: String? = null,   // UdM del producto del catálogo
)

/**
 * Petición al chatbot de asistencia (POST /chatbot).
 *
 * Envía el mensaje del usuario junto con un session_id de conversación
 * (para mantener contexto entre mensajes) y contexto adicional opcional.
 */
data class ChatbotRequest(
    @SerializedName("message") val message: String,                  // Mensaje del usuario
    @SerializedName("session_id") val sessionId: String? = null,     // ID de conversación del chat
    @SerializedName("context") val context: Map<String, Any>? = null // Contexto extra (carrito, obra activa...)
)

/**
 * Respuesta del chatbot de asistencia.
 *
 * Incluye el texto de respuesta, sugerencias rápidas para que el usuario
 * pulse sin escribir, y opcionalmente productos recomendados del catálogo.
 */
data class ChatbotResponse(
    @SerializedName("reply") val reply: String,                              // Texto de respuesta del bot
    @SerializedName("session_id") val sessionId: String,                     // ID de conversación (para continuidad)
    @SerializedName("suggestions") val suggestions: List<String> = emptyList(), // Sugerencias rápidas ("Ver cemento", "Pedir presupuesto")
    @SerializedName("products") val products: List<Product>? = null          // Productos recomendados por el bot
)

/**
 * Estado completo del programa de fidelización del usuario.
 *
 * Contiene el saldo actual, nivel, cuántos puntos faltan para subir,
 * el porcentaje de descuento activo y el historial de movimientos.
 */
data class LoyaltyStatus(
    @SerializedName("points_balance") val pointsBalance: Int,                // Saldo actual de puntos
    @SerializedName("loyalty_level") val loyaltyLevel: LoyaltyLevel,         // Nivel actual (BRONCE, PLATA, etc.)
    @SerializedName("next_level") val nextLevel: LoyaltyLevel? = null,       // Siguiente nivel (null si es PLATINO)
    @SerializedName("points_to_next_level") val pointsToNextLevel: Int = 0,  // Puntos que faltan para subir
    @SerializedName("discount_percentage") val discountPercentage: Double = 0.0, // % de descuento por nivel
    @SerializedName("history") val history: List<LoyaltyTransaction> = emptyList() // Historial de movimientos
)

/**
 * Movimiento individual en el historial de fidelización.
 * Puede ser una ganancia de puntos (compra) o un canje (descuento).
 */
data class LoyaltyTransaction(
    @SerializedName("date") val date: String,               // Fecha ISO (ej. "2026-06-15")
    @SerializedName("type") val type: String,               // "earn" (ganar) o "redeem" (canjear)
    @SerializedName("points") val points: Int,              // Puntos (+ganados o -canjeados)
    @SerializedName("description") val description: String  // Descripción (ej. "Compra pedido #MR-0025")
)
