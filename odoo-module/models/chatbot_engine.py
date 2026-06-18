# -*- coding: utf-8 -*-
# ============================================================================
# CHATBOT DE MATERIALES DE CONSTRUCCIÓN
# Este archivo implementa un asistente virtual que ayuda al cliente a saber
# qué materiales necesita para su proyecto de reforma o construcción.
#
# El cliente escribe algo como: "¿Qué necesito para renovar un baño?"
# Y el chatbot le responde con una lista de materiales recomendados.
#
# CÓMO FUNCIONA:
#   1. MOTOR DE REGLAS (siempre funciona, no necesita internet):
#      - Tiene una "base de conocimiento" con 8 tipos de trabajo
#        (baño, cocina, electricidad, pintura, suelo, tabique, fontanería, aislamiento)
#      - Cada tipo tiene una lista predefinida de materiales con cantidades
#      - Detecta palabras clave en el mensaje del cliente para saber qué busca
#
#   2. ENRIQUECIMIENTO CON IA (opcional, si hay clave de Groq configurada):
#      - Envía la pregunta a Groq (API gratuita de IA con el modelo Llama3)
#      - Complementa la respuesta con información más detallada
#      - Si Groq no está disponible, funciona igualmente con las reglas
# ============================================================================
import json
import logging
import re
import unicodedata
import urllib.request
import urllib.error
from odoo import models, api, fields

_logger = logging.getLogger(__name__)

# =============================================================================
# BASE DE CONOCIMIENTO
# Diccionario con 8 tipos de trabajo de construcción.
# Cada tipo tiene:
#   - categoría: la categoría general del trabajo
#   - descripción: una breve explicación
#   - materiales: lista de materiales con nombre y cantidad estimada
# =============================================================================

