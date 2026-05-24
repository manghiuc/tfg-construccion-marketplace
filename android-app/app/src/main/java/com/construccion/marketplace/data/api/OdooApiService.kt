package com.construccion.marketplace.data.api

import com.construccion.marketplace.data.model.ApiResponse
import com.construccion.marketplace.data.model.CalculatorResultOdoo
import com.construccion.marketplace.data.model.ChatbotRequest
import com.construccion.marketplace.data.model.ChatbotResponse
import com.construccion.marketplace.data.model.LoginRequest
import com.construccion.marketplace.data.model.LoginResponse
import com.construccion.marketplace.data.model.LoyaltyStatus
import com.construccion.marketplace.data.model.MaterialRequest
import com.construccion.marketplace.data.model.MaterialRequestCreateRequest
import com.construccion.marketplace.data.model.Obra
import com.construccion.marketplace.data.model.ObraCreateRequest
import com.construccion.marketplace.data.model.Product
import com.construccion.marketplace.data.model.RedeemRequest
import com.construccion.marketplace.data.model.RegisterRequest
import com.construccion.marketplace.data.model.TransportCalc
import com.construccion.marketplace.data.model.TransportRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OdooApiService {

    // -------------------------------------------------------------------------
    // Autenticación
    // -------------------------------------------------------------------------

    /**
     * Iniciar sesión con login/password de Odoo.
     * Devuelve sessionId y datos del usuario.
     */
    @POST("${AppConfig.API_PREFIX}/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    /**
     * Registro de nuevo usuario (particular / autónomo / empresa).
     */
    @POST("${AppConfig.API_PREFIX}/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<LoginResponse>>

    /**
     * Cerrar sesión y destruir la cookie de Odoo.
     */
    @POST("${AppConfig.API_PREFIX}/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    // -------------------------------------------------------------------------
    // Obras
    // -------------------------------------------------------------------------

    /**
     * Obtener lista de obras del usuario autenticado.
     */
    @GET("${AppConfig.API_PREFIX}/obras")
    suspend fun getObras(): Response<ApiResponse<List<Obra>>>

    /**
     * Detalle de una obra concreta.
     */
    @GET("${AppConfig.API_PREFIX}/obras/{id}")
    suspend fun getObra(
        @Path("id") obraId: Int
    ): Response<ApiResponse<Obra>>

    /**
     * Crear una nueva obra.
     */
    @POST("${AppConfig.API_PREFIX}/obras")
    suspend fun createObra(
        @Body request: ObraCreateRequest
    ): Response<ApiResponse<Obra>>

    // -------------------------------------------------------------------------
    // Catálogo / Productos
    // -------------------------------------------------------------------------

    /**
     * Buscar productos del catálogo.
     * @param search texto libre de búsqueda (opcional)
     * @param categoryId filtrar por categoría (opcional)
     * @param page página de resultados (base 0)
     * @param pageSize tamaño de página
     */
    @GET("${AppConfig.API_PREFIX}/products")
    suspend fun getProducts(
        @Query("search") search: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): Response<ApiResponse<List<Product>>>

    /**
     * Detalle de un producto por ID.
     */
    @GET("${AppConfig.API_PREFIX}/products/{id}")
    suspend fun getProduct(
        @Path("id") productId: Int
    ): Response<ApiResponse<Product>>

    // -------------------------------------------------------------------------
    // Solicitudes de material
    // -------------------------------------------------------------------------

    /**
     * Crear una nueva solicitud de material (pedido).
     */
    @POST("${AppConfig.API_PREFIX}/material_request")
    suspend fun createMaterialRequest(
        @Body request: MaterialRequestCreateRequest
    ): Response<ApiResponse<MaterialRequest>>

    /**
     * Consultar el estado y tracking de una solicitud concreta.
     */
    @GET("${AppConfig.API_PREFIX}/material_request/{id}/status")
    suspend fun getMaterialRequestStatus(
        @Path("id") requestId: Int
    ): Response<ApiResponse<MaterialRequest>>

    /**
     * Listar todas las solicitudes del usuario.
     */
    @GET("${AppConfig.API_PREFIX}/material_request")
    suspend fun getMaterialRequests(
        @Query("state") state: String? = null,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): Response<ApiResponse<List<MaterialRequest>>>

    // -------------------------------------------------------------------------
    // Transporte
    // -------------------------------------------------------------------------

    /**
     * Calcular el coste de transporte según ubicación y peso.
     * @param lat latitud del punto de entrega
     * @param lon longitud del punto de entrega
     * @param weightKg peso total en kilogramos
     * @param isUrgent si el envío es urgente (recargo +50%)
     */
    @POST("${AppConfig.API_PREFIX}/calculate_transport")
    suspend fun calculateTransport(
        @Body request: TransportRequest
    ): Response<ApiResponse<TransportCalc>>

    // -------------------------------------------------------------------------
    // Calculadora de materiales
    // -------------------------------------------------------------------------

    /**
     * Calcular materiales necesarios según tipo de obra y m².
     * @param type tipo de construcción (ej. "tabique", "solera", "cubierta")
     * @param m2 superficie en metros cuadrados
     */
    @GET("${AppConfig.API_PREFIX}/catalog/calculator")
    suspend fun calculateMaterials(
        @Query("type") type: String,
        @Query("m2") m2: Double
    ): Response<ApiResponse<CalculatorResultOdoo>>

    // -------------------------------------------------------------------------
    // Chatbot
    // -------------------------------------------------------------------------

    /**
     * Enviar mensaje al chatbot de asistencia.
     */
    @POST("${AppConfig.API_PREFIX}/chatbot")
    suspend fun sendChatMessage(
        @Body request: ChatbotRequest
    ): Response<ApiResponse<ChatbotResponse>>

    // -------------------------------------------------------------------------
    // Recomendaciones
    // -------------------------------------------------------------------------

    /**
     * Obtener productos recomendados para el usuario.
     * @param limit número máximo de recomendaciones
     */
    @GET("${AppConfig.API_PREFIX}/recommendations")
    suspend fun getRecommendations(
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<List<Product>>>

    // -------------------------------------------------------------------------
    // Programa de fidelización
    // -------------------------------------------------------------------------

    /**
     * Obtener estado del programa de fidelización del usuario.
     */
    @GET("${AppConfig.API_PREFIX}/loyalty/status")
    suspend fun getLoyaltyStatus(): Response<ApiResponse<LoyaltyStatus>>

    /**
     * Canjear puntos de fidelización.
     * Odoo espera un JSON body con {"points_to_redeem": X}.
     */
    @POST("${AppConfig.API_PREFIX}/loyalty/redeem")
    suspend fun redeemLoyaltyPoints(
        @Body request: RedeemRequest
    ): Response<ApiResponse<LoyaltyStatus>>
}
