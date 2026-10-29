<template>
  <!-- 白底铺满整行：底色在宽屏下从两侧透出来 -->
  <div class="dc-header__bar">
    <div class="flex min-h-16 w-full items-center justify-between gap-6 px-5 py-3">
      <div class="dc-header__navigation flex min-w-0 flex-1 items-center gap-6">
        <!-- 数据源插件 Logo + 数据源名（省略 + tooltip） + 数据库选择器 -->
        <MfTooltip :content="sourceName" placement="bottom">
          <span class="flex min-w-0 cursor-default items-center gap-2.5">
            <MfAvatar size="md" :src="pluginLogo" :alt="sourceName">
              <Database :size="18"/>
            </MfAvatar>
            <span class="max-w-[180px] truncate text-lg font-bold text-content">{{ sourceName }}</span>
          </span>
        </MfTooltip>

        <MfSelect v-model="currentDatabase" class="w-44 shrink-0" size="sm"
                  :options="databaseOptions" :placeholder="$t('source.common.database')"/>

        <nav class="flex min-w-0 items-center gap-1">
          <router-link v-for="item in navItems" :key="item.path" :to="item.path"
                       class="dc-header__nav-link inline-flex items-center gap-1.5 whitespace-nowrap border-b-2 px-2.5 py-4 text-sm font-medium transition-colors"
                       :class="isNavActive(item.path) ? 'border-primary text-primary' : 'border-transparent text-content-secondary hover:text-content'">
            <component :size="15" :is="item.icon"/>
            {{ item.label }}
          </router-link>

          <MfDropdown trigger="click" placement="bottomRight">
            <button type="button"
                    class="inline-flex items-center gap-1.5 whitespace-nowrap border-b-2 border-transparent px-2.5 py-4 text-sm font-medium text-content-secondary transition-colors hover:text-content">
              <Ellipsis :size="15"/>
              {{ $t('common.moreNav') }}
              <ChevronDown :size="12"/>
            </button>
            <template #overlay>
              <MfMenu>
                <MfMenuItem v-for="item in moreNavItems" :key_="item.path">
                  <router-link :to="item.path" class="flex items-center gap-2">
                    <component :size="15" v-if="item.icon" :is="item.icon"/>
                    <span>{{ item.label }}</span>
                  </router-link>
                </MfMenuItem>
              </MfMenu>
            </template>
          </MfDropdown>
        </nav>
      </div>

        <MfSpace :size="16" :wrap="false" class="dc-header__actions shrink-0">
          <!-- 全局搜索：在当前数据源内检索数据表 -->
          <div class="relative" @focusin="searchFocused = true" @focusout="onSearchBlur">
            <MfInput ref="searchInputRef"
                     v-model="searchText"
                     class="dc-header__search"
                     :placeholder="$t('common.searchPlaceholder')">
              <template #prefix>
                <Search :size="14" :style="{ color: 'var(--dc-text-secondary)' }"/>
              </template>
              <template #suffix>
                <span class="dc-header__kbd">⌘K</span>
              </template>
            </MfInput>

            <div v-if="searchFocused && searchResults.length > 0"
                 class="absolute right-0 top-full z-50 mt-1 max-h-80 w-80 overflow-y-auto rounded-mf-lg border border-line bg-surface p-1 shadow-mf-lg">
              <button v-for="item in searchResults" :key="`${ item.database }_${ item.table }`" type="button"
                      class="flex w-full items-center gap-2 rounded-mf-md px-2.5 py-1.5 text-left text-sm transition-colors hover:bg-surface-hover"
                      @mousedown.prevent="onSearchPick(item)">
                <Database :size="13" class="shrink-0 text-content-tertiary"/>
                <span class="shrink-0 text-xs text-content-tertiary">{{ item.database }}</span>
                <span class="min-w-0 flex-1 truncate font-mono text-content">{{ item.table }}</span>
              </button>
            </div>
          </div>

          <!-- 帮助 -->
          <MfTooltip :content="$t('common.feedback')">
            <a href="https://github.com/devlive-community/metaforge" target="_blank" rel="noopener noreferrer"
               class="dc-header__icon-action" :aria-label="$t('common.feedback')">
              <CircleHelp :size="18" />
            </a>
          </MfTooltip>

          <div v-if="userInfo" class="flex items-center">
            <MfPopover trigger="click" placement="bottomRight">
              <template #content>
                <div class="dc-notify" style="width: 340px; max-height: 300px; overflow-y: auto;" @scroll="onNotifyScroll">
                  <div v-if="messages.length === 0" class="p-4 text-center text-sm text-content-tertiary">
                    {{ $t('common.noData') }}
                  </div>

                  <div v-for="(item, index) in messages"
                       :key="index"
                       class="dc-notify__item"
                       @click="handleNotificationClick(item)">
                    <div class="mt-1 text-sm text-content-secondary">
                      <div v-if="item.entityExists" class="flex space-x-1">
                        <span>{{ $t(`common.${ item.entityType?.toLowerCase() || '' }`) }}</span>

                        <template v-if="item.entityType === 'DATASET'">
                          <router-link :to="`/dataset/info/${item.entityCode}`" target="_blank" class="hover:text-primary flex items-center">
                            [ {{ item.entityName }} ]
                          </router-link>
                        </template>

                        <template v-else>
                          <router-link class="hover:text-primary" :to="'/' + item.entityType + '/' + item.entityCode">[ {{ item.entityName }} ]</router-link>
                        </template>

                        <span>{{ $t(`common.${ item.type?.toLowerCase() || '' }`) }}</span>
                      </div>
                      <div v-else>
                        {{ $t(`common.${ item.entityType?.toLowerCase() || '' }`) }} [ {{ item.entityName }} ] {{ $t(`common.${ item.type?.toLowerCase() || '' }`) }}
                      </div>
                    </div>

                    <div class="text-xs text-content-tertiary mt-1">{{ item.createTime }}</div>
                  </div>
                </div>
              </template>

              <MfBadge :count="userInfo?.unreadCount || 0">
                <button type="button" class="dc-header__icon-action" :aria-label="$t('notify.text.center')">
                  <Bell :size="20" />
                </button>
              </MfBadge>
            </MfPopover>
          </div>

          <!-- User Info -->
          <MfSpace v-if="!isLoggedIn">
            <router-link to="/auth/signin">
              <MfButton variant="primary">{{ $t('user.common.signin') }}</MfButton>
            </router-link>
            <router-link to="/auth/signup">
              <MfButton>{{ $t('user.common.signup') }}</MfButton>
            </router-link>
          </MfSpace>
          <div v-else class="flex items-center">
            <MfDropdown placement="bottomRight" trigger="click">
              <button type="button" class="inline-flex shrink-0 rounded-mf-full outline-none focus-visible:ring-2 focus-visible:ring-primary/40 focus-visible:ring-offset-2" :aria-label="userInfo?.username || $t('user.common.setting')" aria-haspopup="menu">
                <MfAvatar class="dc-header__avatar"
                          :size="34"
                          :src="userInfo?.avatarConfigure?.path"
                          :alt="userInfo?.username">
                  {{ (userInfo?.username || '?').charAt(0).toUpperCase() }}
                </MfAvatar>
              </button>

              <template #overlay>
                <MfMenu class="min-w-40">
                  <MfMenuItem disabled>
                    <div class="flex flex-col space-y-1">
                      <p class="text-sm font-medium leading-none text-center">{{ userInfo?.username }}</p>
                    </div>
                  </MfMenuItem>

                  <li class="my-1 border-t border-line" role="separator" />

                  <MfMenuItem>
                    <router-link to="/account/profile" class="flex items-center space-x-2">
                      <SettingOutlined :style="{ fontSize: '16px' }"/>
                      <span>{{ $t('user.common.setting') }}</span>
                    </router-link>
                  </MfMenuItem>

                  <MfMenuItem @click="logout">
                    <div class="flex items-center space-x-2">
                      <LogOut :size="16" />
                      <span>{{ $t('user.common.signout') }}</span>
                    </div>
                  </MfMenuItem>
                </MfMenu>
              </template>
            </MfDropdown>
          </div>
        </MfSpace>
      </div>
    </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'
