package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

/**
 * Representa un artículo dentro del carrito de compras.
 *
 * Cada instancia almacena los datos mínimos del producto seleccionado
 * (ID, nombre, cantidad, precio unitario, imagen, unidad de medida y peso)
 * y calcula automáticamente el subtotal como qty × priceUnit.
 *
 * Es inmutable (data class): cualquier cambio de cantidad genera
 * una copia nueva mediante [withQty].
 */
data class CartItem(
    // Identificador del producto en Odoo (product.template)
    @SerializedName("product_id") val productId: Int,
    // Nombre legible del producto para mostrar en la UI
    @SerializedName("product_name") val productName: String,
    // Cantidad seleccionada por el usuario
    @SerializedName("qty") val qty: Double,
    // Precio unitario en euros (sin IVA)
    @SerializedName("price_unit") val priceUnit: Double,
    // Subtotal calculado: qty × priceUnit (se recalcula al cambiar cantidad)
    @SerializedName("subtotal") val subtotal: Double = qty * priceUnit,
    // URL relativa de la imagen del producto (puede ser null si no tiene)
    @SerializedName("image_url") val imageUrl: String? = null,
    // Unidad de medida abreviada (ej. "ud", "kg", "m²")
    @SerializedName("uom") val uom: String = "ud",
    // Peso en kilogramos; se usa para calcular el coste de transporte
    @SerializedName("weight_kg") val weightKg: Double = 0.0
) {
    /**
     * Devuelve una copia del artículo con la cantidad actualizada
     * y el subtotal recalculado automáticamente.
     */
    fun withQty(newQty: Double): CartItem = copy(
        qty = newQty,
        subtotal = newQty * priceUnit
    )
}
