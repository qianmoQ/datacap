/**
 * Runtime theming API.
 *
 * A theme is a flat map of token name -> CSS value. `applyMfTheme` writes
 * each entry as a `--mf-*` custom property, which is exactly what the
 * components and the Tailwind `@theme inline` layer consume, so a theme
 * switch never re-renders component CSS.
 */

export type MfThemeMode = 'light' | 'dark'

export type MfThemeTokenName =
    | 'color-primary'
    | 'color-primary-hover'
    | 'color-primary-active'
    | 'color-on-primary'
    | 'color-success'
    | 'color-success-subtle'
    | 'color-warning'
    | 'color-warning-subtle'
    | 'color-danger'
    | 'color-danger-subtle'
    | 'color-info'
    | 'color-info-subtle'
    | 'color-page'
    | 'color-surface'
    | 'color-surface-hover'
    | 'color-surface-active'
    | 'color-content'
    | 'color-content-secondary'
    | 'color-content-tertiary'
    | 'color-content-disabled'
    | 'color-content-inverse'
    | 'color-line'
    | 'color-line-strong'
    | 'color-overlay'
    | 'radius-sm'
    | 'radius-md'
    | 'radius-lg'
    | 'radius-full'
    | 'shadow-sm'
    | 'shadow-md'
    | 'shadow-lg'
    | 'duration-fast'
    | 'duration-base'
    | 'duration-slow'
    | 'easing'
    | 'font-family'
    | 'font-size-sm'
    | 'font-size-md'
    | 'font-size-lg'

export type MfThemeTokens = Partial<Record<MfThemeTokenName, string>>

export const mfLightPreset: MfThemeTokens = {
    'color-primary': '#2563eb',
    'color-primary-hover': '#3b82f6',
    'color-primary-active': '#1d4ed8',
    'color-on-primary': '#ffffff',
    'color-success': '#16a34a',
    'color-success-subtle': '#dcfce7',
    'color-warning': '#d97706',
    'color-warning-subtle': '#fef3c7',
    'color-danger': '#dc2626',
    'color-danger-subtle': '#fee2e2',
    'color-info': '#0284c7',
    'color-info-subtle': '#e0f2fe',
    'color-page': '#f8fafc',
    'color-surface': '#ffffff',
    'color-surface-hover': '#f1f5f9',
    'color-surface-active': '#e2e8f0',
    'color-content': '#0f172a',
    'color-content-secondary': '#475569',
    'color-content-tertiary': '#94a3b8',
    'color-content-disabled': '#cbd5e1',
    'color-content-inverse': '#ffffff',
    'color-line': '#e2e8f0',
    'color-line-strong': '#cbd5e1',
    'color-overlay': 'rgb(15 23 42 / 0.45)',
    'radius-sm': '4px',
    'radius-md': '6px',
    'radius-lg': '8px',
    'radius-full': '9999px',
    'shadow-sm': '0 1px 2px 0 rgb(15 23 42 / 0.05)',
    'shadow-md': '0 4px 6px -1px rgb(15 23 42 / 0.08), 0 2px 4px -2px rgb(15 23 42 / 0.06)',
    'shadow-lg': '0 10px 15px -3px rgb(15 23 42 / 0.1), 0 4px 6px -4px rgb(15 23 42 / 0.06)',
    'duration-fast': '120ms',
    'duration-base': '180ms',
    'duration-slow': '280ms',
    'easing': 'cubic-bezier(0.4, 0, 0.2, 1)',
    'font-family': "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif",
    'font-size-sm': '12px',
    'font-size-md': '14px',
    'font-size-lg': '16px',
}

export const mfDarkPreset: MfThemeTokens = {
    'color-primary': '#3b82f6',
    'color-primary-hover': '#60a5fa',
    'color-primary-active': '#2563eb',
    'color-on-primary': '#ffffff',
    'color-success': '#4ade80',
    'color-success-subtle': 'rgb(74 222 128 / 0.14)',
    'color-warning': '#fbbf24',
    'color-warning-subtle': 'rgb(251 191 36 / 0.14)',
    'color-danger': '#f87171',
    'color-danger-subtle': 'rgb(248 113 113 / 0.14)',
    'color-info': '#38bdf8',
    'color-info-subtle': 'rgb(56 189 248 / 0.14)',
    'color-page': '#0b1120',
    'color-surface': '#111a2e',
    'color-surface-hover': '#1c2740',
    'color-surface-active': '#24304d',
    'color-content': '#f1f5f9',
    'color-content-secondary': '#94a3b8',
    'color-content-tertiary': '#64748b',
    'color-content-disabled': '#475569',
    'color-content-inverse': '#0f172a',
    'color-line': '#1e293b',
    'color-line-strong': '#334155',
    'color-overlay': 'rgb(2 6 23 / 0.6)',
    'shadow-sm': '0 1px 2px 0 rgb(0 0 0 / 0.4)',
    'shadow-md': '0 4px 6px -1px rgb(0 0 0 / 0.45), 0 2px 4px -2px rgb(0 0 0 / 0.35)',
    'shadow-lg': '0 10px 15px -3px rgb(0 0 0 / 0.5), 0 4px 6px -4px rgb(0 0 0 / 0.4)',
}

/** Build a theme from the light preset plus user overrides. */
export function createMfTheme(overrides: MfThemeTokens = {}): MfThemeTokens {
    return { ...mfLightPreset, ...overrides }
}

export interface MfApplyThemeOptions {
    /** Also set the `data-theme` attribute (drives `[data-theme='dark']`). */
    mode?: MfThemeMode
    /** Element receiving the CSS variables. Defaults to `document.documentElement`. */
    target?: HTMLElement
}

/** Write theme tokens as `--mf-*` custom properties on the target element. */
export function applyMfTheme(tokens: MfThemeTokens, options: MfApplyThemeOptions = {}): void {
    const target = options.target ?? document.documentElement
    for (const [name, value] of Object.entries(tokens)) {
        if (value !== undefined) {
            target.style.setProperty(`--mf-${name}`, value)
        }
    }
    if (options.mode) {
        setMfThemeMode(options.mode, target)
    }
}

/** Switch light/dark by toggling the `data-theme` attribute. */
export function setMfThemeMode(mode: MfThemeMode, target: HTMLElement = document.documentElement): void {
    target.dataset.theme = mode
}
