# Documentación REST API — Construction Marketplace

Base URL: `http://<odoo-host>:8069`

Todos los endpoints devuelven JSON con el siguiente envelope:

```json
// Respuesta exitosa
{ "success": true, "data": { ... } }

// Respuesta de error
{ "success": false, "error": { "code": 400, "message": "Descripción del error" } }
```

La autenticación se realiza mediante sesión Odoo. El endpoint de login devuelve un `session_id` que debe enviarse como cookie `session_id` en todas las peticiones posteriores.

---

## Autenticación

### POST /api/construction/auth/login

Inicia sesión en Odoo y devuelve el identificador de sesión.

- **Auth**: Pública (no requiere sesión previa)
- **Content-Type**: `application/json`

**Body de la petición:**

```json
{
  "db": "nombre_base_de_datos",
  "login": "usuario@ejemplo.com",
  "password": "contraseña"
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `db` | string | No* | Nombre de la BD. Si se omite, usa la BD por defecto |
| `login` | string | Sí | Email o login del usuario Odoo |
| `password` | string | Sí | Contraseña del usuario |

**Respuesta exitosa (200):**

```json
{
  "success": true,
  "data": {
    "uid": 7,
    "name": "Mario García",
    "login": "mario@empresa.com",
    "session_id": "a3f8c2d1e9b047..."
  }
}
```

**Respuesta de error:**

```json
{
  "success": false,
  "error": { "code": 401, "message": "Credenciales incorrectas" }
}
```

| Código | Significado |
|--------|-------------|
| 200 | Login correcto |
| 401 | Credenciales incorrectas o acceso denegado |

---

## Obras

### GET /api/construction/obras

Devuelve el listado de obras de construcción accesibles por el usuario autenticado.

- **Auth**: Usuario autenticado (cookie `session_id`)
- **Content-Type**: `application/json`

**Parámetros de query:**

| Parámetro | Tipo | Requerido | Por defecto | Descripción |
|-----------|------|-----------|-------------|-------------|
| `state` | string | No | (todos) | Filtrar por estado: `draft`, `active`, `finished` |
| `limit` | integer | No | 100 | Número máximo de resultados |
| `offset` | integer | No | 0 | Desplazamiento para paginación |

**Ejemplo de petición:**

```
GET /api/construction/obras?state=active&limit=20&offset=0
```

**Respuesta exitosa (200):**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Reforma Apartamento C/ Mayor 15",
      "code": "OBR/2024/0001",
      "state": "active",
      "partner": "Reformas García S.L.",
      "requests": 3
    },
    {
      "id": 2,
      "name": "Construcción Nave Industrial",
      "code": "OBR/2024/0002",
      "state": "active",
      "partner": "Industrias Pérez S.A.",
      "requests": 7
    }
  ],
  "total": 2
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | integer | Identificador único de la obra |
| `name` | string | Nombre descriptivo |
| `code` | string | Código de referencia (secuencia automática) |
| `state` | string | Estado: `draft`, `active`, `finished` |
| `partner` | string | Nombre del cliente/empresa responsable |
| `requests` | integer | Número de solicitudes de materiales asociadas |
| `total` | integer | Total de registros (sin paginación) |

---

## Solicitudes de Materiales

### POST /api/construction/material_request

Crea una nueva solicitud de materiales para una obra.

- **Auth**: Usuario autenticado
- **Content-Type**: `application/json`

**Body de la petición:**

```json
{
  "obra_id": 1,
  "notes": "Urgente para el lunes",
  "lines": [
    {
      "product_id": 42,
      "product_qty": 5.0,
      "price_unit": 12.50,
      "notes": "Preferiblemente marca Knauf"
    },
    {
      "product_id": 18,
      "product_qty": 10.0,
      "price_unit": 3.20
    }
  ]
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `obra_id` | integer | Sí | ID de la obra destino |
| `notes` | string | No | Observaciones generales de la solicitud |
| `lines` | array | Sí | Líneas de materiales (mínimo 1) |
| `lines[].product_id` | integer | Sí | ID del producto Odoo |
| `lines[].product_qty` | float | No | Cantidad (por defecto 1.0) |
| `lines[].price_unit` | float | No | Precio unitario (por defecto 0.0) |
| `lines[].notes` | string | No | Observaciones específicas del material |

**Respuesta exitosa (200):**

```json
{
  "success": true,
  "data": {
    "id": 15,
    "name": "SOL/2024/0015",
    "state": "draft"
  },
  "message": "Solicitud creada"
}
```

| Código | Significado |
|--------|-------------|
| 200 | Solicitud creada correctamente |
| 404 | Obra no encontrada |
| 422 | Error de validación (campos incorrectos) |
| 500 | Error interno del servidor |

---

### GET /api/construction/material_request/{id}/status

Obtiene el estado detallado de una solicitud de materiales, incluyendo el log de seguimiento y las líneas.

- **Auth**: Usuario autenticado
- **Parámetro de ruta**: `id` — identificador numérico de la solicitud

**Ejemplo de petición:**

```
GET /api/construction/material_request/15/status
```

**Respuesta exitosa (200):**

```json
{
  "success": true,
  "data": {
    "id": 15,
    "name": "SOL/2024/0015",
    "state": "in_progress",
    "tracking": "[2024-11-01 09:15:00] Solicitud confirmada por Mario García\n[2024-11-01 14:30:00] Solicitud puesta en proceso por Ana López",
    "total": 187.50,
    "lines": [
      {
        "product": "Placa de yeso laminado 13mm",
        "qty": 5.0,
        "subtotal": 62.50
      },
      {
        "product": "Perfil montante 48mm",
        "qty": 10.0,
        "subtotal": 32.00
      }
    ]
  }
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `state` | string | Estado actual: `draft`, `confirmed`, `in_progress`, `delivered`, `cancelled` |
| `tracking` | string | Log de cambios de estado con timestamps |
| `total` | float | Importe total en euros |
| `lines[].qty` | float | Cantidad solicitada |
| `lines[].subtotal` | float | Subtotal de la línea (qty × precio_unitario) |

| Código | Significado |
|--------|-------------|
| 200 | OK |
| 404 | Solicitud no encontrada |

---

## Catálogo de Productos

### GET /api/construction/products

Busca y devuelve productos disponibles en el catálogo de Odoo (con `sale_ok = True`).

- **Auth**: Usuario autenticado

**Parámetros de query:**

| Parámetro | Tipo | Requerido | Por defecto | Descripción |
|-----------|------|-----------|-------------|-------------|
| `search` | string | No | (todos) | Texto de búsqueda en el nombre del producto |
| `limit` | integer | No | 50 | Número máximo de resultados |

**Ejemplo de petición:**

```
GET /api/construction/products?search=cemento&limit=10
```

**Respuesta exitosa (200):**

```json
{
  "success": true,
  "data": [
    {
      "id": 42,
      "name": "Cemento Portland CEM I 52.5 R (25 kg)",
      "price": 8.90,
      "uom": "Saco",
      "image": "/web/image/product.product/42/image_128"
    },
    {
      "id": 43,
      "name": "Cemento cola C1 gris (25 kg)",
      "price": 6.50,
      "uom": "Saco",
      "image": "/web/image/product.product/43/image_128"
    }
  ]
}
```

---

## Motor de Recomendaciones

### GET /api/construction/recommendations

Obtiene recomendaciones de materiales personalizadas basadas en el historial de la obra o el tipo de obra, utilizando *Item-based Collaborative Filtering*.

- **Auth**: Usuario autenticado

**Parámetros de query:**

| Parámetro | Tipo | Requerido | Por defecto | Descripción |
|-----------|------|-----------|-------------|-------------|
| `obra_id` | integer | No | - | ID de obra para recomendaciones personalizadas |
| `obra_type` | string | No | `reforma` | Tipo de obra: `reforma`, `construccion_nueva`, `rehabilitacion` |
| `category` | string | No | (todas) | Categoría de producto para filtrar |
| `limit` | integer | No | 10 | Número máximo de recomendaciones |

**Ejemplo de petición:**

```
GET /api/construction/recommendations?obra_id=1&obra_type=reforma&category=fontaneria&limit=5
```

**Respuesta exitosa (200):**

```json
{
  "success": true,
  "data": [
    {
      "product_id": 18,
      "nombre": "Tubo PVC presión Ø25mm",
      "referencia": "TUB-PVC-25",
      "categoria": "fontaneria",
      "precio_estimado": 3.20,
      "score": 2.8750,
      "confianza": 1.0,
      "nivel_confianza": "alta",
      "imagen_url": "/web/image/product.product/18/image_128"
    },
    {
      "product_id": 19,
      "nombre": "Llave de paso 1/2\"",
      "referencia": "VAL-LP-12",
      "categoria": "fontaneria",
      "precio_estimado": 7.80,
      "score": 1.9200,
      "confianza": 0.67,
      "nivel_confianza": "media",
      "imagen_url": "/web/image/product.product/19/image_128"
    }
  ],
  "obra_type": "reforma"
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `score` | float | Puntuación del algoritmo collaborative filtering |
| `confianza` | float | Valor normalizado entre 0.0 y 1.0 |
| `nivel_confianza` | string | `alta` (≥ 0.8), `media` (≥ 0.5), `baja` (< 0.5) |

---

## Chatbot de Asesoramiento

### POST /api/construction/chatbot

Envía un mensaje al chatbot de asesoramiento de materiales. El chatbot utiliza un motor de reglas con base de conocimiento y, opcionalmente, enriquece la respuesta con Groq LLM (Llama 3).

- **Auth**: Usuario autenticado
- **Content-Type**: `application/json`

**Body de la petición:**

```json
{
  "message": "¿Qué materiales necesito para renovar el baño?"
}
```

**Respuesta exitosa (200):**

```json
{
  "success": true,
  "data": {
    "tipo": "baño",
    "mensaje": "Para Renovación completa de baño te recomiendo los siguientes materiales:",
    "materiales": [
      { "nombre": "Azulejos para baño", "cantidad": "15 m²" },
      { "nombre": "Inodoro con cisterna", "cantidad": "1 ud" },
      { "nombre": "Lavabo con pedestal", "cantidad": "1 ud" },
      { "nombre": "Mampara de ducha", "cantidad": "1 ud" },
      { "nombre": "Grifo monomando lavabo", "cantidad": "1 ud" },
      { "nombre": "Grifo monomando ducha", "cantidad": "1 ud" },
      { "nombre": "Tubo PVC 50mm desagüe", "cantidad": "3 m" },
      { "nombre": "Mortero cola para azulejos", "cantidad": "2 sacos" },
      { "nombre": "Impermeabilizante baño", "cantidad": "5 L" }
    ],
    "fuente": "reglas+llm",
    "total_materiales": 9,
    "mensaje_llm": "Además de los materiales listados, considera también una membrana impermeabilizante..."
  }
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `tipo` | string | Categoría detectada en el mensaje |
| `fuente` | string | `reglas` (solo motor de reglas) o `reglas+llm` (enriquecido con IA) |
| `mensaje_llm` | string | Respuesta adicional del LLM (solo presente si hay API key configurada) |

Las categorías soportadas son: `baño`, `cocina`, `electricidad`, `pintura`, `suelo`, `tabique`, `fontaneria`, `aislamiento`.

---

## Transporte

### POST /api/construction/calculate_transport

Calcula el coste estimado de transporte para un pedido en función del peso/volumen y la distancia a la obra.

- **Auth**: Usuario autenticado
- **Content-Type**: `application/json`

**Body de la petición:**

```json
{
  "obra_id": 1,
  "request_id": 15,
  "distance_km": 25.5,
  "weight_kg": 320.0
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `obra_id` | integer | Sí | ID de la obra de destino |
| `request_id` | integer | No | ID de la solicitud (para calcular peso automáticamente) |
| `distance_km` | float | Sí | Distancia en kilómetros al punto de entrega |
| `weight_kg` | float | No | Peso total en kg (si se omite se calcula desde la solicitud) |

**Respuesta exitosa (200):**

```json
{
  "success": true,
  "data": {
    "distance_km": 25.5,
    "weight_kg": 320.0,
    "transport_cost": 48.75,
    "breakdown": {
      "base_cost": 15.00,
      "distance_cost": 12.75,
      "weight_cost": 21.00
    },
    "currency": "EUR"
  }
}
```

---

## Calculadora de Obra

### GET /api/construction/catalog/calculator

Devuelve el catálogo de calculadoras disponibles por tipo de trabajo. Cada entrada contiene los inputs necesarios y la fórmula para estimar materiales y cantidades.

- **Auth**: Usuario autenticado

**Respuesta exitosa (200):**

```json
{
  "success": true,
  "data": [
    {
      "id": "suelo",
      "nombre": "Calculadora de Suelo / Pavimento",
      "descripcion": "Estima materiales para instalación de tarima, parquet o cerámica",
      "inputs": [
        { "key": "area_m2", "label": "Superficie (m²)", "type": "float", "required": true },
        { "key": "tipo", "label": "Tipo", "type": "select", "options": ["tarima", "ceramica", "parquet"] }
      ],
      "materiales_estimados": [
        { "nombre": "Tarima flotante", "formula": "area_m2 * 1.1", "uom": "m²" },
        { "nombre": "Espuma base tarima", "formula": "area_m2", "uom": "m²" },
        { "nombre": "Rodapié", "formula": "sqrt(area_m2) * 4", "uom": "m" }
      ]
    },
    {
      "id": "pintura",
      "nombre": "Calculadora de Pintura",
      "descripcion": "Estima litros de pintura y materiales auxiliares",
      "inputs": [
        { "key": "area_m2", "label": "Superficie a pintar (m²)", "type": "float", "required": true },
        { "key": "capas", "label": "Número de manos", "type": "integer", "default": 2 }
      ],
      "materiales_estimados": [
        { "nombre": "Pintura plástica", "formula": "area_m2 * capas / 10", "uom": "L" },
        { "nombre": "Imprimación", "formula": "area_m2 / 10", "uom": "L" }
      ]
    }
  ]
}
```

---

## Programa de Fidelización

### GET /api/construction/loyalty/status

Devuelve el estado del programa de puntos del usuario autenticado: saldo, nivel actual y puntos para subir de nivel.

- **Auth**: Usuario autenticado

**Respuesta exitosa (200):**

```json
{
  "success": true,
  "data": {
    "partner_id": 12,
    "partner_name": "Mario García",
    "points_balance": 1250.00,
    "loyalty_level": "silver",
    "loyalty_level_label": "Plata",
    "next_level": "gold",
    "next_level_label": "Oro",
    "points_to_next_level": 750.0,
    "earn_rate": "1 punto por cada 10€ de compra",
    "recent_transactions": [
      {
        "date": "2024-11-01T09:15:00",
        "type": "earned",
        "points": 18.75,
        "description": "Puntos ganados por compra de 187.50 €"
      },
      {
        "date": "2024-10-15T14:00:00",
        "type": "redeemed",
        "points": -50.0,
        "description": "Canje de 50.00 puntos"
      }
    ]
  }
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `points_balance` | float | Saldo actual de puntos |
| `loyalty_level` | string | Nivel actual: `bronze`, `silver`, `gold` |
| `points_to_next_level` | float | Puntos necesarios para subir de nivel (0 si ya es Oro) |
| `recent_transactions` | array | Últimas transacciones de puntos |
| `type` | string | `earned` (ganado) o `redeemed` (canjeado) |

---

## Códigos de Error Globales

| Código HTTP | Significado |
|-------------|-------------|
| 200 | Petición procesada correctamente |
| 400 | Petición malformada o datos inválidos |
| 401 | No autenticado o credenciales incorrectas |
| 403 | Sin permisos suficientes |
| 404 | Recurso no encontrado |
| 422 | Error de validación de negocio (Odoo ValidationError/UserError) |
| 500 | Error interno del servidor |
