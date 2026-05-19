# ConstruApp — Marketplace de Materiales de Construcción

![Odoo 17](https://img.shields.io/badge/Odoo-17_Community-714B67?logo=odoo&logoColor=white)
![Android](https://img.shields.io/badge/Android-API_26+-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.11-3776AB?logo=python&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-2024.12-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase_FCM-notificaciones-FFCA28?logo=firebase&logoColor=black)

Plataforma integral de aprovisionamiento de materiales de construcción. Conecta a jefes de obra y autónomos con un distribuidor local mediante una app Android nativa que se integra directamente con un backend Odoo 17 a través de una REST API JSON.

---

## Descripcion del negocio

Un distribuidor de materiales de construccion ofrece a sus clientes (particulares, autónomos y empresas) la posibilidad de realizar pedidos desde obra directamente desde el móvil. El distribuidor gestiona todo el flujo desde el ERP Odoo 17: aprobación de pedidos, picking, transporte y facturación.

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
|  Google Maps SDK          |                                  |  Cálculo transporte       |
|  Firebase FCM             |                                  |  Programa fidelización    |
+---------------------------+                                  +---------------------------+
                                                                          |
                                                               +----------+-----------+
                                                               |  PostgreSQL 15        |
                                                               |  (Base de datos ERP)  |
                                                               +----------------------+
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

**Catalogo de materiales con busqueda**
Catálogo completo con búsqueda full-text, filtros por categoría y ordenación. Imágenes cargadas desde Odoo con caché local mediante Coil.

**Calculadora de obra (m2 a lista de materiales)**
Introduce el tipo de construcción (tabique, solera, cubierta, fachada...) y la superficie en m². La app devuelve la lista exacta de materiales con cantidades y precio estimado, listo para añadir al carrito en un tap.

**Carrito con transporte dinamico por GPS**
El coste de transporte se calcula en tiempo real usando la ubicación GPS de la obra. Fórmula Haversine entre el almacén y el punto de entrega. Tarifa mínima: 15 €. Recargo urgente: +50 %.

**Pedido por foto con IA**
Toma una foto de una lista manuscrita o de una pila de materiales y la IA analiza la imagen para generar automáticamente las líneas del pedido.

**Obras compartidas entre equipo**
Crea obras y compártelas con los miembros de tu equipo. Todos pueden ver el historial de pedidos y hacer nuevas solicitudes para la misma obra.

**Pedidos recurrentes programados**
Programa pedidos automáticos con frecuencia semanal, quincenal o mensual. El sistema crea el pedido en Odoo en la fecha indicada y envía una notificación push.

**Modo urgente (+50 % transporte)**
Activa el modo urgente en el carrito para garantizar salida del pedido el mismo día. El recargo se calcula y se muestra antes de confirmar.

**Programa de puntos Bronce / Plata / Oro**
Cada pedido acumula puntos canjeables como descuento en futuras compras. Niveles: Bronce (0 pts), Plata (500 pts), Oro (2 000 pts), Platino (5 000 pts).

**Panel de administracion completo**
El distribuidor gestiona en Odoo: aprobación de pedidos, gestión de stock, tarifas de transporte, programa de fidelización, usuarios y obras.

**Chatbot IA de materiales**
Asistente conversacional integrado en la app que responde preguntas sobre materiales, normas de construcción y sugiere productos del catálogo.

---

## Stack tecnologico

| Componente | Tecnología |
|---|---|
| Backend ERP | Odoo 17 Community (Python 3.11) |
| Base de datos | PostgreSQL 15 |
| API | REST JSON sobre HTTPS con Bearer Token |
| App móvil | Android (Kotlin 2.1 + Jetpack Compose) |
| Inyección de dependencias | Hilt (Dagger) |
| Red | Retrofit2 + OkHttp |
| Imágenes | Coil 3 |
| Mapas / GPS | Google Maps Compose + Play Services Location |
| Notificaciones push | Firebase Cloud Messaging (FCM) |
| Almacenamiento local | Room (SQLite) + EncryptedSharedPreferences |
| Navegación | Navigation Compose |

---

## Instalacion del modulo Odoo

**Requisitos previos**

- Odoo 17 Community instalado y en ejecución
- PostgreSQL 15 configurado
- Python 3.11 con dependencias de Odoo

**Pasos**

```bash
# 1. Clona el repositorio
git clone https://github.com/tu-usuario/tfg-construccion-marketplace.git
cd tfg-construccion-marketplace

# 2. Copia el módulo al directorio de addons de Odoo
cp -r odoo-module/construccion_marketplace /ruta/a/odoo/addons/

# 3. Instala las dependencias Python del módulo (si las hay)
pip install -r odoo-module/requirements.txt

# 4. Actualiza la lista de módulos e instala
./odoo-bin -d nombre_base_datos -u construccion_marketplace --stop-after-init

# 5. Arranca Odoo normalmente
./odoo-bin -d nombre_base_datos
```

**Variables de entorno recomendadas**

```ini
# .env (no incluir en el repositorio)
ODOO_DB=construccion_db
ODOO_HOST=0.0.0.0
ODOO_PORT=8069
GOOGLE_MAPS_API_KEY=tu_clave_aqui
FIREBASE_SERVER_KEY=tu_clave_fcm_aqui
```

---

## Ejecucion de la app Android

**Requisitos**

- Android Studio Ladybug o superior
- JDK 17
- Dispositivo o emulador con API 26+

**Pasos**

```bash
# 1. Abre el proyecto en Android Studio
# Archivo -> Abrir -> selecciona la carpeta ConstruApp/

# 2. Configura la URL del backend
# Edita: app/src/main/java/com/construccion/marketplace/data/api/AppConfig.kt
const val BASE_URL = "https://tu-odoo-17.tudominio.com"

# 3. Añade tu Google Maps API Key en el AndroidManifest.xml
# Sustituye YOUR_MAPS_API_KEY por tu clave real

# 4. Añade google-services.json (Firebase) en ConstruApp/app/

# 5. Compila y ejecuta
./gradlew assembleDebug
# o usa el botón Run en Android Studio
```

---

## Estructura del repositorio

```
tfg-construccion-marketplace/
├── odoo-module/
│   └── construccion_marketplace/
│       ├── __manifest__.py          # Metadatos del módulo
│       ├── models/
│       │   ├── obra.py              # Modelo de obra
│       │   ├── material_request.py  # Solicitud de material / pedido
│       │   ├── transport.py         # Cálculo de transporte (Haversine)
│       │   └── loyalty.py           # Programa de fidelización
│       ├── controllers/
│       │   └── api.py               # Endpoints REST JSON
│       ├── views/                   # Vistas XML para el panel Odoo
│       └── security/
│           └── ir.model.access.csv  # Permisos de acceso
│
├── ConstruApp/                      # Proyecto Android
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/construccion/marketplace/
│   │       │   ├── data/
│   │       │   │   ├── api/         # OdooApiService (Retrofit)
│   │       │   │   ├── model/       # Data classes (User, Product, etc.)
│   │       │   │   └── repository/  # AuthRepository, ProductRepository, OrderRepository
│   │       │   ├── di/              # Módulos Hilt
│   │       │   ├── session/         # SessionManager (EncryptedSharedPrefs)
│   │       │   ├── ui/              # Pantallas Compose
│   │       │   └── viewmodel/       # AuthViewModel, ProductViewModel, CartViewModel
│   │       ├── res/
│   │       │   ├── values/strings.xml
│   │       │   └── xml/file_provider_paths.xml
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts             # Proyecto (plugins)
│   ├── settings.gradle.kts          # Módulos incluidos
│   └── gradle/libs.versions.toml    # Version catalog
│
└── docs/
    └── api-spec.md                  # Especificación de la API REST
```

---

## Endpoints de la API (resumen)

Todos los endpoints tienen el prefijo `/api/v1` y requieren el header `Authorization: Bearer <token>` excepto `/auth/login` y `/auth/register`.

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/auth/login` | Iniciar sesión |
| POST | `/auth/register` | Registrar usuario |
| POST | `/auth/logout` | Cerrar sesión |
| GET | `/products` | Listar catálogo (`?search=&category_id=&page=&page_size=`) |
| GET | `/products/{id}` | Detalle de producto |
| GET | `/catalog/calculator` | Calcular materiales (`?type=tabique&m2=25.5`) |
| GET | `/recommendations` | Productos recomendados |
| GET | `/obras` | Mis obras |
| GET | `/obras/{id}` | Detalle de obra |
| POST | `/material_request` | Crear pedido |
| GET | `/material_request` | Historial de pedidos (`?state=&page=`) |
| GET | `/material_request/{id}/status` | Estado y tracking de un pedido |
| POST | `/calculate_transport` | Calcular coste de transporte |
| GET | `/loyalty/status` | Estado del programa de puntos |
| POST | `/loyalty/redeem` | Canjear puntos |
| POST | `/chatbot` | Enviar mensaje al chatbot IA |

**Ejemplo de respuesta exitosa:**

```json
{
  "success": true,
  "data": { ... },
  "message": null,
  "total": 42,
  "page": 0,
  "page_size": 20
}
```

**Ejemplo de respuesta de error:**

```json
{
  "success": false,
  "data": null,
  "message": "Credenciales incorrectas",
  "error_code": "INVALID_CREDENTIALS"
}
```

---

## Capturas de pantalla

*Próximamente. Las capturas se añadirán tras completar el desarrollo de la capa de UI.*

| Login | Catálogo | Calculadora | Carrito | Pedidos |
|-------|----------|-------------|---------|---------|
| *pendiente* | *pendiente* | *pendiente* | *pendiente* | *pendiente* |

---

## Licencia

Proyecto académico de Fin de Grado. Todos los derechos reservados.
