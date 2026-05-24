package com.construccion.marketplace.data.repository

import com.construccion.marketplace.data.api.OdooApiService
import com.construccion.marketplace.data.model.Product
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Representa un material calculado por la calculadora de obra.
 * Incluye el [Product] completo junto con la cantidad recomendada.
 */
data class CalculatedMaterial(
    val product: Product,
    val qtyNeeded: Double,
    val uom: String,
    val priceUnit: Double,
    val subtotal: Double
)

/**
 * Repositorio de productos y calculadora de materiales.
 *
 * Encapsula las llamadas al catálogo de productos de Odoo y
 * la lógica de cálculo de materiales por tipo de obra y m².
 */
@Singleton
class ProductRepository @Inject constructor(
    private val apiService: OdooApiService
) {

    /**
     * Obtiene la lista de productos del catálogo.
     *
     * @param search texto de búsqueda libre (vacío = todos)
     * @param limit número máximo de productos a devolver
     * @param categoryId filtro opcional por categoría
     * @return [Result.success] con la lista de [Product],
     *         o [Result.failure] con el mensaje de error.
     */
    suspend fun getProducts(
        search: String = "",
        limit: Int = 50,
        categoryId: Int? = null
    ): Result<List<Product>> {
        return try {
            val response = apiService.getProducts(
                search = search.takeIf { it.isNotBlank() },
                categoryId = categoryId,
                page = 0,
                pageSize = limit
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.success(body.data ?: emptyList())
                } else {
                    Result.failure(Exception(body?.message ?: "Error al cargar el catálogo"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()} al obtener productos"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }

    /**
     * Obtiene el detalle completo de un producto por su ID.
     *
     * @param id identificador único del producto en Odoo
     * @return [Result.success] con el [Product],
     *         o [Result.failure] con el mensaje de error.
     */
    suspend fun getProductById(id: Int): Result<Product> {
        return try {
            val response = apiService.getProduct(id)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "Producto no encontrado"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Producto no encontrado"
                    else -> "Error ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }

    /**
     * Calcula los materiales necesarios para un tipo de obra y superficie dada.
     *
     * La lógica de cálculo reside en el backend de Odoo (módulo personalizado).
     *
     * @param type tipo de construcción (ej. "tabique", "solera", "cubierta")
     * @param m2 superficie en metros cuadrados
     * @return [Result.success] con la lista de [CalculatedMaterial],
     *         o [Result.failure] con el mensaje de error.
     */
    suspend fun calculateMaterials(type: String, m2: Float): Result<List<CalculatedMaterial>> {
        return try {
            val response = apiService.calculateMaterials(type = type, m2 = m2.toDouble())
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val materials = body.data.materials.map { mat ->
                        CalculatedMaterial(
                            product = Product(
                                id = 0,
                                name = mat.material,
                                defaultCode = "",
                                category = com.construccion.marketplace.data.model.ProductCategory(0, ""),
                                uom = mat.unit,
                                price = 0.0
                            ),
                            qtyNeeded = mat.quantity,
                            uom = mat.unit,
                            priceUnit = 0.0,
                            subtotal = 0.0
                        )
                    }
                    Result.success(materials)
                } else {
                    Result.failure(Exception(body?.message ?: "Error en la calculadora"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()} en la calculadora"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }

    /**
     * Obtiene la lista de productos recomendados para un tipo de obra.
     *
     * @param obraType tipo de obra para contextualizar las recomendaciones
     * @return [Result.success] con la lista de [Product] recomendados,
     *         o [Result.failure] con el mensaje de error.
     */
    suspend fun getRecommendations(obraType: String): Result<List<Product>> {
        return try {
            // El backend acepta el tipo de obra como contexto a través del limit
            // En una versión futura se añadirá el parámetro obra_type al endpoint
            val response = apiService.getRecommendations(limit = 10)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.success(body.data ?: emptyList())
                } else {
                    Result.failure(Exception(body?.message ?: "Error al cargar recomendaciones"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()} al obtener recomendaciones"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }

}
