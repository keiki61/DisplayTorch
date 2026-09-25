package com.github.keiki.displaytorch

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Keeps each step under `brightness_<index>`, the key layout existing
 * installs already use.
 */
class PreferencesBrightnessStore(private val prefs: SharedPreferences) : BrightnessStore {

    override fun load(index: Int, default: Float): Float = prefs.getFloat(key(index), default)

    override fun save(values: List<Float>) {
        prefs.edit { values.forEachIndexed { index, value -> putFloat(key(index), value) } }
    }

    private fun key(index: Int) = "brightness_$index"
}
