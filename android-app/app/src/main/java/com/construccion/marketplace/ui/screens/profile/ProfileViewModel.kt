package com.construccion.marketplace.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccion.marketplace.data.model.LoyaltyLevel
import com.construccion.marketplace.data.model.PartnerType
import com.construccion.marketplace.data.repository.AuthRepository
import com.construccion.marketplace.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de perfil.
 * Lee los datos del usuario desde [SessionManager] (guardados al iniciar sesión).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    val userName: String get() = sessionManager.getUserName().ifBlank { "Usuario" }
    val userLogin: String get() = sessionManager.getUserLogin()
    val userType: PartnerType get() = sessionManager.getUserType()
    val loyaltyPoints: Int get() = sessionManager.getLoyaltyPoints()
    val loyaltyLevel: LoyaltyLevel get() = sessionManager.getLoyaltyLevel()

    /** Cierra sesión en el servidor y limpia la sesión local. */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
