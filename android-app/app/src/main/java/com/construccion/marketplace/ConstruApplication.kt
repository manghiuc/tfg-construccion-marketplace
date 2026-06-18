/*
 * ConstruApplication.kt
 * Punto de arranque de la aplicacion Android.
 * Es lo primero que se ejecuta cuando el usuario abre la app.
 * Configura la inyeccion de dependencias (Hilt) para que todas
 * las partes de la app puedan compartir servicios automaticamente.
 */
package com.construccion.marketplace

import dagger.hilt.android.HiltAndroidApp

// Clase principal de la aplicacion, necesaria para que Hilt funcione
@HiltAndroidApp
class ConstruApplication : android.app.Application()
