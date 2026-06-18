# -*- coding: utf-8 -*-
"""
Extensión del cliente (res.partner)
Añade: tipo de cliente, programa de fidelización y token para notificaciones push.
"""
from odoo import api, fields, models, _
from odoo.exceptions import UserError


class ResPartnerExtension(models.Model):
    _inherit = 'res.partner'

    # -- Tipo de cliente (particular, autónomo o empresa) --
    partner_type = fields.Selection(
        selection=[
            ('particular', 'Particular'),
            ('autonomo', 'Autónomo'),
            ('empresa', 'Empresa'),
        ],
        string='Tipo de Cliente',
        default='particular',
    )
    company_cif = fields.Char(string='CIF/NIF Empresa', size=20)

    # -- Programa de fidelización --
    points_balance = fields.Float(
        string='Saldo de Puntos', default=0.0, digits=(16, 2),
        help='Puntos acumulados. Se gana 1 punto por cada 1 € gastado.',
    )
    loyalty_level = fields.Selection(
        selection=[
            ('bronze', 'Bronce'),
            ('silver', 'Plata'),
            ('gold', 'Oro'),
            ('platinum', 'Platino'),
        ],
        string='Nivel de Fidelización',
        compute='_compute_loyalty_level', store=True,
    )
    loyalty_discount = fields.Float(
        string='Descuento por Fidelización (%)',
        compute='_compute_loyalty_level', store=True, digits=(5, 2),
    )

    # -- Notificaciones push (app Android) --
    fcm_token = fields.Char(string='Firebase FCM Token', copy=False)

    # Mapa: nivel → porcentaje de descuento
    _LOYALTY_MAP = {
        'bronze':   0.0,    # < 500 pts
        'silver':   3.0,    # 500 – 2.000 pts
        'gold':     6.0,    # 2.000 – 6.000 pts
        'platinum': 10.0,   # > 6.000 pts
    }

    @api.depends('points_balance')
    def _compute_loyalty_level(self):
        """Calcula el nivel y el descuento según los puntos acumulados."""
        for partner in self:
            pts = partner.points_balance
            if pts < 500:
                level = 'bronze'
            elif pts <= 2000:
                level = 'silver'
            elif pts <= 6000:
                level = 'gold'
            else:
                level = 'platinum'
            partner.loyalty_level = level
            partner.loyalty_discount = self._LOYALTY_MAP.get(level, 0.0)

    def _add_points(self, amount):
        """Suma puntos al cliente: 1 punto por cada 1 € gastado."""
        self.ensure_one()
        points_earned = amount
        if points_earned <= 0:
            return 0.0
        self.points_balance += points_earned
        self.env['construction.loyalty.transaction'].create({
            'partner_id': self.id,
            'points': points_earned,
            'transaction_type': 'earned',
            'description': _('Puntos ganados por compra de %.2f €') % amount,
        })
        return points_earned

    def redeem_points(self, points, request_id=None):
        """Canjea puntos del saldo del cliente."""
        self.ensure_one()
        if points <= 0:
            raise UserError(_('La cantidad de puntos a canjear debe ser positiva.'))
        if self.points_balance < points:
            raise UserError(_(
                'Saldo de puntos insuficiente. Disponible: %.2f, solicitado: %.2f'
            ) % (self.points_balance, points))
        self.points_balance -= points
        self.env['construction.loyalty.transaction'].create({
            'partner_id': self.id,
            'request_id': request_id,
            'points': -points,
            'transaction_type': 'redeemed',
            'description': _('Canje de %.2f puntos') % points,
        })
        return True

    def _get_points_to_next_level(self):
        """Calcula cuántos puntos faltan para subir al siguiente nivel."""
        self.ensure_one()
        pts = self.points_balance
        if pts < 500:
            return {'next_level': 'silver', 'next_level_name': 'Plata', 'points_needed': 500 - pts}
        elif pts <= 2000:
            return {'next_level': 'gold', 'next_level_name': 'Oro', 'points_needed': 2000 - pts}
        elif pts <= 6000:
            return {'next_level': 'platinum', 'next_level_name': 'Platino', 'points_needed': 6000 - pts}
        else:
            return {'next_level': None, 'next_level_name': None, 'points_needed': 0}
