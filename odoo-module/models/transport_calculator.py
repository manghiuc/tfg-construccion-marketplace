# -*- coding: utf-8 -*-
import math
import logging
from odoo import api, fields, models, _
from odoo.exceptions import ValidationError

_logger = logging.getLogger(__name__)


class ConstructionTransportCalculator(models.Model):
    """
    Calculadora de costes de transporte para pedidos de materiales.

    Utiliza la fórmula de Haversine para calcular distancias reales entre
    coordenadas GPS sin depender de APIs externas. Los precios se configuran
    en este mismo modelo (un único registro de configuración por empresa).
    """
    _name = 'construction.transport.calculator'
    _description = 'Calculadora de Transporte'
    _rec_name = 'name'

    name = fields.Char(
        string='Configuración',
        default='Configuración de Transporte',
        required=True,
    )

    # -------------------------------------------------------------------------
    # Ubicación del almacén
    # -------------------------------------------------------------------------
    warehouse_lat = fields.Float(
        string='Latitud del Almacén',
        digits=(9, 6),
        default=40.4168,  # Madrid por defecto
        help='Latitud geográfica del almacén principal (grados decimales)',
    )
    warehouse_lon = fields.Float(
        string='Longitud del Almacén',
        digits=(9, 6),
        default=-3.7038,  # Madrid por defecto
        help='Longitud geográfica del almacén principal (grados decimales)',
    )

    # -------------------------------------------------------------------------
    # Tarifas de transporte
    # -------------------------------------------------------------------------
    base_price = fields.Float(
        string='Precio Base (€)',
        default=15.0,
        digits=(10, 2),
        help='Precio mínimo de transporte para cualquier pedido',
    )
    price_per_km = fields.Float(
        string='Precio por km adicional (€/km)',
        default=1.5,
        digits=(10, 2),
        help='Coste adicional por kilómetro superando los primeros 10 km',
    )
    price_per_ton = fields.Float(
        string='Precio por tonelada extra (€/t)',
        default=5.0,
        digits=(10, 2),
        help='Coste adicional por tonelada completa que supere los 500 kg',
    )
    urgent_multiplier = fields.Float(
        string='Multiplicador urgente',
        default=1.5,
        digits=(10, 2),
        help='Factor multiplicador que se aplica al coste total en pedidos urgentes',
    )

    # =========================================================================
    # Lógica de cálculo
    # =========================================================================

    @api.model
    def _haversine_km(self, lat1, lon1, lat2, lon2):
        """
        Calcula la distancia en kilómetros entre dos puntos geográficos
        usando la fórmula de Haversine.

        La fórmula tiene en cuenta la curvatura de la Tierra (radio medio = 6371 km)
        y produce el arco más corto entre los dos puntos (great-circle distance).

        :param lat1: latitud del punto 1 en grados decimales
        :param lon1: longitud del punto 1 en grados decimales
        :param lat2: latitud del punto 2 en grados decimales
        :param lon2: longitud del punto 2 en grados decimales
        :return: distancia en kilómetros (float)
        """
        R = 6371.0  # Radio medio de la Tierra en km

        # Convertir grados a radianes
        phi1 = math.radians(lat1)
        phi2 = math.radians(lat2)
        delta_phi = math.radians(lat2 - lat1)
        delta_lambda = math.radians(lon2 - lon1)

        # Haversine
        a = (math.sin(delta_phi / 2) ** 2
             + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2)
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

        return R * c

    def calculate_transport_cost(self, delivery_lat, delivery_lon,
                                  total_weight_kg, is_urgent=False):
        """
        Calcula el coste total de transporte para un pedido.

        Lógica de precios:
        1. Distancia: se cobra la tarifa base para los primeros 10 km.
           Por cada km adicional se añade price_per_km.
        2. Peso: los primeros 500 kg son gratuitos.
           Por cada tonelada (fracción de 1000 kg) adicional se suma price_per_ton.
        3. Urgencia: si is_urgent=True el total se multiplica por urgent_multiplier.

        :param delivery_lat: latitud del punto de entrega
        :param delivery_lon: longitud del punto de entrega
        :param total_weight_kg: peso total del pedido en kilogramos
        :param is_urgent: True si el pedido es urgente
        :return: dict con claves:
                 distance_km, base_cost, weight_surcharge,
                 urgent_surcharge, total
        """
        self.ensure_one()

        # --- 1. Distancia ---
        distance_km = self._haversine_km(
            self.warehouse_lat, self.warehouse_lon,
            delivery_lat, delivery_lon
        )

        # Coste por distancia: precio base cubre los primeros 10 km
        extra_km = max(0.0, distance_km - 10.0)
        base_cost = self.base_price + extra_km * self.price_per_km

        # --- 2. Peso ---
        # Toneladas adicionales sobre 500 kg (redondeo hacia abajo a toneladas completas)
        extra_kg = max(0.0, total_weight_kg - 500.0)
        extra_tons = extra_kg / 1000.0          # fracción decimal de toneladas
        weight_surcharge = extra_tons * self.price_per_ton

        # --- 3. Subtotal antes de urgencia ---
        subtotal = base_cost + weight_surcharge

        # --- 4. Suplemento por urgencia ---
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
        """
        Devuelve el primer registro de configuración existente.
        Si no existe ninguno, crea uno con valores por defecto.
        """
        calc = self.search([], limit=1)
        if not calc:
            calc = self.create({'name': 'Configuración de Transporte'})
        return calc
