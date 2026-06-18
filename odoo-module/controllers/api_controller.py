# -*- coding: utf-8 -*-
"""
API REST del Marketplace de Construcción
=========================================
Este archivo es el "cerebro" de la comunicación entre los clientes
(portal web y app Android) y la base de datos de Odoo.

Cada función con @http.route es un endpoint — una URL a la que el
frontend envía peticiones HTTP (GET para pedir datos, POST para enviar datos).

"""
import json
import logging
from odoo import http
from odoo.http import request
from odoo.exceptions import AccessDenied, ValidationError, UserError

_logger = logging.getLogger(__name__)


class ConstructionAPI(http.Controller):

    # =====================================================================
    # FUNCIONES AUXILIARES
    # =====================================================================

    def _ok(self, data=None, **kw):
        """Respuesta JSON de éxito."""
        payload = dict(success=True, data=data)
        payload.update(kw)
        return request.make_json_response(payload, status=200)

    def _err(self, msg, code=400):
        """Respuesta JSON de error."""
        return request.make_json_response(
            dict(success=False, error=dict(code=code, message=msg)),
            status=code,
        )

    def _require_uid(self):
        """Comprueba que el usuario está logueado.
        Soporta dos métodos de autenticación:
          1) Cookie del navegador (portal web)
          2) Header X-Openerp-Session-Id (app Android)
        Devuelve (uid, None) si OK, o (None, error_response) si no está logueado."""
        # Método 1: Cookie estándar del navegador
        uid = request.session.uid
        if uid:
            return uid, None

        # Método 2: Header de sesión (app Android)
        session_id = (
            request.httprequest.headers.get("X-Openerp-Session-Id")
            or request.httprequest.headers.get("X-Openerp-Session-id")
            or ""
        ).strip()
        if session_id:
            try:
                from odoo.http import root as http_root
                stored = http_root.session_store.get(session_id)
                stored_uid = stored.get('uid') if hasattr(stored, 'get') else getattr(stored, 'uid', None)
                if stored and stored_uid:
                    return stored_uid, None
            except Exception as e:
                _logger.warning("Error al cargar sesión '%s': %s", session_id, e)

        return None, self._err("No autenticado. Por favor inicia sesión.", 401)

    def _format_user(self, user, partner=None):
        """Prepara los datos del usuario para enviarlos al frontend.
        Incluye nombre, email, tipo de cliente, puntos y nivel de fidelización."""
        if partner is None:
            partner = user.partner_id
        return dict(
            id=user.id,
            name=user.name,
            login=user.login,
            session_id=request.session.sid or "",
            partner_type="empresa" if partner.is_company else "particular",
            points_balance=int(partner.points_balance or 0),
            loyalty_level=partner.loyalty_level or 'bronze',
            loyalty_discount=float(partner.loyalty_discount or 0),
            phone=partner.phone or None,
            email=partner.email or None,
            partner_id=partner.id,
        )

    # Etiquetas de estado en español para mostrar en el frontend
    _STATE_LABELS = {
        'draft':          'Borrador',
        'confirmed':      'Tramitando',
        'en_preparacion': 'En Preparación',
        'en_reparto':     'En Reparto',
        'delivered':      'Entregado',
        'cancelled':      'Cancelado',
    }

    def _format_request(self, r):
        """Convierte un pedido de la BD en un diccionario JSON
        con todos sus datos: estado, importes, descuento, transporte y líneas."""
        delivery_address = r.delivery_address or ''
        if not delivery_address and r.notes and '📍 Dirección de entrega:' in r.notes:
            delivery_address = r.notes.split('\n')[0].replace('📍 Dirección de entrega:', '').strip()
        return {
            "id": r.id,
            "name": r.name,
            "state": r.state,
            "state_label": self._STATE_LABELS.get(r.state, r.state),
            "obra_id": r.obra_id.id if r.obra_id else 0,
            "obra_name": r.obra_id.name if r.obra_id else "",
            "total_amount": float(r.total_amount or 0),
            "discount_percent": 0,
            "discount_amount": 0,
            "transport_cost": float(r.transport_cost or 0),
            "total_with_transport": float(r.total_with_transport or r.total_amount or 0),
            "is_urgent": bool(r.is_urgent),
            "notes": r.notes or "",
            "delivery_address": delivery_address,
            "create_date": str(r.date_request)[:19] if r.date_request else None,
            "lines": [{
                "id": l.id,
                "product_id": l.product_id.id,
                "product_name": l.product_id.name,
                "qty": float(l.product_qty),
                "uom": l.product_id.uom_id.name if l.product_id.uom_id else "",
                "price_unit": float(l.price_unit),
                "subtotal": float(l.subtotal),
                "image_url": "/web/image/product.product/%d/image_128" % l.product_id.id,
            } for l in r.line_ids],
        }

    # =====================================================================
    # AUTENTICACIÓN — Login, registro y cierre de sesión
    # =====================================================================

    @http.route("/api/construction/auth/login", type="http", auth="public", methods=["POST"], csrf=False)
    def auth_login(self, **kw):
        """Inicio de sesión. Recibe email y contraseña,
        los valida contra la base de datos y devuelve los datos del usuario."""
        try:
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            _login = (p.get("login") or p.get("email") or "").strip()
            _password = p.get("password") or ""
            if not _login or not _password:
                return self._err("Login y password requeridos", 400)
            _db = p.get("db") or request.db or "construction_marketplace"
            uid = request.session.authenticate(_db, _login, _password)
            if not uid:
                return self._err("Credenciales incorrectas", 401)
            user = request.env["res.users"].sudo().browse(uid)
            return self._ok(data=dict(user=self._format_user(user)))
        except AccessDenied:
            return self._err("Acceso denegado", 401)
        except Exception as e:
            _logger.error("Error en login: %s", str(e))
            return self._err(str(e), 500)

    @http.route("/api/construction/auth/register", type="http", auth="public", methods=["POST"], csrf=False)
    def auth_register(self, **kw):
        """Registro de usuario nuevo. Crea el usuario en Odoo,
        le asigna acceso de tipo 'portal' y hace login automático."""
        try:
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            name = (p.get("name") or "").strip()
            email = (p.get("login") or p.get("email") or "").strip().lower()
            password = p.get("password") or ""
            phone = (p.get("phone") or "").strip()
            partner_type = p.get("partner_type") or p.get("customer_type") or "particular"
            company_name = (p.get("company_name") or "").strip()
            vat = (p.get("vat") or "").strip()

            if not name or not email or not password:
                return self._err("Nombre, email y contraseña son obligatorios", 400)
            if len(password) < 6:
                return self._err("La contraseña debe tener al menos 6 caracteres", 400)

            # Comprobar si ya existe un usuario con ese email
            if request.env["res.users"].sudo().search([("login", "=", email)], limit=1):
                return self._err("Ya existe una cuenta con ese correo electrónico", 409)

            # Crear usuario con acceso portal (solo puede ver sus datos)
            portal_group = request.env.ref("base.group_portal")
            new_user = request.env["res.users"].sudo().create({
                "name": name, "login": email, "email": email, "password": password,
                "groups_id": [(6, 0, [portal_group.id])],
            })

            # Completar datos del contacto (partner)
            partner = new_user.partner_id
            is_company = (partner_type == "empresa")
            write_vals = {
                "phone": phone or False,
                "is_company": is_company,
                "partner_type": partner_type,
                "customer_rank": 1,
            }
            if company_name and is_company:
                write_vals["name"] = company_name
            if vat:
                write_vals["vat"] = vat
            partner.sudo().write(write_vals)

            # Intentar login automático tras el registro
            auto_login_ok = False
            try:
                uid = request.session.authenticate(
                    request.db or "construction_marketplace", email, password
                )
                auto_login_ok = bool(uid)
            except Exception:
                pass

            user_data = self._format_user(new_user, partner)
            if not auto_login_ok:
                user_data["session_id"] = ""
            return self._ok(data=dict(
                user=user_data,
                auto_login=auto_login_ok,
                message="Bienvenido/a, %s." % name if auto_login_ok
                        else "Cuenta creada. Por favor inicia sesión.",
            ))
        except Exception as e:
            _logger.error("Error en registro: %s", str(e), exc_info=True)
            return self._err("Error al crear la cuenta: " + str(e), 500)

    @http.route("/api/construction/auth/logout", type="http", auth="public", methods=["POST"], csrf=False)
    def auth_logout(self, **kw):
        """Cierra la sesión del usuario."""
        try:
            request.session.logout(keep_db=True)
        except Exception:
            pass
        return request.make_json_response({"success": True})

    @http.route("/api/construction/auth/session", type="http", auth="public", methods=["GET"], csrf=False)
    def auth_session(self, **kw):
        """Comprueba si la sesión sigue activa (se usa al recargar la página)."""
        uid = request.session.uid
        if not uid:
            return self._err("No autenticado", 401)
        try:
            user = request.env["res.users"].sudo().browse(uid)
            return request.make_json_response({"success": True, "data": {
                "uid": uid, "name": user.name,
                "login": user.login, "partner_id": user.partner_id.id,
            }})
        except Exception as e:
            return self._err(str(e), 500)

    # =====================================================================
    # OBRAS — Crear y listar obras de construcción
    # =====================================================================

    @http.route("/api/construction/obras", type="http", auth="public", methods=["GET"], csrf=False)
    def get_obras(self, page="0", page_size="20", **kw):
        """Lista todas las obras del usuario logueado."""
        uid, err = self._require_uid()
        if err:
            return err
        try:
            orm = request.env
            _limit = int(page_size)
            _offset = int(page) * _limit
            partner = orm["res.users"].sudo().browse(uid).partner_id
            domain = [("partner_id", "=", partner.id)]
            items = orm["construction.obra"].sudo().search(domain, limit=_limit, offset=_offset)
            data = [{
                "id": o.id, "name": o.name, "code": o.code, "state": o.state,
                "address": getattr(o, 'address', "") or "",
                "material_request_count": o.material_request_count,
            } for o in items]
            return self._ok(data=data, total=orm["construction.obra"].sudo().search_count(domain))
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/obras", type="http", auth="public", methods=["POST"], csrf=False)
    def create_obra(self, **kw):
        """Crea una obra nueva con nombre y dirección."""
        uid, err = self._require_uid()
        if err:
            return err
        try:
            orm = request.env
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            name = (p.get("name") or "").strip()
            if not name:
                return self._err("El nombre de la obra es obligatorio", 400)
            partner = orm["res.users"].sudo().browse(uid).partner_id
            code = orm["ir.sequence"].sudo().next_by_code("construction.obra") or ("OBR-%d" % uid)
            vals = {"name": name, "code": code, "partner_id": partner.id, "state": "active"}
            address = (p.get("address") or "").strip()
            if address:
                vals["address"] = address
            obra = orm["construction.obra"].sudo().create(vals)
            return self._ok(data={
                "id": obra.id, "name": obra.name, "code": obra.code,
                "state": obra.state, "address": address,
                "material_request_count": 0,
            })
        except (ValidationError, UserError) as e:
            return self._err(str(e), 422)
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/obras/<int:obra_id>", type="http", auth="public", methods=["GET"], csrf=False)
    def get_obra_detail(self, obra_id, **kw):
        """Devuelve los datos de una obra concreta."""
        try:
            obra = request.env["construction.obra"].sudo().browse(obra_id)
            if not obra.exists():
                return self._err("Obra no encontrada", 404)
            return self._ok(data={
                "id": obra.id, "name": obra.name, "code": obra.code,
                "state": obra.state,
                "address": getattr(obra, 'address', "") or "",
                "partner": obra.partner_id.name if obra.partner_id else "",
                "material_request_count": obra.material_request_count,
            })
        except Exception as e:
            return self._err(str(e), 500)

    # =====================================================================
    # PEDIDOS DE MATERIALES — Crear, listar y consultar estado
    # =====================================================================

    @http.route("/api/construction/material_request", type="http", auth="public", methods=["POST"], csrf=False)
    def create_request(self, **kw):
        """Crea un pedido de materiales.
        Recibe: productos con cantidades, obra destino, dirección, si es urgente.
        Proceso:
          1. Valida los productos contra el catálogo real
          2. Guarda el pedido en la BD
          3. Calcula el transporte (GPS o por peso)
          4. Aplica descuento por fidelización según nivel del cliente
          5. Confirma el pedido (estado: Tramitando)"""
        uid, err = self._require_uid()
        if err:
            return err
        try:
            orm = request.env
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")

            # Determinar a qué cliente pertenece el pedido
            obra_id_raw = p.get("obra_id")
            obra_id = int(obra_id_raw) if obra_id_raw else False
            if obra_id:
                obra = orm["construction.obra"].sudo().browse(obra_id)
                if not obra.exists():
                    return self._err("Obra no encontrada", 404)
                partner_id = obra.partner_id.id or False
            else:
                partner_id = orm["res.users"].sudo().browse(uid).partner_id.id

            # Validar que hay productos y que existen en el catálogo
            raw_lines = p.get("lines", [])
            if not raw_lines:
                return self._err("El pedido no tiene productos", 400)
            lines = []
            for l in raw_lines:
                pid = int(l["product_id"])
                prod = orm["product.product"].sudo().browse(pid)
                if not prod.exists():
                    return self._err("Producto con ID %d no encontrado" % pid, 400)
                lines.append((0, 0, {
                    "product_id": pid,
                    "product_qty": float(l.get("qty") or l.get("product_qty") or 1),
                    "price_unit": float(l.get("price_unit", prod.lst_price or 0)),
                    "notes": l.get("notes", ""),
                }))

            # Datos de entrega
            delivery_address = p.get("delivery_address", "")
            is_urgent = bool(p.get("is_urgent", False))
            delivery_lat = float(p.get("delivery_lat", 0) or 0)
            delivery_lon = float(p.get("delivery_lon", 0) or 0)

            # Guardar el pedido en la base de datos
            vals = {
                "user_id": uid,
                "partner_id": partner_id,
                "notes": p.get("notes", "") or "",
                "delivery_address": delivery_address,
                "is_urgent": is_urgent,
                "delivery_lat": delivery_lat,
                "delivery_lon": delivery_lon,
                "line_ids": lines,
            }
            if obra_id:
                vals["obra_id"] = obra_id
            req = orm["construction.material.request"].sudo().create(vals)

            # Calcular transporte: GPS tiene prioridad sobre cálculo por peso
            if delivery_lat and delivery_lon:
                req.sudo().compute_transport_cost()
            elif float(p.get("transport_cost", 0) or 0) > 0:
                req.sudo().write({"transport_cost": float(p["transport_cost"])})

            # Aplicar descuento por nivel de fidelización del cliente (si el campo existe)
            try:
                partner = orm["res.users"].sudo().browse(uid).partner_id
                discount_pct = float(getattr(partner, 'loyalty_discount', 0) or 0)
                if discount_pct > 0 and 'discount_percent' in orm["construction.material.request"]._fields:
                    req.sudo().write({"discount_percent": discount_pct})
            except Exception as e:
                _logger.warning("No se pudo aplicar descuento de fidelización: %s", e)

            # Confirmar pedido (pasa de Borrador a Tramitando y suma puntos)
            req.sudo().action_confirm()
            return self._ok(data=self._format_request(req), message="Solicitud creada")

        except (ValidationError, UserError) as e:
            return self._err(str(e), 422)
        except Exception as e:
            _logger.error("Error en create_request: %s", str(e), exc_info=True)
            return self._err("Error interno: %s" % str(e), 500)

    @http.route("/api/construction/material_request", type="http", auth="public", methods=["GET"], csrf=False)
    def get_material_requests(self, state=None, page="0", page_size="20", **kw):
        """Lista los pedidos del usuario, del más reciente al más antiguo."""
        uid, err = self._require_uid()
        if err:
            return err
        try:
            orm = request.env
            partner = orm["res.users"].sudo().browse(uid).partner_id
            domain = [("partner_id", "=", partner.id)]
            if state:
                domain.append(("state", "=", state))
            _limit = int(page_size)
            _offset = int(page) * _limit
            reqs = orm["construction.material.request"].sudo().search(
                domain, limit=_limit, offset=_offset, order="id desc"
            )
            total = orm["construction.material.request"].sudo().search_count(domain)
            return self._ok(data=[self._format_request(r) for r in reqs], total=total)
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/material_request/<int:rid>/status", type="http", auth="public", methods=["GET"], csrf=False)
    def request_status(self, rid, **kw):
        """Consulta el estado de un pedido concreto (para seguimiento en tiempo real)."""
        try:
            r = request.env["construction.material.request"].sudo().browse(rid)
            if not r.exists():
                return self._err("No encontrado", 404)
            return self._ok(data=self._format_request(r))
        except Exception as e:
            return self._err(str(e), 500)

    # =====================================================================
    # CATÁLOGO DE PRODUCTOS
    # =====================================================================

    # Prefijos de código para identificar productos de construcción
    _CONSTRUCTION_CODES = (
        'CEM-', 'ARE-', 'LAD-', 'VAR-', 'AZU-', 'MOR-', 'LEC-',
        'YES-', 'PIN-', 'IMP-', 'SIL-', 'CAB-', 'TUC-', 'TUP-',
    )

    # Palabras clave para detectar categorías de construcción
    _CATEG_KW = [
        'construcci', 'materiales', 'herramienta', 'obra', 'reforma',
        'ferralla', 'cemento', 'fontaner', 'electricidad', 'aislamiento',
    ]

    def _get_construction_categ_ids(self, orm):
        """Busca qué categorías de producto son de construcción
        comparando sus nombres con palabras clave."""
        try:
            import unicodedata
            result = []
            for c in orm["product.category"].sudo().search([]):
                cname = (c.complete_name or c.name or '').lower()
                cname_norm = ''.join(
                    ch for ch in unicodedata.normalize('NFD', cname)
                    if unicodedata.category(ch) != 'Mn'
                )
                if any(kw in cname_norm for kw in self._CATEG_KW):
                    result.append(c.id)
            return result
        except Exception:
            return []

    @http.route("/api/construction/products", type="http", auth="public", methods=["GET"], csrf=False)
    def get_products(self, search="", page="0", page_size="20", category_id=None, **kw):
        """Lista los productos del catálogo de construcción con búsqueda y paginación.
        Asigna una categoría visual (Cemento, Hierro, Pintura...) a cada producto."""
        try:
            import unicodedata
            orm = request.env
            _limit = int(page_size)
            _offset = int(page) * _limit

            domain = [("sale_ok", "=", True), ("active", "=", True)]
            if search:
                domain.append(("name", "ilike", search))
            if category_id:
                domain.append(("categ_id", "=", int(category_id)))
            else:
                categ_ids = self._get_construction_categ_ids(orm)
                if categ_ids:
                    domain.append(("categ_id", "in", categ_ids))

            prods = orm["product.product"].sudo().search(domain, limit=_limit, offset=_offset)
            total = orm["product.product"].sudo().search_count(domain)

            # Mapa de palabras clave → categoría visual para el portal
            VISUAL_CATEGORIES = [
                ('cemento', 'Cemento'), ('arena', 'Cemento'), ('mortero', 'Cemento'),
                ('yeso', 'Cemento'), ('ladrillo', 'Cemento'), ('hormigon', 'Cemento'),
                ('varilla', 'Hierro'), ('ferralla', 'Hierro'), ('acero', 'Hierro'),
                ('madera', 'Madera'), ('tablero', 'Madera'),
                ('azulejo', 'Madera'), ('ceramica', 'Madera'), ('porcelan', 'Madera'),
                ('pintura', 'Pintura'), ('imprimacion', 'Pintura'), ('barniz', 'Pintura'),
                ('tubo pvc', 'Fontanería'), ('grifo', 'Fontanería'), ('silicona', 'Fontanería'),
                ('cable', 'Electricidad'), ('lszh', 'Electricidad'),
                ('taladro', 'Herramientas'), ('herramienta', 'Herramientas'),
                ('aislamiento', 'Aislamiento'), ('lana', 'Aislamiento'),
            ]

            data = []
            for p in prods:
                # Quitar tildes para comparar mejor
                pname_norm = ''.join(
                    ch for ch in unicodedata.normalize('NFD', p.name.lower())
                    if unicodedata.category(ch) != 'Mn'
                )
                # Asignar categoría visual según el nombre del producto
                cat_portal = p.categ_id.name if p.categ_id else ""
                for keyword, label in VISUAL_CATEGORIES:
                    if keyword in pname_norm:
                        cat_portal = label
                        break

                # Stock del marketplace (campo propio, independiente del stock nativo)
                qty_mp = getattr(p.product_tmpl_id, 'qty_marketplace', None)
                stock = float(qty_mp) if qty_mp and qty_mp > 0 else float(p.qty_available or 0)

                data.append({
                    "id": p.id,
                    "name": p.name,
                    "price": p.lst_price,
                    "uom": p.uom_id.name,
                    "categ_name": cat_portal,
                    "category": {"name": cat_portal},
                    "description": p.description_sale or "",
                    "default_code": p.default_code or "",
                    "stock_qty": stock,
                    "image": None,
                })
            return self._ok(data=data, total=total)
        except Exception as e:
            _logger.error("Error en get_products: %s", str(e), exc_info=True)
            return self._err(str(e), 500)

    @http.route("/api/construction/products/<int:product_id>", type="http", auth="public", methods=["GET"], csrf=False)
    def get_product_detail(self, product_id, **kw):
        """Devuelve los datos de un producto concreto."""
        try:
            prod = request.env["product.product"].sudo().browse(product_id)
            if not prod.exists() or not prod.active:
                return self._err("Producto no encontrado", 404)
            return self._ok(data={
                "id": prod.id, "name": prod.name,
                "price": prod.lst_price,
                "uom": prod.uom_id.name,
                "description": prod.description_sale or "",
                "default_code": prod.default_code or "",
                "stock_qty": prod.qty_available,
                "category": prod.categ_id.name if prod.categ_id else "",
            })
        except Exception as e:
            return self._err(str(e), 500)

    # =====================================================================
    # CALCULADORA DE MATERIALES
    # Dada una superficie en m² y un tipo de obra, calcula qué materiales
    # se necesitan y en qué cantidad usando recetas predefinidas.
    # =====================================================================

    # Recetas: cada tipo de obra tiene una lista de materiales con su
    # ratio por m² (ej: 1.1 m² de azulejo por cada m² de baño)
    _CALC_RECIPES = {
        'banyo': [
            {'ref': 'AZU-001', 'fallback': 'Azulejo porcelánico',  'ratio': 1.1,  'unit': 'm²'},
            {'ref': 'MOR-001', 'fallback': 'Mortero Cola C2',      'ratio': 5.0,  'unit': 'kg'},
            {'ref': 'LEC-001', 'fallback': 'Lechada de juntas',    'ratio': 0.7,  'unit': 'kg'},
            {'ref': 'SIL-001', 'fallback': 'Silicona sanitaria',   'ratio': 0.15, 'unit': 'ud'},
            {'ref': 'IMP-001', 'fallback': 'Imprimación techo',    'ratio': 0.12, 'unit': 'L'},
            {'ref': 'PIN-001', 'fallback': 'Pintura antihumedad',  'ratio': 0.3,  'unit': 'L'},
        ],
        'solera': [
            {'ref': 'CEM-001', 'fallback': 'Cemento Portland',     'ratio': 0.8,  'unit': 'sacos'},
            {'ref': 'ARE-001', 'fallback': 'Arena de río 0-4 mm',  'ratio': 0.06, 'unit': 'm³'},
            {'ref': 'GRA-001', 'fallback': 'Grava 6-12 mm',        'ratio': 0.08, 'unit': 'm³'},
            {'ref': 'MAL-001', 'fallback': 'Mallazo electrosoldado','ratio': 1.05, 'unit': 'm²'},
        ],
        'tabique_ladrillo': [
            {'ref': 'LAD-001', 'fallback': 'Ladrillo perforado',   'ratio': 55.0, 'unit': 'ud'},
            {'ref': 'CEM-001', 'fallback': 'Mortero de agarre',    'ratio': 1.0,  'unit': 'sacos'},
            {'ref': 'YES-001', 'fallback': 'Yeso proyectado',      'ratio': 10.0, 'unit': 'kg'},
            {'ref': 'IMP-001', 'fallback': 'Imprimación selladora','ratio': 0.15, 'unit': 'L'},
            {'ref': 'PIN-001', 'fallback': 'Pintura plástica',     'ratio': 0.4,  'unit': 'L'},
        ],
        'pintura': [
            {'ref': 'IMP-001', 'fallback': 'Imprimación',          'ratio': 0.15, 'unit': 'L'},
            {'ref': 'PIN-001', 'fallback': 'Pintura',              'ratio': 0.4,  'unit': 'L'},
        ],
        'cocina': [
            {'ref': 'AZU-001', 'fallback': 'Azulejo suelo/pared',  'ratio': 1.1,  'unit': 'm²'},
            {'ref': 'MOR-001', 'fallback': 'Mortero Cola C2',      'ratio': 5.0,  'unit': 'kg'},
            {'ref': 'LEC-001', 'fallback': 'Lechada de juntas',    'ratio': 0.5,  'unit': 'kg'},
            {'ref': 'SIL-001', 'fallback': 'Silicona sanitaria',   'ratio': 0.3,  'unit': 'ud'},
            {'ref': 'PIN-001', 'fallback': 'Pintura paredes',      'ratio': 0.54, 'unit': 'L'},
            {'ref': 'IMP-001', 'fallback': 'Imprimación',          'ratio': 0.18, 'unit': 'L'},
        ],
        'reforma_integral': [
            # Albañilería
            {'ref': 'CEM-001', 'fallback': 'Cemento Portland',     'ratio': 0.5,  'unit': 'sacos'},
            {'ref': 'ARE-001', 'fallback': 'Arena de río',          'ratio': 0.02, 'unit': 'm³'},
            {'ref': 'LAD-001', 'fallback': 'Ladrillo perforado',   'ratio': 12.0, 'unit': 'ud'},
            # Paredes
            {'ref': 'YES-001', 'fallback': 'Yeso proyectado',      'ratio': 2.5,  'unit': 'kg'},
            # Zonas húmedas (45% de la superficie)
            {'ref': 'AZU-001', 'fallback': 'Azulejo porcelánico',  'ratio': 0.5,  'unit': 'm²'},
            {'ref': 'MOR-001', 'fallback': 'Mortero Cola C2',      'ratio': 2.25, 'unit': 'kg'},
            {'ref': 'LEC-001', 'fallback': 'Lechada de juntas',    'ratio': 0.32, 'unit': 'kg'},
            {'ref': 'SIL-001', 'fallback': 'Silicona sanitaria',   'ratio': 0.05, 'unit': 'ud'},
            # Pintura
            {'ref': 'IMP-001', 'fallback': 'Imprimación selladora','ratio': 0.27, 'unit': 'L'},
            {'ref': 'PIN-001', 'fallback': 'Pintura plástica',     'ratio': 0.72, 'unit': 'L'},
            # Electricidad
            {'ref': 'CAB-001', 'fallback': 'Cable LSZH 2,5 mm²',  'ratio': 3.0,  'unit': 'm'},
            {'ref': 'TUC-001', 'fallback': 'Tubo corrugado Ø20',   'ratio': 1.5,  'unit': 'm'},
            {'ref': 'CUA-001', 'fallback': 'Cuadro eléctrico',     'ratio': 0.01, 'unit': 'ud'},
            # Fontanería
            {'ref': 'TUP-001', 'fallback': 'Tubo PVC evacuación',  'ratio': 0.4,  'unit': 'm'},
            {'ref': 'GRI-001', 'fallback': 'Grifo monomando',      'ratio': 0.02, 'unit': 'ud'},
            # Aislamiento
            {'ref': 'LAN-001', 'fallback': 'Lana de roca',         'ratio': 0.3,  'unit': 'm²'},
        ],
    }

    @http.route("/api/construction/catalog/calculator", type="http", auth="public", methods=["GET"], csrf=False)
    def catalog_calculator(self, type="banyo", m2="10", **kw):
        """Calculadora de materiales.
        Dado un tipo de obra y los m², devuelve qué materiales se necesitan
        con cantidades y precios reales del catálogo."""
        try:
            m2_val = float(m2)
            if m2_val <= 0:
                return self._err("m2 debe ser mayor que 0", 400)

            recipe = self._CALC_RECIPES.get(type, self._CALC_RECIPES['banyo'])
            env = request.env

            # Cargar todos los productos necesarios en una sola consulta
            refs = [item['ref'] for item in recipe]
            products_by_ref = {}
            for prod in env['product.product'].sudo().search([('default_code', 'in', refs)]):
                products_by_ref[prod.default_code] = prod

            # Calcular cantidad de cada material según la receta
            materials = []
            for item in recipe:
                qty = round(m2_val * item['ratio'], 2)
                prod = products_by_ref.get(item['ref'])
                materials.append({
                    'material': prod.name if prod else item['fallback'],
                    'unit': prod.uom_id.name if prod else item['unit'],
                    'quantity': qty,
                    'product_id': prod.id if prod else None,
                    'price_unit': prod.lst_price if prod else 0.0,
                })
            return self._ok(data={'obra_type': type, 'm2': m2_val, 'materials': materials})
        except Exception as e:
            return self._err(str(e), 500)

    # =====================================================================
    # TRANSPORTE — Cálculo del coste de envío a obra
    # =====================================================================

    @http.route("/api/construction/calculate_transport", type="http", auth="public", methods=["POST"], csrf=False)
    def calculate_transport(self, **kw):
        """Calcula el coste de transporte usando coordenadas GPS.
        Usa la fórmula de Haversine para calcular la distancia en km
        desde el almacén (Madrid) hasta la obra del cliente."""
        try:
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            delivery_lat = float(p.get("delivery_lat", 0))
            delivery_lon = float(p.get("delivery_lon", 0))
            weight_kg = float(p.get("weight_kg", 0))
            is_urgent = bool(p.get("is_urgent", False))

            if not delivery_lat or not delivery_lon:
                return self._err("Coordenadas GPS obligatorias", 400)

            calculator = request.env["construction.transport.calculator"].sudo().get_default_calculator()
            result = calculator.calculate_transport_cost(
                delivery_lat=delivery_lat,
                delivery_lon=delivery_lon,
                total_weight_kg=weight_kg,
                is_urgent=is_urgent,
            )
            return self._ok(data={
                "distance_km": result["distance_km"],
                "transport_cost": result["total"],
                "breakdown": {
                    "base_cost": result["base_cost"],
                    "weight_surcharge": result["weight_surcharge"],
                    "urgent_surcharge": result["urgent_surcharge"],
                    "total": result["total"],
                },
            })
        except Exception as e:
            return self._err(str(e), 500)

    # =====================================================================
    # FIDELIZACIÓN — Consultar puntos y nivel del cliente
    # =====================================================================

    @http.route("/api/construction/loyalty/status", type="http", auth="public", methods=["GET"], csrf=False)
    def loyalty_status(self, **kw):
        """Devuelve los puntos del usuario, su nivel actual (Bronce/Plata/Oro/Platino),
        el % de descuento que le corresponde y cuántos puntos le faltan para subir."""
        uid, err = self._require_uid()
        if err:
            return err
        try:
            partner = request.env["res.users"].sudo().browse(uid).partner_id
            next_info = partner._get_points_to_next_level()
            return self._ok(data={
                "points_balance": partner.points_balance,
                "loyalty_level": partner.loyalty_level,
                "loyalty_discount": partner.loyalty_discount,
                "points_to_next_level": next_info["points_needed"],
                "next_level_name": next_info["next_level_name"],
            })
        except Exception as e:
            return self._err(str(e), 500)

    # =====================================================================
    # CHATBOT — Asistente de materiales con IA
    # =====================================================================

    @http.route("/api/construction/chatbot", type="http", auth="public", methods=["POST"], csrf=False)
    def chatbot(self, **kw):
        """Recibe un mensaje del usuario y responde con recomendaciones
        de materiales. Usa reglas + Groq API (IA) si está configurada."""
        try:
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            message = (p.get("message") or "").strip()
            if not message:
                return self._err("Mensaje vacío", 400)
            result = request.env["construction.chatbot"].process_message(message)
            return self._ok(data=result)
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/chatbot/message", type="json", auth="public", methods=["POST"], csrf=False)
    def chatbot_json(self, message="", **kw):
        """Endpoint JSON-RPC para el chatbot del portal web."""
        if not message:
            return {"response": "Por favor, escribe tu consulta sobre materiales."}
        try:
            result = request.env["construction.chatbot"].process_message(message)
            text = result.get("mensaje_llm") or result.get("mensaje", "")
            materiales = result.get("materiales", [])
            if materiales:
                text += "\n\nMateriales recomendados:\n"
                for m in materiales[:8]:
                    text += "• %s: %s\n" % (m["nombre"], m["cantidad"])
            return {"response": text}
        except Exception as e:
            _logger.warning("Error chatbot: %s", e)
            return {"response": "Lo siento, ha ocurrido un error. Inténtalo de nuevo."}

    # =====================================================================
    # PORTAL WEB — Sirve la página HTML del marketplace
    # =====================================================================

    @http.route('/construccion/portal', type='http', auth='public', methods=['GET'], csrf=False)
    def portal_web(self, **kw):
        """Sirve el archivo portal.html directamente desde Odoo.
        Se desactiva la caché para que siempre cargue la versión más reciente."""
        import os
        portal_path = os.path.join(
            os.path.dirname(os.path.dirname(__file__)),
            'static', 'portal', 'portal.html',
        )
        with open(portal_path, 'r', encoding='utf-8') as f:
            content = f.read()
        return request.make_response(content, headers=[
            ('Content-Type', 'text/html; charset=utf-8'),
            ('Cache-Control', 'no-cache, no-store, must-revalidate'),
            ('Pragma', 'no-cache'),
            ('Expires', '0'),
        ])
