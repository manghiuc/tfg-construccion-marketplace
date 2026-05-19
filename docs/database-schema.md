# Esquema de Base de Datos — Construction Marketplace

Los modelos Odoo se mapean automáticamente a tablas PostgreSQL siguiendo la convención de reemplazar puntos por guiones bajos en el nombre del modelo (`construction.obra` → `construction_obra`). Todos los campos Many2one generan columnas con sufijo `_id`.

---

## Tablas Principales del Módulo

### construction_obra

Representa una obra de construcción. Tabla central del módulo.

```sql
CREATE TABLE construction_obra (
    id                      SERIAL PRIMARY KEY,
    name                    VARCHAR NOT NULL,
    code                    VARCHAR NOT NULL UNIQUE,
    active                  BOOLEAN DEFAULT TRUE,
    address                 VARCHAR,
    state                   VARCHAR DEFAULT 'draft'
                                CHECK (state IN ('draft', 'active', 'finished')),
    date_start              DATE,
    date_end                DATE,
    description             TEXT,

    -- Relaciones (Foreign Keys a tablas Odoo estándar)
    partner_id              INTEGER REFERENCES res_partner(id)    ON DELETE SET NULL,
    responsible_id          INTEGER REFERENCES res_users(id)      ON DELETE SET NULL,

    -- Columnas de auditoría (heredadas de mail.thread)
    create_date             TIMESTAMP,
    write_date              TIMESTAMP,
    create_uid              INTEGER REFERENCES res_users(id),
    write_uid               INTEGER REFERENCES res_users(id),

    -- Constraint de fechas (aplicado a nivel ORM)
    CONSTRAINT check_dates CHECK (date_end IS NULL OR date_start IS NULL OR date_end >= date_start)
);

-- Índices
CREATE INDEX idx_construction_obra_state      ON construction_obra(state);
CREATE INDEX idx_construction_obra_partner_id ON construction_obra(partner_id);
```

| Columna | Tipo PG | Odoo Field | Descripción |
|---------|---------|------------|-------------|
| `id` | SERIAL | - | Clave primaria auto-incremental |
| `name` | VARCHAR | Char | Nombre de la obra (required) |
| `code` | VARCHAR | Char | Código único (generado por ir.sequence) |
| `active` | BOOLEAN | Boolean | Registro activo/archivado |
| `state` | VARCHAR | Selection | Estado del ciclo de vida |
| `partner_id` | INTEGER | Many2one | Empresa/cliente responsable |
| `responsible_id` | INTEGER | Many2one | Usuario responsable interno |

---

### construction_material_request

Solicitud de materiales para una obra. Puede generar un pedido de venta Odoo.

```sql
CREATE TABLE construction_material_request (
    id                  SERIAL PRIMARY KEY,
    name                VARCHAR NOT NULL DEFAULT 'Nuevo',
    state               VARCHAR DEFAULT 'draft'
                            CHECK (state IN ('draft', 'confirmed', 'in_progress', 'delivered', 'cancelled')),
    date_request        TIMESTAMP DEFAULT NOW(),
    date_required       DATE,
    total_amount        NUMERIC(16, 2) DEFAULT 0.0,
    notes               TEXT,
    tracking_info       TEXT,   -- Log de cambios con timestamps

    -- Relaciones
    obra_id             INTEGER NOT NULL REFERENCES construction_obra(id)   ON DELETE RESTRICT,
    partner_id          INTEGER REFERENCES res_partner(id)                  ON DELETE SET NULL,
    user_id             INTEGER REFERENCES res_users(id)                    ON DELETE SET NULL,
    sale_order_id       INTEGER REFERENCES sale_order(id)                   ON DELETE SET NULL,

    -- Auditoría
    create_date         TIMESTAMP,
    write_date          TIMESTAMP,
    create_uid          INTEGER REFERENCES res_users(id),
    write_uid           INTEGER REFERENCES res_users(id)
);

-- Índices
CREATE INDEX idx_cmr_obra_id         ON construction_material_request(obra_id);
CREATE INDEX idx_cmr_state           ON construction_material_request(state);
CREATE INDEX idx_cmr_date_request    ON construction_material_request(date_request DESC);
CREATE INDEX idx_cmr_sale_order_id   ON construction_material_request(sale_order_id);
```

