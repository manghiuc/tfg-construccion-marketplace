package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

/**
 * Resultado del cálculo de transporte devuelto por el backend.
 *
 * El servidor calcula la distancia Haversine entre el almacén y el
 * punto de entrega, aplica recargos por peso y urgencia, y devuelve
 * el desglose completo para mostrarlo en la pantalla de checkout.
 */
data class TransportCalc(
    // Distancia desde el almacén al punto de entrega (fórmula Haversine)
    @SerializedName("distance_km") val distanceKm: Double,
    // Coste base según la distancia
    @SerializedName("base_cost") val baseCost: Double,
    // Recargo adicional por peso del pedido
    @SerializedName("weight_surcharge") val weightSurcharge: Double,
    // Recargo por urgencia (+50% si isUrgent = true)
    @SerializedName("urgent_surcharge") val urgentSurcharge: Double,
    // Coste total de transporte (base + peso + urgencia)
    @SerializedName("total") val total: Double,
    // Días estimados de entrega (mínimo 1)
    @SerializedName("estimated_days") val estimatedDays: Int = 1,
    // Nombre del transportista asignado (si aplica)
    @SerializedName("carrier_name") val carrierName: String? = null
)

/**
 * Petición que se envía al endpoint /api/transport/calculate.
 *
 * El backend necesita las coordenadas GPS del punto de entrega,
 * el peso total y si es urgente para calcular el coste.
 */
data class TransportRequest(
    // Latitud del punto de entrega
    @SerializedName("lat") val lat: Double,
    // Longitud del punto de entrega
    @SerializedName("lon") val lon: Double,
    // Peso total del pedido en kilogramos
    @SerializedName("weight_kg") val weightKg: Double,
    // Si es true, se aplica recargo de urgencia x1.5
    @SerializedName("is_urgent") val isUrgent: Boolean = false,
    // ID de la obra (opcional, para obtener la dirección predeterminada)
    @SerializedName("obra_id") val obraId: Int? = null
)
