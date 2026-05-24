package com.construccion.marketplace.data.api

import java.text.Normalizer

/**
 * Mapeo de nombre de producto → URL de imagen estática en el servidor Odoo.
 * Usa la misma lógica de keywords que IMG_KW_GLOBAL en el portal web.
 */
object ProductImageHelper {

    // Imágenes embebidas en los assets de la app (siempre disponibles, incluso sin conexión)
    private const val BASE_IMG = "file:///android_asset/img"

    // Orden importante: más específico primero (igual que en el portal)
    private val KW_MAP = listOf(
        listOf("varilla", "ferralla", "hierro", "acero", "barra corrugada") to "varillas.png",
        listOf("corrugado") to "tubo_corrugado.png",
        listOf("tubopvc", "tubo pvc") to "tubopvc.png",
        listOf("pvc") to "tubopvc.png",
        listOf("grifo", "fontaner", "sanitari") to "grifo.png",
        listOf("cable", "conductor", "lszh") to "cable.png",
        listOf("cuadro", "electric", "interruptor") to "cuadro.png",
        listOf("silicona", "sellante", "masilla") to "silicona.png",
        listOf("imprimacion", "sellador") to "imprimacion.png",
        listOf("pintura", "barniz", "esmalte") to "botepintura.png",
        listOf("lechada", "rejuntado", "junta") to "lechada.png",
        listOf("azulejo", "ceramica", "porcelan", "baldosa", "gres", "revestimiento") to "azulejo.png",
        listOf("madera", "tablero", "tarima", "parquet") to "madera.png",
        listOf("mortero") to "mortero.png",
        listOf("arena", "gravilla", "arido", "zahorra") to "arena.png",
        listOf("ladrillo", "bloque", "tocho") to "ladrillo.png",
        listOf("cemento", "yeso", "hormigon", "escayola") to "cemento.png",
        listOf("taladro", "herramienta", "sierra", "broca") to "taladro.png",
        listOf("aislamiento", "lana", "espuma", "poliestireno") to "lana.png",
        listOf("tubo") to "grifo.png",   // tubo genérico al final
    )

    /**
     * Devuelve la URL de imagen para un producto según su nombre.
     * @return URL completa de la imagen, o null si no hay coincidencia.
     */
    fun getImageUrl(productName: String): String? {
        val txt = Normalizer
            .normalize(productName.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("[\\u0300-\\u036f]"), "")

        val filename = KW_MAP.firstOrNull { (kws, _) ->
            kws.any { kw -> txt.contains(kw) }
        }?.second

        return filename?.let { "$BASE_IMG/$it" }
    }
}
