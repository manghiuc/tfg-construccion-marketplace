# ConstruApp — Marketplace de Materiales de Construcción

![Odoo 17](https://img.shields.io/badge/Odoo-17_Community-714B67?logo=odoo&logoColor=white)
![Android](https://img.shields.io/badge/Android-API_26+-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.11-3776AB?logo=python&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-2024.12-4285F4?logo=jetpackcompose&logoColor=white)
![Groq](https://img.shields.io/badge/Groq_API-LLaMA_3-F55036?logo=meta&logoColor=white)

Plataforma integral de aprovisionamiento de materiales de construcción. Conecta a jefes de obra y autónomos con un distribuidor local mediante tres capas: una **app Android nativa**, un **portal web**, y un **backend Odoo 17** con API REST JSON.

---

## Descripción del negocio

Un distribuidor de materiales de construcción ofrece a sus clientes (particulares, autónomos y empresas) la posibilidad de realizar pedidos desde obra directamente desde el móvil o desde el portal web. El distribuidor gestiona todo el flujo desde el ERP Odoo 17: aprobación de pedidos, picking, transporte y facturación.

El coste de transporte se calcula en tiempo real usando la distancia Haversine entre el almacén y la obra. El **mínimo de transporte es 15 €** y existe un **recargo del +50 % para entregas urgentes**.

---

## Arquitectura del sistema

```
+---------------------------+          REST API JSON          +---------------------------+
|   APP ANDROID (Kotlin)    | <------------------------------> |  ODOO 17 COMMUNITY (Python)|
|                           |    HTTPS / Bearer Token          |                           |
|  Jetpack Compose UI       |                                  |  Módulo: construccion_    |
|  ViewModels (Hilt)        |                                  |  marketplace              |
|  Repository pattern       |                                  |                           |
|  Retrofit2 + OkHttp       |                                  |  Controllers HTTP         |
|  EncryptedSharedPrefs     |                                  |  Modelos ORM              |
|  Room (caché offline)     |                                  |  Lógica de negocio        |
|  Google Maps Compose      |                                  |  Cálculo transporte       |
|  Coil (imágenes)          |                                  |  Programa fidelización    |
+---------------------------+                                  |  Chatbot (Groq API)       |
                                                               +---------------------------+
+---------------------------+          REST API JSON                      |
|   PORTAL WEB (HTML/JS)    | <----------------------------------------->|
|                           |                                  +----------+-----------+
|  Catálogo de productos    |                                  |  PostgreSQL 15        |
|  Carrito de compra        |                                  |  (Base de datos ERP)  |
|  Checkout / pedidos       |                                  +----------------------+
|  Seguimiento de pedidos   |
+---------------------------+
```

### Capas de la aplicación Android

```
UI Layer (Compose)
    └── Screens / Composables
            └── ViewModels (Hilt @HiltViewModel)
                    └── Repositories (inyectados por Hilt)
                            └── OdooApiService (Retrofit2)
                            └── SessionManager (EncryptedSharedPreferences)
                            └── Room DAO (caché offline)
```

---

## Funcionalidades principales

**Catálogo de materiales con búsqueda**
Catálogo completo con búsqueda full-text, filtros por categoría y ordenación. Imágenes cargadas desde Odoo con caché local mediante Coil.

**Calculadora de obra (m² a lista de materiales)**
Introduce el tipo de construcción (baño, solera, tabique ladrillo, pintura...) y la superficie en m². La app devuelve la lista exacta de materiales con cantidades y precio estimado, listo para añadir al carrito en un tap.

**Carrito con transporte dinámico por GPS**
El coste de transporte se calcula en tiempo real usando la ubicación GPS de la obra. Fórmula Haversine entre el almacén y el punto de entrega. Tarifa mínima: 15 €. Recargo urgente: +50 %.

**Portal web**
Acceso desde navegador al catálogo completo, carrito, proceso de checkout, historial y seguimiento de pedidos. Construido en HTML/CSS/JS nativo, consume la misma API REST del módulo Odoo.

**Chatbot IA con Groq API**
Asistente conversacional integrado en la app que responde preguntas sobre materiales y normas de construcción. Arquitectura híbrida: motor de reglas propio (siempre disponible) + Groq API (LLaMA 3) para enriquecer respuestas cuando hay conexión.

**Programa de puntos Bronce / Plata / Oro**
Cada pedido acumula puntos canjeables como descuento en futuras compras. Niveles: Bronce (0 pts), Plata (500 pts), Oro (2 000 pts), Platino (5 000 pts).

**Panel de administración completo**
El distribuidor gestiona en Odoo: aprobación de pedidos, gestión de stock, tarifas de transporte, programa de fidelización, usuarios y obras.

---

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Backend ERP | Odoo 17 Community (Python 3.11) |
| Base de datos | PostgreSQL 15 |
| API | REST JSON, auth=none (13 endpoints) |
| App móvil | Android (Kotlin 2.1 + Jetpack Compose) |
| Inyección de dependencias | Hilt (Dagger) |
| Red | Retrofit2 + OkHttp |
| Imágenes | Coil 3 |
| Mapas / GPS | Google Maps Compose + Play Services Location |
| Almacenamiento seguro | EncryptedSharedPreferences |
| Caché offline | Room Database (SQLite) |
| Navegación | Navigation Compose |
| Chatbot IA | Groq API (LLaMA 3 8B) + motor de reglas propio |
| Portal web | HTML5 / CSS3 / JavaScript nativo |

---

## Instalación del módulo Odoo

**Requisitos previos**

- Odoo 17 Community instalado y en ejecución
- PostgreSQL 15 configurado
- Python 3.11 con dependencias de Odoo

**Pasos**

```bash
# 1. Clona el repositorio
git clone https://github.com/manghiuc/tfg-construccion-marketplace.git
cd tfg-construccion-marketplace

# 2. Copia el módulo al directorio de addons de Odoo
cp -r odoo-module/ /ruta/a/odoo/custom_addons/construction_marketplace

# 3. Actualiza la lista de módulos e instala
./odoo-bin -d construction_marketplace -u construction_marketplace --stop-after-init

# 4. Arranca Odoo
./odoo-bin -d construction_marketplace
```

**Configuración del chatbot (Groq API)**

En Odoo → Ajustes → Parámetros técnicos → Parámetros del sistema:
- Clave: `construction.groq_api_key`
- Valor: tu API key de https://console.groq.com

---

## Ejecución de la app Android

**Requisitos**

- Android Studio Ladybug o superior
- JDK 17
- Dispositivo o emulador con API 26+

**Pasos**

```bash
# 1. Abre el proyecto en Android Studio
# Archivo -> Abrir -> selecciona la carpeta android-app/

# 2. Configura la URL del backend en AppConfig.kt
const val BASE_URL = "http://tu-odoo.tudominio.com:8069"

# 3. Añade tu Google Maps API Key en el AndroidManifest.xml

# 4. Compila y ejecuta
./gradlew assembleDebug
# o usa el botón Run en Android Studio
```

---

## Portal web

El portal web consume directamente la API REST de Odoo y no requiere servidor adicional: se puede servir como archivos estáticos.

```bash
# Servir localmente
cd web/
python -m http.server 8080
# Abre http://localhost:8080/portal.html
```

O desplegarlo en cualquier hosting estático (Netlify, GitHub Pages, Nginx...).

---

## Estructura del repositorio

```
tfg-construccion-marketplace/
├── odoo-module/                         # Módulo Odoo 17
│   ├── __manifest__.py                  # Metadatos y versión
│   ├── models/
│   │   ├── construction_material.py     # Modelo producto marketplace
│   │   ├── material_request.py          # Solicitud / pedido
│   │   ├── chatbot_engine.py            # Chatbot con Groq API
│   │   └── loyalty.py                   # Programa de fidelización
│   ├── controllers/
│   │   └── api_controller.py            # 13 endpoints REST JSON
│   ├── views/                           # Vistas XML Odoo
│   ├── static/portal/                   # Fuente del portal web
│   └── security/
│       └── ir.model.access.csv
│
├── android-app/                         # App Android (Kotlin)
│   ├── app/src/main/java/com/construccion/marketplace/
│   │   ├── data/api/                    # OdooApiService (Retrofit)
│   │   ├── data/model/                  # Data classes
│   │   ├── data/repository/             # Repositorios
│   │   ├── di/                          # Módulos Hilt
│   │   ├── session/                     # SessionManager
│   │   └── ui/screens/                  # Pantallas Compose
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── web/                                 # Portal web estático
│   ├── portal.html                      # Página principal
│   └── img/                             # Recursos gráficos
│
└── docs/
    └── api-spec.md                      # Especificación API REST
```

---

## Endpoints de la API

Todos los endpoints tienen el prefijo `/api/construction` y no requieren autenticación (`auth="none"`).

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/auth/login` | Iniciar sesión |
| POST | `/auth/register` | Registrar usuario |
| POST | `/auth/logout` | Cerrar sesión |
| GET | `/products` | Listar catálogo (`?search=&category_id=&page=`) |
| GET | `/products/{id}` | Detalle de producto |
| GET | `/catalog/calculator` | Calcular materiales (`?type=banyo&m2=20`) |
| GET | `/recommendations` | Productos recomendados |
| GET | `/obras` | Mis obras |
| POST | `/material_request` | Crear pedido |
| GET | `/material_request` | Historial de pedidos |
| GET | `/material_request/{id}/status` | Estado y tracking |
| POST | `/calculate_transport` | Calcular coste de transporte |
| GET | `/loyalty/status` | Estado del programa de puntos |
| POST | `/loyalty/redeem` | Canjear puntos |
| POST | `/chatbot` | Enviar mensaje al chatbot IA |

---

## Licencia

Proyecto académico de Fin de Grado — IES El Cañaveral. Todos los derechos reservados.
