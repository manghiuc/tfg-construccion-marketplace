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

/**
 * Interfaz Retrofit que define TODOS los endpoints REST del módulo
 * construction_marketplace de Odoo 17.
 *
 * Retrofit genera automáticamente la implementación de esta interfaz.
 * Cada método es una función suspendida (coroutine) que hace una llamada HTTP
 * y devuelve Response<ApiResponse<T>> donde T es el tipo de datos esperado.
 *
 * Todas las rutas usan el prefijo [AppConfig.API_PREFIX] = "/api/construction".
 * La autenticación se gestiona vía cookie session_id (inyectada por NetworkModule).
 */
interface OdooApiService {

    // =========================================================================
    // AUTENTICACIÓN - Login, registro y logout de usuarios
    // =========================================================================

    /**
     * Inicia sesión en Odoo con email y contraseña.
     *
     * @param request objeto con login (email) y password
     * @return LoginResponse con los datos del usuario (id, nombre, tipo, puntos...)
     *         y el session_id que se usa en todas las peticiones posteriores
     */
    @POST("${AppConfig.API_PREFIX}/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    /**
     * Registra un nuevo usuario en Odoo.
     * Crea el res.partner y el res.users, e inicia sesión automáticamente.
     *
     * @param request datos del nuevo usuario (nombre, email, password, tipo, teléfono...)
     * @return LoginResponse igual que en login (el usuario ya queda autenticado)
     */
    @POST("${AppConfig.API_PREFIX}/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<LoginResponse>>

    /**
     * Cierra la sesión actual en Odoo y destruye la cookie session_id.
     * Después de esto, las peticiones autenticadas devolverán error 401.
     */
    @POST("${AppConfig.API_PREFIX}/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    // =========================================================================
    // OBRAS - Gestión de proyectos/obras de construcción del usuario
    // =========================================================================

    /**
     * Obtiene todas las obras del usuario autenticado.
     * Solo devuelve las obras donde el usuario es el propietario (partner_id).
     *
     * @return lista de [Obra] con id, nombre, dirección, estado, fechas, etc.
     */
    @GET("${AppConfig.API_PREFIX}/obras")
    suspend fun getObras(): Response<ApiResponse<List<Obra>>>

    /**
     * Obtiene el detalle completo de una obra concreta, incluyendo
     * las líneas de pedido y materiales asociados.
     *
     * @param obraId identificador de la obra en Odoo
     */
    @GET("${AppConfig.API_PREFIX}/obras/{id}")
    suspend fun getObra(
        @Path("id") obraId: Int
    ): Response<ApiResponse<Obra>>

    /**
     * Crea una nueva obra en Odoo asociada al usuario actual.
     * Necesita al menos nombre y dirección.
     *
     * @param request datos de la nueva obra (nombre, dirección, fechas, etc.)
     */
    @POST("${AppConfig.API_PREFIX}/obras")
    suspend fun createObra(
        @Body request: ObraCreateRequest
    ): Response<ApiResponse<Obra>>

    // =========================================================================
    // CATÁLOGO / PRODUCTOS - Consulta del inventario de materiales de construcción
    // =========================================================================

    /**
     * Busca productos en el catálogo del marketplace.
     *
     * Soporta búsqueda por texto libre (nombre, código, tags) y filtro por categoría.
     * Los resultados están paginados para no cargar miles de productos de golpe.
     *
     * @param search texto libre de búsqueda (ej. "cemento portland"). Null = sin filtro
     * @param categoryId ID de la categoría de producto en Odoo. Null = todas
     * @param page número de página (empieza en 0)
     * @param pageSize cuántos productos por página (por defecto 20)
     * @return lista paginada de [Product]
     */
    @GET("${AppConfig.API_PREFIX}/products")
    suspend fun getProducts(
        @Query("search") search: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): Response<ApiResponse<List<Product>>>

    /**
     * Obtiene el detalle completo de un producto por su ID.
     * Incluye descripción larga, imágenes, stock, peso, marca, tags, etc.
     *
     * @param productId ID del product.template en Odoo
     */
    @GET("${AppConfig.API_PREFIX}/products/{id}")
    suspend fun getProduct(
        @Path("id") productId: Int
    ): Response<ApiResponse<Product>>

    // =========================================================================
    // SOLICITUDES DE MATERIAL - Pedidos de compra (equivale al carrito → pedido)
    // =========================================================================

    /**
     * Crea una nueva solicitud de material (pedido de compra) en Odoo.
     * Se llama desde el checkout cuando el usuario confirma su pedido.
     * Crea un registro en el modelo personalizado construction.material.request.
     *
     * @param request líneas del pedido (productos, cantidades, obra destino, urgencia, etc.)
     */
    @POST("${AppConfig.API_PREFIX}/material_request")
    suspend fun createMaterialRequest(
        @Body request: MaterialRequestCreateRequest
    ): Response<ApiResponse<MaterialRequest>>

    /**
     * Consulta el estado actual y datos de tracking de una solicitud de material.
     * Útil para que el usuario vea si su pedido está pendiente, en preparación,
     * en reparto o entregado.
     *
     * @param requestId ID de la solicitud en Odoo
     */
    @GET("${AppConfig.API_PREFIX}/material_request/{id}/status")
    suspend fun getMaterialRequestStatus(
        @Path("id") requestId: Int
    ): Response<ApiResponse<MaterialRequest>>

    /**
     * Lista todas las solicitudes de material del usuario autenticado.
     * Se usa en la pantalla de historial de pedidos.
     *
     * @param state filtrar por estado (ej. "draft", "confirmed", "done"). Null = todos
     * @param page página de resultados (base 0)
     * @param pageSize productos por página
     */
    @GET("${AppConfig.API_PREFIX}/material_request")
    suspend fun getMaterialRequests(
        @Query("state") state: String? = null,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): Response<ApiResponse<List<MaterialRequest>>>

    // =========================================================================
    // TRANSPORTE - Cálculo de costes de envío
    // =========================================================================

    /**
     * Calcula el coste de transporte para un pedido.
     *
     * El backend calcula la distancia desde el almacén hasta el punto de entrega,
     * aplica tarifas por peso y, si es urgente, añade un recargo del 50%.
     * Si el total de materiales supera 1000€, el transporte es gratuito.
     *
     * @param request coordenadas GPS del punto de entrega, peso total, urgencia
     * @return [TransportCalc] con distancia, coste base, recargo y total
     */
    @POST("${AppConfig.API_PREFIX}/calculate_transport")
    suspend fun calculateTransport(
        @Body request: TransportRequest
    ): Response<ApiResponse<TransportCalc>>

    // =========================================================================
    // CALCULADORA DE MATERIALES - Estima materiales necesarios para una obra
    // =========================================================================

    /**
     * Calcula los materiales necesarios según el tipo de construcción y la superficie.
     *
     * El módulo Odoo tiene tablas con ratios de consumo por m² para cada tipo
     * de obra (tabique, solera, cubierta, etc.) y devuelve la lista de materiales
     * con las cantidades exactas.
     *
     * @param type tipo de construcción (ej. "tabique", "solera", "cubierta", "forjado")
     * @param m2 superficie en metros cuadrados
     * @return [CalculatorResultOdoo] con lista de materiales, cantidades y unidades
     */
    @GET("${AppConfig.API_PREFIX}/catalog/calculator")
    suspend fun calculateMaterials(
        @Query("type") type: String,
        @Query("m2") m2: Double
    ): Response<ApiResponse<CalculatorResultOdoo>>

    // =========================================================================
    // CHATBOT - Asistente virtual con IA (Ollama)
    // =========================================================================

    /**
     * Envía un mensaje al chatbot de asistencia de ConstruApp.
     *
     * El backend reenvía el mensaje a Ollama (LLM local) con contexto
     * del catálogo de productos para dar recomendaciones relevantes.
     * Puede devolver sugerencias de texto y productos relacionados.
     *
     * @param request mensaje del usuario + session_id del chat + contexto adicional
     * @return respuesta del bot con texto, sugerencias rápidas y productos recomendados
     */
    @POST("${AppConfig.API_PREFIX}/chatbot")
    suspend fun sendChatMessage(
        @Body request: ChatbotRequest
    ): Response<ApiResponse<ChatbotResponse>>

    // =========================================================================
    // RECOMENDACIONES - Productos sugeridos para el usuario
    // =========================================================================

    /**
     * Obtiene una lista de productos recomendados personalizados para el usuario.
     * El backend puede usar el historial de compras, categorías favoritas, etc.
     *
     * @param limit número máximo de productos a devolver (por defecto 10)
     */
    @GET("${AppConfig.API_PREFIX}/recommendations")
    suspend fun getRecommendations(
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<List<Product>>>

    // =========================================================================
    // PROGRAMA DE FIDELIZACIÓN - Puntos, niveles y canjes
    // =========================================================================

    /**
     * Obtiene el estado completo del programa de fidelización del usuario:
     * saldo de puntos, nivel actual, puntos para el siguiente nivel,
     * porcentaje de descuento activo e historial de transacciones.
     */
    @GET("${AppConfig.API_PREFIX}/loyalty/status")
    suspend fun getLoyaltyStatus(): Response<ApiResponse<LoyaltyStatus>>

    /**
     * Canjea puntos de fidelización por un descuento en el próximo pedido.
     * 100 puntos = 1€ de descuento. El backend resta los puntos del saldo
     * y devuelve el nuevo estado actualizado.
     *
     * @param request JSON con "points_to_redeem": cantidad de puntos a canjear
     * @return nuevo [LoyaltyStatus] con el saldo actualizado
     */
    @POST("${AppConfig.API_PREFIX}/loyalty/redeem")
    suspend fun redeemLoyaltyPoints(
        @Body request: RedeemRequest
    ): Response<ApiResponse<LoyaltyStatus>>
}