import { storeToRefs } from 'pinia'
import { TokenUtils } from '@/utils/token'
import router from '@/router'
import NotificationService from '@/services/notification'
import SourceService from '@/services/source'
import MetadataService from '@/services/metadata'
import { FilterModel } from '@/model/filter.ts'
import {
  Bell, ChevronDown, CircleHelp, Database, Download, Ellipsis, GitBranch, GitCompare, History,
  Home, LayoutDashboard, LogOut, Presentation, Search, Settings, SquareDashedBottomCode,
  SquarePlay, Store, Upload
} from '@lucide/vue'

defineOptions({ name: 'WorkbenchHeader' })

const route = useRoute()
const { t } = useI18n()
const userStore = useUserStore()
const filter: FilterModel = new FilterModel()

// ===== 原型导航：源选择器 + 固定导航项 + 更多下拉 =====
const navItems = computed(() => [
  { path: '/home', label: t('common.home'), icon: Home },
  { path: '/source', label: t('source.common.database'), icon: Database },
  { path: '/query', label: 'SQL', icon: SquarePlay },
  { path: '/import', label: t('navigation.dataImport'), icon: Upload },
  { path: '/export', label: t('navigation.dataExport'), icon: Download },
  { path: '/compare', label: t('navigation.dataCompare'), icon: GitCompare }
])

