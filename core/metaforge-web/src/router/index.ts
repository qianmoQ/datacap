import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import i18n from '@/i18n/I18n'
import { createAuthRoute } from '@/router/auth'
import { createDefaultRouter } from '@/router/default'
import { createHttpRoute } from '@/router/http'
import { createSetupRoute } from '@/router/setup'
import SetupService from '@/services/setup'
import { TokenUtils } from '@/utils/token'
import { pageAccess } from './navigation'

const routes: Array<RouteRecordRaw> = []

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

createHttpRoute(router)
createAuthRoute(router)
createSetupRoute(router)
createDefaultRouter(router)

// 首次导航时检查是否已完成首次安装：未安装跳转 /setup，已安装访问 /setup 回登录页。
// 服务端不可达时状态未知：放行导航，交给全局网络错误处理（not_network 页面），
// 不能把请求失败误判成"未安装"而跳去 /setup。
let setupChecked: Promise<boolean | 'unknown'> | undefined

function ensureSetupChecked(): Promise<boolean | 'unknown'> {
    if (!setupChecked) {
        setupChecked = SetupService.getStatus()
            .then(response => (response?.status && response.data?.installed) ? true : false)
            .catch(() => 'unknown')
    }
    return setupChecked
}

/** 安装完成后由向导调用，让守卫按最新状态放行登录页。 */
export function invalidateSetupCheck(): void
{
    setupChecked = undefined
}

// 标题统一为「页面名 - MetaForge」；无页面标题的路由只显示 MetaForge。
router.afterEach(to => {
    const key = to.meta?.title
    const page = typeof key === 'string' ? i18n.global.t(key) : ''
    document.title = page && page !== key ? `${page} - MetaForge` : 'MetaForge'
})

router.beforeEach(async (_to, _from, _next) => {
    if (_to.matched.length === 0) {
        _next('/common/404')
        return
    }
    const installed = await ensureSetupChecked()
    if (installed === 'unknown') {
        _next()
        return
    }
    if (!installed && _to.path !== '/setup') {
        _next('/setup')
    }
    else if (installed && _to.path === '/setup') {
        _next('/auth/signin')
    }
    else {
        const access = pageAccess(_to, TokenUtils.getAuthUser())
        if (access === true) _next()
        else _next(access)
    }
})

export default router
