package com.construccion.marketplace.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.construccion.marketplace.data.model.LoyaltyLevel
import com.construccion.marketplace.data.model.PartnerType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_FILE = "construapp_secure_prefs"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_LOGIN = "user_login"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_LOYALTY_POINTS = "loyalty_points"
        private const val KEY_LOYALTY_LEVEL = "loyalty_level"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val VALUE_NOT_SET = ""
        private const val VALUE_ID_NOT_SET = -1
    }

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
    // Guardar sesión
    // -------------------------------------------------------------------------

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
    // Leer datos de sesión
    // -------------------------------------------------------------------------

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
    // Actualizar puntos de fidelización
    // -------------------------------------------------------------------------

    fun updateLoyaltyPoints(points: Int, level: LoyaltyLevel) {
        prefs.edit()
            .putInt(KEY_LOYALTY_POINTS, points)
            .putString(KEY_LOYALTY_LEVEL, level.name)
            .apply()
    }

    // -------------------------------------------------------------------------
    // Cerrar sesión
    // -------------------------------------------------------------------------

    fun logout() {
        prefs.edit().clear().apply()
    }
}
