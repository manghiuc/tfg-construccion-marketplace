# -*- coding: utf-8 -*-
# ============================================================================
# EXTENSIÓN DEL CONTACTO/CLIENTE: TIPO, FIDELIZACIÓN Y NOTIFICACIONES
# Este archivo amplía la ficha de cada cliente con:
#   1. Tipo de cliente: particular, autónomo o empresa
#   2. Programa de puntos de fidelización (ganas puntos al comprar)
#   3. Token para notificaciones push en el móvil (Firebase)
# ============================================================================
from odoo import api, fields, models, _


class ResPartnerExtension(models.Model):
    """
    Extensión de res.partner con tipos de cliente, programa de fidelización
    y soporte para notificaciones push (Firebase FCM).
    """
    # Hereda del contacto estándar de Odoo (no crea tabla nueva)
    _inherit = 'res.partner'

    # -------------------------------------------------------------------------
    # TIPO DE CLIENTE
    # Sirve para clasificar a los clientes y poder ofrecerles tarifas distintas
    # -------------------------------------------------------------------------
    partner_type = fields.Selection(
        selection=[
            ('particular', 'Particular'),   # Persona física que compra para sí misma
            ('autonomo', 'Autónomo'),        # Trabajador por cuenta propia
            ('empresa', 'Empresa'),          # Empresa con CIF
        ],
        string='Tipo de Cliente',
        default='particular',
        help='Clasificación del cliente para tarifas y condiciones comerciales',
    )

    # CIF o NIF del cliente (identificación fiscal)
    company_cif = fields.Char(
        string='CIF/NIF Empresa',
        size=20,
        help='Número de identificación fiscal (CIF para empresas, NIF para autónomos/particulares)',
    )

    # -------------------------------------------------------------------------
    # PROGRAMA DE FIDELIZACIÓN (puntos que acumulas al comprar)
    # -------------------------------------------------------------------------

    # Saldo actual de puntos del cliente
    points_balance = fields.Float(
        string='Saldo de Puntos',
        default=0.0,
        digits=(16, 2),
        help='Puntos acumulados en el programa de fidelización',
    )

    # Nivel de fidelización: bronce, plata u oro (se calcula automáticamente)
    loyalty_level = fields.Selection(
        selection=[
            ('bronze', 'Bronce'),   # Menos de 500 puntos
            ('silver', 'Plata'),    # Entre 500 y 2000 puntos
            ('gold', 'Oro'),        # Más de 2000 puntos
        ],
        string='Nivel de Fidelización',
        compute='_compute_loyalty_level',
        store=True,
        help='Nivel calculado automáticamente según el saldo de puntos',
    )

    # -------------------------------------------------------------------------
    # NOTIFICACIONES PUSH (para enviar avisos a la app del móvil)
    # -------------------------------------------------------------------------

    # Token del dispositivo móvil para enviar notificaciones (viene de Firebase)
    fcm_token = fields.Char(
        string='Firebase FCM Token',
        help='Token de dispositivo para el envío de notificaciones push a la app móvil',
        copy=False,
    )

    # =========================================================================
    # CÁLCULO AUTOMÁTICO DEL NIVEL DE FIDELIZACIÓN
    # Cada vez que cambian los puntos, se recalcula el nivel:
    #   - Menos de 500 puntos → Bronce
    #   - De 500 a 2000 puntos → Plata
    #   - Más de 2000 puntos → Oro
    # =========================================================================

    @api.depends('points_balance')
    def _compute_loyalty_level(self):
        for partner in self:
            pts = partner.points_balance
            if pts < 500:
                partner.loyalty_level = 'bronze'
            elif pts <= 2000:
                partner.loyalty_level = 'silver'
            else:
                partner.loyalty_level = 'gold'

    # =========================================================================
    # GANAR PUNTOS
    # Cada vez que un cliente compra materiales, gana 1 punto por cada 10€.
    # Ejemplo: compra de 850€ → 85 puntos
    # =========================================================================

    def _add_points(self, amount):
        """
        Añade puntos al cliente según cuánto ha gastado.
        Regla: 1 punto por cada 10€ gastados.
        También registra la transacción en el historial de puntos.
        """
        self.ensure_one()
        points_earned = amount / 10.0
        if points_earned <= 0:
            return 0.0
        # Sumar los puntos al saldo
        self.points_balance += points_earned
        # Guardar un registro de la transacción (para el historial)
        self.env['construction.loyalty.transaction'].create({
            'partner_id': self.id,
            'points': points_earned,
            'transaction_type': 'earned',
            'description': _('Puntos ganados por compra de %.2f €') % amount,
        })
        return points_earned

    # =========================================================================
    # CANJEAR PUNTOS
    # El cliente puede gastar sus puntos (ej: descuentos, regalos)
    # Si no tiene suficientes puntos, le muestra un error.
    # =========================================================================

    def redeem_points(self, points, request_id=None):
        """
        Resta puntos del saldo del cliente.
        Si no tiene suficientes, muestra un error.
        """
        self.ensure_one()
        from odoo.exceptions import UserError
        if points <= 0:
            raise UserError(_('La cantidad de puntos a canjear debe ser positiva.'))
        if self.points_balance < points:
            raise UserError(_(
                'Saldo de puntos insuficiente. Disponible: %.2f, solicitado: %.2f'
            ) % (self.points_balance, points))
        # Restar los puntos
        self.points_balance -= points
        # Guardar un registro del canje
        self.env['construction.loyalty.transaction'].create({
            'partner_id': self.id,
            'request_id': request_id,
            'points': -points,
            'transaction_type': 'redeemed',
            'description': _('Canje de %.2f puntos') % points,
        })
        return True

    # =========================================================================
    # CUÁNTO FALTA PARA EL SIGUIENTE NIVEL
    # Calcula cuántos puntos necesita el cliente para subir de nivel
    # =========================================================================

    def _get_points_to_next_level(self):
        """
        Calcula cuántos puntos le faltan al cliente para llegar al siguiente nivel.
        Ejemplo: si tiene 350 puntos (Bronce), le faltan 150 para llegar a Plata.
        Si ya es Oro, no le falta nada.
        """
        self.ensure_one()
        pts = self.points_balance
        if pts < 500:
            return {'next_level': 'silver', 'next_level_name': 'Plata', 'points_needed': 500 - pts}
        elif pts <= 2000:
            return {'next_level': 'gold', 'next_level_name': 'Oro', 'points_needed': 2000 - pts}
        else:
            return {'next_level': None, 'next_level_name': None, 'points_needed': 0}
