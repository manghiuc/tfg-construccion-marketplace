# -*- coding: utf-8 -*-
"""
Calculadora de transporte.
Calcula el coste de envío usando coordenadas GPS (fórmula de Haversine)
y el peso del pedido. El almacén está en Madrid por defecto.
"""
import math
from odoo import api, fields, models


class ConstructionTransportCalculator(models.Model):
    _name = 'construction.transport.calculator'
    _description = 'Calculadora de Transporte'
    _rec_name = 'name'

    name = fields.Char(string='Configuración', default='Configuración de Transporte', required=True)

    # -- Ubicación del almacén (Madrid por defecto) --
    warehouse_lat = fields.Float(string='Latitud del Almacén', digits=(9, 6), default=40.4168)
    warehouse_lon = fields.Float(string='Longitud del Almacén', digits=(9, 6), default=-3.7038)

    # -- Tarifas --
    base_price = fields.Float(string='Precio Base (€)', default=15.0, digits=(10, 2))
    price_per_km = fields.Float(string='€/km adicional', default=1.5, digits=(10, 2))
    price_per_ton = fields.Float(string='€/tonelada extra', default=5.0, digits=(10, 2))
    urgent_multiplier = fields.Float(string='Multiplicador urgente', default=1.5, digits=(10, 2))

    @api.model
    def _haversine_km(self, lat1, lon1, lat2, lon2):
        """Calcula la distancia en km entre dos puntos GPS usando la fórmula de Haversine.
        Tiene en cuenta la curvatura de la Tierra (radio = 6371 km)."""
        R = 6371.0
        phi1 = math.radians(lat1)
        phi2 = math.radians(lat2)
        delta_phi = math.radians(lat2 - lat1)
        delta_lambda = math.radians(lon2 - lon1)
        a = (math.sin(delta_phi / 2) ** 2
             + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2)
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        return R * c

    def calculate_transport_cost(self, delivery_lat, delivery_lon,
                                  total_weight_kg, is_urgent=False):
        """Calcula el coste total de transporte:
        1. Precio base cubre los primeros 10 km, luego €/km extra
        2. Primeros 500 kg gratis, luego €/tonelada extra
        3. Si es urgente se multiplica el total ×1.5"""
        self.ensure_one()

        distance_km = self._haversine_km(
            self.warehouse_lat, self.warehouse_lon,
            delivery_lat, delivery_lon
        )
        extra_km = max(0.0, distance_km - 10.0)
        base_cost = self.base_price + extra_km * self.price_per_km

        extra_kg = max(0.0, total_weight_kg - 500.0)
        weight_surcharge = (extra_kg / 1000.0) * self.price_per_ton

        subtotal = base_cost + weight_surcharge

        if is_urgent:
            urgent_surcharge = subtotal * (self.urgent_multiplier - 1.0)
            total = subtotal * self.urgent_multiplier
        else:
            urgent_surcharge = 0.0
            total = subtotal

        return {
            'distance_km': round(distance_km, 3),
            'base_cost': round(base_cost, 2),
            'weight_surcharge': round(weight_surcharge, 2),
            'urgent_surcharge': round(urgent_surcharge, 2),
            'total': round(total, 2),
        }

    @api.model
    def get_default_calculator(self):
        """Devuelve la configuración de transporte (crea una si no existe)."""
        calc = self.search([], limit=1)
        if not calc:
            calc = self.create({'name': 'Configuración de Transporte'})
        return calc
