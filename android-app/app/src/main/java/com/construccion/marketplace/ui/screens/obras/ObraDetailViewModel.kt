package com.construccion.marketplace.ui.screens.obras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccion.marketplace.data.api.OdooApiService
import com.construccion.marketplace.data.model.Obra
import com.construccion.marketplace.data.model.ObraState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estados posibles de la pantalla de detalle de obra. */
sealed class ObraDetailUiState {
    object Loading : ObraDetailUiState()
    data class Success(val obra: Obra) : ObraDetailUiState()
    data class Error(val message: String) : ObraDetailUiState()
}

/** Datos mock para que la demo funcione sin conexión al backend. */
private fun mockObra(id: Int) = Obra(
    id = id,
    name = "Reforma Integral Vivienda Madrid",
    code = "OBR-2025-00$id",
    address = "C/ Gran Vía 45, 2ºA, Madrid",
    state = ObraState.ACTIVE,
    partnerName = "Construcciones García S.L.",
    materialRequestCount = 7,
    latitude = 40.4168,
    longitude = -3.7038,
    startDate = "2025-03-01",
    endDate = "2025-09-30",
    description = "Reforma integral de vivienda de 90m². Incluye demolición de tabiques, instalación eléctrica, fontanería, solados y pinturas."
)

/**
 * ViewModel del detalle de una obra.
 *
 * Carga la información completa de la obra desde la API de Odoo.
 * Si falla la conexión, muestra datos mock para que la demo funcione.
 */
@HiltViewModel
class ObraDetailViewModel @Inject constructor(
    private val apiService: OdooApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ObraDetailUiState>(ObraDetailUiState.Loading)
    val uiState: StateFlow<ObraDetailUiState> = _uiState.asStateFlow()

    fun loadObra(obraId: Int) {
        viewModelScope.launch {
            _uiState.value = ObraDetailUiState.Loading
            try {
                val response = apiService.getObra(obraId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val obra = response.body()?.data
                    _uiState.value = if (obra != null) {
                        ObraDetailUiState.Success(obra)
                    } else {
                        ObraDetailUiState.Error("Obra no encontrada")
                    }
                } else {
                    // Fallback a mock para que la demo funcione sin red
                    _uiState.value = ObraDetailUiState.Success(mockObra(obraId))
                }
            } catch (e: Exception) {
                // Sin conexión: mostrar mock para que la demo funcione
                _uiState.value = ObraDetailUiState.Success(mockObra(obraId))
            }
        }
    }
}
