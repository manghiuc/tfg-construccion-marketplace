# -*- coding: utf-8 -*-
# ============================================================================
# MODELO: HISTORIAL DE TRANSACCIONES DE FIDELIZACIÓN
# Cada vez que un cliente gana o canjea puntos, se crea un registro aquí.
# Es como un "extracto bancario" pero de puntos en vez de dinero.
#
# Ejemplo de registros:
#   - +85.00 puntos — Puntos ganados por compra de 850.00€
#   - -50.00 puntos — Canje de 50.00 puntos
# ============================================================================
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
    # Se ordenan por fecha más reciente primero
    _order = 'date desc, id desc'
    # En los desplegables se muestra la descripción
    _rec_name = 'description'

    # -------------------------------------------------------------------------
    # RELACIONES (a quién pertenece esta transacción)
    # -------------------------------------------------------------------------

    # Cliente al que pertenecen estos puntos
    partner_id = fields.Many2one(
        comodel_name='res.partner',
        string='Cliente',
        required=True,
        ondelete='cascade',
        index=True,
        help='Cliente al que pertenece esta transacción de puntos',
    )

    # Solicitud de materiales relacionada (si los puntos vienen de una compra)
    request_id = fields.Many2one(
        comodel_name='construction.material.request',
        string='Solicitud de Materiales',
        ondelete='set null',
        index=True,
        help='Solicitud de materiales asociada a esta transacción (si aplica)',
    )

    # -------------------------------------------------------------------------
    # DATOS DE LA TRANSACCIÓN
    # -------------------------------------------------------------------------

    # Cantidad de puntos: positivo si se ganan, negativo si se canjean
    points = fields.Float(
        string='Puntos',
        required=True,
        digits=(16, 2),
        help='Puntos ganados (positivo) o canjeados (negativo)',
    )

    # Tipo: ganados (por comprar) o canjeados (por gastar puntos)
    transaction_type = fields.Selection(
        selection=[
            ('earned', 'Puntos Ganados'),     # El cliente ganó puntos al comprar
            ('redeemed', 'Puntos Canjeados'), # El cliente gastó puntos
        ],
        string='Tipo de Transacción',
        required=True,
        help='Indica si los puntos se han ganado por compra o canjeado',
    )

    # Descripción de por qué se ganaron o canjearon los puntos
    description = fields.Char(
        string='Descripción',
        help='Descripción breve de la razón de la transacción',
    )

    # Fecha y hora de la transacción
    date = fields.Datetime(
        string='Fecha',
        default=fields.Datetime.now,
        required=True,
        help='Fecha y hora en que se realizó la transacción',
    )

    # =========================================================================
    # VALIDACIÓN: COHERENCIA ENTRE TIPO Y SIGNO
    # Si es "ganados", los puntos deben ser positivos (+)
    # Si es "canjeados", los puntos deben ser negativos (-)
    # Esto evita errores de registro
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
