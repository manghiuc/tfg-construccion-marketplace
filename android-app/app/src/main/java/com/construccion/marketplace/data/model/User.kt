/*
 * User.kt
 * Este archivo define los datos del usuario de la aplicacion.
 * Contiene la informacion personal, el tipo de cliente (particular,
 * autonomo o empresa), el sistema de fidelizacion (puntos y niveles),
 * y los datos necesarios para iniciar sesion y registrarse.
 */
package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

// Datos principales del usuario que llegan del servidor
data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("login") val login: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("partner_type") val partnerType: PartnerType,
    @SerializedName("points_balance") val pointsBalance: Int = 0,
    @SerializedName("loyalty_level") val loyaltyLevel: LoyaltyLevel? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("company_name") val companyName: String? = null,
    @SerializedName("vat") val vat: String? = null
)

enum class PartnerType(val label: String) {
    @SerializedName("particular") PARTICULAR("Particular"),
    @SerializedName("autonomo") AUTONOMO("Autónomo"),
    @SerializedName("empresa") EMPRESA("Empresa")
}

enum class LoyaltyLevel(val label: String, val minPoints: Int) {
    @SerializedName("bronce") BRONCE("Bronce", 0),
    @SerializedName("plata") PLATA("Plata", 500),
    @SerializedName("oro") ORO("Oro", 2000),
    @SerializedName("platino") PLATINO("Platino", 5000)
}

data class LoginRequest(
    @SerializedName("login") val login: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("login") val login: String,
    @SerializedName("password") val password: String,
    @SerializedName("partner_type") val partnerType: PartnerType,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("company_name") val companyName: String? = null,
    @SerializedName("vat") val vat: String? = null
)

data class LoginResponse(
    @SerializedName("user") val user: User,
    @SerializedName("access_token") val accessToken: String? = null
)
