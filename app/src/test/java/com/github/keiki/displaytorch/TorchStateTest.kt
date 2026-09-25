package com.github.keiki.displaytorch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TorchStateTest {

    private class MemoryStore(initial: Map<Int, Float> = emptyMap()) : BrightnessStore {
        private val values = initial.toMutableMap()
        var saved: List<Float>? = null

        override fun load(index: Int, default: Float): Float = values[index] ?: default

        override fun save(values: List<Float>) {
            saved = values.toList()
        }
    }

    private val delta = 1e-6f

    @Test
    fun `starts at the first step in white`() {
        val state = TorchState(MemoryStore())
        assertEquals(0, state.stepIndex)
        assertEquals(LightColor.WHITE, state.color)
        assertEquals(TorchState.DEFAULT_STEPS[0], state.currentBrightness, delta)
        assertEquals(TorchState.DEFAULT_STEPS.size, state.stepCount)
    }

    @Test
    fun `next step wraps around`() {
        val state = TorchState(MemoryStore())
        repeat(state.stepCount - 1) { state.nextStep() }
        assertEquals(state.stepCount - 1, state.stepIndex)
        state.nextStep()
        assertEquals(0, state.stepIndex)
    }

    @Test
    fun `previous step wraps around`() {
        val state = TorchState(MemoryStore())
        state.previousStep()
        assertEquals(state.stepCount - 1, state.stepIndex)
        state.previousStep()
        assertEquals(state.stepCount - 2, state.stepIndex)
    }

    @Test
    fun `toggle colour switches between white and red`() {
        val state = TorchState(MemoryStore())
        state.toggleColor()
        assertEquals(LightColor.RED, state.color)
        state.toggleColor()
        assertEquals(LightColor.WHITE, state.color)
    }

    @Test
    fun `adjust changes only the current step and saves`() {
        val store = MemoryStore()
        val state = TorchState(store)
        state.nextStep()
        state.adjustCurrentStep(TorchState.EDIT_STEP)
        assertEquals(TorchState.DEFAULT_STEPS[1] + TorchState.EDIT_STEP, state.currentBrightness, delta)
        val saved = store.saved!!
        assertEquals(TorchState.DEFAULT_STEPS[0], saved[0], delta)
        assertEquals(state.currentBrightness, saved[1], delta)
    }

    @Test
    fun `adjust clamps at the minimum`() {
        val state = TorchState(MemoryStore())
        state.adjustCurrentStep(-1f)
        assertEquals(TorchState.MIN_BRIGHTNESS, state.currentBrightness, delta)
    }

    @Test
    fun `adjust clamps at the maximum`() {
        val state = TorchState(MemoryStore())
        state.restore(state.stepCount - 1, LightColor.WHITE)
        state.adjustCurrentStep(0.5f)
        assertEquals(TorchState.MAX_BRIGHTNESS, state.currentBrightness, delta)
    }

    @Test
    fun `reset restores the defaults and saves`() {
        val store = MemoryStore(mapOf(0 to 0.5f, 3 to 0.9f))
        val state = TorchState(store)
        state.resetSteps()
        assertEquals(TorchState.DEFAULT_STEPS[0], state.currentBrightness, delta)
        assertEquals(TorchState.DEFAULT_STEPS, store.saved)
    }

    @Test
    fun `stored values override the defaults`() {
        val store = MemoryStore(mapOf(0 to 0.05f))
        val state = TorchState(store)
        assertEquals(0.05f, state.currentBrightness, delta)
        state.nextStep()
        assertEquals(TorchState.DEFAULT_STEPS[1], state.currentBrightness, delta)
        assertNull(store.saved)
    }

    @Test
    fun `restore clamps the index and keeps the colour`() {
        val state = TorchState(MemoryStore())
        state.restore(99, LightColor.RED)
        assertEquals(state.stepCount - 1, state.stepIndex)
        assertEquals(LightColor.RED, state.color)
        state.restore(-3, LightColor.WHITE)
        assertEquals(0, state.stepIndex)
    }
}
