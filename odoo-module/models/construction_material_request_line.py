# -*- coding: utf-8 -*-
from odoo import api, fields, models


class ConstructionMaterialRequestLine(models.Model):
    """
    Líneas de una solicitud de materiales de construcción.
    Cada línea representa un producto concreto con cantidad y precio.
    """
    _name = 'construction.material.request.line'
    _description = 'Línea de Solicitud de Materiales'
    _order = 'sequence, id'

    # -------------------------------------------------------------------------
    # Relación con la cabecera
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
    # Campos de producto
    # -------------------------------------------------------------------------
    product_id = fields.Many2one(
        comodel_name='product.product',
        string='Producto',
        required=True,
        domain=[('sale_ok', '=', True)],
        help='Producto o material a solicitar',
    )
    product_name = fields.Char(
        string='Descripción',
        help='Descripción del producto (auto-rellenado, editable)',
    )
    product_qty = fields.Float(
        string='Cantidad',
        required=True,
        default=1.0,
        digits='Product Unit of Measure',
        help='Cantidad solicitada del producto',
    )
    product_uom_id = fields.Many2one(
        comodel_name='uom.uom',
        string='Unidad de Medida',
        help='Unidad de medida del producto',
    )

    # -------------------------------------------------------------------------
    # Precio y subtotal
    # -------------------------------------------------------------------------
    price_unit = fields.Float(
        string='Precio Unitario',
        digits='Product Price',
        default=0.0,
        help='Precio unitario del material',
    )
    subtotal = fields.Float(
        string='Subtotal',
        compute='_compute_subtotal',
        store=True,
        digits='Account',
        help='Subtotal calculado (cantidad × precio unitario)',
    )

    # -------------------------------------------------------------------------
    # Otros campos
    # -------------------------------------------------------------------------
    sequence = fields.Integer(
        string='Secuencia',
        default=10,
    )
    notes = fields.Text(
        string='Notas',
        help='Observaciones específicas sobre este material (calidad, marca, etc.)',
    )

    # =========================================================================
    # Cómputos
    # =========================================================================

    @api.depends('product_qty', 'price_unit')
    def _compute_subtotal(self):
        for line in self:
            line.subtotal = line.product_qty * line.price_unit

    # =========================================================================
    # Onchanges
    # =========================================================================

    @api.onchange('product_id')
    def _onchange_product_id(self):
        """Rellena automáticamente los campos del producto al seleccionarlo."""
        if not self.product_id:
            return
        self.product_name = self.product_id.name
        self.product_uom_id = self.product_id.uom_id
        self.price_unit = self.product_id.lst_price
