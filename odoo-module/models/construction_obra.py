# -*- coding: utf-8 -*-
from odoo import api, fields, models, _
from odoo.exceptions import ValidationError


class ConstructionObra(models.Model):
    """
    Modelo que representa una obra de construcción.
    Cada obra puede tener múltiples solicitudes de materiales asociadas.
    """
    _name = 'construction.obra'
    _description = 'Obra de Construcción'
    _inherit = ['mail.thread', 'mail.activity.mixin']
    _order = 'name asc'
    _rec_name = 'name'

    # -------------------------------------------------------------------------
    # Campos de identificación
    # -------------------------------------------------------------------------
    name = fields.Char(
        string='Nombre de la Obra',
        required=True,
        tracking=True,
        help='Nombre descriptivo de la obra de construcción',
    )
    code = fields.Char(
        string='Código',
        required=True,
        copy=False,
        default=lambda self: _('Nuevo'),
        help='Código único identificador de la obra',
    )
    active = fields.Boolean(
        string='Activo',
        default=True,
    )

    # -------------------------------------------------------------------------
    # Campos de ubicación y contacto
    # -------------------------------------------------------------------------
    address = fields.Char(
        string='Dirección',
        help='Dirección física de la obra',
    )
    partner_id = fields.Many2one(
        comodel_name='res.partner',
        string='Empresa/Cliente',
        tracking=True,
        help='Empresa o cliente propietario/responsable de la obra',
    )
    responsible_id = fields.Many2one(
        comodel_name='res.users',
        string='Responsable',
        default=lambda self: self.env.user,
        tracking=True,
        help='Usuario responsable de la gestión de esta obra',
    )

    # -------------------------------------------------------------------------
    # Fechas
    # -------------------------------------------------------------------------
    date_start = fields.Date(
        string='Fecha de Inicio',
        tracking=True,
        help='Fecha prevista de inicio de la obra',
    )
    date_end = fields.Date(
        string='Fecha de Fin',
        tracking=True,
        help='Fecha prevista de finalización de la obra',
    )

    # -------------------------------------------------------------------------
    # Estado
    # -------------------------------------------------------------------------
    state = fields.Selection(
        selection=[
            ('draft', 'Borrador'),
            ('active', 'Activa'),
            ('finished', 'Finalizada'),
        ],
        string='Estado',
        default='draft',
        required=True,
        tracking=True,
        help='Estado actual de la obra',
    )

    # -------------------------------------------------------------------------
    # Relaciones
    # -------------------------------------------------------------------------
    material_request_ids = fields.One2many(
        comodel_name='construction.material.request',
        inverse_name='obra_id',
        string='Solicitudes de Materiales',
        help='Solicitudes de materiales asociadas a esta obra',
    )

    # -------------------------------------------------------------------------
    # Campos calculados
    # -------------------------------------------------------------------------
    material_request_count = fields.Integer(
        string='Nº Solicitudes',
        compute='_compute_material_request_count',
        help='Número total de solicitudes de materiales de la obra',
    )

    # -------------------------------------------------------------------------
    # Notas internas
    # -------------------------------------------------------------------------
    description = fields.Text(
        string='Descripción',
        help='Descripción detallada y observaciones sobre la obra',
    )

    # =========================================================================
    # Métodos ORM
    # =========================================================================

    @api.model_create_multi
    def create(self, vals_list):
        for vals in vals_list:
            if vals.get('code', _('Nuevo')) == _('Nuevo'):
                vals['code'] = self.env['ir.sequence'].next_by_code(
                    'construction.obra'
                ) or _('Nuevo')
        return super().create(vals_list)

    # =========================================================================
    # Campos computados
    # =========================================================================

    @api.depends('material_request_ids')
    def _compute_material_request_count(self):
        for record in self:
            record.material_request_count = len(record.material_request_ids)

    # =========================================================================
    # Validaciones
    # =========================================================================

    @api.constrains('date_start', 'date_end')
    def _check_dates(self):
        for record in self:
            if record.date_start and record.date_end:
                if record.date_end < record.date_start:
                    raise ValidationError(_(
                        'La fecha de fin no puede ser anterior a la fecha de inicio.'
                    ))

    # =========================================================================
    # Acciones de estado
    # =========================================================================

    def action_set_active(self):
        """Activa la obra."""
        for record in self:
            record.state = 'active'

    def action_set_finished(self):
        """Marca la obra como finalizada."""
        for record in self:
            record.state = 'finished'

    def action_set_draft(self):
        """Vuelve la obra a borrador."""
        for record in self:
            record.state = 'draft'

    def action_view_material_requests(self):
        """Abre las solicitudes de materiales de esta obra."""
        self.ensure_one()
        return {
            'type': 'ir.actions.act_window',
            'name': _('Solicitudes de Materiales - %s') % self.name,
            'res_model': 'construction.material.request',
            'view_mode': 'list,form',
            'domain': [('obra_id', '=', self.id)],
            'context': {
                'default_obra_id': self.id,
                'default_partner_id': self.partner_id.id,
            },
        }
