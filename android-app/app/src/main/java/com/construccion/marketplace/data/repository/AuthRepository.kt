package com.construccion.marketplace.data.repository

import com.construccion.marketplace.data.api.OdooApiService
import com.construccion.marketplace.data.model.LoginRequest
import com.construccion.marketplace.data.model.LoyaltyLevel
import com.construccion.marketplace.data.model.RegisterRequest
import com.construccion.marketplace.data.model.PartnerType
import com.construccion.marketplace.data.model.User
import com.construccion.marketplace.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Datos necesarios para registrar un nuevo usuario.
 */
data class RegisterData(
    val name: String,
    val email: String,
    val password: String,
    val partnerType: PartnerType,
    val phone: String? = null,
    val companyName: String? = null,
    val vat: String? = null
)

/**
 * Repositorio de autenticación.
 *
 * Encapsula las llamadas a la API de Odoo para login / registro y
 * persiste la sesión mediante [SessionManager].
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: OdooApiService,
    private val sessionManager: SessionManager
) {

    /**
     * Inicia sesión con las credenciales proporcionadas.
     *
     * @param email correo electrónico del usuario (usado como login en Odoo)
     * @param password contraseña en texto plano (enviada por HTTPS)
     * @return [Result.success] con el [User] autenticado,
     *         o [Result.failure] con el mensaje de error.
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = apiService.login(
                LoginRequest(login = email, password = password)
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val loginResponse = body.data
                    val user = loginResponse.user

                    // Persistir la sesión de forma cifrada
                    sessionManager.saveSession(
                        sessionId = user.sessionId,
                        userId = user.id,
                        userName = user.name,
                        userLogin = user.login,
                        userType = user.partnerType,
                        loyaltyPoints = user.pointsBalance,
                        loyaltyLevel = user.loyaltyLevel ?: LoyaltyLevel.BRONCE,
                        accessToken = loginResponse.accessToken
                    )

                    Result.success(user)
                } else {
                    Result.failure(Exception(body?.message ?: "Credenciales incorrectas"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Credenciales incorrectas"
                    403 -> "Acceso denegado"
                    404 -> "Usuario no encontrado"
                    500 -> "Error del servidor. Inténtalo más tarde"
                    else -> "Error ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }

    /**
     * Registra un nuevo usuario en Odoo.
     *
     * @param registerData datos del nuevo usuario
     * @return [Result.success] con el [User] creado y sesión iniciada,
     *         o [Result.failure] con el mensaje de error.
     */
    suspend fun register(registerData: RegisterData): Result<User> {
        return try {
            val request = RegisterRequest(
                name = registerData.name,
                login = registerData.email,
                password = registerData.password,
                partnerType = registerData.partnerType,
                phone = registerData.phone,
                companyName = registerData.companyName,
                vat = registerData.vat
            )

            val response = apiService.register(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val loginResponse = body.data
                    val user = loginResponse.user

                    sessionManager.saveSession(
                        sessionId = user.sessionId,
                        userId = user.id,
                        userName = user.name,
                        userLogin = user.login,
                        userType = user.partnerType,
                        loyaltyPoints = user.pointsBalance,
                        loyaltyLevel = user.loyaltyLevel ?: LoyaltyLevel.BRONCE,
                        accessToken = loginResponse.accessToken
                    )

                    Result.success(user)
                } else {
                    Result.failure(Exception(body?.message ?: "Error al crear la cuenta"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    409 -> "Ya existe una cuenta con ese correo electrónico"
                    422 -> "Datos inválidos. Revisa el formulario"
                    500 -> "Error del servidor. Inténtalo más tarde"
                    else -> "Error ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }

    /**
     * Cierra la sesión local y notifica al backend.
     * No falla aunque el backend no responda (la sesión local se elimina siempre).
     */
    suspend fun logout() {
        try {
            apiService.logout()
        } catch (_: Exception) {
            // El logout remoto es best-effort: siempre limpiamos la sesión local
        } finally {
            sessionManager.logout()
        }
    }

    /** Indica si hay una sesión activa almacenada localmente. */
    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
}
