# -*- coding: utf-8 -*-
{
    'name': 'Construction Marketplace',
    'version': '17.0.1.23.2',
    'category': 'Construction',
    'summary': 'Marketplace de materiales de construcción para gestión de obras y solicitudes',
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
    'author': 'TFG - Construction Marketplace',
    'website': 'https://github.com/construccion-marketplace',
    'license': 'LGPL-3',
    'depends': [
        'sale',
        'stock',
        'account',
        'website',
        'mail',
    ],
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
    'demo': [
        'data/demo_data.xml',
    ],
    'installable': True,
    'application': True,
    'auto_install': False,
    'images': [],
}
