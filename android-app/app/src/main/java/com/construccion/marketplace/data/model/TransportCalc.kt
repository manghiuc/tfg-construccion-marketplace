/*
 * TransportCalc.kt
 * Este archivo define los datos del calculo de transporte.
 * Contiene el resultado del calculo (coste, distancia, dias) y
 * los datos que el usuario envia para pedir ese calculo.
 */
package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

// Resultado del calculo de transporte que devuelve el servidor
data class TransportCalc(
    @SerializedName("distance_km") val distanceKm: Double,       // Distancia en kilometros
    @SerializedName("base_cost") val baseCost: Double,             // Coste base del envio
    @SerializedName("weight_surcharge") val weightSurcharge: Double, // Recargo por peso
    @SerializedName("urgent_surcharge") val urgentSurcharge: Double, // Recargo por urgencia
    @SerializedName("total") val total: Double,                    // Precio total del transporte
    @SerializedName("estimated_days") val estimatedDays: Int = 1,  // Dias estimados de entrega
    @SerializedName("carrier_name") val carrierName: String? = null // Nombre del transportista
)

// Datos que envia la app para pedir un calculo de transporte
data class TransportRequest(
    @SerializedName("lat") val lat: Double,           // Latitud del punto de entrega
    @SerializedName("lon") val lon: Double,           // Longitud del punto de entrega
    @SerializedName("weight_kg") val weightKg: Double, // Peso total en kilogramos
    @SerializedName("is_urgent") val isUrgent: Boolean = false, // Si el envio es urgente
    @SerializedName("obra_id") val obraId: Int? = null // Obra a la que va dirigido (opcional)
)
