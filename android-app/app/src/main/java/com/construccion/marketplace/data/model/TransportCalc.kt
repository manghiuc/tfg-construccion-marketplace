package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

data class TransportCalc(
    @SerializedName("distance_km") val distanceKm: Double,
    @SerializedName("base_cost") val baseCost: Double,
    @SerializedName("weight_surcharge") val weightSurcharge: Double,
    @SerializedName("urgent_surcharge") val urgentSurcharge: Double,
    @SerializedName("total") val total: Double,
    @SerializedName("estimated_days") val estimatedDays: Int = 1,
    @SerializedName("carrier_name") val carrierName: String? = null
)

data class TransportRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("is_urgent") val isUrgent: Boolean = false,
    @SerializedName("obra_id") val obraId: Int? = null
)
