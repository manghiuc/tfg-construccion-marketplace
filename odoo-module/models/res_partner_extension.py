# -*- coding: utf-8 -*-
from odoo import api, fields, models


class ResPartnerConstruction(models.Model):
    """Extensión de res.partner para mostrar pedidos y obras del cliente."""
    _inherit = 'res.partner'

    construction_request_count = fields.Integer(
        string='Pedidos',
        compute='_compute_construction_counts',
    )
    construction_obra_count = fields.Integer(
        string='Obras',
        compute='_compute_construction_counts',
    )

    @api.depends_context('uid')
    def _compute_construction_counts(self):
        RequestModel = self.env['construction.material.request'].sudo()
        ObraModel = self.env['construction.obra'].sudo()
        for partner in self:
            partner.construction_request_count = RequestModel.search_count(
                [('partner_id', '=', partner.id)]
            )
            partner.construction_obra_count = ObraModel.search_count(
                [('partner_id', '=', partner.id)]
            )

    def action_view_construction_requests(self):
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
        self.ensure_one()
        return {
            'type': 'ir.actions.act_window',
            'name': 'Obras',
            'res_model': 'construction.obra',
            'view_mode': 'list,form',
            'domain': [('partner_id', '=', self.id)],
            'context': {'default_partner_id': self.id},
        }
