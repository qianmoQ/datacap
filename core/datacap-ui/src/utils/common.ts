import { useI18n } from 'vue-i18n'

const token = 'DataCapAuthToken'
const menu = 'DataCapAvailableMenus'
const userEditorConfigure = 'DataCapUserEditorConfigure'

/**
 * Retrieves the current user's ID from local storage.
 *
 * @return {number} The current user's ID.
 */
const getCurrentUserCode = (): number => {
    return JSON.parse(localStorage.getItem(token) || '{}').code
}

/**
 * Returns the color based on the origin.
 *
 * @param {string} origin - The origin value.
 * @return {string} The color based on the origin.
 */
const getColor = (origin: string): string => {
    switch (origin) {
        case 'CREATED':
            return 'hsl(220.9 39.3% 11%)'
        case 'RUNNING':
            return 'hsl(221.2 83.2% 53.3%)'
        case 'SUCCESS':
            return 'hsl(142.1 76.2% 36.3%)'
        case 'FAILURE':
            return 'hsl(346.8 77.2% 49.8%)'
        case 'STOPPING':
            return 'hsl(38 92% 50%)'
        case 'STOPPED':
            return '#17233d'
        case 'INTERRUPTED':
            return 'hsl(280 60% 50%)'
        case 'TIMEOUT':
            return 'hsl(47.9 95.8% 53.1%)'
        default:
            return 'hsl(24.6 95% 53.1%)'
    }
}

const fileToBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => {
            const base64String = reader.result as string
            resolve(base64String)
        }
        reader.onerror = reject
        reader.readAsDataURL(file)
    })
}

export default {
    token: token,
    menu: menu,
    getCurrentUserCode: getCurrentUserCode,
    userEditorConfigure: userEditorConfigure,
    getColor: getColor,
    fileToBase64: fileToBase64
}

export function useUtil()
{
    const { t } = useI18n()

    /**
     * Retrieves the text based on the given origin value.
     *
     * @param {string} origin - the origin value to determine the text to retrieve
     * @return {string} the text based on the origin value
     */
    const getText = (origin: string): string => {
        switch (origin) {
            case 'CREATED':
                return t('state.common.create')
            case 'RUNNING':
                return t('state.common.running')
            case 'SUCCESS':
                return t('state.common.success')
            case 'FAILURE':
                return t('state.common.failure')
            case 'STOPPING':
                return t('state.common.stopping')
            case 'STOPPED':
                return t('state.common.stop')
            case 'INTERRUPTED':
                return t('state.common.interrupted')
            case 'TIMEOUT':
                return t('state.common.timeout')
            case 'QUEUE':
                return t('state.common.queue')
            default:
                return origin
        }
    }

    return {
        getText
    }
}
