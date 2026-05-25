package com.construccion.marketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.construccion.marketplace.data.model.PartnerType
import com.construccion.marketplace.session.SessionManager
import com.construccion.marketplace.ui.navigation.Screen
import com.construccion.marketplace.viewmodel.CartViewModel
import com.construccion.marketplace.ui.screens.admin.AdminPanelScreen
import com.construccion.marketplace.ui.screens.auth.LoginScreen
import com.construccion.marketplace.ui.screens.auth.RegisterScreen
import com.construccion.marketplace.ui.screens.cart.CartScreen
import com.construccion.marketplace.ui.screens.cart.CheckoutScreen
import com.construccion.marketplace.ui.screens.catalog.CalculadoraScreen
import com.construccion.marketplace.ui.screens.catalog.CatalogScreen
import com.construccion.marketplace.ui.screens.catalog.CatalogViewModel
import com.construccion.marketplace.ui.screens.catalog.ProductDetailScreen
import com.construccion.marketplace.ui.screens.chatbot.ChatbotScreen
import com.construccion.marketplace.ui.screens.home.HomeScreen
import com.construccion.marketplace.ui.screens.loyalty.LoyaltyScreen
import com.construccion.marketplace.ui.screens.obras.MyProjectsScreen
import com.construccion.marketplace.ui.screens.obras.ObraDetailScreen
import com.construccion.marketplace.ui.screens.orders.OrderDetailScreen
import com.construccion.marketplace.ui.screens.orders.OrderHistoryScreen
import com.construccion.marketplace.ui.screens.profile.ProfileScreen
import com.construccion.marketplace.ui.theme.ConstruAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        setContent {
            ConstruAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    // CartViewModel con scope de Activity para compartirlo entre todas las pantallas
                    val cartViewModel: CartViewModel = hiltViewModel()

                    LaunchedEffect(Unit) {
                        delay(500)
                        keepSplash = false
                    }

                    ConstruAppNavHost(
                        navController = navController,
                        sessionManager = sessionManager,
                        cartViewModel = cartViewModel
                    )
                }
            }
        }
    }
}

// Pantallas donde NO se muestra el bottom bar (auth + checkout)
private val noBottomBarRoutes = setOf(
    Screen.Splash.route,
    Screen.Login.route,
    Screen.Register.route,
    Screen.Checkout.route,
)