| Columna | Tipo PG | Descripción |
|---------|---------|-------------|
| `state` | VARCHAR | Flujo: draft → confirmed → in_progress → delivered / cancelled |
| `total_amount` | NUMERIC | Suma de subtotales de líneas (campo stored computed) |
| `tracking_info` | TEXT | Log acumulativo de cambios de estado con timestamps |
| `sale_order_id` | INTEGER | FK a `sale_order` cuando se convierte la solicitud a pedido de venta |

---

### construction_material_request_line

Líneas de detalle de una solicitud. Cada fila representa un producto con cantidad y precio.

```sql
CREATE TABLE construction_material_request_line (
    id              SERIAL PRIMARY KEY,
    sequence        INTEGER DEFAULT 10,
    product_name    VARCHAR,
    product_qty     NUMERIC(16, 4) DEFAULT 1.0,
    price_unit      NUMERIC(16, 4) DEFAULT 0.0,
    subtotal        NUMERIC(16, 2) DEFAULT 0.0,   -- product_qty * price_unit (stored computed)
    notes           TEXT,

    -- Relaciones
    request_id      INTEGER NOT NULL REFERENCES construction_material_request(id)  ON DELETE CASCADE,
    product_id      INTEGER NOT NULL REFERENCES product_product(id)                ON DELETE RESTRICT,
    product_uom_id  INTEGER REFERENCES uom_uom(id)                                ON DELETE SET NULL,

    -- Auditoría
    create_date     TIMESTAMP,
    write_date      TIMESTAMP,
    create_uid      INTEGER REFERENCES res_users(id),
    write_uid       INTEGER REFERENCES res_users(id)
);

-- Índices
CREATE INDEX idx_cmrl_request_id  ON construction_material_request_line(request_id);
CREATE INDEX idx_cmrl_product_id  ON construction_material_request_line(product_id);
```

| Columna | Tipo PG | Descripción |
|---------|---------|-------------|
| `sequence` | INTEGER | Orden de la línea dentro de la solicitud |
| `product_qty` | NUMERIC | Cantidad solicitada (precisión: 4 decimales) |
| `price_unit` | NUMERIC | Precio unitario del material |
| `subtotal` | NUMERIC | `product_qty * price_unit` (calculado y almacenado) |

---

## Extensión de Tablas Odoo Estándar

### res_partner (extendida)

El módulo añade columnas a la tabla estándar `res_partner` de Odoo para soportar el programa de fidelización y las notificaciones push.

```sql
-- Columnas añadidas por el módulo (vía _inherit = 'res.partner')
ALTER TABLE res_partner ADD COLUMN IF NOT EXISTS partner_type   VARCHAR DEFAULT 'particular'
    CHECK (partner_type IN ('particular', 'autonomo', 'empresa'));
ALTER TABLE res_partner ADD COLUMN IF NOT EXISTS company_cif    VARCHAR(20);
ALTER TABLE res_partner ADD COLUMN IF NOT EXISTS points_balance NUMERIC(16, 2) DEFAULT 0.0;
ALTER TABLE res_partner ADD COLUMN IF NOT EXISTS loyalty_level  VARCHAR DEFAULT 'bronze'
    CHECK (loyalty_level IN ('bronze', 'silver', 'gold'));
ALTER TABLE res_partner ADD COLUMN IF NOT EXISTS fcm_token      VARCHAR;  -- Firebase FCM token
```

| Columna | Tipo PG | Descripción |
|---------|---------|-------------|
| `partner_type` | VARCHAR | Tipo de cliente: `particular`, `autonomo`, `empresa` |
| `company_cif` | VARCHAR | CIF/NIF fiscal |
| `points_balance` | NUMERIC | Saldo actual del programa de fidelización |
| `loyalty_level` | VARCHAR | Nivel calculado: `bronze` (< 500 pts), `silver` (500-2000 pts), `gold` (> 2000 pts) |
| `fcm_token` | VARCHAR | Token Firebase para notificaciones push |

