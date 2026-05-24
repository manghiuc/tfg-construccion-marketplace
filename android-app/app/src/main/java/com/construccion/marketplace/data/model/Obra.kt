package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

data class Obra(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("address") val address: String,
    @SerializedName("state") val state: ObraState,
    @SerializedName("partner_name") val partnerName: String,
    @SerializedName("material_request_count") val materialRequestCount: Int = 0,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("description") val description: String? = null
)

data class ObraCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String? = null
)

enum class ObraState(val label: String) {
    @SerializedName("draft") DRAFT("Borrador"),
    @SerializedName("active") ACTIVE("En curso"),
    @SerializedName("paused") PAUSED("Pausada"),
    @SerializedName("done") DONE("Finalizada"),
    @SerializedName("cancelled") CANCELLED("Cancelada")
}
