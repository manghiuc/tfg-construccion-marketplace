# -*- coding: utf-8 -*-
from odoo import fields, models


class ProductTemplateMarketplace(models.Model):
    """Añade campo de stock editable para el marketplace de construcción."""
    _inherit = 'product.template'

    qty_marketplace = fields.Integer(
        string='Stock Marketplace',
        default=100,
        help='Cantidad de stock disponible en el marketplace. '
             'Edita este valor directamente desde Inventario → Inventario de Materiales.',
    )