**Regla de negocio del nivel de fidelización:**

```
points_balance < 500         → loyalty_level = 'bronze'
500 ≤ points_balance ≤ 2000  → loyalty_level = 'silver'
points_balance > 2000        → loyalty_level = 'gold'
```

**Acumulación de puntos:** 1 punto por cada 10€ de compra (sin incluir transporte).

---

### construction_loyalty_transaction

Registro transaccional del programa de puntos. Cada ganancia o canje de puntos genera una fila.

```sql
CREATE TABLE construction_loyalty_transaction (
    id                  SERIAL PRIMARY KEY,
    points              NUMERIC(16, 2) NOT NULL,  -- Positivo: ganado. Negativo: canjeado.
    transaction_type    VARCHAR NOT NULL
                            CHECK (transaction_type IN ('earned', 'redeemed')),
    description         VARCHAR,
    transaction_date    TIMESTAMP DEFAULT NOW(),

    -- Relaciones
    partner_id          INTEGER NOT NULL REFERENCES res_partner(id)                     ON DELETE CASCADE,
    request_id          INTEGER REFERENCES construction_material_request(id)            ON DELETE SET NULL,

    -- Auditoría
    create_date         TIMESTAMP,
    create_uid          INTEGER REFERENCES res_users(id)
);

-- Índices
CREATE INDEX idx_clt_partner_id ON construction_loyalty_transaction(partner_id);
CREATE INDEX idx_clt_date       ON construction_loyalty_transaction(transaction_date DESC);
```

| Columna | Tipo PG | Descripción |
|---------|---------|-------------|
| `points` | NUMERIC | Puntos de la transacción (positivo = ganado, negativo = canjeado) |
| `transaction_type` | VARCHAR | `earned` o `redeemed` |
| `request_id` | INTEGER | Solicitud asociada al canje (opcional) |

---

## Tablas Odoo Estándar Referenciadas

El módulo tiene dependencias sobre las siguientes tablas estándar de Odoo que no requieren modificación:

| Tabla Odoo | Modelo ORM | Uso en el módulo |
|------------|------------|-----------------|
| `res_partner` | `res.partner` | Cliente/empresa de obra y loyalty |
| `res_users` | `res.users` | Usuario responsable y solicitante |
| `product_product` | `product.product` | Materiales solicitados |
| `product_template` | `product.template` | Plantilla de productos |
| `uom_uom` | `uom.uom` | Unidades de medida |
| `sale_order` | `sale.order` | Pedido de venta generado desde solicitud |
| `sale_order_line` | `sale.order.line` | Líneas del pedido de venta |
| `ir_sequence` | `ir.sequence` | Generación de códigos (`OBR/XXXX/NNNN`, `SOL/XXXX/NNNN`) |

---

## Diagrama Entidad-Relación Simplificado

```
res_partner ─────────────────────────────────────────────┐
    │                                                      │
    │ (partner_id)          (partner_id)                   │
    ▼                                ▼                     │
construction_obra ◄──── construction_material_request      │
    (1)                       (N)         │                │
                                          │ (1)            │
                                          ▼                │
                          construction_material_request_line│
                                          │                │
                                          │ (product_id)   │
                                          ▼                │
                                   product_product         │
                                                           │
construction_loyalty_transaction ◄─────────────────────────┘
    (partner_id, request_id)
```

---

## Secuencias Automáticas

El módulo registra dos secuencias en `ir.sequence` para la generación automática de códigos:

| Secuencia | Código | Formato | Ejemplo |
|-----------|--------|---------|---------|
| Obras | `construction.obra` | `OBR/%(year)s/%(seq)05d` | `OBR/2024/00001` |
| Solicitudes | `construction.material.request` | `SOL/%(year)s/%(seq)05d` | `SOL/2024/00015` |
