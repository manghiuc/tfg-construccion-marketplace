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

    @http.route("/api/construction/auth/login", type="json", auth="public", methods=["POST"], csrf=False)
    def auth_login(self):
        p = request.get_json_data() or {}
        try:
            uid = request.session.authenticate(p.get("db", request.db), p["login"], p["password"])
            if not uid:
                return dict(success=False, error=dict(code=401, message="Credenciales incorrectas"))
            u = request.env["res.users"].sudo().browse(uid)
            return dict(success=True, data=dict(uid=uid, name=u.name, login=u.login, session_id=request.session.sid))
        except AccessDenied:
            return dict(success=False, error=dict(code=401, message="Acceso denegado"))

    @http.route("/api/construction/obras", type="http", auth="user", methods=["GET"], csrf=False)
    def get_obras(self, state=None, limit="100", offset="0", **kw):
        try:
            orm = request.env
            d = [("state", "=", state)] if state else []
            items = orm["construction.obra"].search(d, limit=int(limit), offset=int(offset))
            data = [{"id": o.id, "name": o.name, "code": o.code, "state": o.state,
                     "partner": o.partner_id.name, "requests": o.material_request_count} for o in items]
            return self._ok(data=data, total=orm["construction.obra"].search_count(d))
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/material_request", type="http", auth="user", methods=["POST"], csrf=False)
    def create_request(self, **kw):
        try:
            orm = request.env
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            obra = orm["construction.obra"].browse(int(p["obra_id"]))
            if not obra.exists():
                return self._err("Obra no encontrada", 404)
            lines = [(0, 0, {"product_id": int(l["product_id"]),
                              "product_qty": float(l.get("product_qty", 1)),
                              "price_unit": float(l.get("price_unit", 0)),
                              "notes": l.get("notes", "")}) for l in p.get("lines", [])]
            req = orm["construction.material.request"].create({
                "obra_id": int(p["obra_id"]), "user_id": request.uid,
                "partner_id": obra.partner_id.id or False,
                "notes": p.get("notes", ""), "line_ids": lines})
            return self._ok(data={"id": req.id, "name": req.name, "state": req.state}, message="Solicitud creada")
        except (ValidationError, UserError) as e:
            return self._err(str(e), 422)
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/material_request/<int:rid>/status", type="http", auth="user", methods=["GET"], csrf=False)
    def request_status(self, rid, **kw):
        try:
            orm = request.env
            r = orm["construction.material.request"].browse(rid)
            if not r.exists():
                return self._err("No encontrado", 404)
            return self._ok(data={"id": r.id, "name": r.name, "state": r.state,
                                  "tracking": r.tracking_info or "", "total": r.total_amount,
                                  "lines": [{"product": l.product_id.name, "qty": l.product_qty,
                                              "subtotal": l.subtotal} for l in r.line_ids]})
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/products", type="http", auth="user", methods=["GET"], csrf=False)
    def get_products(self, search="", limit="50", **kw):
        try:
            orm = request.env
            d = [("sale_ok", "=", True), ("active", "=", True)]
            if search:
                d.append(("name", "ilike", search))
            prods = orm["product.product"].search(d, limit=int(limit))
            return self._ok(data=[{"id": p.id, "name": p.name, "price": p.lst_price,
                                    "uom": p.uom_id.name, "image": "/web/image/product.product/%d/image_128" % p.id}
                                   for p in prods])
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/recommendations", type="http", auth="user", methods=["GET"], csrf=False)
    def recommendations(self, obra_id=None, obra_type="reforma", category=None, limit="10", **kw):
        try:
            orm = request.env
            recs = orm["construction.recommendation.engine"].get_recommendations(
                obra_id=int(obra_id) if obra_id else None, tipo_obra=obra_type,
                categoria=category, limit=int(limit))
            return self._ok(data=recs, obra_type=obra_type)
        except Exception as e:
            return self._err(str(e), 500)

    @http.route("/api/construction/chatbot", type="json", auth="user", methods=["POST"], csrf=False)
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

    @http.route("/api/construction/calculate_transport", type="http", auth="user", methods=["POST"], csrf=False)
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

    @http.route("/api/construction/loyalty/status", type="http", auth="user", methods=["GET"], csrf=False)
    def loyalty_status(self, **kw):
        """
        Devuelve el estado del programa de fidelización del usuario autenticado.

        Respuesta:
          {
            "success": true,
            "data": {
              "points_balance":    <float>,
              "loyalty_level":     "bronze"|"silver"|"gold",
              "points_to_next_level": <float>
            }
          }
        """
        try:
            orm = request.env
            partner = orm["res.users"].sudo().browse(request.uid).partner_id
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

    @http.route("/api/construction/loyalty/redeem", type="http", auth="user", methods=["POST"], csrf=False)
    def loyalty_redeem(self, **kw):
        """
        Canjea puntos de fidelización.

        Body JSON esperado:
          {
            "points_to_redeem": <float>,
            "request_id":       <int, opcional>
          }

        Respuesta:
          {
            "success": true,
            "data": {
              "redeemed_points":  <float>,
              "new_balance":      <float>,
              "loyalty_level":    <str>
            }
          }
        """
        try:
            orm = request.env
            p = json.loads(request.httprequest.get_data(as_text=True) or "{}")
            points = float(p.get("points_to_redeem", 0))
            request_id = int(p["request_id"]) if p.get("request_id") else None

            if points <= 0:
                return self._err("points_to_redeem debe ser mayor que 0", 400)

            partner = orm["res.users"].sudo().browse(request.uid).partner_id
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
    # Calculador de materiales por tipo de obra
    # =========================================================================

    # Fórmulas de materiales por tipo de obra (unidades por m²)
    _MATERIAL_FORMULAS = {
        'banyo': [
            {'material': 'Azulejo',      'unit': 'm²',   'formula': lambda m2: round(m2 * 1.1, 3)},
            {'material': 'Mortero cola', 'unit': 'kg',   'formula': lambda m2: round(m2 * 5, 3)},
            {'material': 'Lechada',      'unit': 'kg',   'formula': lambda m2: round(m2 * 0.3, 3)},
        ],
        'solera': [
            {'material': 'Cemento',      'unit': 'sacos', 'formula': lambda m2: round(m2 * 0.3, 3)},
            {'material': 'Arena',        'unit': 'm³',    'formula': lambda m2: round(m2 * 0.02, 3)},
            {'material': 'Agua',         'unit': 'L',     'formula': lambda m2: round(m2 * 15, 3)},
        ],
        'tabique_ladrillo': [
            {'material': 'Ladrillo',     'unit': 'ud',    'formula': lambda m2: round(m2 * 50, 0)},
            {'material': 'Cemento',      'unit': 'sacos', 'formula': lambda m2: round(m2 * 0.1, 3)},
            {'material': 'Arena',        'unit': 'm³',    'formula': lambda m2: round(m2 * 0.01, 3)},
        ],
        'pintura': [
            {'material': 'Pintura',      'unit': 'L',     'formula': lambda m2: round(m2 * 0.3, 3)},
            {'material': 'Imprimación',  'unit': 'L',     'formula': lambda m2: round(m2 * 0.1, 3)},
        ],
    }

    @http.route("/api/construction/catalog/calculator", type="http", auth="user", methods=["GET"], csrf=False)
    def catalog_calculator(self, type=None, m2=None, **kw):
        """
        Calcula la lista de materiales necesarios para un tipo de obra y superficie.

        Query params:
          type  — tipo de obra: banyo | solera | tabique_ladrillo | pintura
          m2    — superficie en metros cuadrados (float)

        Respuesta:
          {
            "success": true,
            "data": {
              "obra_type": <str>,
              "m2":        <float>,
              "materials": [
                {"material": <str>, "unit": <str>, "quantity": <float>},
                ...
              ]
            }
          }
        """
        try:
            if not type:
                return self._err("Parámetro 'type' obligatorio", 400)
            if not m2:
                return self._err("Parámetro 'm2' obligatorio", 400)

            obra_type = type.lower().strip()
            superficie = float(m2)

            if superficie <= 0:
                return self._err("El valor de m2 debe ser mayor que 0", 400)

            formulas = self._MATERIAL_FORMULAS.get(obra_type)
            if formulas is None:
                tipos_validos = ', '.join(self._MATERIAL_FORMULAS.keys())
                return self._err(
                    "Tipo de obra '%s' no reconocido. Tipos válidos: %s" % (obra_type, tipos_validos),
                    400
                )

            materials = [
                {
                    'material': item['material'],
                    'unit': item['unit'],
                    'quantity': item['formula'](superficie),
                }
                for item in formulas
            ]

            return self._ok(data={
                'obra_type': obra_type,
                'm2': superficie,
                'materials': materials,
            })
        except ValueError:
            return self._err("El valor de m2 debe ser numérico", 400)
        except Exception as e:
            return self._err(str(e), 500)

    # =========================================================================
    # Detalle de obra y producto
    # =========================================================================

    @http.route("/api/construction/obras/<int:obra_id>", type="http", auth="user", methods=["GET"], csrf=False)
    def get_obra_detail(self, obra_id, **kw):
        try:
            obra = request.env["construction.obra"].browse(obra_id)
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

    @http.route("/api/construction/products/<int:product_id>", type="http", auth="user", methods=["GET"], csrf=False)
    def get_product_detail(self, product_id, **kw):
        try:
            prod = request.env["product.product"].browse(product_id)
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

    @http.route("/api/construction/auth/register", type="json", auth="public", methods=["POST"], csrf=False)
    def auth_register(self):
        p = request.params if hasattr(request, 'params') else (request.get_json_data() or {})
        try:
            name     = (p.get("name") or "").strip()
            email    = (p.get("login") or p.get("email") or "").strip().lower()
            password = p.get("password") or ""
            phone    = (p.get("phone") or "").strip()
            customer_type = p.get("customer_type", "particular")  # particular | autonomo | empresa

            if not name or not email or not password:
                return dict(success=False, error=dict(code=400, message="name, login y password son obligatorios"))
            if len(password) < 6:
                return dict(success=False, error=dict(code=400, message="La contraseña debe tener al menos 6 caracteres"))

            # Comprobar si ya existe un usuario con ese email
            existing = request.env["res.users"].sudo().search([("login", "=", email)], limit=1)
            if existing:
                return dict(success=False, error=dict(code=409, message="Ya existe una cuenta con ese correo electrónico"))

            # Obtener el grupo portal de Odoo
            portal_group = request.env.ref("base.group_portal")

            # Determinar si es empresa
            is_company = (customer_type == "empresa")

            # Crear el usuario (Odoo crea automáticamente el res.partner asociado)
            new_user = request.env["res.users"].sudo().create({
                "name": name,
                "login": email,
                "email": email,
                "password": password,
                "groups_id": [(6, 0, [portal_group.id])],
            })

            # Actualizar el partner con datos adicionales
            partner = new_user.partner_id
            partner.sudo().write({
                "phone": phone,
                "is_company": is_company,
                "customer_rank": 1,
                "comment": "Registrado vía portal web Construction Marketplace. Tipo: %s" % customer_type,
            })

            # Auto-login tras registro (puede fallar para usuarios portal; no es crítico)
            auto_login_ok = False
            uid = None
            try:
                uid = request.session.authenticate(request.db or "construction_marketplace", email, password)
                auto_login_ok = bool(uid)
            except Exception:
                pass

            return dict(success=True, data=dict(
                uid=uid or 0,
                name=new_user.name,
                login=new_user.login,
                partner_id=partner.id,
                session_id=request.session.sid if auto_login_ok else None,
                customer_type=customer_type,
                auto_login=auto_login_ok,
                message="Cuenta creada. Por favor inicia sesión." if not auto_login_ok else "Bienvenido/a, %s." % new_user.name,
            ))

        except Exception as e:
            import traceback
            tb = traceback.format_exc()
            _logger.error("Error en registro: %s\n%s", str(e), tb)
            return dict(success=False, error=dict(code=500, message="Error al crear la cuenta: " + str(e), traceback=tb))

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