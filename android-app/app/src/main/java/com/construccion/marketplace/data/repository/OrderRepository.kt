package com.construccion.marketplace.data.repository

import com.construccion.marketplace.data.api.OdooApiService
import com.construccion.marketplace.data.model.MaterialRequest
import com.construccion.marketplace.data.model.MaterialRequestCreateRequest
import com.construccion.marketplace.data.model.MaterialRequestLineCreate
import com.construccion.marketplace.data.model.TransportCalc
import com.construccion.marketplace.data.model.TransportRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cuerpo de la petición para crear una solicitud de material.
 *
 * Abstrae los detalles del modelo de red y ofrece una interfaz
 * más cómoda al ViewModel.
 */
data class CreateRequestBody(
    val obraId: Int?,
    val lines: List<OrderLine>,
    val deliveryAddress: String? = null,
    val isUrgent: Boolean = false,
    val deliveryLat: Double? = null,
    val deliveryLon: Double? = null,
    val transportCost: Double? = null,
    val notes: String? = null,
    val useLoyaltyPoints: Boolean = false,
    val loyaltyPointsAmount: Int = 0
)

data class OrderLine(
    val productId: Int,
    val qty: Double,
    val priceUnit: Double
)

/**
 * Repositorio de pedidos (solicitudes de material).
 *
 * Gestiona la creación de pedidos, consulta de historial,
 * seguimiento de estado y cálculo de transporte.
 */
@Singleton
class OrderRepository @Inject constructor(
    private val apiService: OdooApiService
) {

    /**
     * Crea una nueva solicitud de material en Odoo.
     *
     * @param requestBody datos del pedido a crear
     * @return [Result.success] con el [MaterialRequest] creado,
     *         o [Result.failure] con el mensaje de error.
     */
    suspend fun createOrder(requestBody: CreateRequestBody): Result<MaterialRequest> {
        return try {
            val request = MaterialRequestCreateRequest(
                obraId = requestBody.obraId,
                lines = requestBody.lines.map { line ->
                    MaterialRequestLineCreate(
                        productId = line.productId,
                        qty = line.qty,
                        priceUnit = line.priceUnit
                    )
                },
                deliveryAddress = requestBody.deliveryAddress,
                isUrgent = requestBody.isUrgent,
                deliveryLat = requestBody.deliveryLat,
                deliveryLon = requestBody.deliveryLon,
                transportCost = requestBody.transportCost,
                notes = requestBody.notes,
                useLoyaltyPoints = requestBody.useLoyaltyPoints,
                loyaltyPointsAmount = requestBody.loyaltyPointsAmount
            )

            val response = apiService.createMaterialRequest(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "Error al crear el pedido"))
                }
            } else {
                // Intentar leer el mensaje real de error del servidor
                val serverMsg = try {
                    val errBody = response.errorBody()?.string() ?: ""
                    val json = com.google.gson.JsonParser.parseString(errBody).asJsonObject
                    json.getAsJsonObject("error")?.get("message")?.asString
                        ?: json.get("message")?.asString
                } catch (_: Exception) { null }

                val errorMsg = serverMsg ?: when (response.code()) {
                    400 -> "Datos del pedido inválidos"
                    401 -> "Sesión expirada. Inicia sesión de nuevo"
                    403 -> "No tienes permiso para crear pedidos en esta obra"
                    404 -> "Obra no encontrada"
                    422 -> "Revisa las líneas del pedido"
                    500 -> "Error del servidor. Inténtalo más tarde"
                    else -> "Error ${response.code()} al crear el pedido"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }

    /**
     * Obtiene la lista de pedidos del usuario autenticado.
     *
     * @param obraId filtrar por obra (null = todas las obras)
     * @param state filtrar por estado (null = todos los estados)
     *              Valores válidos: "draft", "confirmed", "approved",
     *              "in_progress", "shipped", "delivered", "cancelled"
     * @return [Result.success] con la lista de [MaterialRequest],
     *         o [Result.failure] con el mensaje de error.
     */
    suspend fun getOrders(
        obraId: Int? = null,
        state: String? = null
    ): Result<List<MaterialRequest>> {
        return try {
            val response = apiService.getMaterialRequests(
                state = state,
                page = 0,
                pageSize = 50
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    var orders = body.data ?: emptyList()
                    // Filtrado local por obraId si el backend no lo soporta directamente
                    if (obraId != null) {
                        orders = orders.filter { it.obraId == obraId }
                    }
                    Result.success(orders)
                } else {
                    Result.failure(Exception(body?.message ?: "Error al cargar los pedidos"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Sesión expirada. Inicia sesión de nuevo"
                    403 -> "No tienes permiso para ver los pedidos"
                    500 -> "Error del servidor. Inténtalo más tarde"
                    else -> "Error ${response.code()} al obtener pedidos"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }

    /**
     * Obtiene el estado actualizado y la información de seguimiento de un pedido.
     *
     * @param id identificador del pedido en Odoo
     * @return [Result.success] con el [MaterialRequest] actualizado,
     *         o [Result.failure] con el mensaje de error.
     */
    suspend fun getOrderStatus(id: Int): Result<MaterialRequest> {
        return try {
            val response = apiService.getMaterialRequestStatus(id)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "Pedido no encontrado"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Pedido no encontrado"
                    403 -> "No tienes permiso para ver este pedido"
                    else -> "Error ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }

    /**
     * Calcula el coste de transporte para una entrega.
     *
     * Utiliza la fórmula Haversine en el backend para calcular la distancia
     * real y aplica las tarifas correspondientes.
     * El coste mínimo de transporte es 15 €.
     *
     * @param lat latitud del punto de entrega
     * @param lon longitud del punto de entrega
     * @param weightKg peso total del pedido en kilogramos
     * @param isUrgent si true, se aplica un recargo del +50% sobre el transporte
     * @return [Result.success] con el [TransportCalc] detallado,
     *         o [Result.failure] con el mensaje de error.
     */
    suspend fun calculateTransport(
        lat: Double,
        lon: Double,
        weightKg: Float,
        isUrgent: Boolean
    ): Result<TransportCalc> {
        return try {
            val response = apiService.calculateTransport(
                TransportRequest(
                    lat = lat,
                    lon = lon,
                    weightKg = weightKg.toDouble(),
                    isUrgent = isUrgent
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "Error al calcular el transporte"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()} al calcular transporte"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }
}
