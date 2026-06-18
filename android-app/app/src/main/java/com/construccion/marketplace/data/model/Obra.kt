package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

/**
 * Representa una obra de construcción registrada en Odoo.
 *
 * Cada usuario (empresa constructora o particular) puede tener
 * varias obras; los pedidos de material se asocian a una obra
 * para tener trazabilidad de dónde se destinan los materiales.
 */
data class Obra(
    // Identificador único de la obra en Odoo
    @SerializedName("id") val id: Int,
    // Nombre descriptivo de la obra (ej. "Reforma cocina C/ Mayor 12")
    @SerializedName("name") val name: String,
    // Código de referencia generado automáticamente (secuencia OBR/XXXX)
    @SerializedName("code") val code: String,
    // Dirección física de la obra
    @SerializedName("address") val address: String,
    // Estado actual del flujo de la obra
    @SerializedName("state") val state: ObraState,
    // Nombre del partner (cliente) propietario de la obra
    @SerializedName("partner_name") val partnerName: String,
    // Cantidad de solicitudes de material asociadas
    @SerializedName("material_request_count") val materialRequestCount: Int = 0,
    // Coordenadas GPS (opcionales) para cálculo de transporte
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    // Fechas de inicio y fin previstas (formato ISO)
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    // Descripción libre de la obra
    @SerializedName("description") val description: String? = null
)

/**
 * Petición para crear una nueva obra desde la app.
 * Solo se necesita el nombre; la dirección es opcional.
 */
data class ObraCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String? = null
)

/**
 * Estados posibles de una obra en su ciclo de vida.
 * El label en español se muestra directamente en la UI.
 */
enum class ObraState(val label: String) {
    @SerializedName("draft") DRAFT("Borrador"),
    @SerializedName("active") ACTIVE("En curso"),
    @SerializedName("paused") PAUSED("Pausada"),
    @SerializedName("done") DONE("Finalizada"),
    @SerializedName("cancelled") CANCELLED("Cancelada")
}
