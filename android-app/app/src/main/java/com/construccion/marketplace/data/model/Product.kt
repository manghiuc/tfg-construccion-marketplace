package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

data class Product(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("default_code") val defaultCode: String,
    @SerializedName("category") val category: ProductCategory,
    @SerializedName("uom") val uom: String,
    @SerializedName("price") val price: Double,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("stock_qty") val stockQty: Double = 0.0,
    @SerializedName("weight_kg") val weightKg: Double = 0.0,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("tags") val tags: List<String> = emptyList()
)

data class ProductCategory(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("parent_name") val parentName: String? = null
)
