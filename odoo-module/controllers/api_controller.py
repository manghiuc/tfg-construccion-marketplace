# -*- coding: utf-8 -*-
import json, logging
from odoo import http
from odoo.http import request
from odoo.exceptions import AccessDenied, ValidationError, UserError
_logger = logging.getLogger(__name__)

class ConstructionAPI(http.Controller):
    def _ok(self, data=None, **kw):
        p = dict(success=True, data=data); p.update(kw)
        return request.make_json_response(p, status=200)
    def _err(self, msg, code=400):
        return request.make_json_response(dict(success=False, error=dict(code=code, message=msg)), status=code)

    def _require_uid(self):
        """
        Devuelve (uid, None) si hay sesión válida, o (None, response_401) si no.
        Siempre devuelve JSON, nunca HTML.

        Estrategia de autenticación (en orden):
          1. Cookie session_id  → la carga Odoo automáticamente en request.session
          2. Header X-Openerp-Session-Id → la leemos manualmente del session store
        """
        # 1. Cookie estándar (funciona tras login o si NetworkModule envía Cookie header)
        uid = request.session.uid
        if uid:
            return uid, None

        # 2. Header X-Openerp-Session-Id (app Android sin cookie)
        session_id = (
            request.httprequest.headers.get("X-Openerp-Session-Id") or
            request.httprequest.headers.get("X-Openerp-Session-id") or ""
        ).strip()
        if session_id:
            try:
                from odoo.http import root as http_root
                stored = http_root.session_store.get(session_id)
                if stored and stored.uid:
                    return stored.uid, None
            except Exception as e:
                _logger.warning("_require_uid: fallo al cargar sesión '%s': %s", session_id, e)

        return None, self._err("No autenticado. Por favor inicia sesión.", 401)

    def _get_partner_type(self, partner):
        if partner.is_company:
            return "empresa"
        return "particular"

    def _format_user(self, user, partner=None):
        if partner is None:
            partner = user.partner_id
        return dict(
            id=user.id,
            name=user.name,
            login=user.login,
            session_id=request.session.sid or "",
            partner_type=self._get_partner_type(partner),
            points_balance=int(getattr(partner, 'points_balance', 0) or 0),
            loyalty_level=getattr(partner, 'loyalty_level', 'bronce') or 'bronce',
            phone=partner.phone or None,
            email=partner.email or None,
            partner_id=partner.id,
        )

    _STATE_LABELS = {
        'draft':          'Borrador',
        'confirmed':      'Tramitando',
        'en_preparacion': 'En Preparación',
        'en_reparto':     'En Reparto',
        'in_progress':    'En Camino',
        'delivered':      'Entregado',
        'cancelled':      'Cancelado',
    }

    def _format_request(self, r):
        delivery_address = getattr(r, 'delivery_address', '') or ''
        if not delivery_address:
            notes = r.notes or ''
            if '📍 Dirección de entrega:' in notes:
                delivery_address = notes.split('\n')[0].replace('📍 Dirección de entrega:', '').strip()
        return {
            "id": r.id, "name": r.name, "state": r.state,
            "state_label": self._STATE_LABELS.get(r.state, r.state),
            "obra_id": r.obra_id.id if r.obra_id else 0,
            "obra_name": r.obra_id.name if r.obra_id else "",
            "total_amount": float(getattr(r, 'total_amount', 0) or 0),
            "transport_cost": float(getattr(r, 'transport_cost', 0) or 0),
            "total_with_transport": float(getattr(r, 'total_with_transport', 0) or getattr(r, 'total_amount', 0) or 0),
            "is_urgent": bool(r.is_urgent),
            "notes": r.notes or "",
            "delivery_address": delivery_address,
            "create_date": str(r.date_request)[:19] if getattr(r, 'date_request', None) else str(r.create_date)[:19] if r.create_date else None,
            "lines": [{"id": l.id, "product_id": l.product_id.id,
                        "product_name": l.product_id.name,
                        "qty": float(l.product_qty),
                        "uom": l.product_id.uom_id.name if l.product_id.uom_id else "",
                        "price_unit": float(l.price_unit),
                        "subtotal": float(getattr(l, 'subtotal', l.product_qty * l.price_unit)),
                        "image_url": "/web/image/product.product/%d/image_128" % l.product_id.id}
                       for l in r.line_ids],
        }

    @http.route("/api/construction/auth/login", type="http", auth="public", methods=["POST"], csrf=False)
    def auth_login(self, **kw):
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
            u = request.env["res.users"].sudo().browse(uid)
            return self._ok(data=dict(user=self._format_user(u)))
        except AccessDenied:
            return self._err("Acceso denegado", 401)
        except Exception as e:
            _logger.error("Error en login: %s", str(e))
            return self._err(str(e), 500)

    @http.route("/api/construction/obras", type="http", auth="none", methods=["GET"], csrf=False)
    def get_obras(self, state=None, limit=None, offset="0", page="0", page_size="20", **kw):
        uid, err = self._require_uid()
        if err: return err
        try:
            orm = request.env
            _limit = int(limit) if limit else int(page_size)
            _offset = int(offset) if int(offset) > 0 else int(page) * _limit
            partner = orm["res.users"].sudo().browse(uid).partner_id
            d = [("partner_id", "=", partner.id)]
            if state:
                d.append(("state", "=", state))
            items = orm["construction.obra"].sudo().search(d, limit=_limit, offset=_offset)
            data = [{"id": o.id, "name": o.name, "code": o.code, "state": o.state,
                     "partner_name": o.partner_id.name if o.partner_id else "",
                     "partner_id": o.partner_id.id if o.partner_id else 0,
                     "address": getattr(o, 'address', "") or "",
                     "description": getattr(o, 'description', "") or "",
                     "material_request_count": o.material_request_count} for o in items]
            return self._ok(data=data, total=orm["construction.obra"].sudo().search_count(d))
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/obras", type="http", auth="none", methods=["POST"], csrf=False)
    def create_obra(self, **kw):
        uid, err = self._require_uid()
        if err: return err
        try:
            orm = request.env
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            name = (p.get("name") or "").strip()
            if not name:
                return self._err("El nombre de la obra es obligatorio", 400)
            address = (p.get("address") or "").strip()
            partner = orm["res.users"].sudo().browse(uid).partner_id
            code = orm["ir.sequence"].sudo().next_by_code("construction.obra") or ("OBR-%d" % uid)
            vals = {
                "name": name,
                "code": code,
                "partner_id": partner.id,
                "state": "active",
            }
            if address:
                vals["address"] = address
            obra = orm["construction.obra"].sudo().create(vals)
            return self._ok(data={
                "id": obra.id,
                "name": obra.name,
                "code": obra.code,
                "state": obra.state,
                "address": getattr(obra, 'address', "") or "",
                "partner_name": obra.partner_id.name if obra.partner_id else "",
                "partner_id": obra.partner_id.id if obra.partner_id else 0,
                "material_request_count": 0,
                "description": "",
            })
        except (ValidationError, UserError) as e:
            return self._err(str(e), 422)
        except Exception as e:
            _logger.error("Error en create_obra: %s", str(e), exc_info=True)
            return self._err(str(e), 500)

    @http.route("/api/construction/material_request", type="http", auth="none", methods=["POST"], csrf=False)
    def create_request(self, **kw):
        uid, err = self._require_uid()
        if err: return err
        try:
            orm = request.env
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            # obra_id es opcional — si viene vacío/false se crea sin obra
            obra_id_raw = p.get("obra_id")
            obra_id = int(obra_id_raw) if obra_id_raw else False
            partner_id = False
            if obra_id:
                obra = orm["construction.obra"].sudo().browse(obra_id)
                if not obra.exists():
                    return self._err("Obra no encontrada", 404)
                partner_id = obra.partner_id.id or False
            else:
                partner_id = orm["res.users"].sudo().browse(uid).partner_id.id
            raw_lines = p.get("lines", [])
            if not raw_lines:
                return self._err("El pedido no tiene productos", 400)
            lines = []
            for l in raw_lines:
                pid = int(l["product_id"])
                prod = orm["product.product"].sudo().browse(pid)
                if not prod.exists():
                    return self._err("Producto con ID %d no encontrado. Por favor recarga la página e inicia sesión para ver productos reales." % pid, 400)
                lines.append((0, 0, {
                    "product_id": pid,
                    "product_qty": float(l.get("qty") or l.get("product_qty") or 1),
                    "price_unit": float(l.get("price_unit", prod.lst_price or 0)),
                    "notes": l.get("notes", ""),
                }))
            # Extraer dirección de entrega de las notas si se envía por separado
            raw_notes = p.get("notes", "") or ""
            delivery_address = p.get("delivery_address", "")
            if not delivery_address and "📍 Dirección de entrega:" in raw_notes:
                delivery_address = raw_notes.split("\n")[0].replace("📍 Dirección de entrega:", "").strip()
            is_urgent = bool(p.get("is_urgent", False))
            vals = {
                "user_id": uid,
                "partner_id": partner_id,
                "notes": raw_notes,
                "delivery_address": delivery_address,
                "is_urgent": is_urgent,
                "line_ids": lines,
            }
            if obra_id:
                vals["obra_id"] = obra_id
            req = orm["construction.material.request"].sudo().create(vals)
            # Confirmar automáticamente el pedido del portal
            req.sudo().action_confirm()
            return self._ok(data=self._format_request(req), message="Solicitud creada")
        except (ValidationError, UserError) as e:
            return self._err(str(e), 422)
        except Exception as e:
            _logger.error("Error en create_request: %s", str(e), exc_info=True)
            return self._err(str(e), 500)

    @http.route("/api/construction/material_request", type="http", auth="none", methods=["GET"], csrf=False)
    def get_material_requests(self, state=None, page="0", page_size="20", **kw):
        uid, err = self._require_uid()
        if err: return err
        try:
            orm = request.env
            _limit = int(page_size)
            _offset = int(page) * _limit
            partner = orm["res.users"].sudo().browse(uid).partner_id
            domain = [("partner_id", "=", partner.id)]
            if state:
                domain.append(("state", "=", state))
            reqs = orm["construction.material.request"].sudo().search(domain, limit=_limit, offset=_offset, order="id desc")
            total = orm["construction.material.request"].sudo().search_count(domain)
            return self._ok(data=[self._format_request(r) for r in reqs], total=total)
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/material_request/<int:rid>/status", type="http", auth="none", methods=["GET"], csrf=False)
    def request_status(self, rid, **kw):
        try:
            orm = request.env
            r = orm["construction.material.request"].sudo().browse(rid)
            if not r.exists():
                return self._err("No encontrado", 404)
            return self._ok(data=self._format_request(r))
        except Exception as e:
            return self._err(str(e), 500)

    # Códigos internos de productos de construcción (prefijos)
    _CONSTRUCTION_CODES = ('CEM-', 'ARE-', 'LAD-', 'VAR-', 'AZU-', 'MOR-', 'LEC-',
                            'YES-', 'PIN-', 'IMP-', 'SIL-', 'CAB-', 'TUC-', 'TUP-')
    # Palabras clave para detectar categorías de construcción por nombre
    _CATEG_KW = ['construcci', 'materiales', 'herramienta', 'obra', 'reforma',
                 'ferralla', 'cemento', 'fontaner', 'electricidad', 'aislamiento']

    def _get_construction_categ_ids(self, orm):
        """Devuelve IDs de product.category relacionadas con construcción."""
        try:
            all_categs = orm["product.category"].sudo().search([])
            result = []
            for c in all_categs:
                # Comparar contra nombre completo en minúsculas (sin tildes)
                cname = (c.complete_name or c.name or '').lower()
                import unicodedata
                cname_norm = ''.join(
                    ch for ch in unicodedata.normalize('NFD', cname)
                    if unicodedata.category(ch) != 'Mn'
                )
                if any(kw in cname_norm for kw in self._CATEG_KW):
                    result.append(c.id)
            return result
        except Exception as e:
            _logger.warning("Error buscando categorías de construcción: %s", e)
            return []

    @http.route("/api/construction/products", type="http", auth="none", methods=["GET"], csrf=False)
    def get_products(self, search="", limit=None, page="0", page_size="20", category_id=None, **kw):
        try:
            orm = request.env
            _limit = int(limit) if limit else int(page_size)
            _offset = int(page) * _limit
            d = [("sale_ok", "=", True), ("active", "=", True)]
            if search:
                d.append(("name", "ilike", search))
            if category_id:
                d.append(("categ_id", "=", int(category_id)))
            else:
                # 1. Intentar filtrar por categorías de construcción
                categ_ids = self._get_construction_categ_ids(orm)
                if categ_ids:
                    d.append(("categ_id", "in", categ_ids))
                else:
                    # 2. Fallback: filtrar por default_code que empiece con prefijos de construcción
                    _logger.warning("No se encontraron categorías de construcción, usando filtro por código")
                    code_domain = ['|'] * (len(self._CONSTRUCTION_CODES) - 1)
                    for code in self._CONSTRUCTION_CODES:
                        code_domain.append(('default_code', 'like', code + '%'))
                    d.extend(code_domain)

            prods = orm["product.product"].sudo().search(d, limit=_limit, offset=_offset)
            total = orm["product.product"].sudo().search_count(d)

            _logger.info("API products: %d encontrados con categ_ids=%s", len(prods), categ_ids if not category_id else [category_id])

            data = []
            for p in prods:
                pname_lower = p.name.lower()
                import unicodedata
                pname_norm = ''.join(
                    ch for ch in unicodedata.normalize('NFD', pname_lower)
                    if unicodedata.category(ch) != 'Mn'
                )
                # Asignar categoría visual para que el portal muestre la imagen correcta
                cat_portal = p.categ_id.name if p.categ_id else ""
                for kw, label in [
                    ('cemento', 'Cemento'), ('arena', 'Cemento'), ('mortero', 'Cemento'),
                    ('yeso', 'Cemento'), ('ladrillo', 'Cemento'), ('hormigon', 'Cemento'),
                    ('varilla', 'Hierro'), ('ferralla', 'Hierro'), ('acero', 'Hierro'), ('corrugad', 'Hierro'),
                    ('madera', 'Madera'), ('tablero', 'Madera'), ('tarima', 'Madera'),
                    ('azulejo', 'Madera'), ('ceramica', 'Madera'), ('porcelan', 'Madera'), ('lechada', 'Madera'),
                    ('pintura', 'Pintura'), ('imprimacion', 'Pintura'), ('barniz', 'Pintura'), ('selladora', 'Pintura'),
                    ('tubo pvc', 'Fontanería'), ('evacuacion', 'Fontanería'), ('grifo', 'Fontanería'),
                    ('silicona', 'Fontanería'),
                    ('cable', 'Electricidad'), ('lszh', 'Electricidad'), ('corrugado', 'Electricidad'),
                    ('taladro', 'Herramientas'), ('herramienta', 'Herramientas'), ('sierra', 'Herramientas'),
                    ('aislamiento', 'Aislamiento'), ('lana', 'Aislamiento'),
                ]:
                    if kw in pname_norm:
                        cat_portal = label
                        break
                # Stock: qty_marketplace si está definido y > 0, si no qty_available real
                qty_mp = getattr(p.product_tmpl_id, 'qty_marketplace', None)
                if qty_mp is not None and qty_mp > 0:
                    stock = float(qty_mp)
                else:
                    qty_avail = p.qty_available
                    stock = 0.0 if (qty_avail is None or qty_avail is False) else float(qty_avail)
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
                    "image": None,  # El portal elige imagen por keyword (fotos reales locales)
                })
            return self._ok(data=data, total=total)
        except Exception as e:
            _logger.error("Error en get_products: %s", str(e), exc_info=True)
            return self._err(str(e), 500)

    # ── Calculadora de materiales ────────────────────────────────────────────────
    # Recetas por tipo de obra (ratio por m²) enlazadas con productos reales del catálogo
    _CALC_RECIPES = {
        'banyo': [
            {'ref': 'MOR-001', 'fallback': 'Mortero Cola', 'ratio': 4.0,  'unit': 'kg'},
            {'ref': 'LEC-001', 'fallback': 'Lechada',      'ratio': 0.7,  'unit': 'kg'},
            {'ref': 'SIL-001', 'fallback': 'Silicona',     'ratio': 0.1,  'unit': 'ud'},
        ],
        'solera': [
            {'ref': 'CEM-001', 'fallback': 'Cemento',      'ratio': 7.0,  'unit': 'kg'},
            {'ref': 'VAR-012', 'fallback': 'Varilla',      'ratio': 0.04, 'unit': 'ud'},
        ],
        'tabique_ladrillo': [
            {'ref': 'LAD-001', 'fallback': 'Ladrillo',     'ratio': 55.0, 'unit': 'ud'},
            {'ref': 'MOR-001', 'fallback': 'Mortero',      'ratio': 3.0,  'unit': 'kg'},
            {'ref': 'YES-001', 'fallback': 'Yeso',         'ratio': 2.0,  'unit': 'kg'},
        ],
        'pintura': [
            {'ref': 'IMP-001', 'fallback': 'Imprimación',  'ratio': 0.15, 'unit': 'L'},
            {'ref': 'PIN-001', 'fallback': 'Pintura',      'ratio': 0.4,  'unit': 'L'},
        ],
    }

    @http.route("/api/construction/catalog/calculator", type="http", auth="none", methods=["GET"], csrf=False)
    def catalog_calculator(self, type="banyo", m2="10", **kw):
        """Calcula los materiales necesarios para una obra y los vincula con productos del catálogo."""
        try:
            m2_val = float(m2)
            if m2_val <= 0:
                return self._err("m2 debe ser mayor que 0", 400)

            recipe = self._CALC_RECIPES.get(type, self._CALC_RECIPES['banyo'])
            env = request.env
            materials = []
            refs = [item['ref'] for item in recipe]

            # Cargar todos los productos necesarios en una sola consulta
            products_by_ref = {}
            prods = env['product.product'].sudo().search([('default_code', 'in', refs)])
            for prod in prods:
                products_by_ref[prod.default_code] = prod

            for item in recipe:
                qty = round(m2_val * item['ratio'], 2)
                prod = products_by_ref.get(item['ref'])
                materials.append({
                    'material':      prod.name if prod else item['fallback'],
                    'unit':          prod.uom_id.name if prod else item['unit'],
                    'quantity':      qty,
                    'product_id':    prod.id if prod else None,
                    'product_name':  prod.name if prod else None,
                    'price_unit':    prod.lst_price if prod else 0.0,
                    'product_uom':   prod.uom_id.name if prod else item['unit'],
                })

            return self._ok(data={
                'obra_type':  type,
                'm2':         m2_val,
                'materials':  materials,
            })
        except Exception as e:
            _logger.error("Error en catalog_calculator: %s", str(e), exc_info=True)
            return self._err(str(e), 500)

    @http.route("/api/construction/recommendations", type="http", auth="none", methods=["GET"], csrf=False)
    def recommendations(self, obra_id=None, obra_type="reforma", category=None, limit="10", **kw):
        try:
            orm = request.env
            recs = orm["construction.recommendation.engine"].get_recommendations(
                obra_id=int(obra_id) if obra_id else None, tipo_obra=obra_type,
                categoria=category, limit=int(limit))
            return self._ok(data=recs, obra_type=obra_type)
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/chatbot", type="json", auth="none", methods=["POST"], csrf=False)
    def chatbot(self):
        try:
            p = request.get_json_data() or {}
            msg = p.get("message", "").strip()
            if not msg:
                return dict(success=False, error=dict(code=400, message="message obligatorio"))
            response = request.env["construction.chatbot"].process_message(msg)
            return dict(success=True, data=response)
        except Exception as e:
            return dict(success=False, error=dict(code=500, message=str(e)))

    # =========================================================================
    # Transporte
    # =========================================================================

    @http.route("/api/construction/calculate_transport", type="http", auth="none", methods=["POST"], csrf=False)
    def calculate_transport(self, **kw):
        """
        Calcula el coste de transporte para unas coordenadas y peso dados.

        Body JSON esperado:
          {
            "delivery_lat":  <float>,
            "delivery_lon":  <float>,
            "weight_kg":     <float>,
            "is_urgent":     <bool, opcional>
          }

        Respuesta:
          {
            "success": true,
            "data": {
              "distance_km":     <float>,
              "transport_cost":  <float>,
              "breakdown": {
                "base_cost":         <float>,
                "weight_surcharge":  <float>,
                "urgent_surcharge":  <float>,
                "total":             <float>
              }
            }
          }
        """
        try:
            orm = request.env
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            delivery_lat = float(p.get("delivery_lat", 0))
            delivery_lon = float(p.get("delivery_lon", 0))
            weight_kg = float(p.get("weight_kg", 0))
            is_urgent = bool(p.get("is_urgent", False))

            if not delivery_lat or not delivery_lon:
                return self._err("delivery_lat y delivery_lon son obligatorios", 400)

            calculator = orm["construction.transport.calculator"].sudo().get_default_calculator()
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
        except (ValueError, KeyError) as e:
            return self._err("Parámetros inválidos: %s" % str(e), 400)
        except Exception as e:
            return self._err(str(e), 500)

    # =========================================================================
    # Fidelización
    # =========================================================================

    @http.route("/api/construction/loyalty/status", type="http", auth="none", methods=["GET"], csrf=False)
    def loyalty_status(self, **kw):
        uid, err = self._require_uid()
        if err: return err
        try:
            orm = request.env
            partner = orm["res.users"].sudo().browse(uid).partner_id
            if not partner.exists():
                return self._err("Partner no encontrado", 404)
            next_level_info = partner._get_points_to_next_level()
            return self._ok(data={
                "points_balance": partner.points_balance,
                "loyalty_level": partner.loyalty_level,
                "points_to_next_level": next_level_info["points_needed"],
                "next_level": next_level_info["next_level"],
                "next_level_name": next_level_info["next_level_name"],
            })
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/loyalty/redeem", type="http", auth="none", methods=["POST"], csrf=False)
    def loyalty_redeem(self, **kw):
        uid, err = self._require_uid()
        if err: return err
        try:
            orm = request.env
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            points = float(p.get("points_to_redeem", 0))
            request_id = int(p["request_id"]) if p.get("request_id") else None

            if points <= 0:
                return self._err("points_to_redeem debe ser mayor que 0", 400)

            partner = orm["res.users"].sudo().browse(uid).partner_id
            partner.redeem_points(points, request_id=request_id)

            return self._ok(data={
                "redeemed_points": points,
                "new_balance": partner.points_balance,
                "loyalty_level": partner.loyalty_level,
            })
        except (ValidationError, UserError) as e:
            return self._err(str(e), 422)
        except Exception as e:
            return self._err(str(e), 500)

    # =========================================================================
    # Detalle de obra y producto
    # =========================================================================

    @http.route("/api/construction/obras/<int:obra_id>", type="http", auth="none", methods=["GET"], csrf=False)
    def get_obra_detail(self, obra_id, **kw):
        try:
            obra = request.env["construction.obra"].sudo().browse(obra_id)
            if not obra.exists():
                return self._err("Obra no encontrada", 404)
            return self._ok(data={
                "id": obra.id,
                "name": obra.name,
                "code": obra.code,
                "state": obra.state,
                "address": obra.address if hasattr(obra, 'address') else "",
                "partner": obra.partner_id.name if obra.partner_id else "",
                "partner_id": obra.partner_id.id if obra.partner_id else False,
                "description": obra.description if hasattr(obra, 'description') else "",
                "start_date": str(obra.date_start) if hasattr(obra, 'date_start') and obra.date_start else None,
                "end_date": str(obra.date_end) if hasattr(obra, 'date_end') and obra.date_end else None,
                "material_request_count": obra.material_request_count,
            })
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/products/<int:product_id>", type="http", auth="none", methods=["GET"], csrf=False)
    def get_product_detail(self, product_id, **kw):
        try:
            prod = request.env["product.product"].sudo().browse(product_id)
            if not prod.exists() or not prod.active:
                return self._err("Producto no encontrado", 404)
            return self._ok(data={
                "id": prod.id,
                "name": prod.name,
                "price": prod.lst_price,
                "uom": prod.uom_id.name,
                "description": prod.description_sale or "",
                "image": "/web/image/product.product/%d/image_128" % prod.id,
                "stock_qty": prod.qty_available,
                "category": prod.categ_id.name if prod.categ_id else "",
                "default_code": prod.default_code or "",
            })
        except Exception as e:
            return self._err(str(e), 500)

    # =========================================================================
    # Registro y cierre de sesión
    # =========================================================================

    @http.route("/api/construction/auth/register", type="http", auth="public", methods=["POST"], csrf=False)
    def auth_register(self, **kw):
        try:
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            name          = (p.get("name") or "").strip()
            email         = (p.get("login") or p.get("email") or "").strip().lower()
            password      = p.get("password") or ""
            phone         = (p.get("phone") or "").strip()
            partner_type  = p.get("partner_type") or p.get("customer_type") or "particular"
            company_name  = (p.get("company_name") or "").strip()
            vat           = (p.get("vat") or "").strip()

            if not name or not email or not password:
                return self._err("name, login y password son obligatorios", 400)
            if len(password) < 6:
                return self._err("La contraseña debe tener al menos 6 caracteres", 400)

            existing = request.env["res.users"].sudo().search([("login", "=", email)], limit=1)
            if existing:
                return request.make_json_response(
                    dict(success=False, error=dict(code=409, message="Ya existe una cuenta con ese correo electrónico")),
                    status=409
                )

            portal_group = request.env.ref("base.group_portal")
            is_company = (partner_type == "empresa")

            new_user = request.env["res.users"].sudo().create({
                "name": name, "login": email, "email": email, "password": password,
                "groups_id": [(6, 0, [portal_group.id])],
            })
            partner = new_user.partner_id
            write_vals = {
                "phone": phone or False,
                "is_company": is_company,
                "customer_rank": 1,
                "comment": "Registrado vía Construction Marketplace. Tipo: %s" % partner_type,
            }
            if company_name and is_company:
                write_vals["name"] = company_name
            if vat:
                write_vals["vat"] = vat
            partner.sudo().write(write_vals)

            # Auto-login (puede fallar para usuarios portal — no es crítico)
            auto_login_ok = False
            try:
                uid = request.session.authenticate(request.db or "construction_marketplace", email, password)
                auto_login_ok = bool(uid)
            except Exception:
                uid = None

            user_data = self._format_user(new_user, partner)
            if not auto_login_ok:
                user_data["session_id"] = ""
            return self._ok(data=dict(user=user_data, auto_login=auto_login_ok,
                                      message="Cuenta creada. Por favor inicia sesión." if not auto_login_ok
                                      else "Bienvenido/a, %s." % new_user.name))

        except Exception as e:
            import traceback
            _logger.error("Error en registro: %s\n%s", str(e), traceback.format_exc())
            return self._err("Error al crear la cuenta: " + str(e), 500)

    @http.route("/api/construction/auth/logout", type="http", auth="public", methods=["POST"], csrf=False)
    def auth_logout(self, **kw):
        try:
            request.session.logout(keep_db=True)
        except Exception:
            pass
        return request.make_json_response({"success": True})

    @http.route("/api/construction/auth/session", type="http", auth="public", methods=["GET"], csrf=False)
    def auth_session(self, **kw):
        uid = request.session.uid
        if not uid:
            return request.make_json_response({"success": False, "error": {"code": 401, "message": "No autenticado"}}, status=401)
        try:
            user = request.env["res.users"].sudo().browse(uid)
            return request.make_json_response({"success": True, "data": {
                "uid": uid,
                "name": user.name,
                "login": user.login,
                "partner_id": user.partner_id.id,
            }})
        except Exception as e:
            return request.make_json_response({"success": False, "error": {"code": 500, "message": str(e)}}, status=500)

    # =========================================================================
    # Listar solicitudes de material del usuario
    # =========================================================================

    @http.route("/api/construction/user_requests", type="http", auth="none", methods=["GET"], csrf=False)
    def list_requests(self, state=None, limit="50", offset="0", **kw):
        uid, err = self._require_uid()
        if err: return err
        try:
            orm = request.env
            partner = orm["res.users"].sudo().browse(uid).partner_id
            domain = [("partner_id", "=", partner.id)]
            if state:
                domain.append(("state", "=", state))
            reqs = orm["construction.material.request"].sudo().search(
                domain, limit=int(limit), offset=int(offset), order="id desc"
            )
            data = []
            for r in reqs:
                data.append({
                    "id": r.id,
                    "name": r.name,
                    "state": r.state,
                    "obra": r.obra_id.name if r.obra_id else "",
                    "obra_id": r.obra_id.id if r.obra_id else False,
                    "total": r.total_amount,
                    "lines_count": len(r.line_ids),
                    "notes": r.notes or "",
                    "create_date": str(r.date_request) if r.date_request else "",
                })
            total = orm["construction.material.request"].sudo().search_count(domain)
            return self._ok(data=data, total=total)
        except Exception as e:
            return self._err(str(e), 500)

    # =========================================================================
    # Portal web estático
    # =========================================================================

    @http.route('/construccion/portal', type='http', auth='public', methods=['GET'], csrf=False)
    def portal_web(self, **kw):
        """Sirve el portal SPA de materiales de construcción."""
        import os
        portal_path = os.path.join(
            os.path.dirname(os.path.dirname(__file__)),
            'static', 'portal', 'portal.html'
        )
        with open(portal_path, 'r', encoding='utf-8') as f:
            content = f.read()
        return request.make_response(
            content,
            headers=[('Content-Type', 'text/html; charset=utf-8')]
        )