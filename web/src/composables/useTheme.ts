import { useDark, useToggle } from '@vueuse/core'
import { watch } from 'vue'
import { useSettingsStore } from '@/stores/settings'

/**
 * Bridges VueUse's `useDark` with the persisted theme setting:
 *   - 'system' → follow OS preference
 *   - 'light' / 'dark' → force the chosen mode
 * Mutating the returned `isDark` ref also flips the setting away from 'system'.
 */
export function useTheme() {
  const settings = useSettingsStore()
  const isDark = useDark({
    selector: 'html',
    valueDark: 'dark',
    valueLight: '',
    initialValue: settings.theme === 'system' ? 'auto' : settings.theme,
  })
  const toggleDark = useToggle(isDark)

  watch(
    () => settings.theme,
    (mode) => {
      if (mode === 'dark') isDark.value = true
      else if (mode === 'light') isDark.value = false
      else {
        const prefersDark =
          typeof window !== 'undefined' &&
          window.matchMedia('(prefers-color-scheme: dark)').matches
        isDark.value = prefersDark
      }
    },
    { immediate: true },
  )

  return { isDark, toggleDark, theme: settings }
}
