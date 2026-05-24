package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

/**
 * Envoltorio genérico para todas las respuestas de la API Odoo.
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error_code") val errorCode: String? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("page_size") val pageSize: Int? = null
)

/**
 * Resultado del calculo de materiales de la calculadora.
 */
data class CalculatorResult(
    @SerializedName("type") val type: String,
    @SerializedName("m2") val m2: Double,
    @SerializedName("products") val products: List<CalculatorProduct>,
    @SerializedName("total_estimate") val totalEstimate: Double
)

data class CalculatorProduct(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String,
    @SerializedName("qty_needed") val qtyNeeded: Double,
    @SerializedName("uom") val uom: String,
    @SerializedName("price_unit") val priceUnit: Double,
    @SerializedName("subtotal") val subtotal: Double
)

/**
 * Request body para canjear puntos de fidelización (POST loyalty/redeem).
 */
data class RedeemRequest(
    @SerializedName("points_to_redeem") val pointsToRedeem: Int,
    @SerializedName("request_id") val requestId: Int? = null
)

/**
 * Resultado simplificado de la calculadora — estructura real que devuelve Odoo.
 */
data class CalculatorResultOdoo(
    @SerializedName("obra_type") val obraType: String,
    @SerializedName("m2") val m2: Double,
    @SerializedName("materials") val materials: List<ObraMaterial>
)

data class ObraMaterial(
    @SerializedName("material") val material: String,
    @SerializedName("unit") val unit: String,
    @SerializedName("quantity") val quantity: Double,
    // Campos opcionales que el endpoint embebe desde el catálogo
    @SerializedName("product_id") val productId: Int? = null,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("price_unit") val priceUnit: Double = 0.0,
    @SerializedName("product_uom") val productUom: String? = null,
)

/**
 * Mensajes del chatbot.
 */
data class ChatbotRequest(
    @SerializedName("message") val message: String,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("context") val context: Map<String, Any>? = null
)

data class ChatbotResponse(
    @SerializedName("reply") val reply: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("suggestions") val suggestions: List<String> = emptyList(),
    @SerializedName("products") val products: List<Product>? = null
)

/**
 * Estado de fidelización del usuario.
 */
data class LoyaltyStatus(
    @SerializedName("points_balance") val pointsBalance: Int,
    @SerializedName("loyalty_level") val loyaltyLevel: LoyaltyLevel,
    @SerializedName("next_level") val nextLevel: LoyaltyLevel? = null,
    @SerializedName("points_to_next_level") val pointsToNextLevel: Int = 0,
    @SerializedName("discount_percentage") val discountPercentage: Double = 0.0,
    @SerializedName("history") val history: List<LoyaltyTransaction> = emptyList()
)

data class LoyaltyTransaction(
    @SerializedName("date") val date: String,
    @SerializedName("type") val type: String,
    @SerializedName("points") val points: Int,
    @SerializedName("description") val description: String
)
