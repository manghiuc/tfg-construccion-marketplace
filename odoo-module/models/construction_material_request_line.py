# -*- coding: utf-8 -*-
# ============================================================================
# MODELO: LÍNEAS DE SOLICITUD DE MATERIALES
# Cada solicitud de materiales tiene varias "líneas". Cada línea es un
# producto concreto que se pide. Por ejemplo:
#   - Línea 1: 500 sacos de cemento a 8.50€
#   - Línea 2: 15 m³ de arena a 45€
# Es como las líneas de un carrito de la compra.
# ============================================================================
from odoo import api, fields, models


class ConstructionMaterialRequestLine(models.Model):
    """
    Líneas de una solicitud de materiales de construcción.
    Cada línea representa un producto concreto con cantidad y precio.
    """
    # Nombre interno en la base de datos
    _name = 'construction.material.request.line'
    _description = 'Línea de Solicitud de Materiales'
    # Se ordenan por el campo "secuencia" (para poder arrastrar y reordenar)
    _order = 'sequence, id'

    # -------------------------------------------------------------------------
    # CONEXIÓN CON LA SOLICITUD (a qué pedido pertenece esta línea)
    # "cascade" significa que si se borra la solicitud, se borran también sus líneas
    # -------------------------------------------------------------------------
    request_id = fields.Many2one(
        comodel_name='construction.material.request',
        string='Solicitud',
        required=True,
        ondelete='cascade',
        index=True,
        help='Solicitud de materiales a la que pertenece esta línea',
    )

    # -------------------------------------------------------------------------
    # DATOS DEL PRODUCTO (qué material se pide)
    # -------------------------------------------------------------------------

    # Enlace al producto del catálogo de Odoo
    # Solo muestra productos que se pueden vender (sale_ok = True)
    product_id = fields.Many2one(
        comodel_name='product.product',
        string='Producto',
        required=True,
        domain=[('sale_ok', '=', True)],
        help='Producto o material a solicitar',
    )

    # Descripción del producto (se rellena sola al elegir el producto, pero se puede editar)
    product_name = fields.Char(
        string='Descripción',
        help='Descripción del producto (auto-rellenado, editable)',
    )

    # Cantidad que se quiere pedir (por defecto 1)
    product_qty = fields.Float(
        string='Cantidad',
        required=True,
        default=1.0,
        digits='Product Unit of Measure',
        help='Cantidad solicitada del producto',
    )

    # Unidad de medida (unidades, metros, kg, litros...)
    product_uom_id = fields.Many2one(
        comodel_name='uom.uom',
        string='Unidad de Medida',
        help='Unidad de medida del producto',
    )

    # -------------------------------------------------------------------------
    # PRECIO Y SUBTOTAL
    # -------------------------------------------------------------------------

    # Precio de cada unidad del producto
    price_unit = fields.Float(
        string='Precio Unitario',
        digits='Product Price',
        default=0.0,
        help='Precio unitario del material',
    )

    # Subtotal = cantidad × precio unitario (se calcula solo)
    subtotal = fields.Float(
        string='Subtotal',
        compute='_compute_subtotal',
        store=True,
        digits='Account',
        help='Subtotal calculado (cantidad × precio unitario)',
    )

    # -------------------------------------------------------------------------
    # OTROS CAMPOS
    # -------------------------------------------------------------------------

    # Número de orden para poder arrastrar y reordenar las líneas
    sequence = fields.Integer(
        string='Secuencia',
        default=10,
    )

    # Notas específicas sobre este material (marca preferida, calidad, etc.)
    notes = fields.Text(
        string='Notas',
        help='Observaciones específicas sobre este material (calidad, marca, etc.)',
    )

    # =========================================================================
    # CÁLCULO DEL SUBTOTAL
    # Cada vez que cambia la cantidad o el precio, recalcula el subtotal
    # Ejemplo: 500 unidades × 8.50€ = 4.250€
    # =========================================================================

    @api.depends('product_qty', 'price_unit')
    def _compute_subtotal(self):
        for line in self:
            line.subtotal = line.product_qty * line.price_unit

    # =========================================================================
    # AUTORELLENO AL ELEGIR UN PRODUCTO
    # Cuando el usuario selecciona un producto del catálogo, automáticamente
    # se rellenan el nombre, la unidad de medida y el precio de venta.
    # =========================================================================

    @api.onchange('product_id')
    def _onchange_product_id(self):
        """Rellena automáticamente los campos del producto al seleccionarlo."""
        if not self.product_id:
            return
        # Copia el nombre del producto
        self.product_name = self.product_id.name
        # Copia la unidad de medida (kg, m², unidades...)
        self.product_uom_id = self.product_id.uom_id
        # Copia el precio de venta del producto
        self.price_unit = self.product_id.lst_price
