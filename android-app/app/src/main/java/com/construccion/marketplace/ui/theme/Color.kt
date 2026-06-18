package com.construccion.marketplace.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores de ConstruApp.
 *
 * Organizada por roles (primario, secundario, terciario, superficies,
 * errores, estados de obra/pedido y niveles de fidelización).
 * Los colores se usan en Theme.kt para construir los esquemas
 * claro y oscuro de Material 3.
 */

// ---- Paleta principal (Naranja construcción — identidad de marca) ----
val OrangeConstruction = Color(0xFFE65100)
val OrangeConstructionLight = Color(0xFFFF8330)
val OrangeConstructionDark = Color(0xFFAC1900)
val OrangeContainer = Color(0xFFFFDBCC)
val OnOrangeContainer = Color(0xFF380D00)

// ---- Paleta secundaria (Gris acero) ----
val SteelGray = Color(0xFF37474F)
val SteelGrayLight = Color(0xFF62727B)
val SteelGrayDark = Color(0xFF102027)
val SteelGrayContainer = Color(0xFFCFD8DC)
val OnSteelGrayContainer = Color(0xFF001F27)

// ---- Terciaria (Amarillo seguridad) ----
val SafetyYellow = Color(0xFFF9A825)
val SafetyYellowLight = Color(0xFFFFDA6A)
val SafetyYellowDark = Color(0xFFC17900)

// ---- Superficies ----
val SurfaceLight = Color(0xFFFFFBFF)
val SurfaceVariantLight = Color(0xFFF4EDE8)
val BackgroundLight = Color(0xFFFFFBFF)

val SurfaceDark = Color(0xFF1C1B1F)
val SurfaceVariantDark = Color(0xFF3A3236)
val BackgroundDark = Color(0xFF1C1B1F)

// ---- Errores ----
val ErrorRed = Color(0xFFBA1A1A)
val ErrorRedContainer = Color(0xFFFFDAD6)
val OnErrorRedContainer = Color(0xFF410002)

// ---- Neutrales ----
val OutlineLight = Color(0xFF857470)
val OutlineDark = Color(0xFF9F8D89)

// ---- Estados de obra / pedido ----
val StateActive = Color(0xFF2E7D32)      // Verde: en curso
val StateWarning = Color(0xFFF57C00)     // Naranja: pendiente
val StateError = Color(0xFFD32F2F)       // Rojo: cancelado
val StateNeutral = Color(0xFF455A64)     // Gris: borrador
val StateDone = Color(0xFF1565C0)        // Azul: finalizado

// ---- Fidelización ----
val LoyaltyBronce = Color(0xFFCD7F32)
val LoyaltyPlata = Color(0xFFC0C0C0)
val LoyaltyOro = Color(0xFFFFD700)
val LoyaltyPlatino = Color(0xFFE5E4E2)
