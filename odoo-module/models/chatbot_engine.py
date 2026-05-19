# -*- coding: utf-8 -*-
"""
Chatbot de Materiales de Construcción - TFG
============================================
Arquitectura híbrida:
  1. Motor de reglas + keywords: siempre disponible, sin dependencias externas
  2. Groq API (gratuita): enriquece la respuesta si hay API key configurada

Endpoint: POST /api/construction/chatbot  {message: "¿Qué necesito para renovar un baño?"}
"""
import json
import logging
import re
import unicodedata
import urllib.request
import urllib.error
from odoo import models, api, fields

_logger = logging.getLogger(__name__)

# Base de conocimiento: keyword → {categoria, descripcion, materiales}
KNOWLEDGE_BASE = {
    'baño': {
        'categoria': 'fontaneria',
        'descripcion': 'Renovación completa de baño',
        'materiales': [
            {'nombre': 'Azulejos para baño', 'cantidad': '15 m²'},
            {'nombre': 'Inodoro con cisterna', 'cantidad': '1 ud'},
            {'nombre': 'Lavabo con pedestal', 'cantidad': '1 ud'},
            {'nombre': 'Mampara de ducha', 'cantidad': '1 ud'},
            {'nombre': 'Grifo monomando lavabo', 'cantidad': '1 ud'},
            {'nombre': 'Grifo monomando ducha', 'cantidad': '1 ud'},
            {'nombre': 'Tubo PVC 50mm desagüe', 'cantidad': '3 m'},
            {'nombre': 'Mortero cola para azulejos', 'cantidad': '2 sacos'},
            {'nombre': 'Impermeabilizante baño', 'cantidad': '5 L'},
        ],
    },
    'cocina': {
        'categoria': 'fontaneria',
        'descripcion': 'Reforma de cocina',
        'materiales': [
            {'nombre': 'Alicatado cocina', 'cantidad': '10 m²'},
            {'nombre': 'Fregadero acero inoxidable', 'cantidad': '1 ud'},
            {'nombre': 'Grifo monomando cocina', 'cantidad': '1 ud'},
            {'nombre': 'Válvula escuadra 1/2"', 'cantidad': '2 ud'},
            {'nombre': 'Silicona neutra transparente', 'cantidad': '2 cartuchos'},
            {'nombre': 'Encimera compacto', 'cantidad': '3 m²'},
        ],
    },
    'electricidad': {
        'categoria': 'electricidad',
        'descripcion': 'Instalación eléctrica',
        'materiales': [
            {'nombre': 'Cable flexible H07V-K 1.5mm²', 'cantidad': '50 m'},
            {'nombre': 'Cable flexible H07V-K 2.5mm²', 'cantidad': '30 m'},
            {'nombre': 'Tubo corrugado PVC Ø20mm', 'cantidad': '30 m'},
            {'nombre': 'Caja empotrar rectangular', 'cantidad': '10 ud'},
            {'nombre': 'Interruptor simple', 'cantidad': '5 ud'},
            {'nombre': 'Enchufe schuko', 'cantidad': '8 ud'},
            {'nombre': 'Cuadro eléctrico ICP+IGA', 'cantidad': '1 ud'},
            {'nombre': 'Diferencial 40A 30mA', 'cantidad': '1 ud'},
        ],
    },
    'pintura': {
        'categoria': 'pintura',
        'descripcion': 'Pintado de habitación o piso',
        'materiales': [
            {'nombre': 'Pintura plástica mate blanca', 'cantidad': '15 L'},
            {'nombre': 'Imprimación selladora', 'cantidad': '5 L'},
            {'nombre': 'Rodillo de pintura 23cm', 'cantidad': '2 ud'},
            {'nombre': 'Brocha de corte 5cm', 'cantidad': '2 ud'},
            {'nombre': 'Cinta de carrocero', 'cantidad': '3 rollos'},
            {'nombre': 'Plástico protector suelo', 'cantidad': '20 m²'},
        ],
    },
    'suelo': {
        'categoria': 'pavimentos',
        'descripcion': 'Instalación de suelo',
        'materiales': [
            {'nombre': 'Tarima flotante o parquet', 'cantidad': '20 m²'},
            {'nombre': 'Espuma base tarima', 'cantidad': '20 m²'},
            {'nombre': 'Rodapié de madera o PVC', 'cantidad': '20 m'},
            {'nombre': 'Cola para parquet', 'cantidad': '2 kg'},
            {'nombre': 'Separadores para tarima', 'cantidad': '1 bolsa'},
        ],
    },
    'tabique': {
        'categoria': 'albanileria',
        'descripcion': 'Construcción de tabique de pladur o ladrillo',
        'materiales': [
            {'nombre': 'Ladrillo hueco doble', 'cantidad': '100 ud'},
            {'nombre': 'Cemento cola', 'cantidad': '5 sacos'},
            {'nombre': 'Arena fina', 'cantidad': '200 kg'},
            {'nombre': 'Placa de yeso laminado', 'cantidad': '10 m²'},
            {'nombre': 'Perfil montante 48mm', 'cantidad': '10 ud'},
            {'nombre': 'Tornillos autoperforantes', 'cantidad': '100 ud'},
        ],
    },
    'fontaneria': {
        'categoria': 'fontaneria',
        'descripcion': 'Instalación de fontanería general',
        'materiales': [
            {'nombre': 'Tubo PVC presión Ø25mm', 'cantidad': '10 m'},
            {'nombre': 'Codo PVC 90° Ø25mm', 'cantidad': '5 ud'},
            {'nombre': 'Llave de paso 1/2"', 'cantidad': '3 ud'},
            {'nombre': 'Teflon 12mm', 'cantidad': '5 rollos'},
            {'nombre': 'Soldadura fontanería', 'cantidad': '1 bote'},
        ],
    },
    'aislamiento': {
        'categoria': 'aislamiento',
        'descripcion': 'Aislamiento térmico o acústico',
        'materiales': [
            {'nombre': 'Lana de roca 50mm', 'cantidad': '20 m²'},
            {'nombre': 'Poliestireno expandido 3cm', 'cantidad': '20 m²'},
            {'nombre': 'Membrana impermeabilizante', 'cantidad': '10 m²'},
            {'nombre': 'Cinta butilada sellado', 'cantidad': '2 rollos'},
        ],
    },
}

