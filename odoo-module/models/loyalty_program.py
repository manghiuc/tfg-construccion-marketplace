# -*- coding: utf-8 -*-
"""
Historial de transacciones del programa de fidelización.
Registra cada vez que un cliente gana o canjea puntos.
"""
from odoo import api, fields, models, _
from odoo.exceptions import ValidationError


class ConstructionLoyaltyTransaction(models.Model):
    _name = 'construction.loyalty.transaction'
    _description = 'Transacción de Programa de Fidelización'
    _order = 'date desc, id desc'
    _rec_name = 'description'

    partner_id = fields.Many2one(
        'res.partner', string='Cliente',
        required=True, ondelete='cascade', index=True,
    )
    request_id = fields.Many2one(
        'construction.material.request', string='Solicitud de Materiales',
        ondelete='set null', index=True,
    )
    points = fields.Float(
        string='Puntos', required=True, digits=(16, 2),
        help='Positivo = ganados, negativo = canjeados',
    )
    transaction_type = fields.Selection([
        ('earned', 'Puntos Ganados'),
        ('redeemed', 'Puntos Canjeados'),
    ], string='Tipo', required=True)
    description = fields.Char(string='Descripción')
    date = fields.Datetime(string='Fecha', default=fields.Datetime.now, required=True)

    @api.constrains('points', 'transaction_type')
    def _check_points_sign(self):
        """Los puntos ganados deben ser positivos, los canjeados negativos."""
        for rec in self:
            if rec.transaction_type == 'earned' and rec.points <= 0:
                raise ValidationError(_('Los puntos ganados deben ser un valor positivo.'))
            if rec.transaction_type == 'redeemed' and rec.points >= 0:
                raise ValidationError(_('Los puntos canjeados deben ser un valor negativo.'))
