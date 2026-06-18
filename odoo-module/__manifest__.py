# -*- coding: utf-8 -*-
# ============================================================================
# ARCHIVO DE CONFIGURACIÓN DEL MÓDULO (Manifest)
# Este archivo le dice a Odoo toda la información sobre nuestro módulo:
# cómo se llama, qué hace, de qué otros módulos depende,
# y qué archivos tiene que cargar cuando se instala.
# ============================================================================
{
    # Nombre que aparece en la lista de aplicaciones de Odoo
    'name': 'Construction Marketplace',

    # Versión del módulo (17.0 = versión de Odoo, 1.23.4 = nuestra versión)
    'version': '17.0.1.25.0',

    # Categoría para organizar el módulo en Odoo
    'category': 'Construction',

    # Resumen corto que aparece debajo del nombre en la lista de apps
    'summary': 'Marketplace de materiales de construcción para gestión de obras y solicitudes',

    # Descripción larga con todo lo que hace el módulo
    'description': """
        Módulo de marketplace para la gestión de materiales de construcción.
        Permite a las obras solicitar materiales, convertirlas en pedidos de venta
        y hacer seguimiento del estado de cada solicitud.

        Funcionalidades principales:
        - Gestión de obras de construcción
        - Solicitudes de materiales con líneas de producto
        - Conversión automática a pedidos de venta de Odoo
        - API REST para integración con aplicaciones externas
        - Panel de control con seguimiento de estado
    """,

    # Quién ha hecho el módulo
    'author': 'TFG - Construction Marketplace',
    'website': 'https://github.com/construccion-marketplace',

    # Tipo de licencia (código abierto)
    'license': 'LGPL-3',

    # Módulos de Odoo que necesitamos tener instalados para que este funcione:
    # - sale: para crear pedidos de venta
    # - stock: para gestionar inventario/almacén
    # - account: para facturación y contabilidad
    # - website: para el portal web público
    # - mail: para el sistema de mensajería y seguimiento (chatter)
    'depends': [
        'sale',
        'stock',
        'account',
        'website',
        'mail',
    ],

    # Archivos que Odoo tiene que cargar al instalar el módulo:
    # - security: permisos de acceso (quién puede ver/editar cada cosa)
    # - data: datos iniciales (numeración automática, tareas programadas, productos)
    # - views: las pantallas que verá el usuario en Odoo
    'data': [
        'security/ir.model.access.csv',
        'data/sequences.xml',
        'data/cron_jobs.xml',
        'data/construction_products.xml',
        'views/construction_obra_views.xml',
        'views/construction_material_request_views.xml',
        'views/construction_menu_views.xml',
        'views/res_partner_views.xml',
    ],

    # Datos de ejemplo que se cargan solo si se activa el modo demostración
    'demo': [
        'data/demo_data.xml',
    ],

    # Se puede instalar = Sí
    'installable': True,
    # Aparece como aplicación independiente en Odoo (con su propio icono)
    'application': True,
    # No se instala solo al instalar otro módulo
    'auto_install': False,
    'images': [],
}