# Sinónimos y variantes de palabras clave
SYNONYMS = {
    'baño': ['bano', 'lavabo', 'wc', 'aseo', 'ducha', 'bañera', 'banera'],
    'cocina': ['fregadero', 'encimera', 'alicatado'],
    'electricidad': ['electrico', 'luz', 'enchufe', 'cable', 'interruptor', 'electricidad'],
    'pintura': ['pintar', 'pincel', 'rodillo', 'imprimacion'],
    'suelo': ['suelos', 'parquet', 'tarima', 'pavimento', 'baldosa', 'ceramica'],
    'tabique': ['pared', 'pladur', 'ladrillo', 'muro', 'mamposteria'],
    'fontaneria': ['agua', 'fontanero', 'tubo', 'grifo', 'valvula', 'desague'],
    'aislamiento': ['aislar', 'frio', 'calor', 'ruido', 'lana roca', 'poliestireno'],
}


def _normalize(text):
    """Normaliza texto: minúsculas, sin tildes, sin puntuación."""
    text = text.lower()
    text = ''.join(c for c in unicodedata.normalize('NFD', text) if unicodedata.category(c) != 'Mn')
    text = re.sub(r'[^\w\s]', ' ', text)
    return text


class ConstructionChatbot(models.AbstractModel):
    _name = 'construction.chatbot'
    _description = 'Chatbot de Materiales de Construcción'

    @api.model
    def _detect_topics(self, message_normalized):
        """Detecta los temas mencionados en el mensaje."""
        topics = []
        for topic, synonyms in SYNONYMS.items():
            if topic in message_normalized:
                topics.append(topic)
                continue
            for syn in synonyms:
                if syn in message_normalized:
                    topics.append(topic)
                    break
        return list(set(topics))

    @api.model
    def _build_rules_response(self, topics):
        """Construye respuesta basada en reglas con los materiales detectados."""
        if not topics:
            return {
                'tipo': 'desconocido',
                'mensaje': 'No he podido identificar el tipo de trabajo. '
                           'Prueba con palabras como: baño, cocina, electricidad, pintura, suelo, tabique, fontanería o aislamiento.',
                'materiales': [],
                'fuente': 'reglas',
            }

        all_materials = []
        descriptions = []
        for topic in topics:
            kb = KNOWLEDGE_BASE.get(topic, {})
            if kb:
                descriptions.append(kb.get('descripcion', topic))
                all_materials.extend(kb.get('materiales', []))

        mensaje = 'Para %s te recomiendo los siguientes materiales:' % ' y '.join(descriptions)
        return {
            'tipo': ', '.join(topics),
            'mensaje': mensaje,
            'materiales': all_materials,
            'fuente': 'reglas',
            'total_materiales': len(all_materials),
        }

    @api.model
    def _call_groq_api(self, message, rules_response):
        """Enriquece la respuesta con Groq LLM si hay API key configurada (gratuito)."""
        api_key = self.env['ir.config_parameter'].sudo().get_param('construction.groq_api_key', '')
        if not api_key:
            return None

        system_prompt = (
            'Eres un asistente experto en materiales de construcción y reformas en España. '
            'Responde siempre en español de forma concisa (máx 150 palabras). '
            'Cuando des listas de materiales, sé específico con cantidades estimadas.'
        )

        base_materials = ', '.join(m['nombre'] for m in rules_response.get('materiales', [])[:5])
        user_prompt = (
            f'Un cliente pregunta: "{message}"\n\n'
            f'Ya tenemos identificados estos materiales básicos: {base_materials}.\n'
            'Complementa con información adicional relevante o cantidades más precisas.'
        )

        payload = json.dumps({
            'model': 'llama3-8b-8192',
            'messages': [
                {'role': 'system', 'content': system_prompt},
                {'role': 'user', 'content': user_prompt},
            ],
            'max_tokens': 300,
            'temperature': 0.7,
        }).encode('utf-8')

        try:
            req = urllib.request.Request(
                'https://api.groq.com/openai/v1/chat/completions',
                data=payload,
                headers={
                    'Authorization': 'Bearer %s' % api_key,
                    'Content-Type': 'application/json',
                },
                method='POST',
            )
            with urllib.request.urlopen(req, timeout=8) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                return data['choices'][0]['message']['content']
        except Exception as e:
            _logger.warning('Groq API no disponible: %s', e)
            return None

    @api.model
    def process_message(self, message):
        """Procesa un mensaje del usuario y retorna respuesta con materiales."""
        msg_norm = _normalize(message)
        topics = self._detect_topics(msg_norm)
        response = self._build_rules_response(topics)

        # Intentar enriquecer con LLM (no bloquea si falla)
        llm_text = self._call_groq_api(message, response)
        if llm_text:
            response['mensaje_llm'] = llm_text
            response['fuente'] = 'reglas+llm'

        return response
