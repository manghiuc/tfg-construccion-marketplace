package com.construccion.marketplace.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo del usuario autenticado en Odoo.
 *
 * Contiene todos los datos que el servidor devuelve tras un login o registro exitoso.
 * @SerializedName mapea los nombres JSON del backend a las propiedades Kotlin
 * (ej: "partner_type" en JSON → partnerType en Kotlin).
 */
data class User(
    @SerializedName("id") val id: Int,                             // ID del res.users en Odoo
    @SerializedName("name") val name: String,                      // Nombre completo del usuario
    @SerializedName("login") val login: String,                    // Email usado como login
    @SerializedName("session_id") val sessionId: String,           // Cookie de sesión de Odoo
    @SerializedName("partner_type") val partnerType: PartnerType,  // Tipo de cliente
    @SerializedName("points_balance") val pointsBalance: Int = 0,  // Puntos de fidelidad acumulados
    @SerializedName("loyalty_level") val loyaltyLevel: LoyaltyLevel? = null, // Nivel (puede ser null si Odoo no lo calcula)
    @SerializedName("phone") val phone: String? = null,            // Teléfono de contacto
    @SerializedName("email") val email: String? = null,            // Email alternativo
    @SerializedName("company_name") val companyName: String? = null, // Nombre de empresa (si aplica)
    @SerializedName("vat") val vat: String? = null                 // CIF o NIF fiscal
)

/**
 * Tipos de cliente en el marketplace de construcción.
 *
 * Cada tipo tiene diferentes permisos y vistas en la app:
 * - PARTICULAR: cliente final, compra para reformas personales
 * - AUTONOMO: profesional autónomo, tiene acceso a "Mis obras"
 * - EMPRESA: empresa constructora, acceso a obras + panel admin
 *
 * @SerializedName asegura que Gson deserialice "particular" → PARTICULAR, etc.
 */
enum class PartnerType(val label: String) {
    @SerializedName("particular") PARTICULAR("Particular"),
    @SerializedName("autonomo") AUTONOMO("Autónomo"),
    @SerializedName("empresa") EMPRESA("Empresa")
}

/**
 * Niveles del programa de fidelización.
 *
 * Cada nivel tiene un umbral mínimo de puntos. El usuario sube de nivel
 * automáticamente al acumular suficientes puntos:
 * - BRONCE: 0+ puntos (nivel inicial)
 * - PLATA: 500+ puntos (descuento 3%)
 * - ORO: 2000+ puntos (descuento 5%)
 * - PLATINO: 5000+ puntos (descuento 10%)
 *
 * El companion object proporciona [fromPoints] para calcular el nivel
 * correcto a partir de los puntos, útil cuando el servidor no envía el nivel.
 */
enum class LoyaltyLevel(val label: String, val minPoints: Int) {
    @SerializedName("bronce") BRONCE("Bronce", 0),
    @SerializedName("plata") PLATA("Plata", 500),
    @SerializedName("oro") ORO("Oro", 2000),
    @SerializedName("platino") PLATINO("Platino", 5000);

    companion object {
        /**
         * Calcula el nivel de fidelidad correspondiente a una cantidad de puntos.
         *
         * Ordena los niveles de mayor a menor umbral y devuelve el primero
         * donde los puntos del usuario son >= al mínimo requerido.
         * Ejemplo: 1333 puntos → PLATINO(5000)? NO → ORO(2000)? NO → PLATA(500)? SÍ → PLATA
         */
        fun fromPoints(points: Int): LoyaltyLevel =
            entries.sortedByDescending { it.minPoints }.first { points >= it.minPoints }
    }
}

/**
 * Body JSON para la petición POST /auth/login.
 * El campo "login" es el email del usuario en Odoo.
 */
data class LoginRequest(
    @SerializedName("login") val login: String,      // Email del usuario
    @SerializedName("password") val password: String  // Contraseña en texto plano (viaja por HTTPS)
)

/**
 * Body JSON para la petición POST /auth/register.
 * Crea un nuevo res.partner + res.users en Odoo.
 */
data class RegisterRequest(
    @SerializedName("name") val name: String,                      // Nombre completo
    @SerializedName("login") val login: String,                    // Email (será el login)
    @SerializedName("password") val password: String,              // Contraseña elegida
    @SerializedName("partner_type") val partnerType: PartnerType,  // Tipo de cliente
    @SerializedName("phone") val phone: String? = null,            // Teléfono (opcional)
    @SerializedName("company_name") val companyName: String? = null, // Empresa (obligatorio si tipo=EMPRESA)
    @SerializedName("vat") val vat: String? = null                 // CIF/NIF (opcional)
)

/**
 * Respuesta del servidor tras login o registro exitoso.
 * Contiene el usuario completo y opcionalmente un access_token JWT.
 */
data class LoginResponse(
    @SerializedName("user") val user: User,                        // Datos completos del usuario
    @SerializedName("access_token") val accessToken: String? = null // Token JWT (si el módulo lo genera)
)
