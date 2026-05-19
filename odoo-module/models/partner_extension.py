# -*- coding: utf-8 -*-
from odoo import api, fields, models, _


class ResPartnerExtension(models.Model):
    """
    Extensión de res.partner con tipos de cliente, programa de fidelización
    y soporte para notificaciones push (Firebase FCM).
    """
    _inherit = 'res.partner'

    # -------------------------------------------------------------------------
    # Tipo de cliente
    # -------------------------------------------------------------------------
    partner_type = fields.Selection(
        selection=[
            ('particular', 'Particular'),
            ('autonomo', 'Autónomo'),
            ('empresa', 'Empresa'),
        ],
        string='Tipo de Cliente',
        default='particular',
        help='Clasificación del cliente para tarifas y condiciones comerciales',
    )

    company_cif = fields.Char(
        string='CIF/NIF Empresa',
        size=20,
        help='Número de identificación fiscal (CIF para empresas, NIF para autónomos/particulares)',
    )

    # -------------------------------------------------------------------------
    # Programa de fidelización
    # -------------------------------------------------------------------------
    points_balance = fields.Float(
        string='Saldo de Puntos',
        default=0.0,
        digits=(16, 2),
        help='Puntos acumulados en el programa de fidelización',
    )

    loyalty_level = fields.Selection(
        selection=[
            ('bronze', 'Bronce'),
            ('silver', 'Plata'),
            ('gold', 'Oro'),
        ],
        string='Nivel de Fidelización',
        compute='_compute_loyalty_level',
        store=True,
        help='Nivel calculado automáticamente según el saldo de puntos',
    )

    # -------------------------------------------------------------------------
    # Notificaciones push
    # -------------------------------------------------------------------------
    fcm_token = fields.Char(
        string='Firebase FCM Token',
        help='Token de dispositivo para el envío de notificaciones push a la app móvil',
        copy=False,
    )

    # =========================================================================
    # Campos computados
    # =========================================================================

    @api.depends('points_balance')
    def _compute_loyalty_level(self):
        """
        Calcula el nivel de fidelización según el saldo de puntos:
        - Bronce: menos de 500 puntos
        - Plata:  entre 500 y 2000 puntos (inclusive)
        - Oro:    más de 2000 puntos
        """
        for partner in self:
            pts = partner.points_balance
            if pts < 500:
                partner.loyalty_level = 'bronze'
            elif pts <= 2000:
                partner.loyalty_level = 'silver'
            else:
                partner.loyalty_level = 'gold'

    # =========================================================================
    # Métodos del programa de fidelización
    # =========================================================================

    def _add_points(self, amount):
        """
        Añade puntos al partner en función del importe gastado (sin transporte).
        Regla: 1 punto por cada €10 gastados.

        :param amount: importe en euros sobre el que calcular los puntos
        :return: puntos añadidos
        """
        self.ensure_one()
        points_earned = amount / 10.0
        if points_earned <= 0:
            return 0.0
        self.points_balance += points_earned
        # Registrar la transacción
        self.env['construction.loyalty.transaction'].create({
            'partner_id': self.id,
            'points': points_earned,
            'transaction_type': 'earned',
            'description': _('Puntos ganados por compra de %.2f €') % amount,
        })
        return points_earned

    def redeem_points(self, points, request_id=None):
        """
        Canjea puntos del saldo del partner.

        :param points: cantidad de puntos a canjear (positivo)
        :param request_id: ID de la solicitud de materiales asociada (opcional)
        :raises UserError: si el saldo es insuficiente
        :return: True si el canje se realizó correctamente
        """
        self.ensure_one()
        from odoo.exceptions import UserError
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
        """
        Calcula cuántos puntos faltan para subir al siguiente nivel.
        :return: dict con level_name y points_needed (0 si ya es Oro)
        """
        self.ensure_one()
        pts = self.points_balance
        if pts < 500:
            return {'next_level': 'silver', 'next_level_name': 'Plata', 'points_needed': 500 - pts}
        elif pts <= 2000:
            return {'next_level': 'gold', 'next_level_name': 'Oro', 'points_needed': 2000 - pts}
        else:
            return {'next_level': None, 'next_level_name': None, 'points_needed': 0}
