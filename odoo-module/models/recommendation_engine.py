# -*- coding: utf-8 -*-
"""
Motor de Recomendación de Materiales - TFG Marketplace Construcción
====================================================================
Algoritmo basado en Item-based Collaborative Filtering con Similitud Coseno.
Referencias académicas: Sarwar et al. (2001) "Item-based collaborative
filtering recommendation algorithms", WWW Conference.

Proceso:
1. Construye perfil de materiales de cada obra (vector de frecuencias)
2. Calcula similitud coseno entre obras
3. KNN: encuentra las K obras más parecidas
4. Score ponderado: score(material) = Σ(similitud × frecuencia)
5. Reglas de dominio: bonus por conocimiento experto (cold start)
"""
import math
import logging
from collections import defaultdict
from odoo import models, api

_logger = logging.getLogger(__name__)


class MaterialRecommendationEngine(models.AbstractModel):
    _name = 'construction.recommendation.engine'
    _description = 'Motor de Recomendación de Materiales (Collaborative Filtering)'

    @api.model
    def _get_obra_material_profile(self, obra_id=None, tipo_obra=None):
        """Construye vector de frecuencias normalizadas (norma L2) para una obra."""
        domain = []
        if obra_id:
            domain = [('request_id.obra_id', '=', obra_id)]
        elif tipo_obra:
            domain = [('request_id.obra_id.obra_type', '=', tipo_obra)]

        lineas = self.env['construction.material.request.line'].search(domain)
        frecuencias = defaultdict(float)
        for linea in lineas:
            frecuencias[linea.product_id.id] += linea.product_qty

        if not frecuencias:
            return {}

        magnitud = math.sqrt(sum(v ** 2 for v in frecuencias.values()))
        if magnitud == 0:
            return {}
        return {pid: freq / magnitud for pid, freq in frecuencias.items()}

    @api.model
    def _cosine_similarity(self, perfil_a, perfil_b):
        """Similitud coseno entre dos perfiles. Vectores ya normalizados → producto punto."""
        if not perfil_a or not perfil_b:
            return 0.0
        comunes = set(perfil_a.keys()) & set(perfil_b.keys())
        return sum(perfil_a[p] * perfil_b[p] for p in comunes)

    @api.model
    def _find_similar_obras(self, obra_id, tipo_obra, top_k=5):
        """KNN: encuentra las K obras con mayor similitud coseno."""
        perfil_ref = self._get_obra_material_profile(obra_id=obra_id, tipo_obra=tipo_obra)
        if not perfil_ref and tipo_obra:
            perfil_ref = self._get_obra_material_profile(tipo_obra=tipo_obra)
        if not perfil_ref:
            return []

        domain = [('id', '!=', obra_id)] if obra_id else []
        obras = self.env['construction.obra'].search(domain)
        similitudes = []
        for obra in obras:
            perfil = self._get_obra_material_profile(obra_id=obra.id)
            if perfil:
                sim = self._cosine_similarity(perfil_ref, perfil)
                if sim > 0:
                    similitudes.append((obra.id, sim))

        similitudes.sort(key=lambda x: x[1], reverse=True)
        return similitudes[:top_k]

    @api.model
    def _calculate_material_scores(self, obras_similares, categoria=None):
        """Score(material) = Σ(similitud_obra × frecuencia_material_en_obra)."""
        scores = defaultdict(float)
        for obra_id, similitud in obras_similares:
            if similitud < 0.01:
                continue
            domain = [('request_id.obra_id', '=', obra_id)]
            if categoria:
                domain.append(('product_id.categ_id.name', 'ilike', categoria))
            for linea in self.env['construction.material.request.line'].search(domain):
                scores[linea.product_id.id] += similitud * linea.product_qty
        return scores

    # Reglas de dominio para resolver el cold start (sin historial)
    DOMAIN_RULES = {
        'reforma': {
            'fontaneria': ['tubo pvc', 'grifo', 'sifon', 'manguera'],
            'electricidad': ['cable', 'caja empotrar', 'interruptor'],
            'albanileria': ['cemento', 'arena', 'ladrillo'],
        },
        'construccion_nueva': {
            'albanileria': ['cemento', 'bloque hormigon', 'hierro', 'arena'],
            'fontaneria': ['tubo cobre', 'valvula', 'contador'],
            'electricidad': ['cable 2.5mm', 'cuadro electrico'],
        },
        'rehabilitacion': {
            'aislamiento': ['lana roca', 'poliestireno', 'membrana'],
            'pintura': ['pintura plastica', 'imprimacion', 'brocha'],
        },
    }

    @api.model
    def _apply_domain_rules_bonus(self, scores, tipo_obra, categoria):
        """Añade bonus a materiales según reglas de dominio (mitiga cold start)."""
        keywords = self.DOMAIN_RULES.get(tipo_obra, {}).get(categoria, [])
        for keyword in keywords:
            for prod in self.env['product.product'].search([('name', 'ilike', keyword)], limit=3):
                scores[prod.id] = scores.get(prod.id, 0) + 0.5
        return scores

    @api.model
    def get_recommendations(self, obra_id=None, tipo_obra='reforma', categoria=None, limit=10):
        """
        Punto de entrada principal del motor de recomendaciones.

        Returns: lista de dicts con product_id, nombre, score, confianza, nivel_confianza
        """
        obras_similares = self._find_similar_obras(obra_id, tipo_obra, top_k=5)
        scores = self._calculate_material_scores(obras_similares, categoria)
        scores = self._apply_domain_rules_bonus(scores, tipo_obra, categoria)

        if not scores:
            return self._get_popular_materials(categoria, limit)

        # Excluir materiales ya pedidos para esta obra
        if obra_id:
            ya_usados = set(
                self.env['construction.material.request.line'].search([
                    ('request_id.obra_id', '=', obra_id)
                ]).mapped('product_id.id')
            )
            scores = {pid: s for pid, s in scores.items() if pid not in ya_usados}

        top = sorted(scores.items(), key=lambda x: x[1], reverse=True)[:limit]
        score_max = top[0][1] if top else 1.0

        result = []
        for product_id, score in top:
            prod = self.env['product.product'].browse(product_id)
            if not prod.exists():
                continue
            confianza = min(score / score_max, 1.0) if score_max > 0 else 0.0
            result.append({
                'product_id': product_id,
                'nombre': prod.name,
                'referencia': prod.default_code or '',
                'categoria': categoria or 'varios',
                'precio_estimado': prod.lst_price,
                'score': round(score, 4),
                'confianza': round(confianza, 2),
                'nivel_confianza': 'alta' if confianza >= 0.8 else ('media' if confianza >= 0.5 else 'baja'),
                'imagen_url': '/web/image/product.product/%d/image_128' % product_id,
            })
        return result

    @api.model
    def _get_popular_materials(self, categoria=None, limit=10):
        """Fallback: materiales más solicitados globalmente cuando no hay historial."""
        domain = []
        if categoria:
            domain.append(('product_id.categ_id.name', 'ilike', categoria))
        grupos = self.env['construction.material.request.line'].read_group(
            domain=domain,
            fields=['product_id', 'product_qty:sum'],
            groupby=['product_id'],
            orderby='product_qty desc',
            limit=limit,
        )
        result = []
        for g in grupos:
            if not g.get('product_id'):
                continue
            pid = g['product_id'][0]
            prod = self.env['product.product'].browse(pid)
            result.append({
                'product_id': pid,
                'nombre': prod.name,
                'referencia': prod.default_code or '',
                'categoria': categoria or 'varios',
                'precio_estimado': prod.lst_price,
                'score': g.get('product_qty', 0),
                'confianza': 0.5,
                'nivel_confianza': 'media',
                'imagen_url': '/web/image/product.product/%d/image_128' % pid,
            })
        return result
