# -*- coding: utf-8 -*-
# ============================================================================
# EXTENSIÓN DEL PRODUCTO: STOCK DEL MARKETPLACE
# Este archivo añade un campo nuevo a los productos de Odoo: "Stock Marketplace".
# Es un número que se puede editar a mano para indicar cuánto stock hay
# disponible para vender en el marketplace de construcción.
# No depende del inventario real de Odoo, es un valor independiente.
# ============================================================================
from odoo import fields, models


class ProductTemplateMarketplace(models.Model):
    """Añade campo de stock editable para el marketplace de construcción."""
    # Hereda del producto estándar de Odoo (no crea tabla nueva, la amplía)
    _inherit = 'product.template'

    # Cantidad de stock disponible para el marketplace (editable a mano)
    # Por defecto empieza en 100 unidades
    qty_marketplace = fields.Integer(
        string='Stock Marketplace',
        default=100,
        help='Cantidad de stock disponible en el marketplace. '
             'Edita este valor directamente desde Inventario → Inventario de Materiales.',
    )
