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
        },
        setup: {
            title: '安装 MetaForge',
            subtitle: '首次启动引导，配置管理员账号后即可使用',
            installedTip: '该实例已完成安装，即将前往登录页',
            steps: {
                environment: '环境检查',
                account: '管理员账号',
                finish: '完成安装'
            },
            env: {
                database: '数据库',
                product: '数据库类型',
                version: '数据库版本',
                connected: '已连接',
                disconnected: '连接失败',
                test: '测试连接',
                testing: '测试中...',
                elapsed: '耗时',
                testFailed: '连接测试失败'
            },
            account: {
                username: '用户名',
                password: '密码',
                confirmPassword: '确认密码',
                name: '昵称（可选）',
                usernameTip: '请输入用户名',
                usernameSizeTip: '用户名长度须为 3-20 个字符',
                passwordTip: '请输入密码',
                passwordSizeTip: '密码长度须为 6-20 个字符',
                passwordNotMatchTip: '两次输入的密码不一致'
            },
            install: {
                running: '正在初始化数据库并创建管理员账号...',
                success: '安装完成',
                successTip: '管理员账号已创建，请使用该账号登录系统',
                failed: '安装失败',
                goToSignin: '前往登录',
                retry: '重试'
            },
            next: '下一步',
            back: '上一步'
        }
    },
    en: {
        common: {
            loading: 'Loading...',
            networkError: 'Network error',
            loadingFailed: 'Loading failed'
        },
        setup: {
            title: 'Install MetaForge',
            subtitle: 'First-boot wizard, set up the administrator to continue',
            installedTip: 'This instance is already installed, redirecting to sign in',
            steps: {
                environment: 'Environment',
                account: 'Administrator',
                finish: 'Finish'
            },
            env: {
                database: 'Database',
                product: 'Database type',
                version: 'Database version',
                connected: 'Connected',
                disconnected: 'Connection failed',
                test: 'Test connection',
                testing: 'Testing...',
                elapsed: 'Elapsed',
                testFailed: 'Connection test failed'
            },
            account: {
                username: 'Username',
                password: 'Password',
                confirmPassword: 'Confirm password',
                name: 'Nickname (optional)',
                usernameTip: 'Please input the username',
                usernameSizeTip: 'Username must be 3-20 characters',
                passwordTip: 'Please input the password',
                passwordSizeTip: 'Password must be 6-20 characters',
                passwordNotMatchTip: 'Passwords do not match'
            },
            install: {
                running: 'Initializing database and creating administrator...',
                success: 'Installation finished',
                successTip: 'The administrator account is ready, sign in with it',
                failed: 'Installation failed',
                goToSignin: 'Go to sign in',
                retry: 'Retry'
            },
            next: 'Next',
            back: 'Back'
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
