/**
 * Pantalla de registro de nuevo usuario.
 *
 * Formulario con: nombre, email, contraseña, tipo de cuenta
 * (particular/autónomo/empresa), teléfono, y campos adicionales
 * para empresas (nombre comercial, CIF). Usa [AuthViewModel].
 */
package com.construccion.marketplace.ui.screens.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.construccion.marketplace.data.model.PartnerType
import com.construccion.marketplace.viewmodel.AuthUiState
import com.construccion.marketplace.viewmodel.AuthViewModel

// ─── Tipos de cuenta ────────────────────────────────────────────────────────

enum class AccountType(
    val label: String,
    val description: String,
    val icon: ImageVector
) {
    PARTICULAR("Particular", "Uso personal\ny reformas", Icons.Filled.Person),
    AUTONOMO("Autónomo", "Profesional\nindependiente", Icons.Filled.Build),
    EMPRESA("Empresa", "Gestión\nempresarial", Icons.Filled.Business)
}

// ─── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: (userType: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading

    var selectedType by remember { mutableStateOf(AccountType.PARTICULAR) }

    // Campos comunes
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Campos autónomo
    var nif by remember { mutableStateOf("") }

    // Campos empresa
    var cif by remember { mutableStateOf("") }
    var razonSocial by remember { mutableStateOf("") }

    // Términos
    var aceptaTerminos by remember { mutableStateOf(false) }

    // Estado de errores locales (validación UI)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Manejo de estados API
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                onRegisterSuccess(state.user.partnerType.name.lowercase())
                viewModel.resetState()
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message, duration = SnackbarDuration.Short)
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Selector tipo de cuenta ──
            Text(
                text = "Tipo de cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccountType.values().forEach { type ->
                    AccountTypeCard(
                        type = type,
                        isSelected = selectedType == type,
                        onClick = { selectedType = type },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()

            // ── Campos comunes ──
            Text(
                text = "Datos personales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (selectedType == AccountType.EMPRESA) "Nombre de contacto" else "Nombre completo") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Teléfono") },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            // ── Campos específicos por tipo ──
            when (selectedType) {
                AccountType.AUTONOMO -> {
                    HorizontalDivider()
                    Text(
                        text = "Datos profesionales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = nif,
                        onValueChange = { nif = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("NIF") },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true,
                        placeholder = { Text("12345678A") }
                    )
                }
                AccountType.EMPRESA -> {
                    HorizontalDivider()
                    Text(
                        text = "Datos empresariales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = razonSocial,
                        onValueChange = { razonSocial = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Razón social") },
                        leadingIcon = { Icon(Icons.Filled.Business, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cif,
                        onValueChange = { cif = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("CIF") },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true,
                        placeholder = { Text("B12345678") }
                    )
                }
                else -> Unit
            }

            HorizontalDivider()

            // ── Contraseñas ──
            Text(
                text = "Seguridad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirmar contraseña") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Text("Las contraseñas no coinciden", color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true
            )

            // ── Términos y condiciones ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = aceptaTerminos,
                    onCheckedChange = { aceptaTerminos = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Acepto los ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "términos y condiciones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { /* Abrir términos */ }
                )
            }

            // ── Botón crear cuenta ──
            Button(
                onClick = {
                    when {
                        nombre.isBlank() || email.isBlank() || password.isBlank() ->
                            errorMessage = "Por favor, rellena todos los campos obligatorios"
                        password != confirmPassword ->
                            errorMessage = "Las contraseñas no coinciden"
                        password.length < 6 ->
                            errorMessage = "La contraseña debe tener al menos 6 caracteres"
                        !aceptaTerminos ->
                            errorMessage = "Debes aceptar los términos y condiciones"
                        else -> viewModel.register(
                            name = nombre.trim(),
                            email = email.trim(),
                            password = password,
                            partnerType = when (selectedType) {
                                AccountType.AUTONOMO -> PartnerType.AUTONOMO
                                AccountType.EMPRESA  -> PartnerType.EMPRESA
                                else                 -> PartnerType.PARTICULAR
                            },
                            phone = telefono.takeIf { it.isNotBlank() },
                            companyName = if (selectedType == AccountType.EMPRESA) razonSocial.takeIf { it.isNotBlank() } else null,
                            vat = when (selectedType) {
                                AccountType.AUTONOMO -> nif.takeIf { it.isNotBlank() }
                                AccountType.EMPRESA  -> cif.takeIf { it.isNotBlank() }
                                else                 -> null
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = aceptaTerminos && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Crear cuenta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Ir al login ──
            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ya tengo cuenta")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Componente AccountTypeCard ──────────────────────────────────────────────

@Composable
private fun AccountTypeCard(
    type: AccountType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onClick),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = type.label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = type.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = type.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}
