# -*- coding: utf-8 -*-
# ============================================================================
# CARGA DE TODOS LOS MODELOS (tablas de la base de datos)
# Cada línea importa un archivo Python que define una parte del sistema.
# Odoo necesita que estén todos listados aquí para reconocerlos.
# ============================================================================
from . import construction_obra                    # Gestión de obras de construcción
from . import construction_material_request        # Solicitudes de materiales (la cabecera del pedido)
from . import construction_material_request_line   # Líneas de cada solicitud (los productos concretos)
from . import recommendation_engine                # Motor de recomendaciones inteligente
from . import chatbot_engine                       # Chatbot que sugiere materiales
from . import partner_extension                    # Extensión de clientes (tipo, puntos, FCM)
from . import transport_calculator                 # Calculadora de costes de transporte
from . import loyalty_program                      # Historial de puntos de fidelización
from . import res_partner_extension                # Contadores de pedidos/obras en ficha de cliente
from . import product_extension                    # Campo de stock editable para el marketplace