const moreNavItems = computed(() => [
  { path: '/dataset', label: t('common.dataset'), icon: Database },
  { path: '/workflow', label: t('common.workflow'), icon: GitBranch },
  { path: '/snippet', label: t('common.snippet'), icon: SquareDashedBottomCode },
  { path: '/history', label: t('common.history'), icon: History },
  { path: '/report', label: t('common.report'), icon: Presentation },
  { path: '/dashboard', label: t('common.dashboard'), icon: LayoutDashboard },
  { path: '/store', label: t('common.store'), icon: Store },
  ...(userStore.userInfo?.roles?.some((role: any) => String(role.code).includes('ADMIN'))
      ? [{ path: '/admin/user', label: t('navigation.systemManagement'), icon: Settings }]
      : [])
])

const isNavActive = (path: string) => {
  if (path === '/home') return route.path === '/home' || route.path === '/'
  if (path === '/source') return route.path.startsWith('/source') || route.path.startsWith('/compare')
  if (path === '/query') return route.path.startsWith('/query')
  return route.path.startsWith(path)
}

const searchIndex = ref<{ database: string; table: string }[]>([])
const searchFocused = ref(false)
let searchIndexSource = ''

// 工作台头部跟随当前路由的数据源上下文
const routeSource = computed(() => String(route.params.source || ''))
const sourceInfo = ref<any | null>(null)
const sourceName = computed(() => String(sourceInfo.value?.name || routeSource.value || 'MetaForge'))
const pluginLogo = computed(() => {
  const type = String(sourceInfo.value?.type || '')
  return type ? `/static/images/plugin/${ type.toLowerCase() }.svg` : ''
})

const databaseOptions = ref<{ label: string; value: string }[]>([])
const currentDatabase = computed({
  get: () => String(route.params.database || ''),
  set: (value: string) => {
    if (value && value !== route.params.database) {
      router.push(`/source/${ routeSource.value }/d/${ value }`)
    }
  }
})

watch(routeSource, () => {
  sourceInfo.value = null
  searchIndex.value = []
  searchIndexSource = ''
  if (!routeSource.value) return
  SourceService.getByCode(routeSource.value)
               .then(response => {
                 if (response.status) {
                   sourceInfo.value = response.data
                 }
               })
  MetadataService.getDatabaseBySource(routeSource.value)
                 .then(response => {
                   if (response.status) {
                     databaseOptions.value = (response.data?.columns || [])
                         .map((item: any) => ({ label: item.object_name || item.schema_name, value: item.object_name || item.schema_name }))
                         .filter((item: any) => item.value)
                   }
                 })
}, { immediate: true })

const buildSearchIndex = () => {
  const source = routeSource.value
  if (!source || searchIndexSource === source) return
  searchIndexSource = source
  MetadataService.getDatabaseBySource(source)
                 .then(response => {
                   const databases = (response.data?.columns || [])
                       .map((item: any) => item.object_name || item.schema_name)
                       .filter(Boolean)
                       .slice(0, 20)
                   Promise.all(databases.map((database: string) =>
                       MetadataService.getTablesByDatabase(source, database)
                           .then(res => (res.data?.columns || [])
                               .filter((item: any) => item.type_name === 'table')
                               .map((item: any) => ({ database, table: item.object_name })))
                           .catch(() => [] as { database: string; table: string }[])
                   ))
                       .then(groups => {
                         if (routeSource.value === source) {
                           searchIndex.value = groups.flat()
                         }
                       })
                 })
}

const searchResults = computed(() => {
  const keyword = searchText.value.trim().toLowerCase()
  if (!keyword) return []
  return searchIndex.value
      .filter(item => item.table.toLowerCase().includes(keyword) || item.database.toLowerCase().includes(keyword))
      .slice(0, 20)
})

// 切库后清空搜索索引，重新构建
watch(currentDatabase, () => {
  searchIndex.value = []
  searchIndexSource = ''
  if (routeSource.value) buildSearchIndex()
})

