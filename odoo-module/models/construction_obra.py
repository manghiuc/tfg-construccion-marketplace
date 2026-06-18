# -*- coding: utf-8 -*-
"""
Obras de construcción.
Cada obra pertenece a un cliente y puede tener múltiples pedidos de materiales.
"""
from odoo import api, fields, models, _
from odoo.exceptions import ValidationError


class ConstructionObra(models.Model):
    _name = 'construction.obra'
    _description = 'Obra de Construcción'
    _inherit = ['mail.thread', 'mail.activity.mixin']
    _order = 'name asc'
    _rec_name = 'name'

    # -- Identificación --
    name = fields.Char(string='Nombre de la Obra', required=True, tracking=True)
    code = fields.Char(
        string='Código', required=True, copy=False,
        default=lambda self: _('Nuevo'),
    )
    active = fields.Boolean(string='Activo', default=True)

    # -- Ubicación y contacto --
    address = fields.Char(string='Dirección')
    partner_id = fields.Many2one(
        'res.partner', string='Empresa/Cliente', tracking=True,
    )
    responsible_id = fields.Many2one(
        'res.users', string='Responsable',
        default=lambda self: self.env.user, tracking=True,
    )

    # -- Fechas --
    date_start = fields.Date(string='Fecha de Inicio', tracking=True)
    date_end = fields.Date(string='Fecha de Fin', tracking=True)

    # -- Estado --
    state = fields.Selection([
        ('draft', 'Borrador'),
        ('active', 'Activa'),
        ('finished', 'Finalizada'),
    ], string='Estado', default='draft', required=True, tracking=True)

    # -- Pedidos asociados --
    material_request_ids = fields.One2many(
        'construction.material.request', 'obra_id',
        string='Solicitudes de Materiales',
    )
    material_request_count = fields.Integer(
        string='Nº Solicitudes', compute='_compute_material_request_count',
    )

    # -- Descripción --
    description = fields.Text(string='Descripción')

    # =====================================================================
    # Creación: genera código automático (OBR-2026-0001, etc.)
    # =====================================================================

    @api.model_create_multi
    def create(self, vals_list):
        for vals in vals_list:
            if vals.get('code', _('Nuevo')) == _('Nuevo'):
                vals['code'] = self.env['ir.sequence'].next_by_code(
                    'construction.obra'
                ) or _('Nuevo')
        return super().create(vals_list)

    @api.depends('material_request_ids')
    def _compute_material_request_count(self):
        for record in self:
            record.material_request_count = len(record.material_request_ids)

    @api.constrains('date_start', 'date_end')
    def _check_dates(self):
        for record in self:
            if record.date_start and record.date_end:
                if record.date_end < record.date_start:
                    raise ValidationError(_(
                        'La fecha de fin no puede ser anterior a la fecha de inicio.'
                    ))

    # =====================================================================
    # Acciones de estado
    # =====================================================================

    def action_set_active(self):
        for record in self:
            record.state = 'active'

    def action_set_finished(self):
        for record in self:
            record.state = 'finished'

    def action_set_draft(self):
        for record in self:
            record.state = 'draft'

    def action_view_material_requests(self):
        """Abre los pedidos de materiales de esta obra."""
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
