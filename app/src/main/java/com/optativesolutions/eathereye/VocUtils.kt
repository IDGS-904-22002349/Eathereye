package com.optativesolutions.eathereye

object VocUtils {
    // Esta lista es la "única fuente de verdad" para los nombres y claves de los VOCs.
    // Es la misma que está en tu HomeViewModel.
    private val vocs = listOf(
        "benzene" to "Acetona",
        "toluene" to "Alcohol Isopropílico"
    )

    /**
     * Devuelve el nombre amigable de un VOC a partir de su clave.
     * @param key La clave del sensor (ej. "benzene").
     * @return El nombre amigable (ej. "Acetona") o la misma clave si no se encuentra.
     */
    fun getVocNameByKey(key: String): String {
        return vocs.find { it.first == key }?.second ?: key.capitalize()
    }
}