# -*- coding: utf-8 -*-
# ============================================================================
# MODELO: OBRA DE CONSTRUCCIÓN
# Este archivo define la "ficha" de cada obra de construcción.
# Una obra es un proyecto (ej: "Reforma piso en Madrid", "Edificio nuevo").
# Cada obra puede tener muchas solicitudes de materiales asociadas.
# ============================================================================
from odoo import api, fields, models, _
from odoo.exceptions import ValidationError


class ConstructionObra(models.Model):
    """
    Modelo que representa una obra de construcción.
    Cada obra puede tener múltiples solicitudes de materiales asociadas.
    """
    # Nombre interno de la tabla en la base de datos
    _name = 'construction.obra'
    # Nombre que aparece en las pantallas de Odoo
    _description = 'Obra de Construcción'
    # Hereda funcionalidades de mensajería (para tener el chatter/historial de mensajes)
    _inherit = ['mail.thread', 'mail.activity.mixin']
    # Las obras se ordenan por nombre de la A a la Z
    _order = 'name asc'
    # Cuando se muestra en un desplegable, se usa el campo "name"
    _rec_name = 'name'

    # -------------------------------------------------------------------------
    # CAMPOS DE IDENTIFICACIÓN (datos básicos de la obra)
    # -------------------------------------------------------------------------

    # Nombre de la obra (obligatorio). "tracking=True" significa que se registra
    # en el historial cada vez que se cambia.
    name = fields.Char(
        string='Nombre de la Obra',
        required=True,
        tracking=True,
        help='Nombre descriptivo de la obra de construcción',
    )

    # Código único que se genera automáticamente (ej: OBR-2025-0001)
    # "copy=False" significa que si duplicas una obra, el código no se copia
    code = fields.Char(
        string='Código',
        required=True,
        copy=False,
        default=lambda self: _('Nuevo'),
        help='Código único identificador de la obra',
    )

    # Si la obra está activa o archivada
    active = fields.Boolean(
        string='Activo',
        default=True,
    )

    # -------------------------------------------------------------------------
    # CAMPOS DE UBICACIÓN Y CONTACTO
    # -------------------------------------------------------------------------

    # Dirección física donde se hace la obra
    address = fields.Char(
        string='Dirección',
        help='Dirección física de la obra',
    )

    # Empresa o persona que es dueña de la obra (enlace a la ficha de contacto)
    partner_id = fields.Many2one(
        comodel_name='res.partner',
        string='Empresa/Cliente',
        tracking=True,
        help='Empresa o cliente propietario/responsable de la obra',
    )

    # Usuario de Odoo que es responsable de gestionar la obra
    # Por defecto se pone el usuario que la crea
    responsible_id = fields.Many2one(
        comodel_name='res.users',
        string='Responsable',
        default=lambda self: self.env.user,
        tracking=True,
        help='Usuario responsable de la gestión de esta obra',
    )

    # -------------------------------------------------------------------------
    # FECHAS (cuándo empieza y acaba la obra)
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
    # ESTADO (en qué fase está la obra)
    # Puede ser: Borrador → Activa → Finalizada
    # -------------------------------------------------------------------------
    state = fields.Selection(
        selection=[
            ('draft', 'Borrador'),       # Recién creada, todavía no ha empezado
            ('active', 'Activa'),         # La obra está en marcha
            ('finished', 'Finalizada'),   # La obra ya se ha terminado
        ],
        string='Estado',
        default='draft',
        required=True,
        tracking=True,
        help='Estado actual de la obra',
    )

    # -------------------------------------------------------------------------
    # RELACIONES (conexiones con otros datos)
    # -------------------------------------------------------------------------

    # Lista de todas las solicitudes de materiales que tiene esta obra
    # "One2many" = una obra tiene muchas solicitudes
    material_request_ids = fields.One2many(
        comodel_name='construction.material.request',
        inverse_name='obra_id',
        string='Solicitudes de Materiales',
        help='Solicitudes de materiales asociadas a esta obra',
    )

    # -------------------------------------------------------------------------
    # CAMPOS CALCULADOS (se calculan solos, no los rellena el usuario)
    # -------------------------------------------------------------------------

    # Cuenta cuántas solicitudes de materiales tiene la obra
    material_request_count = fields.Integer(
        string='Nº Solicitudes',
        compute='_compute_material_request_count',
        help='Número total de solicitudes de materiales de la obra',
    )

    # -------------------------------------------------------------------------
    # NOTAS
    # -------------------------------------------------------------------------
    description = fields.Text(
        string='Descripción',
        help='Descripción detallada y observaciones sobre la obra',
    )

    # =========================================================================
    # CUANDO SE CREA UNA OBRA NUEVA
    # Este método se ejecuta automáticamente al crear una obra.
    # Si el código está como "Nuevo", le genera uno automático (ej: OBR-2025-0001)
    # =========================================================================

    @api.model_create_multi
    def create(self, vals_list):
        for vals in vals_list:
            # Si el código es "Nuevo" (valor por defecto), genera uno automático
            if vals.get('code', _('Nuevo')) == _('Nuevo'):
                vals['code'] = self.env['ir.sequence'].next_by_code(
                    'construction.obra'
                ) or _('Nuevo')
        return super().create(vals_list)

    # =========================================================================
    # CÁLCULO DEL NÚMERO DE SOLICITUDES
    # Cada vez que cambian las solicitudes de la obra, recuenta cuántas hay
    # =========================================================================

    @api.depends('material_request_ids')
    def _compute_material_request_count(self):
        for record in self:
            record.material_request_count = len(record.material_request_ids)

    # =========================================================================
    # VALIDACIÓN DE FECHAS
    # Comprueba que la fecha de fin no sea antes que la de inicio.
    # Si alguien pone la fecha al revés, le muestra un error.
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
    # BOTONES PARA CAMBIAR EL ESTADO DE LA OBRA
    # Estos métodos se ejecutan cuando el usuario pulsa los botones en la pantalla
    # =========================================================================

    def action_set_active(self):
        """Botón "Activar": pone la obra en estado Activa (en marcha)."""
        for record in self:
            record.state = 'active'

    def action_set_finished(self):
        """Botón "Finalizar": marca la obra como terminada."""
        for record in self:
            record.state = 'finished'

    def action_set_draft(self):
        """Botón "Volver a Borrador": devuelve la obra al estado inicial."""
        for record in self:
            record.state = 'draft'

    def action_view_material_requests(self):
        """
        Botón "Ver Solicitudes": abre una ventana con todas las solicitudes
        de materiales que pertenecen a esta obra.
        """
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
