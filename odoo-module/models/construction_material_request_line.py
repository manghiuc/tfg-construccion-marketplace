# -*- coding: utf-8 -*-
"""
Líneas de un pedido de materiales.
Cada línea es un producto con su cantidad y precio.
"""
from odoo import api, fields, models


class ConstructionMaterialRequestLine(models.Model):
    _name = 'construction.material.request.line'
    _description = 'Línea de Solicitud de Materiales'
    _order = 'sequence, id'

    # -- Relación con el pedido --
    request_id = fields.Many2one(
        'construction.material.request', string='Solicitud',
        required=True, ondelete='cascade', index=True,
    )

    # -- Producto --
    product_id = fields.Many2one(
        'product.product', string='Producto', required=True,
        domain=[('sale_ok', '=', True)],
    )
    product_name = fields.Char(string='Descripción')
    product_qty = fields.Float(
        string='Cantidad', required=True, default=1.0,
        digits='Product Unit of Measure',
    )
    product_uom_id = fields.Many2one('uom.uom', string='Unidad de Medida')

    # -- Precio --
    price_unit = fields.Float(
        string='Precio Unitario', digits='Product Price', default=0.0,
    )
    subtotal = fields.Float(
        string='Subtotal', compute='_compute_subtotal',
        store=True, digits='Account',
    )

    # -- Otros --
    sequence = fields.Integer(string='Secuencia', default=10)
    notes = fields.Text(string='Notas')

    @api.depends('product_qty', 'price_unit')
    def _compute_subtotal(self):
        """Subtotal = cantidad × precio unitario."""
        for line in self:
            line.subtotal = line.product_qty * line.price_unit

    @api.onchange('product_id')
    def _onchange_product_id(self):
        """Al seleccionar un producto, rellena nombre, unidad y precio."""
        if not self.product_id:
            return
        self.product_name = self.product_id.name
        self.product_uom_id = self.product_id.uom_id
        self.price_unit = self.product_id.lst_price