@Composable
private fun ConstruAppBottomBar(navController: NavHostController, cartItemCount: Int = 0) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Inicio") },
            selected = currentRoute == Screen.Home.route,
            onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Catálogo") },
            selected = currentRoute == Screen.Catalog.route,
            onClick = {
                navController.navigate(Screen.Catalog.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = {
                        if (cartItemCount > 0) {
                            Badge {
                                Text(
                                    text = if (cartItemCount > 99) "99+" else cartItemCount.toString()
                                )
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                }
            },
            label = { Text("Carrito") },
            selected = currentRoute == Screen.Cart.route,
            onClick = {
                navController.navigate(Screen.Cart.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
            label = { Text("Calc.") },
            selected = currentRoute == Screen.Calculator.route,
            onClick = {
                navController.navigate(Screen.Calculator.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Perfil") },
            selected = currentRoute == Screen.Profile.route,
            onClick = {
                navController.navigate(Screen.Profile.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
fun ConstruAppNavHost(
    navController: NavHostController,
    sessionManager: SessionManager,
    cartViewModel: CartViewModel
) {
    val startDestination = remember {
        if (sessionManager.isLoggedIn()) Screen.Home.route else Screen.Splash.route
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute != null && currentRoute !in noBottomBarRoutes
    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                ConstruAppBottomBar(
                    navController = navController,
                    cartItemCount = cartState.itemCount
                )
            }
        }
    ) { innerPadding ->

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = androidx.compose.ui.Modifier.padding(innerPadding)
    ) {

        // ---- Splash ----
        composable(Screen.Splash.route) {
            SplashDestination(
                isLoggedIn = sessionManager.isLoggedIn(),
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ---- Login ----
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        // ---- Registro ----
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ---- Home ----
        composable(Screen.Home.route) {
            HomeScreen(
                userType = sessionManager.getUserType().name.lowercase(),
                cartItemCount = cartState.itemCount,
                onCartClick = { navController.navigate(Screen.Cart.route) },
                onNotificationsClick = { navController.navigate(Screen.OrderHistory.route) },
                onSearchClick = { navController.navigate(Screen.Catalog.route) },
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onAddToCart = { homeProduct ->
                    cartViewModel.addItem(
                        id = homeProduct.id,
                        name = homeProduct.name,
                        price = homeProduct.price,
                        unit = homeProduct.unit
                    )
                }
            )
        }

        // ---- Catálogo ----
        composable(Screen.Catalog.route) {
            val catalogVm: CatalogViewModel = viewModel()
            CatalogScreen(
                viewModel = catalogVm,
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ---- Detalle de producto ----
        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId") ?: return@composable
            ProductDetailScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() },
                onViewCart = { navController.navigate(Screen.Cart.route) }
            )
        }

        // ---- Carrito ----
        composable(Screen.Cart.route) {
            CartScreen(
                viewModel = cartViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCheckout = { navController.navigate(Screen.Checkout.route) }
            )
        }

        // ---- Checkout ----
        composable(Screen.Checkout.route) {
            val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
            CheckoutScreen(
                cartItems = cartState.items,
                isUrgent = cartState.isUrgent,
                onNavigateBack = { navController.popBackStack() },
                onOrderConfirmed = { orderId ->
                    cartViewModel.clearCart()
                    val numericId = orderId.toIntOrNull() ?: 0
                    navController.navigate(Screen.OrderDetail.createRoute(numericId)) {
                        popUpTo(Screen.Cart.route) { inclusive = true }
                    }
                }
            )
        }

        // ---- Historial de pedidos ----
        composable(Screen.OrderHistory.route) {
            OrderHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onPedidoClick = { orderId ->
                    val numericId = orderId.toIntOrNull() ?: 0
                    navController.navigate(Screen.OrderDetail.createRoute(numericId))
                }
            )
        }

        // ---- Detalle de pedido ----
        composable(
            route = Screen.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.IntType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId") ?: 0
            OrderDetailScreen(
                pedidoId = orderId.toString(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ---- Mis Obras ----
        composable(Screen.Obras.route) {
            MyProjectsScreen(
                onNavigateBack = { navController.popBackStack() },
                onObraDetalle = { obraId ->
                    navController.navigate(Screen.ObraDetail.createRoute(obraId.toIntOrNull() ?: 0))
                },
                onPedirParaObra = {
                    navController.navigate(Screen.Catalog.route)
                }
            )
        }

        // ---- Detalle de obra ----
        composable(
            route = Screen.ObraDetail.route,
            arguments = listOf(navArgument("obraId") { type = NavType.IntType })
        ) { backStackEntry ->
            val obraId = backStackEntry.arguments?.getInt("obraId") ?: return@composable
            ObraDetailScreen(
                obraId = obraId,
                onNavigateBack = { navController.popBackStack() },
                onPedirMateriales = { navController.navigate(Screen.Catalog.route) }
            )
        }

        // ---- Calculadora de materiales ----
        composable(Screen.Calculator.route) {
            CalculadoraScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddToCart = { id, name, price, unit, qty ->
                    cartViewModel.addItem(id = id, name = name, price = price, unit = unit, qty = qty)
                }
            )
        }

        // ---- Chatbot ----
        composable(Screen.Chatbot.route) {
            ChatbotScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ---- Perfil ----
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onMisObras = { navController.navigate(Screen.Obras.route) },
                onCalculadora = { navController.navigate(Screen.Calculator.route) },
                onLoyalty = { navController.navigate(Screen.Loyalty.route) },
                onCerrarSesion = {
                    // El ProfileViewModel llamará a authRepository.logout() (API + local)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ---- Programa de Fidelización ----
        composable(Screen.Loyalty.route) {
            LoyaltyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ---- Panel de administración ----
        composable(Screen.AdminPanel.route) {
            if (sessionManager.getUserType() != PartnerType.EMPRESA) {
                navController.popBackStack()
                return@composable
            }
            AdminPanelScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    } // cierra Scaffold
}

// ---- Splash mientras carga ----
@Composable
private fun SplashDestination(
    isLoggedIn: Boolean,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1200)
        if (isLoggedIn) onNavigateToHome() else onNavigateToLogin()
    }
}

// ---- Placeholder para pantallas en desarrollo ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(title: String, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Próximamente\n$title",
                textAlign = TextAlign.Center
            )
        }
    }
}
