// Vitest setup: global helpers, polyfills, and matchers can be wired here.

import { afterEach, vi } from 'vitest'

afterEach(() => {
  vi.restoreAllMocks()
})
