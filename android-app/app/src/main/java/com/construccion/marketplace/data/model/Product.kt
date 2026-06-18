package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

/**
 * Producto del catálogo del marketplace de construcción.
 *
 * Mapea el modelo product.template de Odoo 17. Cada producto tiene un código
 * interno (default_code), categoría, precio, stock disponible, peso, etc.
 * La imagen puede venir como URL del servidor o ser null (en cuyo caso
 * ProductImageHelper busca una imagen local por keywords del nombre).
 */
data class Product(
    @SerializedName("id") val id: Int,                        // ID del product.template en Odoo
    @SerializedName("name") val name: String,                  // Nombre del producto (ej. "Cemento Portland CEM II")
    @SerializedName("default_code") val defaultCode: String,   // Código interno / referencia (ej. "CEM-001")
    @SerializedName("category") val category: ProductCategory, // Categoría del producto (Cemento, Hierro, etc.)
    @SerializedName("uom") val uom: String,                    // Unidad de medida (kg, ud, m², litro, etc.)
    @SerializedName("price") val price: Double,                // Precio unitario con IVA incluido
    @SerializedName("image_url") val imageUrl: String? = null, // URL de la imagen en Odoo (null → se usa imagen local)
    @SerializedName("description") val description: String? = null, // Descripción larga del producto
    @SerializedName("stock_qty") val stockQty: Double = 0.0,   // Stock disponible en el almacén
    @SerializedName("weight_kg") val weightKg: Double = 0.0,   // Peso en kg (usado para cálculo de transporte)
    @SerializedName("brand") val brand: String? = null,        // Marca comercial del producto
    @SerializedName("tags") val tags: List<String> = emptyList() // Etiquetas para búsqueda y filtrado
)

/**
 * Categoría de producto del marketplace.
 *
 * Corresponde a product.category en Odoo. Puede tener una categoría padre
 * (ej. "Cemento" → padre "Materiales básicos") para navegación jerárquica.
 */
data class ProductCategory(
    @SerializedName("id") val id: Int,                         // ID de la categoría en Odoo
    @SerializedName("name") val name: String,                  // Nombre de la categoría (ej. "Cemento")
    @SerializedName("parent_name") val parentName: String? = null // Nombre de la categoría padre (opcional)
)
