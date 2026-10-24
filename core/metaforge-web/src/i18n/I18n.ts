import { createI18n } from 'vue-i18n'
import { inject, provide, ref } from 'vue'
import I18nService from '@/services/i18n'

// 默认语言配置
// Default locale
const DEFAULT_LOCALE = 'zh_cn'
const DEFAULT_MESSAGES = {
    zh_cn: {
        // 基础的回退翻译
        // Basic fallback translation
        common: {
            loading: 'Loading...',
            networkError: 'Network error',
            loadingFailed: 'Loading failed'
        }
    }
}

const language = (navigator.language || 'en').toLocaleLowerCase()

const i18n = createI18n({
    fallbackLocale: DEFAULT_LOCALE,
    globalInjection: true,
    legacy: false,
    silentFallbackWarn: true,
    fallbackWarn: false,
    silentTranslationWarn: true,
    missingWarn: false,
    locale: localStorage.getItem('locale') || language.split('-')[0] || DEFAULT_LOCALE,
    messages: DEFAULT_MESSAGES
})

export const I18N_KEY = Symbol('i18n-handler')

export const loadingState = ref(false)
export const loadingText = ref('')
export const errorState = ref(false)
export const errorMessage = ref('')

export function provideI18nHandler()
{
    const resetState = () => {
        loadingState.value = false
        loadingText.value = ''
        errorState.value = false
        errorMessage.value = ''
    }

    const loadLocale = async (locale: string) => {
        // 如果请求的语言包已经加载过，直接使用
        // If the requested language package has been loaded, use it directly
        // @ts-ignore
        if (i18n.global.availableLocales.includes(locale) &&
            Object.keys(i18n.global.getLocaleMessage(locale)).length > 1) {
            // @ts-ignore
            i18n.global.locale.value = locale
            localStorage.setItem('locale', locale)
            return true
        }

        resetState()
        loadingState.value = true
        loadingText.value = i18n.global.t('common.loading')

        try {
            const response = await I18nService.getLocale(locale)
            if (response.status && response.data && Object.keys(response.data).length > 0) {
                i18n.global.setLocaleMessage(locale, {
                    ...DEFAULT_MESSAGES[DEFAULT_LOCALE],
                    ...response.data
                })
                // @ts-ignore
                i18n.global.locale.value = locale
                localStorage.setItem('locale', locale)
                resetState()
                return true
            }
            else {
                throw new Error('Invalid locale data received')
            }
        }
        catch (error) {
            errorState.value = true
            errorMessage.value = error instanceof Error ? error.message : i18n.global.t('common.loadingFailed')

            // 如果加载失败，确保至少有基础翻译可用
            // Ensure that at least the basic translation is available
            // @ts-ignore
            if (!i18n.global.availableLocales.includes(locale)) {
                i18n.global.setLocaleMessage(locale, DEFAULT_MESSAGES[DEFAULT_LOCALE])
            }

            // 如果当前语言加载失败，尝试回退到默认语言
            // If the current language fails to load, try to fall back to the default language
            if (locale !== DEFAULT_LOCALE &&
                !i18n.global.availableLocales.includes(i18n.global.locale.value)) {
                i18n.global.locale.value = DEFAULT_LOCALE
                localStorage.setItem('locale', DEFAULT_LOCALE)
            }

            console.error('Failed to load locale:', error)
            return false
        }
        finally {
            loadingState.value = false
        }
    }

    const i18nHandler = {
        loadLocale,
        loadingState,
        loadingText,
        errorState,
        errorMessage,
        resetState
    }

    provide(I18N_KEY, i18nHandler)

    return i18nHandler
}

// Hook
export function useI18nHandler()
{
    const i18nHandler = inject(I18N_KEY)
    if (!i18nHandler) {
        throw new Error('useI18nHandler must be used after provideI18nHandler')
    }
    return i18nHandler
}

export default i18n
