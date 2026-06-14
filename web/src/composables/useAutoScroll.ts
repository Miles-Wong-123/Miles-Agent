import { type Ref, nextTick, onBeforeUnmount, ref, watch } from 'vue'

interface AutoScrollOptions {
  /** px tolerance around the bottom — within this, we still consider it "at bottom" */
  threshold?: number
}

/**
 * Sticks the scroll container to its bottom while content is appended.
 * If the user scrolls up manually, the auto-follow pauses until they scroll
 * back to the bottom (or click "scroll to bottom").
 *
 * Returns:
 *   - atBottom: ref<boolean>           — whether the viewport is currently within `threshold` of bottom
 *   - showJumpToBottom: ref<boolean>   — whether to show the jump-to-bottom button (auto-follow paused)
 *   - scrollToBottom(): void            — programmatically jump to bottom and resume follow
 *   - notify(): void                    — call after content appended; scrolls if follow is enabled
 */
export function useAutoScroll(target: Ref<HTMLElement | null>, options: AutoScrollOptions = {}) {
  const threshold = options.threshold ?? 32
  const atBottom = ref(true)
  const showJumpToBottom = ref(false)
  let follow = true

  function isNearBottom(el: HTMLElement) {
    return el.scrollHeight - el.scrollTop - el.clientHeight <= threshold
  }

  function onScroll() {
    const el = target.value
    if (!el) return
    const near = isNearBottom(el)
    atBottom.value = near
    if (!near) {
      follow = false
      showJumpToBottom.value = true
    } else {
      follow = true
      showJumpToBottom.value = false
    }
  }

  function scrollToBottom() {
    const el = target.value
    if (!el) return
    el.scrollTop = el.scrollHeight
    follow = true
    showJumpToBottom.value = false
    atBottom.value = true
  }

  async function notify() {
    if (!follow) return
    await nextTick()
    const el = target.value
    if (!el) return
    el.scrollTop = el.scrollHeight
  }

  watch(target, (el, _old, onCleanup) => {
    if (!el) return
    el.addEventListener('scroll', onScroll, { passive: true })
    onCleanup(() => el.removeEventListener('scroll', onScroll))
  })

  onBeforeUnmount(() => {
    const el = target.value
    if (el) el.removeEventListener('scroll', onScroll)
  })

  return { atBottom, showJumpToBottom, scrollToBottom, notify }
}
