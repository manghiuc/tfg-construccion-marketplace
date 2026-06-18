package com.construccion.marketplace.ui.navigation

/**
 * Definición centralizada de todas las rutas de navegación de ConstruApp.
 *
 * Cada objeto representa una pantalla de la app. Las rutas con parámetros
 * (ej. "product/{productId}") exponen un helper createRoute() para generar
 * la ruta con el valor concreto al navegar.
 *
 * Se usa como argumento de navController.navigate(Screen.Xxx.route) y
 * en composable() { ... } dentro del NavHost de MainActivity.
 */
sealed class Screen(val route: String) {

    // ---- Flujo de inicio: autenticación ----
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")

    // ---- Flujo principal: catálogo de productos ----
    object Home : Screen("home")
    object Catalog : Screen("catalog")
    /** Detalle de un producto; requiere el ID como argumento de ruta. */
    object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: Int) = "product/$productId"
    }

    // ---- Carrito y proceso de compra ----
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")

    // ---- Historial de pedidos y perfil de usuario ----
    object OrderHistory : Screen("order_history")
    /** Detalle de un pedido; requiere el ID como argumento de ruta. */
    object OrderDetail : Screen("order/{orderId}") {
        fun createRoute(orderId: Int) = "order/$orderId"
    }
    object Profile : Screen("profile")

    // ---- Obras de construcción ----
    object Obras : Screen("obras")
    /** Detalle de una obra; requiere el ID como argumento de ruta. */
    object ObraDetail : Screen("obra/{obraId}") {
        fun createRoute(obraId: Int) = "obra/$obraId"
    }

    // ---- Calculadora de materiales por tipo de obra y m² ----
    object Calculator : Screen("calculator")

    // ---- Chatbot ConstruBot (Ollama local) ----
    object Chatbot : Screen("chatbot")

    // ---- Panel de administración (solo userType == EMPRESA o admin) ----
    object AdminPanel : Screen("admin_panel")

    // ---- Sistema de fidelización y puntos ----
    object Loyalty : Screen("loyalty")
}
