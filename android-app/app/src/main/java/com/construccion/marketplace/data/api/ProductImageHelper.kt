package com.construccion.marketplace.data.api

import java.text.Normalizer

/**
 * Helper que asocia cada producto a una imagen local según su nombre.
 *
 * La app incluye imágenes estáticas en la carpeta assets/img/ para que los
 * productos tengan imagen incluso sin conexión a internet. Este objeto busca
 * palabras clave en el nombre del producto y devuelve la URL del asset
 * correspondiente.
 *
 * Replica la misma lógica de keywords (IMG_KW_GLOBAL) que usa el portal web
 * de Odoo, para mantener consistencia visual entre web y app móvil.
 */
object ProductImageHelper {

    // Ruta base de los assets de imágenes empaquetados en el APK
    private const val BASE_IMG = "file:///android_asset/img"

    /**
     * Mapa de palabras clave → nombre de fichero de imagen.
     *
     * Cada entrada es: lista de keywords que pueden aparecer en el nombre del producto
     * → fichero .png dentro de assets/img/.
     *
     * IMPORTANTE: el orden importa. Las entradas más específicas van primero para
     * evitar falsos positivos. Por ejemplo "corrugado" (tubo corrugado eléctrico)
     * va antes que "tubo" (genérico), y "tubopvc" va antes de "pvc".
     */
    private val KW_MAP = listOf(
        // Hierro / acero - varillas corrugadas de construcción
        listOf("varilla", "ferralla", "hierro", "acero", "barra corrugada") to "varillas.png",
        // Tubo corrugado eléctrico (no confundir con PVC de fontanería)
        listOf("corrugado") to "tubo_corrugado.png",
        // Tubería PVC para evacuación / saneamiento
        listOf("tubopvc", "tubo pvc") to "tubopvc.png",
        listOf("pvc") to "tubopvc.png",
        // Fontanería y sanitarios
        listOf("grifo", "fontaner", "sanitari") to "grifo.png",
        // Material eléctrico - cables
        listOf("cable", "conductor", "lszh") to "cable.png",
        // Material eléctrico - cuadros e interruptores
        listOf("cuadro", "electric", "interruptor") to "cuadro.png",
        // Sellantes y siliconas
        listOf("silicona", "sellante", "masilla") to "silicona.png",
        // Imprimaciones y selladores de superficie
        listOf("imprimacion", "sellador") to "imprimacion.png",
        // Pinturas, barnices y esmaltes
        listOf("pintura", "barniz", "esmalte") to "botepintura.png",
        // Lechadas y rejuntados para azulejos
        listOf("lechada", "rejuntado", "junta") to "lechada.png",
        // Cerámica, azulejos y revestimientos
        listOf("azulejo", "ceramica", "porcelan", "baldosa", "gres", "revestimiento") to "azulejo.png",
        // Madera y derivados
        listOf("madera", "tablero", "tarima", "parquet") to "madera.png",
        // Mortero de albañilería
        listOf("mortero") to "mortero.png",
        // Áridos (arena, gravilla, etc.)
        listOf("arena", "gravilla", "arido", "zahorra") to "arena.png",
        // Ladrillos y bloques
        listOf("ladrillo", "bloque", "tocho") to "ladrillo.png",
        // Cementos, yesos y hormigones
        listOf("cemento", "yeso", "hormigon", "escayola") to "cemento.png",
        // Herramientas manuales y eléctricas
        listOf("taladro", "herramienta", "sierra", "broca") to "taladro.png",
        // Aislamientos térmicos y acústicos
        listOf("aislamiento", "lana", "espuma", "poliestireno") to "lana.png",
        // "tubo" genérico al final como fallback (podría ser de fontanería)
        listOf("tubo") to "grifo.png",
    )

    /**
     * Busca en el nombre del producto una coincidencia con las keywords
     * y devuelve la URL del asset de imagen correspondiente.
     *
     * El nombre se normaliza (minúsculas + eliminar acentos) antes de buscar
     * para que "Cemento Pórtland" coincida con la keyword "cemento".
     *
     * @param productName nombre del producto tal como viene de Odoo
     * @return URL file:///android_asset/img/xxx.png, o null si no hay coincidencia
     */
    fun getImageUrl(productName: String): String? {
        // Normalizar: minúsculas + quitar acentos/diacríticos (NFD descompone, regex elimina marcas)
        val txt = Normalizer
            .normalize(productName.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("[\\u0300-\\u036f]"), "")

        // Buscar la primera entrada cuya lista de keywords tenga alguna coincidencia
        val filename = KW_MAP.firstOrNull { (kws, _) ->
            kws.any { kw -> txt.contains(kw) }
        }?.second

        // Devolver la URL completa del asset o null
        return filename?.let { "$BASE_IMG/$it" }
    }
}
