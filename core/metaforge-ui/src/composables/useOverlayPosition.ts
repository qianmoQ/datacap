import { onBeforeUnmount, ref, watch, type Ref } from 'vue'

export type OverlayPlacement = 'bottomLeft' | 'bottom' | 'bottomRight' | 'topLeft' | 'top' | 'topRight'

interface OverlayPositionOptions {
    /** Trigger element the overlay is anchored to. */
    root: Ref<HTMLElement | undefined>
    open: Readonly<Ref<boolean>>
    placement: () => OverlayPlacement
    /** Distance between trigger and overlay in pixels. */
    gap?: number
}

/**
 * Pins a teleported overlay to the trigger's viewport rect with
 * `position: fixed`, so popups escape `overflow: hidden/auto` ancestors
 * (tables, cards) instead of being clipped or forcing scrollbars.
 */
export function useOverlayPosition(options: OverlayPositionOptions): Ref<Record<string, string>> {
    const style = ref<Record<string, string>>({})

    function update(): void {
        const trigger = options.root.value
        if (!trigger) return
        const rect = trigger.getBoundingClientRect()
        const gap = options.gap ?? 6
        const center = rect.left + rect.width / 2
        const topEdge = window.innerHeight - rect.top + gap
        style.value = {
            bottomLeft: { top: `${rect.bottom + gap}px`, left: `${rect.left}px` },
            bottom: { top: `${rect.bottom + gap}px`, left: `${center}px`, transform: 'translateX(-50%)' },
            bottomRight: { top: `${rect.bottom + gap}px`, left: `${rect.right}px`, transform: 'translateX(-100%)' },
            topLeft: { bottom: `${topEdge}px`, left: `${rect.left}px` },
            top: { bottom: `${topEdge}px`, left: `${center}px`, transform: 'translateX(-50%)' },
            topRight: { bottom: `${topEdge}px`, left: `${rect.right}px`, transform: 'translateX(-100%)' },
        }[options.placement()]
    }

    function start(): void {
        update()
        window.addEventListener('resize', update)
        // Capture phase catches scrolls from any ancestor container while open.
        document.addEventListener('scroll', update, true)
    }

    function stop(): void {
        window.removeEventListener('resize', update)
        document.removeEventListener('scroll', update, true)
    }

    watch(options.open, value => (value ? start() : stop()))
    onBeforeUnmount(stop)

    return style
}
