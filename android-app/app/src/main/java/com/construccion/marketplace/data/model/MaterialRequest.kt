package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

data class MaterialRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("state") val state: MaterialRequestState,
    @SerializedName("obra_name") val obraName: String,
    @SerializedName("obra_id") val obraId: Int,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("transport_cost") val transportCost: Double,
    @SerializedName("total_with_transport") val totalWithTransport: Double,
    @SerializedName("tracking_info") val trackingInfo: TrackingInfo? = null,
    @SerializedName("lines") val lines: List<MaterialRequestLine> = emptyList(),
    @SerializedName("delivery_address") val deliveryAddress: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("create_date") val createDate: String? = null,
    @SerializedName("scheduled_date") val scheduledDate: String? = null,
    @SerializedName("loyalty_discount") val loyaltyDiscount: Double = 0.0,
    @SerializedName("points_used") val pointsUsed: Int = 0,
    @SerializedName("is_urgent") val isUrgent: Boolean = false
)

data class MaterialRequestLine(
    @SerializedName("id") val id: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String,
    @SerializedName("qty") val qty: Double,
    @SerializedName("uom") val uom: String,
    @SerializedName("price_unit") val priceUnit: Double,
    @SerializedName("subtotal") val subtotal: Double,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class TrackingInfo(
    @SerializedName("status") val status: String,
    @SerializedName("carrier") val carrier: String? = null,
    @SerializedName("tracking_number") val trackingNumber: String? = null,
    @SerializedName("estimated_delivery") val estimatedDelivery: String? = null,
    @SerializedName("last_update") val lastUpdate: String? = null,
    @SerializedName("location") val location: String? = null
)

enum class MaterialRequestState(val label: String) {
    @SerializedName("draft")          DRAFT("Borrador"),
    @SerializedName("confirmed")      CONFIRMED("Tramitando"),
    @SerializedName("approved")       APPROVED("Aprobado"),
    @SerializedName("en_preparacion") EN_PREPARACION("En preparación"),
    @SerializedName("in_progress")    IN_PROGRESS("En preparación"),   // alias legacy
    @SerializedName("en_reparto")     EN_REPARTO("En reparto"),
    @SerializedName("shipped")        SHIPPED("En reparto"),           // alias legacy
    @SerializedName("delivered")      DELIVERED("Entregado"),
    @SerializedName("cancelled")      CANCELLED("Cancelado")
}

data class MaterialRequestCreateRequest(
    @SerializedName("obra_id") val obraId: Int,
    @SerializedName("lines") val lines: List<MaterialRequestLineCreate>,
    @SerializedName("delivery_address") val deliveryAddress: String? = null,
    @SerializedName("is_urgent") val isUrgent: Boolean = false,
    @SerializedName("delivery_lat") val deliveryLat: Double? = null,
    @SerializedName("delivery_lon") val deliveryLon: Double? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("use_loyalty_points") val useLoyaltyPoints: Boolean = false,
    @SerializedName("loyalty_points_amount") val loyaltyPointsAmount: Int = 0
)

data class MaterialRequestLineCreate(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("qty") val qty: Double,
    @SerializedName("price_unit") val priceUnit: Double
)
