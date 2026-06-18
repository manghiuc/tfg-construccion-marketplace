# -*- coding: utf-8 -*-
# ============================================================================
# EXTENSIÓN DEL CONTACTO: CONTADORES DE PEDIDOS Y OBRAS
# Este archivo añade a la ficha de cada cliente/contacto dos contadores:
#   - Cuántos pedidos de materiales tiene
#   - Cuántas obras tiene asociadas
# También añade botones para ver esos pedidos y obras directamente.
# ============================================================================
from odoo import api, fields, models


class ResPartnerConstruction(models.Model):
    """Extensión de res.partner para mostrar pedidos y obras del cliente."""
    # Hereda del contacto estándar de Odoo
    _inherit = 'res.partner'

    # Número de pedidos de materiales que tiene este cliente
    construction_request_count = fields.Integer(
        string='Pedidos',
        compute='_compute_construction_counts',
    )

    # Número de obras que tiene este cliente
    construction_obra_count = fields.Integer(
        string='Obras',
        compute='_compute_construction_counts',
    )

    # =========================================================================
    # CÁLCULO DE LOS CONTADORES
    # Cuenta cuántos pedidos y cuántas obras tiene cada cliente
    # =========================================================================

    @api.depends_context('uid')
    def _compute_construction_counts(self):
        # Acceso a los modelos de solicitudes y obras con permisos de admin
        RequestModel = self.env['construction.material.request'].sudo()
        ObraModel = self.env['construction.obra'].sudo()
        for partner in self:
            # Cuenta las solicitudes donde el cliente es este contacto
            partner.construction_request_count = RequestModel.search_count(
                [('partner_id', '=', partner.id)]
            )
            # Cuenta las obras donde el cliente es este contacto
            partner.construction_obra_count = ObraModel.search_count(
                [('partner_id', '=', partner.id)]
            )

    # =========================================================================
    # BOTONES PARA VER PEDIDOS Y OBRAS DEL CLIENTE
    # =========================================================================

    def action_view_construction_requests(self):
        """Botón que abre la lista de pedidos de materiales de este cliente."""
        self.ensure_one()
        return {
            'type': 'ir.actions.act_window',
            'name': 'Pedidos de Materiales',
            'res_model': 'construction.material.request',
            'view_mode': 'list,form',
            'domain': [('partner_id', '=', self.id)],
            'context': {'default_partner_id': self.id},
        }

    def action_view_construction_obras(self):
        """Botón que abre la lista de obras de este cliente."""
        self.ensure_one()
        return {
            'type': 'ir.actions.act_window',
            'name': 'Obras',
            'res_model': 'construction.obra',
            'view_mode': 'list,form',
            'domain': [('partner_id', '=', self.id)],
            'context': {'default_partner_id': self.id},
        }
