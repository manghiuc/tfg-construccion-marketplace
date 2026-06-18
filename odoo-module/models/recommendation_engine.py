# -*- coding: utf-8 -*-
# ============================================================================
# MOTOR DE RECOMENDACIÓN DE MATERIALES
# Este archivo tiene una "inteligencia artificial" sencilla que recomienda
# materiales al cliente basándose en lo que otras obras parecidas han comprado.
#
# CÓMO FUNCIONA (Filtrado Colaborativo basado en Ítems):
#
#   1. Mira qué materiales ha comprado cada obra
#   2. Busca obras que compren materiales parecidos a la tuya
#   3. Recomienda los materiales que esas obras similares compraron
#      pero tú todavía no tienes
#
# Es el mismo principio que usa Amazon con "Los clientes que compraron
# esto también compraron..." pero aplicado a materiales de construcción.
#
# CUANDO NO HAY HISTORIAL (cold start):
#   Usa reglas fijas de conocimiento experto (ej: para reformas de baño
#   normalmente se necesita fontanería, electricidad y albañilería)
#
# Referencia académica: Sarwar et al. (2001) "Item-based collaborative
# filtering recommendation algorithms"
# ============================================================================
import math
import logging
from collections import defaultdict
from odoo import models, api

_logger = logging.getLogger(__name__)


