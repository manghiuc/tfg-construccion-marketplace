package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

data class CartItem(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String,
    @SerializedName("qty") val qty: Double,
    @SerializedName("price_unit") val priceUnit: Double,
    @SerializedName("subtotal") val subtotal: Double = qty * priceUnit,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("uom") val uom: String = "ud",
    @SerializedName("weight_kg") val weightKg: Double = 0.0
) {
    /** Recalcula el subtotal al cambiar la cantidad */
    fun withQty(newQty: Double): CartItem = copy(
        qty = newQty,
        subtotal = newQty * priceUnit
    )
}