KNOWLEDGE_BASE = {
    # --- BAÑO: materiales típicos para reformar un baño completo ---
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
    # --- COCINA: materiales para reformar una cocina ---
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
    # --- ELECTRICIDAD: materiales para una instalación eléctrica ---
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
    # --- PINTURA: materiales para pintar una habitación o piso ---
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
    # --- SUELO: materiales para instalar un suelo ---
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
    # --- TABIQUE: materiales para levantar una pared ---
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
    # --- FONTANERÍA: materiales para instalación de tuberías ---
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
    # --- AISLAMIENTO: materiales para aislar del frío/calor/ruido ---
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

# =============================================================================
# SINÓNIMOS Y VARIANTES
# Para que el chatbot entienda distintas formas de decir lo mismo.
# Ejemplo: si el usuario dice "ducha", "aseo" o "wc", entiende que habla del baño.
# =============================================================================

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
    """
    Limpia el texto para poder comparar palabras sin problemas:
    - Lo pone todo en minúsculas
    - Quita las tildes (á → a, ñ → n, etc.)
    - Quita signos de puntuación
    """
    text = text.lower()
    # Quitar tildes
    text = ''.join(c for c in unicodedata.normalize('NFD', text) if unicodedata.category(c) != 'Mn')
    # Quitar puntuación
    text = re.sub(r'[^\w\s]', ' ', text)
    return text


class ConstructionChatbot(models.AbstractModel):
    """
    Chatbot de materiales de construcción.
    No crea tabla en la base de datos (AbstractModel), solo ofrece métodos.
    """
    _name = 'construction.chatbot'
    _description = 'Chatbot de Materiales de Construcción'

    # =========================================================================
    # DETECTAR TEMAS
    # Lee el mensaje del usuario y detecta de qué está hablando
    # (baño, cocina, electricidad, etc.)
    # =========================================================================

    @api.model
    def _detect_topics(self, message_normalized):
        """
        Busca palabras clave en el mensaje para saber de qué habla el usuario.
        Ejemplo: "quiero renovar el baño y pintar" → detecta ['baño', 'pintura']
        """
        topics = []
        for topic, synonyms in SYNONYMS.items():
            # Comprobar si la palabra clave directa está en el mensaje
            if topic in message_normalized:
                topics.append(topic)
                continue
            # Comprobar si algún sinónimo está en el mensaje
            for syn in synonyms:
                if syn in message_normalized:
                    topics.append(topic)
                    break
        return list(set(topics))

    # =========================================================================
    # CONSTRUIR RESPUESTA CON REGLAS
    # A partir de los temas detectados, busca los materiales correspondientes
    # en la base de conocimiento
    # =========================================================================

    @api.model
    def _build_rules_response(self, topics):
        """
        Genera la respuesta con los materiales recomendados según las reglas.
        Si no se detectó ningún tema, muestra un mensaje de ayuda.
        """
        if not topics:
            return {
                'tipo': 'desconocido',
                'mensaje': 'No he podido identificar el tipo de trabajo. '
                           'Prueba con palabras como: baño, cocina, electricidad, pintura, suelo, tabique, fontanería o aislamiento.',
                'materiales': [],
                'fuente': 'reglas',
            }

        # Juntar los materiales de todos los temas detectados
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

    # =========================================================================
    # ENRIQUECER CON INTELIGENCIA ARTIFICIAL (GROQ API)
    # Si hay una clave de API de Groq configurada en Odoo, envía la pregunta
    # al modelo de IA Llama3 para obtener una respuesta más detallada.
    # Groq es gratuito y rápido. Si no está configurado, no pasa nada:
    # el chatbot funciona igual con las reglas.
    # =========================================================================

    @api.model
    def _call_groq_api(self, message, rules_response):
        """
        Envía la pregunta del usuario a la IA de Groq para complementar
        la respuesta con información más específica.
        Solo se ejecuta si hay una clave de API configurada en Odoo.
        """
        # Buscar la clave de API en la configuración de Odoo
        api_key = self.env['ir.config_parameter'].sudo().get_param('construction.groq_api_key', '')
        if not api_key:
            return None  # No hay clave → no se usa IA

        # Instrucciones para la IA (cómo debe responder)
        system_prompt = (
            'Eres un asistente experto en materiales de construcción y reformas en España. '
            'Responde siempre en español de forma concisa (máx 150 palabras). '
            'Cuando des listas de materiales, sé específico con cantidades estimadas.'
        )

        # La pregunta que se envía a la IA, incluyendo los materiales ya detectados
        base_materials = ', '.join(m['nombre'] for m in rules_response.get('materiales', [])[:5])
        user_prompt = (
            f'Un cliente pregunta: "{message}"\n\n'
            f'Ya tenemos identificados estos materiales básicos: {base_materials}.\n'
            'Complementa con información adicional relevante o cantidades más precisas.'
        )

        # Preparar la petición HTTP a la API de Groq
        payload = json.dumps({
            'model': 'llama3-8b-8192',  # Modelo de IA gratuito de Groq
            'messages': [
                {'role': 'system', 'content': system_prompt},
                {'role': 'user', 'content': user_prompt},
            ],
            'max_tokens': 300,
            'temperature': 0.7,  # Un poco de variedad en las respuestas
        }).encode('utf-8')

        try:
            # Enviar la petición a Groq (tiempo límite: 8 segundos)
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
            # Si falla la IA, no pasa nada: se usa solo la respuesta de reglas
            _logger.warning('Groq API no disponible: %s', e)
            return None

    # =========================================================================
    # PROCESAR MENSAJE (función principal del chatbot)
    # Es el método que se llama desde el API cuando el usuario envía un mensaje.
    # Combina las reglas con la IA (si está disponible).
    # =========================================================================

    @api.model
    def process_message(self, message):
        """
        Procesa un mensaje del usuario y devuelve la respuesta del chatbot.

        Pasos:
        1. Normaliza el texto (quita tildes, minúsculas)
        2. Detecta los temas (baño, cocina, etc.)
        3. Genera respuesta con las reglas predefinidas
        4. Si Groq está configurado, enriquece con IA
        """
        # Limpiar el texto del usuario
        msg_norm = _normalize(message)
        # Detectar de qué habla
        topics = self._detect_topics(msg_norm)
        # Generar respuesta con reglas
        response = self._build_rules_response(topics)

        # Intentar mejorar con IA (no bloquea si falla)
        llm_text = self._call_groq_api(message, response)
        if llm_text:
            response['mensaje_llm'] = llm_text
            response['fuente'] = 'reglas+llm'  # Indicar que la respuesta viene de ambas fuentes

        return response
