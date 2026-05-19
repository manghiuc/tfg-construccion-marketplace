# -*- coding: utf-8 -*-
from odoo import api, fields, models, _
from odoo.exceptions import UserError, ValidationError


class ConstructionMaterialRequest(models.Model):
    """
    Solicitud de materiales de construcción.
    Representa una petición de materiales para una obra concreta.
    Puede convertirse en un pedido de venta de Odoo.
    """
    _name = 'construction.material.request'
    _description = 'Solicitud de Materiales de Construcción'
    _inherit = ['mail.thread', 'mail.activity.mixin']
    _order = 'date_request desc, id desc'
    _rec_name = 'name'

    # -------------------------------------------------------------------------
    # Campos de identificación
    # -------------------------------------------------------------------------
    name = fields.Char(
        string='Referencia',
        required=True,
        copy=False,
        default=lambda self: _('Nuevo'),
        tracking=True,
        help='Referencia única de la solicitud de materiales',
    )

    # -------------------------------------------------------------------------
    # Relaciones principales
    # -------------------------------------------------------------------------
    obra_id = fields.Many2one(
        comodel_name='construction.obra',
        string='Obra',
        required=True,
        tracking=True,
        index=True,
        ondelete='restrict',
        help='Obra de construcción a la que va destinada esta solicitud',
    )
    partner_id = fields.Many2one(
        comodel_name='res.partner',
        string='Empresa/Cliente',
        tracking=True,
        help='Empresa o cliente que realiza la solicitud',
    )
    user_id = fields.Many2one(
        comodel_name='res.users',
        string='Solicitante',
        default=lambda self: self.env.user,
        tracking=True,
        help='Usuario que crea y gestiona la solicitud',
    )

    # -------------------------------------------------------------------------
    # Líneas
    # -------------------------------------------------------------------------
    line_ids = fields.One2many(
        comodel_name='construction.material.request.line',
        inverse_name='request_id',
        string='Líneas de Materiales',
        help='Detalle de los materiales solicitados',
    )

    # -------------------------------------------------------------------------
    # Estado y flujo
    # -------------------------------------------------------------------------
    state = fields.Selection(
        selection=[
            ('draft', 'Borrador'),
            ('confirmed', 'Confirmada'),
            ('in_progress', 'En Proceso'),
            ('delivered', 'Entregada'),
            ('cancelled', 'Cancelada'),
        ],
        string='Estado',
        default='draft',
        required=True,
        tracking=True,
        copy=False,
        help='Estado actual de la solicitud de materiales',
    )

    # -------------------------------------------------------------------------
    # Integración con venta
    # -------------------------------------------------------------------------
    sale_order_id = fields.Many2one(
        comodel_name='sale.order',
        string='Pedido de Venta',
        tracking=True,
        copy=False,
        readonly=True,
        help='Pedido de venta generado automáticamente al confirmar la solicitud',
    )

    # -------------------------------------------------------------------------
    # Fechas
    # -------------------------------------------------------------------------
    date_request = fields.Datetime(
        string='Fecha de Solicitud',
        default=fields.Datetime.now,
        tracking=True,
        help='Fecha y hora en que se realizó la solicitud',
    )
    date_required = fields.Date(
        string='Fecha Requerida',
        help='Fecha en la que se necesitan los materiales en la obra',
    )

    # -------------------------------------------------------------------------
    # Importe total
    # -------------------------------------------------------------------------
    total_amount = fields.Float(
        string='Importe Total',
        compute='_compute_total_amount',
        store=True,
        digits='Account',
        help='Importe total de todos los materiales solicitados',
    )

    # -------------------------------------------------------------------------
    # Información adicional
    # -------------------------------------------------------------------------
    notes = fields.Text(
        string='Notas Internas',
        help='Observaciones internas sobre la solicitud',
    )
    tracking_info = fields.Text(
        string='Información de Seguimiento',
        help='Registro de seguimiento y eventos del proceso de entrega',
        readonly=True,
        copy=False,
    )

    # =========================================================================
    # ORM
    # =========================================================================

    @api.model_create_multi
    def create(self, vals_list):
        for vals in vals_list:
            if vals.get('name', _('Nuevo')) == _('Nuevo'):
                vals['name'] = self.env['ir.sequence'].next_by_code(
                    'construction.material.request'
                ) or _('Nuevo')
            # Heredar partner de la obra si no se especifica
            if vals.get('obra_id') and not vals.get('partner_id'):
                obra = self.env['construction.obra'].browse(vals['obra_id'])
                if obra.partner_id:
                    vals['partner_id'] = obra.partner_id.id
        return super().create(vals_list)

    # =========================================================================
    # Campos computados
    # =========================================================================

    @api.depends('line_ids.subtotal')
    def _compute_total_amount(self):
        """Calcula el importe total sumando los subtotales de todas las líneas."""
        for record in self:
            record.total_amount = sum(record.line_ids.mapped('subtotal'))

    # =========================================================================
    # Onchanges
    # =========================================================================

    @api.onchange('obra_id')
    def _onchange_obra_id(self):
        """Rellena el partner desde la obra seleccionada."""
        if self.obra_id and self.obra_id.partner_id:
            self.partner_id = self.obra_id.partner_id

    # =========================================================================
    # Validaciones
    # =========================================================================

    @api.constrains('line_ids')
    def _check_lines(self):
        for record in self:
            if record.state not in ('draft', 'cancelled') and not record.line_ids:
                raise ValidationError(_(
                    'La solicitud "%s" debe tener al menos una línea de material.'
                ) % record.name)

    # =========================================================================
    # Métodos de flujo / acciones de estado
    # =========================================================================

    def action_confirm(self):
        """
        Confirma la solicitud de materiales.
        Verifica que existan líneas y cambia el estado a 'confirmed'.
        """
        for record in self:
            if not record.line_ids:
                raise UserError(_(
                    'No se puede confirmar la solicitud "%s" sin líneas de materiales.'
                ) % record.name)
            record.state = 'confirmed'
            record._add_tracking_entry(
                _('Solicitud confirmada por %s') % self.env.user.name
            )
        return True

    def action_set_in_progress(self):
        """Marca la solicitud como en proceso de preparación/envío."""
        for record in self:
            if record.state != 'confirmed':
                raise UserError(_(
                    'Solo se pueden poner en proceso solicitudes confirmadas.'
                ))
            record.state = 'in_progress'
            record._add_tracking_entry(
                _('Solicitud puesta en proceso por %s') % self.env.user.name
            )

    def action_set_delivered(self):
        """Marca la solicitud como entregada."""
        for record in self:
            record.state = 'delivered'
            record._add_tracking_entry(
                _('Materiales entregados. Marcado por %s') % self.env.user.name
            )

    def action_cancel(self):
        """Cancela la solicitud."""
        for record in self:
            if record.state == 'delivered':
                raise UserError(_(
                    'No se puede cancelar una solicitud ya entregada.'
                ))
            record.state = 'cancelled'
            record._add_tracking_entry(
                _('Solicitud cancelada por %s') % self.env.user.name
            )

    def action_reset_draft(self):
        """Vuelve la solicitud a borrador."""
        for record in self:
            if record.state not in ('confirmed', 'cancelled'):
                raise UserError(_(
                    'Solo se pueden resetear solicitudes confirmadas o canceladas.'
                ))
            record.state = 'draft'

    def action_convert_to_sale(self):
        """
        Convierte la solicitud de materiales en un pedido de venta de Odoo.
        Crea un sale.order con todas las líneas de la solicitud y vincula ambos registros.
        """
        self.ensure_one()

        if self.state not in ('confirmed', 'in_progress'):
            raise UserError(_(
                'Solo se pueden convertir a pedido de venta las solicitudes confirmadas o en proceso.'
            ))

        if self.sale_order_id:
            raise UserError(_(
                'Esta solicitud ya tiene un pedido de venta asociado: %s'
            ) % self.sale_order_id.name)

        if not self.line_ids:
            raise UserError(_(
                'No se puede convertir una solicitud sin líneas de materiales.'
            ))

        # Preparar valores del pedido de venta
        sale_order_vals = {
            'partner_id': self.partner_id.id or self.obra_id.partner_id.id,
            'origin': self.name,
            'note': _(
                'Pedido generado desde solicitud de materiales: %s\nObra: %s'
            ) % (self.name, self.obra_id.name),
            'order_line': [],
        }

        # Añadir las líneas del pedido de venta
        for line in self.line_ids:
            sale_line_vals = (0, 0, {
                'product_id': line.product_id.id,
                'name': line.product_name or line.product_id.name,
                'product_uom_qty': line.product_qty,
                'product_uom': line.product_uom_id.id or line.product_id.uom_id.id,
                'price_unit': line.price_unit,
            })
            sale_order_vals['order_line'].append(sale_line_vals)

        # Crear el pedido de venta
        sale_order = self.env['sale.order'].create(sale_order_vals)
        self.sale_order_id = sale_order

        # Registrar en el tracking
        self._add_tracking_entry(
            _('Pedido de venta %s creado por %s') % (sale_order.name, self.env.user.name)
        )

        # Notificar en el chatter
        self.message_post(
            body=_(
                'Se ha creado el pedido de venta <b>%s</b> desde esta solicitud.'
            ) % sale_order.name,
            subtype_xmlid='mail.mt_note',
        )

        # Abrir el pedido de venta recién creado
        return {
            'type': 'ir.actions.act_window',
            'name': _('Pedido de Venta'),
            'res_model': 'sale.order',
            'res_id': sale_order.id,
            'view_mode': 'form',
            'target': 'current',
        }

    # =========================================================================
    # Métodos auxiliares
    # =========================================================================

    def _add_tracking_entry(self, message):
        """Añade una entrada al campo tracking_info con timestamp."""
        now = fields.Datetime.now()
        entry = '[%s] %s' % (fields.Datetime.to_string(now), message)
        for record in self:
            current = record.tracking_info or ''
            record.tracking_info = (current + '\n' + entry).strip()

    def action_view_sale_order(self):
        """Navega al pedido de venta vinculado."""
        self.ensure_one()
        if not self.sale_order_id:
            raise UserError(_('Esta solicitud no tiene un pedido de venta asociado.'))
        return {
            'type': 'ir.actions.act_window',
            'name': _('Pedido de Venta'),
            'res_model': 'sale.order',
            'res_id': self.sale_order_id.id,
            'view_mode': 'form',
            'target': 'current',
        }
