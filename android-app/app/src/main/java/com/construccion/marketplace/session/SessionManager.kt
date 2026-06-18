package com.construccion.marketplace.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.construccion.marketplace.data.model.LoyaltyLevel
import com.construccion.marketplace.data.model.PartnerType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona la sesión del usuario de forma segura.
 *
 * Almacena las credenciales y datos de sesión usando
 * [EncryptedSharedPreferences] (cifrado AES-256) para que
 * el session_id de Odoo nunca quede en texto plano en disco.
 *
 * También expone un evento [sessionExpired] que los interceptores
 * de red emiten cuando Odoo devuelve 401, para que la UI
 * redirija automáticamente al login.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // SharedFlow que emite Unit cuando el servidor devuelve 401 (sesión expirada).
    // extraBufferCapacity = 1 evita que se pierda el evento si nadie está escuchando aún.
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    /**
     * Señala que la sesión ha expirado en el servidor.
     * Limpia los datos locales y emite el evento para que la UI redirija al login.
     */
    fun onSessionExpired() {
        if (isLoggedIn()) {
            logout()
            _sessionExpired.tryEmit(Unit)
        }
    }

    companion object {
        // Nombre del fichero de preferencias cifrado
        private const val PREFS_FILE = "construapp_secure_prefs"
        // Claves para cada campo almacenado en EncryptedSharedPreferences
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_LOGIN = "user_login"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_LOYALTY_POINTS = "loyalty_points"
        private const val KEY_LOYALTY_LEVEL = "loyalty_level"
        private const val KEY_ACCESS_TOKEN = "access_token"
        // Valores por defecto cuando no hay dato almacenado
        private const val VALUE_NOT_SET = ""
        private const val VALUE_ID_NOT_SET = -1
    }

    // Inicialización lazy: las EncryptedSharedPreferences se crean solo la primera
    // vez que se accede, usando una clave maestra AES-256 del Keystore de Android.
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // -------------------------------------------------------------------------
    // Guardar sesión — persiste todos los datos del usuario tras login/registro
    // -------------------------------------------------------------------------

    /**
     * Guarda todos los datos de sesión de forma cifrada.
     * Se llama tras un login o registro exitoso.
     */
    fun saveSession(
        sessionId: String,
        userId: Int,
        userName: String,
        userLogin: String,
        userType: PartnerType,
        loyaltyPoints: Int = 0,
        loyaltyLevel: LoyaltyLevel = LoyaltyLevel.BRONCE,
        accessToken: String? = null
    ) {
        prefs.edit()
            .putString(KEY_SESSION_ID, sessionId)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_USER_LOGIN, userLogin)
            .putString(KEY_USER_TYPE, userType.name)
            .putInt(KEY_LOYALTY_POINTS, loyaltyPoints)
            .putString(KEY_LOYALTY_LEVEL, loyaltyLevel.name)
            .putString(KEY_ACCESS_TOKEN, accessToken ?: VALUE_NOT_SET)
            .apply()
    }

    // -------------------------------------------------------------------------
    // Leer datos de sesión — getters para cada campo almacenado
    // -------------------------------------------------------------------------

    /** Devuelve true si hay un session_id guardado (no vacío). */
    fun isLoggedIn(): Boolean =
        prefs.getString(KEY_SESSION_ID, VALUE_NOT_SET)?.isNotBlank() == true

    fun getSessionId(): String =
        prefs.getString(KEY_SESSION_ID, VALUE_NOT_SET) ?: VALUE_NOT_SET

    fun getUserId(): Int =
        prefs.getInt(KEY_USER_ID, VALUE_ID_NOT_SET)

    fun getUserName(): String =
        prefs.getString(KEY_USER_NAME, VALUE_NOT_SET) ?: VALUE_NOT_SET

    fun getUserLogin(): String =
        prefs.getString(KEY_USER_LOGIN, VALUE_NOT_SET) ?: VALUE_NOT_SET

    /** Devuelve el tipo de partner (PARTICULAR o EMPRESA). Fallback: PARTICULAR. */
    fun getUserType(): PartnerType {
        val typeName = prefs.getString(KEY_USER_TYPE, PartnerType.PARTICULAR.name)
        return try {
            PartnerType.valueOf(typeName ?: PartnerType.PARTICULAR.name)
        } catch (e: IllegalArgumentException) {
            PartnerType.PARTICULAR
        }
    }

    fun getLoyaltyPoints(): Int =
        prefs.getInt(KEY_LOYALTY_POINTS, 0)

    /** Devuelve el nivel de fidelización guardado. Fallback: BRONCE. */
    fun getLoyaltyLevel(): LoyaltyLevel {
        val levelName = prefs.getString(KEY_LOYALTY_LEVEL, LoyaltyLevel.BRONCE.name)
        return try {
            LoyaltyLevel.valueOf(levelName ?: LoyaltyLevel.BRONCE.name)
        } catch (e: IllegalArgumentException) {
            LoyaltyLevel.BRONCE
        }
    }

    fun getAccessToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, VALUE_NOT_SET) ?: VALUE_NOT_SET
        return if (token.isBlank()) null else token
    }

    // -------------------------------------------------------------------------
    // Actualizar puntos de fidelización (se llama al refrescar desde el backend)
    // -------------------------------------------------------------------------

    /** Actualiza los puntos y el nivel de fidelización en las preferencias. */
    fun updateLoyaltyPoints(points: Int, level: LoyaltyLevel) {
        prefs.edit()
            .putInt(KEY_LOYALTY_POINTS, points)
            .putString(KEY_LOYALTY_LEVEL, level.name)
            .apply()
    }

    // -------------------------------------------------------------------------
    // Cerrar sesión — borra TODOS los datos cifrados
    // -------------------------------------------------------------------------

    /** Elimina todas las preferencias cifradas (session_id, token, datos de usuario). */
    fun logout() {
        prefs.edit().clear().apply()
    }
}
