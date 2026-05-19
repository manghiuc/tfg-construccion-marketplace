# Arquitectura del Sistema — TFG Marketplace de Construcción

## Visión General

El sistema sigue una arquitectura cliente-servidor en dos capas:

- **Backend**: Odoo 17 como ERP central, con un módulo personalizado (`construction_marketplace`) que expone una REST API sobre HTTP.
- **Cliente**: Aplicación Android nativa desarrollada en Kotlin con Jetpack Compose.

La comunicación entre capas se realiza exclusivamente mediante la REST API del módulo, usando JSON como formato de intercambio.

---

## Diagrama de Componentes

```
┌──────────────────────────────────────────────────────────────────┐
│                      CLIENTE MÓVIL                               │
│               Android (Kotlin + Jetpack Compose)                 │
│                                                                  │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌──────────┐  │
│  │  Obras     │  │ Solicitudes│  │  Chatbot   │  │ Loyalty  │  │
│  │  Screen    │  │  Screen    │  │  Screen    │  │  Screen  │  │
│  └────────────┘  └────────────┘  └────────────┘  └──────────┘  │
│         │               │               │               │        │
│  ┌──────┴───────────────┴───────────────┴───────────────┴─────┐ │
│  │                    Retrofit HTTP Client                     │ │
│  │              (OkHttp + Cookie Session Auth)                 │ │
│  └──────────────────────────────┬──────────────────────────────┘ │
└─────────────────────────────────┼────────────────────────────────┘
                                  │ HTTPS / HTTP REST (JSON)
                                  │
┌─────────────────────────────────┼────────────────────────────────┐
│                         ODOO 17 BACKEND                          │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │              ConstructionAPI (HTTP Controller)             │  │
│  │                  controllers/api_controller.py             │  │
│  └───────────────────────────┬────────────────────────────────┘  │
│                              │                                   │
│  ┌───────────────────────────┼────────────────────────────────┐  │
│  │                    CAPA DE MODELOS (ORM)                   │  │
│  │                                                            │  │
│  │  ┌────────────┐  ┌──────────────────┐  ┌───────────────┐  │  │
│  │  │ConstructionObra│ │MaterialRequest  │  │ RecommendationEngine│ │
│  │  │            │  │  + Lines         │  │ (collab. filter)│  │
│  │  └────────────┘  └──────────────────┘  └───────────────┘  │  │
│  │                                                            │  │
│  │  ┌────────────┐  ┌──────────────────┐                     │  │
│  │  │  Chatbot   │  │  res.partner     │                     │  │
│  │  │ (Groq LLM) │  │  + Loyalty       │                     │  │
│  │  └────────────┘  └──────────────────┘                     │  │
│  └───────────────────────────┬────────────────────────────────┘  │
│                              │ ORM (psycopg2)                    │
│  ┌───────────────────────────▼────────────────────────────────┐  │
│  │                  PostgreSQL 15                             │  │
│  │    construction_obra  |  construction_material_request     │  │
│  │    construction_material_request_line  |  res_partner      │  │
│  │    construction_loyalty_transaction  |  sale_order         │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
         │                                          │
         │                                          │
┌────────▼────────┐                    ┌────────────▼──────────┐
│   Groq API      │                    │  Firebase FCM         │
│ (Llama 3 LLM)   │                    │  (Push Notifications) │
│  api.groq.com   │                    │  fcm.googleapis.com   │
└─────────────────┘                    └───────────────────────┘
```

---

## Módulo Odoo: Estructura Interna

```
construction_marketplace/
├── __manifest__.py                  # Dependencias: sale, stock, account, website, mail
├── __init__.py
│
├── controllers/
│   └── api_controller.py            # 10 endpoints REST (HTTP Controller)
│
├── models/
│   ├── construction_obra.py         # Ciclo de vida de obras
│   ├── construction_material_request.py   # Solicitudes + conversión a sale.order
│   ├── construction_material_request_line.py  # Líneas de producto
│   ├── partner_extension.py         # Programa de puntos + FCM token
│   ├── chatbot_engine.py            # Chatbot híbrido (reglas + Groq)
│   └── recommendation_engine.py    # Collaborative filtering (cosine similarity)
│
├── views/
│   ├── construction_obra_views.xml
│   ├── construction_material_request_views.xml
│   └── construction_menu_views.xml
│
├── data/
│   ├── sequences.xml                # ir.sequence para códigos OBR/ y SOL/
│   ├── cron_jobs.xml                # Tareas programadas
│   └── demo_data.xml                # Datos de demostración
│
└── security/
    └── ir.model.access.csv          # Permisos CRUD por modelo y grupo
```

