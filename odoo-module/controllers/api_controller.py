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
            p = json.loads(request.get_data(as_text=True) or "{}")
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