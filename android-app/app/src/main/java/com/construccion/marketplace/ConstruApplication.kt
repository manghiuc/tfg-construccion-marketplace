package com.construccion.marketplace

import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application de ConstruApp.
 *
 * La anotación @HiltAndroidApp activa la inyección de dependencias de Hilt
 * en toda la aplicación. Hilt genera automáticamente un componente raíz
 * (SingletonComponent) que contiene todos los proveedores de NetworkModule
 * y los repositorios marcados como @Singleton.
 */
@HiltAndroidApp
class ConstruApplication : android.app.Application()
