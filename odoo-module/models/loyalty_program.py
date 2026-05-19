# -*- coding: utf-8 -*-
from odoo import api, fields, models, _
from odoo.exceptions import ValidationError


class ConstructionLoyaltyTransaction(models.Model):
    """
    Registro de transacciones del programa de fidelización.

    Cada vez que un cliente gana o canjea puntos se genera una entrada
    en este modelo para mantener el historial completo de movimientos.
    """
    _name = 'construction.loyalty.transaction'
    _description = 'Transacción de Programa de Fidelización'
    _order = 'date desc, id desc'
    _rec_name = 'description'

    # -------------------------------------------------------------------------
    # Relaciones
    # -------------------------------------------------------------------------
    partner_id = fields.Many2one(
        comodel_name='res.partner',
        string='Cliente',
        required=True,
        ondelete='cascade',
        index=True,
        help='Cliente al que pertenece esta transacción de puntos',
    )

    request_id = fields.Many2one(
        comodel_name='construction.material.request',
        string='Solicitud de Materiales',
        ondelete='set null',
        index=True,
        help='Solicitud de materiales asociada a esta transacción (si aplica)',
    )

    # -------------------------------------------------------------------------
    # Datos de la transacción
    # -------------------------------------------------------------------------
    points = fields.Float(
        string='Puntos',
        required=True,
        digits=(16, 2),
        help='Puntos ganados (positivo) o canjeados (negativo)',
    )

    transaction_type = fields.Selection(
        selection=[
            ('earned', 'Puntos Ganados'),
            ('redeemed', 'Puntos Canjeados'),
        ],
        string='Tipo de Transacción',
        required=True,
        help='Indica si los puntos se han ganado por compra o canjeado',
    )

    description = fields.Char(
        string='Descripción',
        help='Descripción breve de la razón de la transacción',
    )

    date = fields.Datetime(
        string='Fecha',
        default=fields.Datetime.now,
        required=True,
        help='Fecha y hora en que se realizó la transacción',
    )

    # =========================================================================
    # Validaciones
    # =========================================================================

    @api.constrains('points', 'transaction_type')
    def _check_points_sign(self):
        """Garantiza coherencia entre el tipo de transacción y el signo de los puntos."""
        for rec in self:
            if rec.transaction_type == 'earned' and rec.points <= 0:
                raise ValidationError(_(
                    'Los puntos ganados deben ser un valor positivo.'
                ))
            if rec.transaction_type == 'redeemed' and rec.points >= 0:
                raise ValidationError(_(
                    'Los puntos canjeados deben registrarse como valor negativo.'
                ))
