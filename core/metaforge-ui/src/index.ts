import type { App } from 'vue'

export const version = '2026.0.0'

export function install(app: App): void {
  // Component library entry; Mf-prefixed components are registered here
  // once the Tailwind + token based implementation lands.
  void app
}

export default { install, version }
