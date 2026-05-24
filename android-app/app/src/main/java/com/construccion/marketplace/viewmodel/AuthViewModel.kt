package com.construccion.marketplace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccion.marketplace.data.model.PartnerType
import com.construccion.marketplace.data.model.User
import com.construccion.marketplace.data.repository.AuthRepository
import com.construccion.marketplace.data.repository.RegisterData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estados posibles de la UI de autenticación.
 */
sealed class AuthUiState {
    /** Estado inicial antes de cualquier interacción. */
    data object Idle : AuthUiState()

    /** Operación en curso (login / registro / logout). */
    data object Loading : AuthUiState()

    /** Autenticación completada correctamente. */
    data class Success(val user: User) : AuthUiState()

    /** Ha ocurrido un error. El campo [message] describe el problema. */
    data class Error(val message: String) : AuthUiState()
}

/**
 * ViewModel de autenticación.
 *
 * Expone un [StateFlow] de [AuthUiState] que las pantallas de Login
 * y Registro observan para reaccionar a los cambios de estado.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    /**
     * Inicia sesión con email y contraseña.
     *
     * Actualiza [uiState] a [AuthUiState.Loading] durante la operación y
     * a [AuthUiState.Success] o [AuthUiState.Error] según el resultado.
     *
     * @param email correo electrónico del usuario
     * @param password contraseña en texto plano
     */
    fun login(email: String, password: String) {
        if (!validateLoginInputs(email, password)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.login(email.trim(), password)

            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { error -> AuthUiState.Error(error.message ?: "Error desconocido") }
            )
        }
    }

    // -------------------------------------------------------------------------
    // Registro
    // -------------------------------------------------------------------------

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param name nombre completo del usuario
     * @param email correo electrónico (también sirve como login)
     * @param password contraseña elegida
     * @param partnerType tipo de cliente: PARTICULAR, AUTONOMO o EMPRESA
     * @param phone teléfono de contacto (opcional)
     * @param companyName nombre de la empresa (obligatorio si partnerType == EMPRESA)
     * @param vat CIF o NIF (opcional)
     */
    fun register(
        name: String,
        email: String,
        password: String,
        partnerType: PartnerType,
        phone: String? = null,
        companyName: String? = null,
        vat: String? = null
    ) {
        if (!validateRegisterInputs(name, email, password, partnerType, companyName)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val registerData = RegisterData(
                name = name.trim(),
                email = email.trim(),
                password = password,
                partnerType = partnerType,
                phone = phone?.trim()?.takeIf { it.isNotBlank() },
                companyName = companyName?.trim()?.takeIf { it.isNotBlank() },
                vat = vat?.trim()?.takeIf { it.isNotBlank() }
            )

            val result = authRepository.register(registerData)

            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { error -> AuthUiState.Error(error.message ?: "Error desconocido") }
            )
        }
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    /**
     * Cierra la sesión del usuario actual.
     * La UI vuelve al estado [AuthUiState.Idle] tras cerrar sesión.
     */
    fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    // -------------------------------------------------------------------------
    // Control de estado
    // -------------------------------------------------------------------------

    /**
     * Restablece el estado a [AuthUiState.Idle].
     * Útil para limpiar errores tras mostrarlos al usuario.
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    /** Indica si hay una sesión activa almacenada localmente. */
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    // -------------------------------------------------------------------------
    // Validaciones de entrada
    // -------------------------------------------------------------------------

    private fun validateLoginInputs(email: String, password: String): Boolean {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("El correo electrónico es obligatorio")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState.Error("Introduce un correo electrónico válido")
            return false
        }
        if (password.isBlank()) {
            _uiState.value = AuthUiState.Error("La contraseña es obligatoria")
            return false
        }
        return true
    }

    private fun validateRegisterInputs(
        name: String,
        email: String,
        password: String,
        partnerType: PartnerType,
        companyName: String?
    ): Boolean {
        if (name.isBlank()) {
            _uiState.value = AuthUiState.Error("El nombre es obligatorio")
            return false
        }
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState.Error("Introduce un correo electrónico válido")
            return false
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("La contraseña debe tener al menos 6 caracteres")
            return false
        }
        if (partnerType == PartnerType.EMPRESA && companyName.isNullOrBlank()) {
            _uiState.value = AuthUiState.Error("El nombre de empresa es obligatorio para empresas")
            return false
        }
        return true
    }
}
