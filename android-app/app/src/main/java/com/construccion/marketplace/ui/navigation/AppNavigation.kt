package com.construccion.marketplace.ui.navigation

/**
 * Definición centralizada de todas las rutas de navegación.
 */
sealed class Screen(val route: String) {

    // ---- Flujo de inicio ----
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")

    // ---- Flujo principal ----
    object Home : Screen("home")
    object Catalog : Screen("catalog")
    object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: Int) = "product/$productId"
    }

    // ---- Carrito y compra ----
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")

    // ---- Historial y perfil ----
    object OrderHistory : Screen("order_history")
    object OrderDetail : Screen("order/{orderId}") {
        fun createRoute(orderId: Int) = "order/$orderId"
    }
    object Profile : Screen("profile")

    // ---- Obras ----
    object Obras : Screen("obras")
    object ObraDetail : Screen("obra/{obraId}") {
        fun createRoute(obraId: Int) = "obra/$obraId"
    }

    // ---- Calculadora de materiales ----
    object Calculator : Screen("calculator")

    // ---- Chatbot ----
    object Chatbot : Screen("chatbot")

    // ---- Panel de administración (solo userType == EMPRESA o admin) ----
    object AdminPanel : Screen("admin_panel")

    // ---- Fidelización ----
    object Loyalty : Screen("loyalty")
}
