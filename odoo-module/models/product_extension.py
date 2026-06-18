# -*- coding: utf-8 -*-
"""
Extensión de producto: añade un campo de stock propio para el marketplace.
Independiente del stock nativo de Odoo, editable directamente.
"""
from odoo import fields, models


class ProductTemplateMarketplace(models.Model):
    _inherit = 'product.template'

    qty_marketplace = fields.Integer(
        string='Stock Marketplace', default=100,
        help='Stock disponible en el marketplace. Se resta al preparar un pedido.',
    )
