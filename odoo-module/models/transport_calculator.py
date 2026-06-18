# -*- coding: utf-8 -*-
# ============================================================================
# CALCULADORA DE COSTES DE TRANSPORTE
# Este archivo calcula cuánto cuesta enviar los materiales desde el almacén
# hasta la obra del cliente.
#
# Usa las coordenadas GPS (latitud/longitud) para calcular la distancia
# real entre dos puntos de la Tierra, sin necesidad de Google Maps.
#
# Fórmula de precio:
#   1. Precio base (15€) cubre los primeros 10 km
#   2. Cada km extra cuesta 1.50€
#   3. Los primeros 500 kg son gratis; cada tonelada extra cuesta 5€
#   4. Si es urgente, el total se multiplica por 1.5 (50% más caro)
# ============================================================================
import math
import logging
from odoo import api, fields, models, _
from odoo.exceptions import ValidationError

_logger = logging.getLogger(__name__)


class ConstructionTransportCalculator(models.Model):
    """
    Calculadora de costes de transporte para pedidos de materiales.
    Usa la fórmula de Haversine para calcular distancias entre coordenadas GPS.
    """
    _name = 'construction.transport.calculator'
    _description = 'Calculadora de Transporte'
    _rec_name = 'name'

    # Nombre de la configuración
    name = fields.Char(
        string='Configuración',
        default='Configuración de Transporte',
        required=True,
    )

    # -------------------------------------------------------------------------
    # UBICACIÓN DEL ALMACÉN (desde dónde salen los materiales)
    # Por defecto está puesto Madrid, pero se puede cambiar
    # -------------------------------------------------------------------------
    warehouse_lat = fields.Float(
        string='Latitud del Almacén',
        digits=(9, 6),
        default=40.4168,  # Madrid
        help='Latitud geográfica del almacén principal (grados decimales)',
    )
    warehouse_lon = fields.Float(
        string='Longitud del Almacén',
        digits=(9, 6),
        default=-3.7038,  # Madrid
        help='Longitud geográfica del almacén principal (grados decimales)',
    )

    # -------------------------------------------------------------------------
    # TARIFAS DE TRANSPORTE (precios configurables)
    # -------------------------------------------------------------------------

    # Precio mínimo de cualquier envío (cubre los primeros 10 km)
    base_price = fields.Float(
        string='Precio Base (€)',
        default=15.0,
        digits=(10, 2),
        help='Precio mínimo de transporte para cualquier pedido',
    )

    # Cuánto se cobra por cada km extra (después de los 10 km)
    price_per_km = fields.Float(
        string='Precio por km adicional (€/km)',
        default=1.5,
        digits=(10, 2),
        help='Coste adicional por kilómetro superando los primeros 10 km',
    )

    # Cuánto se cobra por cada tonelada extra (después de 500 kg)
    price_per_ton = fields.Float(
        string='Precio por tonelada extra (€/t)',
        default=5.0,
        digits=(10, 2),
        help='Coste adicional por tonelada completa que supere los 500 kg',
    )

    # Multiplicador para pedidos urgentes (1.5 = 50% más caro)
    urgent_multiplier = fields.Float(
        string='Multiplicador urgente',
        default=1.5,
        digits=(10, 2),
        help='Factor multiplicador que se aplica al coste total en pedidos urgentes',
    )

    # =========================================================================
    # FÓRMULA DE HAVERSINE
    # Calcula la distancia entre dos puntos del planeta usando sus coordenadas.
    # Tiene en cuenta que la Tierra es redonda (no plana).
    # Resultado en kilómetros.
    # =========================================================================

    @api.model
    def _haversine_km(self, lat1, lon1, lat2, lon2):
        """
        Calcula la distancia en kilómetros entre dos puntos de la Tierra
        usando sus coordenadas GPS.
        """
        R = 6371.0  # Radio de la Tierra en km

        # Convertir grados a radianes (unidad matemática para ángulos)
        phi1 = math.radians(lat1)
        phi2 = math.radians(lat2)
        delta_phi = math.radians(lat2 - lat1)
        delta_lambda = math.radians(lon2 - lon1)

        # Aplicar la fórmula de Haversine
        a = (math.sin(delta_phi / 2) ** 2
             + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2)
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

        return R * c

    # =========================================================================
    # CALCULAR EL COSTE TOTAL DE TRANSPORTE
    # Combina la distancia + el peso + la urgencia para dar un precio final
    # =========================================================================

    def calculate_transport_cost(self, delivery_lat, delivery_lon,
                                  total_weight_kg, is_urgent=False):
        """
        Calcula cuánto cuesta llevar los materiales a la obra.

        Pasos:
        1. Calcula la distancia entre el almacén y la obra
        2. Cobra la tarifa base (primeros 10 km gratis) + extra por km
        3. Si pesa más de 500 kg, cobra por las toneladas extra
        4. Si es urgente, multiplica el total por 1.5
        """
        self.ensure_one()

        # --- Paso 1: Calcular la distancia ---
        distance_km = self._haversine_km(
            self.warehouse_lat, self.warehouse_lon,
            delivery_lat, delivery_lon
        )

        # --- Paso 2: Coste por distancia ---
        # Los primeros 10 km están incluidos en el precio base
        extra_km = max(0.0, distance_km - 10.0)
        base_cost = self.base_price + extra_km * self.price_per_km

        # --- Paso 3: Coste por peso ---
        # Los primeros 500 kg son gratis
        extra_kg = max(0.0, total_weight_kg - 500.0)
        extra_tons = extra_kg / 1000.0
        weight_surcharge = extra_tons * self.price_per_ton

        # --- Paso 4: Subtotal antes de urgencia ---
        subtotal = base_cost + weight_surcharge

        # --- Paso 5: Suplemento si es urgente ---
        if is_urgent:
            urgent_surcharge = subtotal * (self.urgent_multiplier - 1.0)
            total = subtotal * self.urgent_multiplier
        else:
            urgent_surcharge = 0.0
            total = subtotal

        # Devolver todos los detalles del cálculo
        return {
            'distance_km': round(distance_km, 3),
            'base_cost': round(base_cost, 2),
            'weight_surcharge': round(weight_surcharge, 2),
            'urgent_surcharge': round(urgent_surcharge, 2),
            'total': round(total, 2),
        }

    # =========================================================================
    # OBTENER LA CONFIGURACIÓN DE TRANSPORTE
    # Si no existe ninguna configuración, crea una con los valores por defecto
    # =========================================================================

    @api.model
    def get_default_calculator(self):
        """
        Busca la configuración de transporte. Si no existe, crea una nueva
        con los valores por defecto (almacén en Madrid, tarifa estándar).
        """
        calc = self.search([], limit=1)
        if not calc:
            calc = self.create({'name': 'Configuración de Transporte'})
        return calc