---

## Motor de Recomendaciones (Algoritmo)

Se implementa Item-based Collaborative Filtering con similitud coseno, siguiendo el algoritmo de Sarwar et al. (2001).

```
Entrada: obra_id (o tipo_obra como fallback)

1. Construir perfil de la obra de referencia:
   perfil[product_id] = Σ(product_qty) / ||vector||  (normalización L2)

2. Para cada obra en el sistema:
   similitud = perfil_ref · perfil_obra  (producto punto de vectores normalizados)

3. KNN: seleccionar top-5 obras más similares

4. Calcular scores de materiales:
   score(producto) = Σ(similitud_obra × frecuencia_producto_en_obra)

5. Aplicar bonus de reglas de dominio (cold start):
   score(producto) += 0.5  si el nombre coincide con keywords por tipo_obra

6. Excluir materiales ya pedidos para la obra objetivo

7. Ordenar por score descendente, retornar top-N con nivel de confianza
```

---

## Chatbot: Arquitectura Híbrida

```
Mensaje usuario
     │
     ▼
  Normalización (minúsculas, sin tildes, sin puntuación)
     │
     ▼
  Detección de temas (keywords + sinónimos en SYNONYMS dict)
     │
     ▼
  Motor de reglas → lista de materiales por categoría (KNOWLEDGE_BASE)
     │
     ├── Si hay GROQ_API_KEY configurada:
     │        └── Llamada HTTP a api.groq.com/openai/v1/chat/completions
     │                └── Modelo: llama3-8b-8192 (gratuito)
     │                └── Timeout: 8 segundos
     │                └── Si falla → no bloquea, devuelve solo reglas
     │
     └── Respuesta final: materiales + (opcional) texto enriquecido del LLM
```

---

## Flujo de una Solicitud de Materiales

```
App Android                     Odoo REST API                   PostgreSQL
     │                               │                               │
     │  POST /material_request       │                               │
     │──────────────────────────────►│                               │
     │                               │  INSERT construction_material_request
     │                               │──────────────────────────────►│
     │                               │  INSERT construction_material_request_line (×N)
     │                               │──────────────────────────────►│
     │  { id, name, state: draft }   │                               │
     │◄──────────────────────────────│                               │
     │                               │                               │
     │  [Usuario confirma en Odoo backend o vía app]                 │
     │                               │  UPDATE state = 'confirmed'   │
     │                               │──────────────────────────────►│
     │                               │  INSERT loyalty_transaction    │
     │                               │──────────────────────────────►│
     │                               │                               │
     │  [Almacén procesa el pedido]  │                               │
     │                               │  UPDATE state = 'in_progress' │
     │                               │──────────────────────────────►│
     │                               │  Firebase FCM push notification│
     │                               │──────────────────────────────►│ FCM
     │  [Push notification recibida] │                               │
     │◄──────────────────────────────│                               │
     │                               │                               │
     │  GET /material_request/15/status                              │
     │──────────────────────────────►│                               │
     │  { state, tracking, lines }   │                               │
     │◄──────────────────────────────│                               │
```

---

## Decisiones de Diseño

| Decisión | Alternativa considerada | Justificación |
|----------|------------------------|---------------|
| Odoo 17 como backend | Django REST Framework | Odoo proporciona ERP completo (ventas, stock, contabilidad) listo para usar sin desarrollo adicional |
| Groq API (gratuita) para LLM | OpenAI GPT-4 | Coste cero para el TFG; Llama 3 es suficiente para el caso de uso |
| Motor de reglas como base del chatbot | Solo LLM | Disponibilidad offline y respuesta garantizada sin dependencia de API externas |
| Collaborative filtering simple | ML con scikit-learn | Implementable dentro del ORM de Odoo sin librerías externas; suficiente para el volumen del TFG |
| Sesión cookie de Odoo para auth | JWT propio | Reutiliza la autenticación nativa de Odoo sin duplicar lógica de seguridad |