class MaterialRecommendationEngine(models.AbstractModel):
    """
    Motor de recomendación de materiales de construcción.
    No crea tabla en la base de datos (AbstractModel), solo ofrece métodos
    que otros modelos o el API pueden llamar.
    """
    _name = 'construction.recommendation.engine'
    _description = 'Motor de Recomendación de Materiales (Collaborative Filtering)'

    # =========================================================================
    # PASO 1: CONSTRUIR EL PERFIL DE MATERIALES DE UNA OBRA
    # Cuenta cuántas unidades de cada material se han pedido para esa obra
    # y normaliza las cantidades (las convierte a proporciones entre 0 y 1)
    # =========================================================================

    @api.model
    def _get_obra_material_profile(self, obra_id=None, tipo_obra=None):
        """
        Construye un "vector de frecuencias" para una obra.
        Es como una lista de qué materiales se usan y en qué proporción.
        El resultado se normaliza con norma L2 para poder comparar obras
        de distinto tamaño.
        """
        # Buscar las líneas de pedidos de esa obra
        domain = []
        if obra_id:
            domain = [('request_id.obra_id', '=', obra_id)]
        elif tipo_obra:
            domain = [('request_id.obra_id.obra_type', '=', tipo_obra)]

        lineas = self.env['construction.material.request.line'].search(domain)

        # Contar cuántas unidades de cada producto se han pedido
        frecuencias = defaultdict(float)
        for linea in lineas:
            frecuencias[linea.product_id.id] += linea.product_qty

        if not frecuencias:
            return {}

        # Normalizar: convertir a proporciones (norma L2)
        # Esto permite comparar obras que han hecho muchos pedidos con obras que han hecho pocos
        magnitud = math.sqrt(sum(v ** 2 for v in frecuencias.values()))
        if magnitud == 0:
            return {}
        return {pid: freq / magnitud for pid, freq in frecuencias.items()}

    # =========================================================================
    # PASO 2: CALCULAR SIMILITUD ENTRE DOS OBRAS
    # Usa la "similitud coseno": mide el ángulo entre los dos vectores.
    # Si dos obras compran exactamente lo mismo → similitud = 1 (idénticas)
    # Si no comparten ningún material → similitud = 0 (nada en común)
    # =========================================================================

    @api.model
    def _cosine_similarity(self, perfil_a, perfil_b):
        """
        Calcula cuánto se parecen dos obras mirando qué materiales comparten.
        Resultado entre 0 (nada en común) y 1 (idénticas).
        """
        if not perfil_a or not perfil_b:
            return 0.0
        # Solo mirar los materiales que ambas obras han comprado
        comunes = set(perfil_a.keys()) & set(perfil_b.keys())
        # Como los vectores ya están normalizados, el producto punto = similitud coseno
        return sum(perfil_a[p] * perfil_b[p] for p in comunes)

    # =========================================================================
    # PASO 3: ENCONTRAR LAS OBRAS MÁS PARECIDAS (KNN)
    # Compara la obra actual con todas las demás y se queda con las K más
    # parecidas (K = 5 por defecto)
    # =========================================================================

    @api.model
    def _find_similar_obras(self, obra_id, tipo_obra, top_k=5):
        """
        Busca las K obras más parecidas a la obra actual.
        KNN = K-Nearest Neighbors (los K vecinos más cercanos).
        """
        # Obtener el perfil de la obra actual
        perfil_ref = self._get_obra_material_profile(obra_id=obra_id, tipo_obra=tipo_obra)
        if not perfil_ref and tipo_obra:
            # Si la obra no tiene pedidos, usar el perfil del tipo de obra
            perfil_ref = self._get_obra_material_profile(tipo_obra=tipo_obra)
        if not perfil_ref:
            return []

        # Comparar con todas las demás obras
        domain = [('id', '!=', obra_id)] if obra_id else []
        obras = self.env['construction.obra'].search(domain)
        similitudes = []
        for obra in obras:
            perfil = self._get_obra_material_profile(obra_id=obra.id)
            if perfil:
                sim = self._cosine_similarity(perfil_ref, perfil)
                if sim > 0:
                    similitudes.append((obra.id, sim))

        # Ordenar por similitud (de mayor a menor) y quedarse con las K mejores
        similitudes.sort(key=lambda x: x[1], reverse=True)
        return similitudes[:top_k]

    # =========================================================================
    # PASO 4: PUNTUAR LOS MATERIALES
    # Para cada material que aparece en las obras parecidas, calcula una
    # puntuación: cuanto más parecida es la obra y más cantidad se pidió,
    # mayor es la puntuación
    # =========================================================================

    @api.model
    def _calculate_material_scores(self, obras_similares, categoria=None):
        """
        Calcula la puntuación de cada material:
        Score = Σ (similitud de la obra × cantidad pedida en esa obra)

        Así, un material que aparece mucho en obras muy parecidas
        tendrá mayor puntuación.
        """
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

    # =========================================================================
    # REGLAS DE CONOCIMIENTO EXPERTO (para cuando no hay historial)
    # Cuando el sistema es nuevo y no hay datos de compras anteriores,
    # usa estas reglas predefinidas para hacer recomendaciones básicas.
    # =========================================================================

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
        """
        Añade un bonus a los materiales que coinciden con las reglas de experto.
        Esto ayuda cuando hay pocos datos (cold start).
        """
        keywords = self.DOMAIN_RULES.get(tipo_obra, {}).get(categoria, [])
        for keyword in keywords:
            for prod in self.env['product.product'].search([('name', 'ilike', keyword)], limit=3):
                scores[prod.id] = scores.get(prod.id, 0) + 0.5
        return scores

    # =========================================================================
    # FUNCIÓN PRINCIPAL: OBTENER RECOMENDACIONES
    # Une todos los pasos anteriores para generar una lista de materiales
    # recomendados con su puntuación y nivel de confianza
    # =========================================================================

    @api.model
    def get_recommendations(self, obra_id=None, tipo_obra='reforma', categoria=None, limit=10):
        """
        Genera recomendaciones de materiales para una obra.

        Pasos:
        1. Busca obras similares
        2. Puntúa los materiales de esas obras
        3. Aplica reglas de experto (bonus)
        4. Quita los materiales que ya se han pedido
        5. Devuelve los mejores con su nivel de confianza (alta/media/baja)

        Si no hay historial suficiente, devuelve los materiales más populares.
        """
        # Buscar obras parecidas y calcular puntuaciones
        obras_similares = self._find_similar_obras(obra_id, tipo_obra, top_k=5)
        scores = self._calculate_material_scores(obras_similares, categoria)
        scores = self._apply_domain_rules_bonus(scores, tipo_obra, categoria)

        # Si no hay puntuaciones, devolver los materiales más populares
        if not scores:
            return self._get_popular_materials(categoria, limit)

        # Quitar materiales que esta obra ya ha pedido (no recomendar lo que ya tienen)
        if obra_id:
            ya_usados = set(
                self.env['construction.material.request.line'].search([
                    ('request_id.obra_id', '=', obra_id)
                ]).mapped('product_id.id')
            )
            scores = {pid: s for pid, s in scores.items() if pid not in ya_usados}

        # Coger los mejores y calcular el nivel de confianza
        top = sorted(scores.items(), key=lambda x: x[1], reverse=True)[:limit]
        score_max = top[0][1] if top else 1.0

        result = []
        for product_id, score in top:
            prod = self.env['product.product'].browse(product_id)
            if not prod.exists():
                continue
            # Confianza: proporción respecto al material con mayor puntuación
            confianza = min(score / score_max, 1.0) if score_max > 0 else 0.0
            result.append({
                'product_id': product_id,
                'nombre': prod.name,
                'referencia': prod.default_code or '',
                'categoria': categoria or 'varios',
                'precio_estimado': prod.lst_price,
                'score': round(score, 4),
                'confianza': round(confianza, 2),
                # Alta si >= 80%, media si >= 50%, baja si < 50%
                'nivel_confianza': 'alta' if confianza >= 0.8 else ('media' if confianza >= 0.5 else 'baja'),
                'imagen_url': '/web/image/product.product/%d/image_128' % product_id,
            })
        return result

    # =========================================================================
    # PLAN B: MATERIALES MÁS POPULARES
    # Cuando no hay datos suficientes para hacer recomendaciones personalizadas,
    # simplemente devuelve los materiales que más se han pedido en general
    # =========================================================================

    @api.model
    def _get_popular_materials(self, categoria=None, limit=10):
        """
        Devuelve los materiales más pedidos de todo el sistema.
        Se usa cuando no hay suficiente historial para personalizar.
        """
        domain = []
        if categoria:
            domain.append(('product_id.categ_id.name', 'ilike', categoria))
        # Agrupar por producto y sumar cantidades
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
                'confianza': 0.5,               # Confianza media (no es personalizado)
                'nivel_confianza': 'media',
                'imagen_url': '/web/image/product.product/%d/image_128' % pid,
            })
        return result
