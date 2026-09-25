package com.github.keiki.displaytorch

enum class LightColor { WHITE, RED }

/** Persists the fine-tuned brightness of each step. */
interface BrightnessStore {
    fun load(index: Int, default: Float): Float
    fun save(values: List<Float>)
}

/**
 * The torch's state: five brightness steps, the selected step and the light
 * colour. Pure Kotlin so it can be unit-tested; [MainActivity] applies it to
 * the window and the views.
 */
class TorchState(private val store: BrightnessStore) {

    companion object {
        val DEFAULT_STEPS = listOf(0.02f, 0.1f, 0.3f, 0.5f, 1.0f)

        /** Change per volume-key press in edit mode. */
        const val EDIT_STEP = 0.01f
        const val MIN_BRIGHTNESS = 0.01f
        const val MAX_BRIGHTNESS = 1.0f
    }

    private val steps = DEFAULT_STEPS
        .mapIndexed { index, default -> store.load(index, default) }
        .toMutableList()

    val stepCount: Int get() = steps.size

    var stepIndex: Int = 0
        private set

    var color: LightColor = LightColor.WHITE
        private set

    val currentBrightness: Float get() = steps[stepIndex]

    /** Reapplies state saved across a configuration change. */
    fun restore(stepIndex: Int, color: LightColor) {
        this.stepIndex = stepIndex.coerceIn(0, steps.lastIndex)
        this.color = color
    }

    fun nextStep() {
        stepIndex = (stepIndex + 1) % steps.size
    }

    fun previousStep() {
        stepIndex = (stepIndex - 1 + steps.size) % steps.size
    }

    fun toggleColor() {
        color = if (color == LightColor.WHITE) LightColor.RED else LightColor.WHITE
    }

    fun adjustCurrentStep(delta: Float) {
        steps[stepIndex] = (steps[stepIndex] + delta).coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        store.save(steps)
    }

    fun resetSteps() {
        DEFAULT_STEPS.forEachIndexed { index, value -> steps[index] = value }
        store.save(steps)
    }
}
