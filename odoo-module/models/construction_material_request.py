# -*- coding: utf-8 -*-
# ============================================================================
# MODELO: SOLICITUD DE MATERIALES DE CONSTRUCCIÓN
# Este es el modelo principal del marketplace. Representa un "pedido" de
# materiales que un cliente hace para una obra.
#
# Flujo de estados:
#   Borrador → Tramitando → En Preparación → En Reparto → Entregado
#                                                        → Cancelado
#
# Cada solicitud puede convertirse en un Pedido de Venta de Odoo para
# facturar al cliente.
# ============================================================================
import logging
from odoo import api, fields, models, _
from odoo.exceptions import UserError, ValidationError

_logger = logging.getLogger(__name__)


class ConstructionMaterialRequest(models.Model):
    """
    Solicitud de materiales de construcción.
    Representa una petición de materiales para una obra concreta.
    Puede convertirse en un pedido de venta de Odoo.
    """
    _name = 'construction.material.request'
    _description = 'Solicitud de Materiales de Construcción'
    # Hereda mensajería para tener historial de cambios
    _inherit = ['mail.thread', 'mail.activity.mixin']
    # Se ordenan por fecha más reciente primero
    _order = 'date_request desc, id desc'
    _rec_name = 'name'

    # -------------------------------------------------------------------------
    # REFERENCIA (número de la solicitud, ej: SOL-2025-0001)
    # Se genera automáticamente al crear la solicitud
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
    # RELACIONES PRINCIPALES (a quién y para qué obra)
    # -------------------------------------------------------------------------

    # Obra de construcción a la que va destinado el pedido
    obra_id = fields.Many2one(
        comodel_name='construction.obra',
        string='Obra',
        required=False,
        tracking=True,
        index=True,
        ondelete='restrict',
        help='Obra de construcción a la que va destinada esta solicitud',
    )

    # Cliente que hace la solicitud
    partner_id = fields.Many2one(
        comodel_name='res.partner',
        string='Empresa/Cliente',
        tracking=True,
        help='Empresa o cliente que realiza la solicitud',
    )

    # Usuario de Odoo que crea la solicitud (por defecto, el usuario actual)
    user_id = fields.Many2one(
        comodel_name='res.users',
        string='Solicitante',
        default=lambda self: self.env.user,
        tracking=True,
        help='Usuario que crea y gestiona la solicitud',
    )

    # -------------------------------------------------------------------------
    # LÍNEAS DE MATERIALES (los productos que se piden)
    # Es una lista de líneas, cada una con un producto, cantidad y precio
    # -------------------------------------------------------------------------
    line_ids = fields.One2many(
        comodel_name='construction.material.request.line',
        inverse_name='request_id',
        string='Líneas de Materiales',
        help='Detalle de los materiales solicitados',
    )

    # -------------------------------------------------------------------------
    # ESTADO (en qué fase está la solicitud)
    # "group_expand" hace que en la vista kanban se muestren todas las columnas
    # -------------------------------------------------------------------------
    state = fields.Selection(
        selection=[
            ('draft', 'Borrador'),              # Recién creada, sin confirmar
            ('confirmed', 'Tramitando'),         # Confirmada, pendiente de preparar
            ('en_preparacion', 'En Preparación'),# Se están preparando los materiales en almacén
            ('en_reparto', 'En Reparto'),         # Salió del almacén, va de camino a la obra
            ('in_progress', 'En Proceso'),        # Estado antiguo (se mantiene por compatibilidad)
            ('delivered', 'Entregado'),            # Los materiales llegaron a la obra
            ('cancelled', 'Cancelado'),            # La solicitud se canceló
        ],
        string='Estado',
        default='draft',
        required=True,
        tracking=True,
        copy=False,
        group_expand='_read_group_state',
        help='Estado actual de la solicitud de materiales',
    )

    @api.model
    def _read_group_state(self, values, domain, order=None):
        """Muestra siempre todas las columnas en la vista kanban, aunque no haya registros."""
        return ['confirmed', 'en_preparacion', 'en_reparto', 'delivered']

    # Dirección donde hay que entregar los materiales
    delivery_address = fields.Char(
        string='Dirección de Entrega',
        help='Dirección de entrega introducida por el cliente al tramitar el pedido',
    )

    # -------------------------------------------------------------------------
    # CONEXIÓN CON EL PEDIDO DE VENTA DE ODOO
    # Cuando se convierte la solicitud en pedido de venta, se guarda aquí
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
    # FECHAS
    # -------------------------------------------------------------------------

    # Cuándo se hizo la solicitud (por defecto, ahora mismo)
    date_request = fields.Datetime(
        string='Fecha de Solicitud',
        default=fields.Datetime.now,
        tracking=True,
        help='Fecha y hora en que se realizó la solicitud',
    )

    # Para cuándo se necesitan los materiales
    date_required = fields.Date(
        string='Fecha Requerida',
        help='Fecha en la que se necesitan los materiales en la obra',
    )

    # -------------------------------------------------------------------------
    # IMPORTE TOTAL (se calcula sumando todas las líneas)
    # -------------------------------------------------------------------------
    total_amount = fields.Float(
        string='Importe Total',
        compute='_compute_total_amount',
        store=True,
        digits='Account',
        help='Importe total de todos los materiales solicitados',
    )

    # -------------------------------------------------------------------------
    # ENTREGA Y TRANSPORTE
    # -------------------------------------------------------------------------

    # Si el pedido es urgente (se aplica un suplemento extra al transporte)
    is_urgent = fields.Boolean(
        string='Pedido Urgente',
        default=False,
        tracking=True,
        help='Activa el suplemento de urgencia en el cálculo del transporte',
    )

    # Instrucciones para el repartidor (horario, cómo acceder a la obra...)
    delivery_notes = fields.Text(
        string='Preferencias / Notas de Entrega',
        help='Instrucciones especiales para el repartidor: horario, acceso, etc.',
    )

    # Coordenadas GPS del punto de entrega (las envía la app Android)
    delivery_lat = fields.Float(
        string='Latitud de Entrega',
        digits=(9, 6),
        help='Coordenada GPS de latitud del punto de entrega',
    )
    delivery_lon = fields.Float(
        string='Longitud de Entrega',
        digits=(9, 6),
        help='Coordenada GPS de longitud del punto de entrega',
    )

    # Coste del transporte calculado
    transport_cost = fields.Float(
        string='Coste de Transporte (€)',
        digits=(10, 2),
        default=0.0,
        help='Coste calculado del transporte según distancia, peso y urgencia',
    )

    # Distancia desde el almacén hasta el punto de entrega
    transport_distance_km = fields.Float(
        string='Distancia de Entrega (km)',
        digits=(10, 3),
        default=0.0,
        help='Distancia en km entre el almacén y el punto de entrega',
    )

    # Descuento por nivel de fidelización del cliente (% aplicado al crear el pedido)
    discount_percent = fields.Float(
        string='Descuento Fidelización (%)',
        digits=(5, 2),
        default=0.0,
        help='Porcentaje de descuento aplicado automáticamente según el nivel de fidelización del cliente',
    )

    # Total final = precio materiales + coste transporte
    total_with_transport = fields.Float(
        string='Total con Transporte (€)',
        compute='_compute_total_with_transport',
        store=True,
        digits='Account',
        help='Suma del importe total de materiales y el coste de transporte',
    )

    # -------------------------------------------------------------------------
    # NOTAS E INFORMACIÓN DE SEGUIMIENTO
    # -------------------------------------------------------------------------

    # Observaciones internas
    notes = fields.Text(
        string='Notas Internas',
        help='Observaciones internas sobre la solicitud',
    )

    # -------------------------------------------------------------------------
    # CONTROL DE STOCK (para no descontar dos veces)
    # Este campo se marca como "Sí" cuando ya se ha restado el stock
    # al pasar el pedido a "En Preparación". Así, si alguien vuelve a
    # pulsar el botón por error, no se descuenta otra vez.
    # -------------------------------------------------------------------------
    stock_reducido = fields.Boolean(
        string='Stock ya descontado',
        default=False,
        copy=False,
        help='Se marca automáticamente cuando el stock se descuenta al entrar en preparación. '
             'Evita que se descuente dos veces por error.',
    )

    # Historial de seguimiento (se rellena solo cada vez que cambia el estado)
    tracking_info = fields.Text(
        string='Información de Seguimiento',
        help='Registro de seguimiento y eventos del proceso de entrega',
        readonly=True,
        copy=False,
    )

    # =========================================================================
    # CUANDO SE CREA UNA SOLICITUD NUEVA
    # - Genera un número automático (SOL-2025-0001)
    # - Si la obra tiene un cliente asignado, lo copia a la solicitud
    # =========================================================================

    @api.model_create_multi
    def create(self, vals_list):
        for vals in vals_list:
            # Generar referencia automática
            if vals.get('name', _('Nuevo')) == _('Nuevo'):
                vals['name'] = self.env['ir.sequence'].sudo().next_by_code(
                    'construction.material.request'
                ) or _('Nuevo')
            # Si se elige una obra y no se ha puesto cliente, copiar el de la obra
            if vals.get('obra_id') and not vals.get('partner_id'):
                obra = self.env['construction.obra'].browse(vals['obra_id'])
                if obra.partner_id:
                    vals['partner_id'] = obra.partner_id.id
        return super().create(vals_list)

    # =========================================================================
    # CÁLCULOS AUTOMÁTICOS
    # =========================================================================

    @api.depends('line_ids.subtotal')
    def _compute_total_amount(self):
        """Suma los subtotales de todas las líneas para obtener el total."""
        for record in self:
            record.total_amount = sum(record.line_ids.mapped('subtotal'))

    @api.depends('total_amount', 'transport_cost')
    def _compute_total_with_transport(self):
        """Suma materiales + transporte para obtener el total final."""
        for record in self:
            record.total_with_transport = record.total_amount + record.transport_cost

    # =========================================================================
    # AUTORELLENO
    # Cuando se elige una obra, se copia automáticamente su cliente
    # =========================================================================

    @api.onchange('obra_id')
    def _onchange_obra_id(self):
        """Rellena el partner desde la obra seleccionada."""
        if self.obra_id and self.obra_id.partner_id:
            self.partner_id = self.obra_id.partner_id

    # =========================================================================
    # VALIDACIONES (comprobaciones de seguridad)
    # =========================================================================

    @api.constrains('line_ids')
    def _check_lines(self):
        """No permite solicitudes confirmadas sin ningún material."""
        for record in self:
            if record.state not in ('draft', 'cancelled') and not record.line_ids:
                raise ValidationError(_(
                    'La solicitud "%s" debe tener al menos una línea de material.'
                ) % record.name)

    # =========================================================================
    # BOTONES PARA CAMBIAR EL ESTADO DE LA SOLICITUD
    # Cada método es un botón que el usuario pulsa en la pantalla
    # =========================================================================

    def action_confirm(self):
        """
        Botón "Confirmar": pasa la solicitud de Borrador a Tramitando.
        - Comprueba que tenga al menos una línea de material
        - Registra quién confirmó y cuándo en el historial
        - Otorga puntos de fidelización al cliente (1 punto por cada 10€)
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
            # Dar puntos de fidelización al cliente (no bloquea si falla)
            if record.partner_id and record.total_amount:
                try:
                    record.partner_id._add_points(record.total_amount)
                except Exception as e:
                    _logger.warning(
                        "Error al otorgar puntos de fidelización en solicitud %s: %s",
                        record.name, e
                    )
        return True

    def action_set_en_preparacion(self):
        """
        Botón "En Preparación": indica que se están preparando los materiales
        en el almacén.

        Además, descuenta automáticamente el stock del marketplace para cada
        producto del pedido. Solo lo hace UNA VEZ: si el pedido ya tuvo su
        stock descontado antes, no lo vuelve a hacer.

        Si un producto no tiene suficiente stock, se permite continuar pero
        se deja un aviso en el registro (log) para que alguien lo revise.
        """
        for record in self:
            if record.state not in ('confirmed', 'draft'):
                raise UserError(_('Solo se pueden poner en preparación solicitudes tramitadas.'))
            record.state = 'en_preparacion'

            # --- REDUCCIÓN DE STOCK ---
            # Solo descontamos si no se ha hecho antes (para no restar dos veces)
            if not record.stock_reducido:
                record._reducir_stock_marketplace()

            record._add_tracking_entry(
                _('Solicitud en preparación en almacén. Acción de %s') % self.env.user.name
            )
        return True

    def _reducir_stock_marketplace(self):
        """
        Recorre cada línea del pedido y resta la cantidad pedida del stock
        del marketplace (campo qty_marketplace en el producto).

        - Si el producto tiene stock suficiente, lo resta sin problemas.
        - Si NO tiene suficiente stock, resta lo que haya (puede quedar en
          negativo) y deja un aviso en el log para que alguien lo revise.
        - Al final, marca el pedido como "stock ya descontado" para que no
          se pueda descontar otra vez por error.
        """
        self.ensure_one()
        for linea in self.line_ids:
            producto = linea.product_id
            plantilla = producto.product_tmpl_id
            cantidad_pedida = linea.product_qty
            stock_actual = plantilla.qty_marketplace or 0

            # Comprobar si hay suficiente stock
            if stock_actual < cantidad_pedida:
                # No hay suficiente, pero dejamos continuar y avisamos
                _logger.warning(
                    "Stock insuficiente para '%s' en solicitud %s: "
                    "se pidieron %.2f pero solo hay %d en el marketplace.",
                    producto.name, self.name, cantidad_pedida, stock_actual
                )
                # Registrar el aviso en el historial del pedido
                self._add_tracking_entry(
                    _('AVISO: Stock insuficiente para "%s". '
                      'Se pidieron %.2f unidades pero solo hay %d disponibles.')
                    % (producto.name, cantidad_pedida, stock_actual)
                )

            # Restar la cantidad pedida del stock del marketplace
            nuevo_stock = stock_actual - int(cantidad_pedida)
            plantilla.sudo().write({'qty_marketplace': nuevo_stock})

            _logger.info(
                "Stock actualizado para '%s': %d → %d (solicitud %s, cantidad: %.2f)",
                producto.name, stock_actual, nuevo_stock, self.name, cantidad_pedida
            )

        # Marcar que ya se descontó el stock (para no hacerlo dos veces)
        self.stock_reducido = True
        self._add_tracking_entry(
            _('Stock del marketplace descontado automáticamente para todos los productos del pedido.')
        )

    def action_set_en_reparto(self):
        """Botón "En Reparto": los materiales han salido del almacén y van de camino a la obra."""
        for record in self:
            if record.state not in ('en_preparacion', 'confirmed', 'in_progress'):
                raise UserError(_('Solo se pueden poner en reparto solicitudes en preparación.'))
            record.state = 'en_reparto'
            record._add_tracking_entry(
                _('Materiales en reparto / en camino a la obra. Acción de %s') % self.env.user.name
            )

    def action_set_in_progress(self):
        """Estado antiguo: redirige a "En Preparación" por compatibilidad."""
        return self.action_set_en_preparacion()

    def action_set_delivered(self):
        """Botón "Entregado": los materiales han llegado a la obra."""
        for record in self:
            record.state = 'delivered'
            record._add_tracking_entry(
                _('Materiales entregados en obra. Marcado por %s') % self.env.user.name
            )

    def action_cancel(self):
        """Botón "Cancelar": cancela la solicitud (no se puede si ya está entregada)."""
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
        """Botón "Volver a Borrador": devuelve la solicitud al estado inicial."""
        for record in self:
            if record.state not in ('confirmed', 'cancelled'):
                raise UserError(_(
                    'Solo se pueden resetear solicitudes confirmadas o canceladas.'
                ))
            record.state = 'draft'

    def action_convert_to_sale(self):
        """
        Botón "Convertir a Pedido de Venta":
        Crea un Pedido de Venta de Odoo con todos los materiales de la solicitud.
        Así se puede facturar al cliente desde el flujo normal de Odoo.
        - Copia todas las líneas (producto, cantidad, precio)
        - Vincula el pedido de venta con la solicitud
        - Registra la acción en el historial
        - Abre el pedido de venta creado
        """
        self.ensure_one()

        # Solo se puede convertir si está confirmada o en proceso
        if self.state not in ('confirmed', 'en_preparacion', 'en_reparto', 'in_progress'):
            raise UserError(_(
                'Solo se pueden convertir a pedido de venta las solicitudes confirmadas o en proceso.'
            ))

        # No se puede crear dos veces el pedido de venta
        if self.sale_order_id:
            raise UserError(_(
                'Esta solicitud ya tiene un pedido de venta asociado: %s'
            ) % self.sale_order_id.name)

        # Tiene que tener materiales
        if not self.line_ids:
            raise UserError(_(
                'No se puede convertir una solicitud sin líneas de materiales.'
            ))

        # Preparar los datos del pedido de venta
        sale_order_vals = {
            'partner_id': self.partner_id.id or self.obra_id.partner_id.id,
            'origin': self.name,
            'note': _(
                'Pedido generado desde solicitud de materiales: %s\nObra: %s'
            ) % (self.name, self.obra_id.name),
            'order_line': [],
        }

        # Copiar cada línea de material al pedido de venta
        for line in self.line_ids:
            sale_line_vals = (0, 0, {
                'product_id': line.product_id.id,
                'name': line.product_name or line.product_id.name,
                'product_uom_qty': line.product_qty,
                'product_uom': line.product_uom_id.id or line.product_id.uom_id.id,
                'price_unit': line.price_unit,
            })
            sale_order_vals['order_line'].append(sale_line_vals)

        # Crear el pedido de venta en Odoo
        sale_order = self.env['sale.order'].create(sale_order_vals)
        self.sale_order_id = sale_order

        # Registrar en el historial
        self._add_tracking_entry(
            _('Pedido de venta %s creado por %s') % (sale_order.name, self.env.user.name)
        )

        # Poner un mensaje en el chatter
        self.message_post(
            body=_(
                'Se ha creado el pedido de venta <b>%s</b> desde esta solicitud.'
            ) % sale_order.name,
            subtype_xmlid='mail.mt_note',
        )

        # Abrir el pedido de venta recién creado en pantalla
        return {
            'type': 'ir.actions.act_window',
            'name': _('Pedido de Venta'),
            'res_model': 'sale.order',
            'res_id': sale_order.id,
            'view_mode': 'form',
            'target': 'current',
        }

    # =========================================================================
    # CÁLCULO DE TRANSPORTE
    # Usa la calculadora de transporte para estimar el coste de envío
    # basándose en las coordenadas GPS, el peso y si es urgente
    # =========================================================================

    def compute_transport_cost(self):
        """
        Calcula el coste de enviar los materiales a la obra.
        Necesita que la app Android haya enviado las coordenadas GPS.
        El peso se estima sumando la cantidad de cada producto × su peso unitario.
        """
        for record in self:
            # Si no hay coordenadas GPS, no se puede calcular
            if not record.delivery_lat or not record.delivery_lon:
                continue
            # Obtener la configuración de transporte
            calculator = self.env['construction.transport.calculator'].get_default_calculator()
            # Calcular el peso total (suma de cantidad × peso de cada producto)
            total_weight_kg = sum(
                line.product_qty * (line.product_id.weight or 1.0)
                for line in record.line_ids
            )
            # Calcular el coste
            result = calculator.calculate_transport_cost(
                delivery_lat=record.delivery_lat,
                delivery_lon=record.delivery_lon,
                total_weight_kg=total_weight_kg,
                is_urgent=record.is_urgent,
            )
            # Guardar los resultados
            record.transport_cost = result['total']
            record.transport_distance_km = result['distance_km']
            record._add_tracking_entry(
                _('Transporte calculado: %.2f € — %.3f km') % (result['total'], result['distance_km'])
            )
        return True

    # =========================================================================
    # MÉTODOS AUXILIARES
    # =========================================================================

    def _add_tracking_entry(self, message):
        """
        Añade una línea al historial de seguimiento con la fecha y hora actual.
        Ejemplo: "[2025-04-10 09:30:00] Solicitud confirmada por Admin"
        """
        now = fields.Datetime.now()
        entry = '[%s] %s' % (fields.Datetime.to_string(now), message)
        for record in self:
            current = record.tracking_info or ''
            record.tracking_info = (current + '\n' + entry).strip()

    def action_view_sale_order(self):
        """Botón que abre el pedido de venta vinculado a esta solicitud."""
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

    # =========================================================================
    # TAREAS PROGRAMADAS (CRON JOBS)
    # Son procesos que se ejecutan solos cada cierto tiempo, sin que nadie
    # tenga que hacer nada. Odoo los ejecuta automáticamente.
    # =========================================================================

    @api.model
    def _cron_update_request_states(self):
        """
        TAREA AUTOMÁTICA DIARIA:
        Revisa todas las solicitudes confirmadas y, si su pedido de venta
        en Odoo ya está como "hecho/entregado", marca la solicitud como
        entregada también. Así se sincronizan ambos sistemas.
        """
        solicitudes = self.search([('state', 'in', ['confirmed', 'in_progress'])])
        for sol in solicitudes:
            if sol.sale_order_id and sol.sale_order_id.state == 'done':
                now_str = fields.Datetime.to_string(fields.Datetime.now())
                msg = '[AUTO] Pedido entregado según Odoo - %s' % now_str
                sol.write({
                    'state': 'delivered',
                    'tracking_info': ((sol.tracking_info or '') + '\n' + msg).strip(),
                })

    @api.model
    def _cron_alert_pending_requests(self):
        """
        TAREA AUTOMÁTICA CADA 4 HORAS:
        Busca solicitudes que llevan más de 48 horas confirmadas sin que nadie
        las haya empezado a procesar, y pone un aviso en el chatter para que
        alguien las atienda.
        """
        from datetime import datetime, timedelta
        hace_48h = datetime.now() - timedelta(hours=48)
        solicitudes = self.search([
            ('state', '=', 'confirmed'),
            ('date_request', '<', hace_48h),
        ])
        for sol in solicitudes:
            sol.message_post(
                body=_(
                    'ALERTA: Esta solicitud lleva más de 48 horas confirmada sin iniciar. '
                    'Por favor, revísala.'
                ),
                subtype_xmlid='mail.mt_note',
            )

    @api.model
    def _cron_weekly_material_report(self):
        """
        TAREA AUTOMÁTICA SEMANAL:
        Genera un informe con los 10 materiales más pedidos de la semana
        y lo publica como mensaje interno para que el equipo vea qué se
        está vendiendo más.
        """
        from collections import Counter
        lineas = self.env['construction.material.request.line'].search([])
        # Contar cuántas unidades se han pedido de cada producto
        conteo = Counter()
        for linea in lineas:
            conteo[linea.product_id.name] += linea.product_qty
        # Coger los 10 más pedidos
        top10 = conteo.most_common(10)
        # Crear el mensaje en formato HTML
        body = '<h3>Top 10 Materiales más solicitados esta semana</h3><ol>'
        for nombre, qty in top10:
            body += '<li>%s: %.0f unidades</li>' % (nombre, qty)
        body += '</ol>'
        # Publicar el mensaje
        self.env['mail.message'].create({
            'subject': 'Reporte semanal: materiales más solicitados',
            'body': body,
            'message_type': 'email',
            'model': 'construction.material.request',
        })