const onSearchBlur = () => {
  window.setTimeout(() => (searchFocused.value = false), 150)
}

const onSearchPick = (item: { database: string; table: string }) => {
  searchFocused.value = false
  searchText.value = ''
  router.push(`/source/${ routeSource.value }/d/${ item.database }/t/structure/${ item.table }`)
}

const messages = ref<any[]>([])
const pageIndex = ref<number>(1)
const hasMoreData = ref(true)
const loading = ref(false)

const { userInfo, isLoggedIn } = storeToRefs(userStore)

const fetchMessages = async (value: number = 1, append = false) => {
  filter.page = value
  filter.orders = [{ column: 'createTime', order: 'desc' }]
  loading.value = true
  try {
    const response = await NotificationService.getAll(filter)
    if (response.status && response.data) {
      const items = response.data.content.map((item: any) => {
        item.read = item.isRead
        return item
      })
      messages.value = append ? [...messages.value, ...items] : items
      pageIndex.value = response.data.page
      hasMoreData.value = response.data.page < response.data.totalPage
    }
    else {
      MfMessage.error(response.message)
    }
  }
  finally {
    loading.value = false
  }
}

const onNotifyScroll = (event: any) => {
  const target = event.target
  if (loading.value || !hasMoreData.value) {
    return
  }
  if (target.scrollTop + target.offsetHeight >= target.scrollHeight - 10) {
    fetchMessages(pageIndex.value + 1, true)
  }
}

onMounted(async () => {
  if (TokenUtils.getAuthUser()) {
    await Promise.all([userStore.fetchUserInfo(), userStore.fetchMenus()])
    await fetchMessages()
  }
  window.addEventListener('keydown', onGlobalKeydown)
})

onBeforeUnmount(() => window.removeEventListener('keydown', onGlobalKeydown))

const searchText = ref('')
const searchInputRef = ref<HTMLInputElement | null>(null)

const focusSearch = () => {
  searchInputRef.value?.focus()
}

const onGlobalKeydown = (event: KeyboardEvent) => {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    focusSearch()
  }
}

const logout = () => {
  userStore.logout()
  router.push('/auth/signin')
}

const handleNotificationClick = (msg: any) => {
  const { id, code } = msg
  const payload = { id, code, isRead: true }
  NotificationService.saveOrUpdate(payload)
                     .then(response => {
                       if (response.status && response.data) {
                         fetchMessages()
                         userStore.fetchUserInfo()
                       }
                       else {
                         MfMessage.error(response.message)
                       }
                     })
}
</script>

<style scoped>
.dc-notify__item {
    padding: 8px 12px;
    border-radius: 6px;
    cursor: pointer;
}

.dc-notify__item:hover {
    background-color: var(--mf-color-surface-hover);
}

.dc-header__bar {
    min-height: 64px;
    display: flex;
    align-items: center;
    background: var(--dc-header);
    backdrop-filter: blur(8px);
}

.dc-header__wordmark {
    font-size: 18px;
    font-weight: 700;
    color: var(--dc-text-title);
    letter-spacing: 0.2px;
}

.dc-header__icon-action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    flex-shrink: 0;
    border-radius: var(--mf-radius-md);
    color: var(--mf-color-content-secondary);
    cursor: pointer;
    transition: color var(--mf-duration-fast), background-color var(--mf-duration-fast);
}

.dc-header__icon-action:hover {
    color: var(--mf-color-primary);
    background-color: var(--mf-color-surface-hover);
}

.dc-header__icon-action:focus-visible {
    outline: 2px solid var(--mf-color-primary);
    outline-offset: 2px;
}

.dc-header__search {
    width: clamp(160px, 15vw, 240px);
}

:deep(.dc-header__menu) {
    flex-wrap: wrap;
}

@media (max-width: 1200px) {
    .dc-header__layout {
        flex-wrap: wrap;
        row-gap: 12px;
    }

    .dc-header__navigation {
        flex-basis: 100%;
    }

    .dc-header__actions {
        margin-left: auto;
    }
}

.dc-header__kbd {
    display: inline-flex;
    align-items: center;
    padding: 0 5px;
    border: 1px solid var(--dc-border);
    border-radius: 4px;
    font-size: 11px;
    line-height: 16px;
    color: var(--dc-text-secondary);
    background: var(--dc-bg);
}

.dc-header__avatar {
    background: var(--dc-gradient-avatar);
    box-shadow: var(--dc-shadow-avatar);
    font-weight: 600;
}
</style>
