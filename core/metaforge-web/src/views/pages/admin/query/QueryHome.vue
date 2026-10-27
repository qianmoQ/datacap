<template>
  <div class="flex h-full min-h-0 gap-3">
    <aside v-show="!focusMode"
           class="flex w-[280px] shrink-0 flex-col overflow-hidden rounded-mf-lg border border-line bg-surface shadow-mf-sm">
      <QuerySourcePicker v-model="selectSource.full as string" @change="onChange($event)"/>

      <div class="border-t border-line px-3 py-2.5">
        <MfInput ref="treeSearchRef"
                 v-model="treeKeyword"
                 size="sm"
                 :placeholder="$t('query.tree.search')">
          <template #prefix>
            <Search :size="13"/>
          </template>
          <template #suffix>
            <span class="rounded-mf-sm border border-line bg-surface-hover px-1.5 py-0.5 font-mono text-[10px] leading-none text-content-tertiary">⌘K</span>
          </template>
        </MfInput>
      </div>

      <MetadataTree v-if="selectSource.code"
                    :code="selectSource.code as string"
                    :keyword="treeKeyword"
                    @select-table="onSelectTable"/>

      <div v-else class="flex flex-1 items-center justify-center px-4 pb-6 text-center text-xs text-content-tertiary">
        {{ $t('source.tip.selected') }}
      </div>
    </aside>

    <section class="flex min-w-0 flex-1 flex-col gap-3">
      <div class="flex h-[42%] max-h-[520px] min-h-[260px] shrink-0 flex-col overflow-hidden rounded-mf-lg border border-line bg-surface shadow-mf-sm">
        <div class="flex shrink-0 items-center border-b border-line pl-2 pr-1.5">
          <button v-for="item in Object.values(editors)"
                  :key="item.key"
                  type="button"
                  class="group flex items-center gap-1.5 border-b-2 px-3 py-2.5 text-sm transition-[color,border-color] duration-(--mf-duration-base) ease-(--mf-easing)"
                  :class="activeEditor === item.key ? 'border-b-primary font-medium text-content' : 'border-b-transparent text-content-secondary hover:text-content'"
                  @click="activeEditor = item.key">
            <span class="size-1.5 rounded-full" :class="activeEditor === item.key ? 'bg-primary' : 'bg-content-placeholder'"></span>
            {{ item.title }}
            <span v-if="Object.keys(editors).length > 1"
                  class="inline-flex size-4 items-center justify-center rounded-mf-sm text-content-tertiary hover:bg-surface-hover hover:text-content"
                  role="button"
                  @click.stop="onEditTabs(item.key, 'remove')">
              <X :size="11"/>
            </span>
          </button>

          <div class="ml-auto">
            <MfTooltip :content="$t('common.createEditor')">
              <MfButton variant="ghost" size="sm" shape="circle" :disabled="!selectSource.code" @click="onPlusEditor">
                <Plus :size="15"/>
              </MfButton>
            </MfTooltip>
          </div>
        </div>

        <div class="flex shrink-0 items-center gap-2 border-b border-line px-3 py-2">
          <div class="inline-flex items-stretch">
            <MfButton variant="primary"
                      class="rounded-r-none!"
                      :loading="loading.running"
                      :disabled="!selectSource.id || loading.running"
                      @click="onRun()">
              <Play :size="15"/>
              {{ $t('query.common.execute') }}
            </MfButton>

            <MfDropdown trigger="click" placement="bottomLeft">
              <MfButton variant="primary"
                        class="rounded-l-none! border-l border-on-primary/30 px-1.5!"
                        :disabled="!selectSource.id">
                <ChevronDown :size="14"/>
              </MfButton>
              <template #overlay>
                <div class="flex flex-col">
                  <button type="button"
                          class="rounded-mf-md px-2.5 py-1.5 text-left text-sm text-content-secondary hover:bg-surface-hover hover:text-content disabled:cursor-not-allowed disabled:opacity-40"
                          :disabled="!loading.running"
                          @click="onCancel()">
                    {{ $t('common.cancel') }}
                  </button>
                </div>
              </template>
            </MfDropdown>
          </div>

          <MfButton :loading="loading.formatting"
                    :disabled="(!selectSource.id && !loading.formatting) || loading.formatting"
                    @click="onFormat()">
            <Eraser :size="15"/>
            {{ $t('query.common.format') }}
          </MfButton>

          <MfButton variant="ghost"
                    class="text-danger! hover:bg-danger-subtle! hover:text-danger!"
                    :disabled="!selectSource.code"
                    @click="onClearEditor()">
            <Trash2 :size="15"/>
            {{ $t('common.clear') }}
          </MfButton>

          <MfButton :disabled="!activeEditorContent" @click="visibleSnippet(true)">
            <Save :size="15"/>
            {{ $t('common.save') }}
          </MfButton>

          <MfButton :loading="loading.planning"
                    :disabled="(!selectSource.id && !loading.planning) || loading.running || loading.planning"
                    @click="onRunPlan()">
            <ListTree :size="15"/>
            {{ $t('query.common.plan') }}
          </MfButton>

          <MfButton v-if="selectSource.id && (responseConfigure.response?.data || !responseConfigure.response?.status)"
                    @click="visibleQueryHelp(true)">
            <Bot :size="15"/>
            {{ $t('query.common.help') }}
          </MfButton>

          <div class="ml-auto flex items-center gap-2">
            <MfTooltip v-if="responseConfigure.response?.status">
              <template #content>
                <div class="flex flex-col">
                  <span class="text-xs">
                    {{ $t('query.common.connectionTime') }}
                    {{ responseConfigure.response.data.connection.elapsed }} ms
                  </span>
                  <span class="text-xs">
                    {{ $t('query.common.executionTime') }}
                    {{ responseConfigure.response.data.processor.elapsed }} ms
                  </span>
                </div>
              </template>
              <span class="inline-flex h-8 items-center gap-1.5 rounded-mf-md border border-line bg-surface px-2.5 text-sm text-content-secondary shadow-mf-sm">
                <Clock :size="14" class="text-content-tertiary"/>
                {{ responseConfigure.response.data.processor.elapsed }} ms
              </span>
            </MfTooltip>

            <MfDropdown trigger="click" placement="bottomRight">
              <MfTooltip :content="$t('query.tip.editorSettings')">
                <MfButton variant="ghost" size="sm" shape="circle">
                  <Settings :size="15"/>
                </MfButton>
              </MfTooltip>
              <template #overlay>
                <div class="flex w-64 flex-col gap-3 p-1.5">
                  <div class="flex flex-col gap-1.5">
                    <div class="text-xs font-medium text-content-tertiary">{{ $t('user.common.fontSize') }}</div>
                    <div class="flex items-center gap-1">
                      <button v-for="size in fontSizeOptions"
                              :key="size"
                              type="button"
                              class="rounded-mf-md border px-2 py-1 text-xs transition-[color,border-color,background-color] duration-(--mf-duration-fast)"
                              :class="editorConfigure.fontSize === size ? 'border-primary bg-primary/10 font-medium text-primary' : 'border-line text-content-secondary hover:bg-surface-hover hover:text-content'"
                              @click="onFontSizeSelect(size)">
                        {{ size }}
                      </button>
                    </div>
                  </div>

                  <div class="flex flex-col gap-1.5">
                    <div class="text-xs font-medium text-content-tertiary">{{ $t('user.common.theme') }}</div>
                    <div class="flex flex-wrap items-center gap-1">
                      <button v-for="theme in themeOptions"
                              :key="theme"
                              type="button"
                              class="rounded-mf-md border px-2 py-1 text-xs transition-[color,border-color,background-color] duration-(--mf-duration-fast)"
                              :class="editorConfigure.theme === theme ? 'border-primary bg-primary/10 font-medium text-primary' : 'border-line text-content-secondary hover:bg-surface-hover hover:text-content'"
                              @click="onThemeSelect(theme)">
                        {{ theme }}
                      </button>
                    </div>
                  </div>
                </div>
              </template>
            </MfDropdown>

            <MfTooltip :content="focusMode ? $t('query.tip.exitFocus') : $t('query.tip.focus')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="focusMode = !focusMode">
                <Minimize2 v-if="focusMode" :size="15"/>
                <Maximize2 v-else :size="15"/>
              </MfButton>
            </MfTooltip>
          </div>
        </div>

        <div class="min-h-0 flex-1">
          <div v-if="!selectSource.code" class="flex h-full items-center justify-center">
            <MfAlert :title="$t('source.tip.selected')"/>
          </div>

          <AceEditor v-else
                     :key="`ace-configure-${ editorConfigureKey }`"
                     :value="activeEditorInstance?.content ?? ''"
                     height="100%"
                     @update:value="onEditorInput"
                     :auto-complete="{
                         endpoint: () => `${ baseUrl ? baseUrl : '' }/api/v1/metadata/${ selectSource.code }/suggests`,
                         method: 'GET',
                         trigger: ['.', '@'],
                         headers: { 'Authorization': auth?.type + ' ' + auth?.token },
                         requestParams: (context) => ({
                             keyword: context.word
                         }),
                         transform: (response: any) => {
                           return response[0].data.columns.map((item: any) => ({
                             label: item.object_name,
                             insertText: item.object_name,
                             detail: $t(item.object_key),
                             icon: item.object_type
                           }))
                         }
                     }">
          </AceEditor>
        </div>
      </div>

      <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-mf-lg border border-line bg-surface shadow-mf-sm">
        <QueryResultPanel :response="responseConfigure.response"
                          :running="loading.running || loading.planning"
                          :source-id="selectSource.id as unknown as number"
                          :source-code="(selectSource.code as string)"
                          :query="responseConfigure.query"
                          @add-dataset="onAddDataset"
                          @refresh="onRun"/>
      </div>
    </section>

    <aside v-show="!focusMode"
           class="flex w-[320px] shrink-0 flex-col overflow-hidden rounded-mf-lg border border-line bg-surface shadow-mf-sm">
      <QuerySidePanel :source-code="(selectSource.code as string)"
                      :selected-table="selectedTable"
                      :refreshed="sidePanelRefresh"
                      @run-sql="onSideRunSql"
                      @load-sql="onSideLoadSql"
                      @add-dataset="onAddDataset"
                      @create-snippet="visibleSnippet(true)"
                      @edit-snippet="onEditSnippet"/>
    </aside>
  </div>

  <QueryHelp v-if="visibility.queryHelp"
             :is-visible="visibility.queryHelp"
             :content="editors[activeEditor]?.content"
             :help-type="queryConfigure.queryType"
             :engine="selectSource.engine as string"
             :message="responseConfigure.message as string"
             @close="visibleQueryHelp(false)">
  </QueryHelp>

  <SnippetInfo v-if="dataInfoVisible"
               :is-visible="dataInfoVisible"
               :info="dataInfo"
               @close="onSnippetClosed">
  </SnippetInfo>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import { Bot, ChevronDown, Clock, Eraser, ListTree, Maximize2, Minimize2, Play, Plus, Save, Search, Settings, Trash2, X } from '@lucide/vue'
import SnippetService from '@/services/snippet'
import AuditService from '@/services/audit'
import UserService from '@/services/user'
import { ExecuteModel } from '@/model/execute'
import ExecuteService from '@/services/execute'
import { ResponseModel } from '@/model/response'
import FormatService from '@/services/format'
import { HelpType } from '@/views/pages/admin/query/HelpType'
import QueryHelp from '@/views/pages/admin/query/QueryHelp.vue'
import QuerySourcePicker from '@/views/pages/admin/query/QuerySourcePicker.vue'
import QueryResultPanel from '@/views/pages/admin/query/QueryResultPanel.vue'
import QuerySidePanel from '@/views/pages/admin/query/QuerySidePanel.vue'
import MetadataTree from '@/views/components/tree/MetadataTree.vue'
import AceEditor from '@/views/components/editor/AceEditor.vue'
import { SnippetModel, SnippetRequest } from '@/model/snippet'
import { UserEditor } from '@/model/user'
import SnippetInfo from '@/views/pages/admin/snippet/SnippetInfo.vue'
import { TokenUtils } from '@/utils/token.ts'
import Common from '@/utils/common'
import axios from 'axios'

interface EditorInstance
{
  title: string
  key: string
  content: string
}

defineOptions({ name: 'QueryHome' })

const route = useRoute()
const router = useRouter()
const auth = TokenUtils.getAuthUser()

const treeSearchRef = ref<{ focus: () => void } | null>(null)
const treeKeyword = ref('')
const focusMode = ref(false)
const selectedTable = ref<{ database: string; table: string } | null>(null)
const sidePanelRefresh = ref(0)
let editorSequence = 0

const editorConfigure = reactive<UserEditor>({ fontSize: 12, theme: 'chrome' })
const editorConfigureKey = ref(0)
const fontSizeOptions = [12, 13, 14, 16, 18]
const themeOptions = ['chrome', 'sqlserver', 'github', 'eclipse', 'dreamweaver', 'cloud9_day']

const loading = reactive({
  running: false,
  formatting: false,
  planning: false,
  froming: false
})
const visibility = reactive({
  queryHelp: false
})
const selectSource = reactive({
  id: null as string | null | undefined,
  type: null as string | null | undefined,
  engine: null as string | null | undefined,
  code: null as string | null | undefined,
  full: null as string | null
})
const editors = ref<Record<string, EditorInstance>>({})
const activeEditor = ref<string | null>(null)
const queryConfigure = reactive({
  configure: null as ExecuteModel | null,
  cancelToken: null as any | null,
  queryType: [HelpType.ANALYSIS, HelpType.OPTIMIZE] as HelpType[]
})
const responseConfigure = reactive({
  response: null as ResponseModel | null,
  message: null as string | null,
  query: ''
})
const dataInfo = ref<SnippetModel>(null as unknown as SnippetModel)
const dataInfoVisible = ref(false)
const baseUrl = import.meta.env.VITE_API_BASE_URL

const activeEditorInstance = computed(() => editors.value[activeEditor.value as string])

const activeEditorContent = computed(() => {
  const editor = activeEditorInstance.value
  return editor && editor.content.trim().length > 0
})

const onEditorInput = (value: string) => {
  const editor = activeEditorInstance.value
  if (editor) {
    editor.content = value
  }
}

const createEditor = () => {
  editorSequence += 1
  const newEditor: EditorInstance = {
    title: `SQL ${ editorSequence }`,
    key: `editor-${ Date.now() }`,
    content: ''
  }

  activeEditor.value = newEditor.key
  editors.value = {
    ...editors.value,
    [newEditor.key]: newEditor
  }
}

const onChange = (value: string) => {
  const idAndType = value.split(':')
  selectSource.id = idAndType[0]
  selectSource.type = idAndType[1]
  selectSource.engine = idAndType[1]
  selectSource.code = idAndType[2]
}

const handlerInitialize = () => {
  createEditor()

  queryConfigure.configure = { name: selectSource.id as string, content: '', mode: 'ADHOC' }
  const params = route.params
  if (params) {
    const code = params.code
    const type = params.type
    if (code && type) {
      if (type === 'snippet') {
        loading.froming = true
        queryConfigure.configure.mode = 'SNIPPET'
        SnippetService.getByCode(code as string)
                      .then((response) => {
                        if (response.status && response.data?.code) {
                          const active = editors.value[activeEditor.value as string]
                          active.content = response.data.context
                        }
                      })
                      .finally(() => (loading.froming = false))
      }
      else if (type === 'history') {
        loading.froming = true
        queryConfigure.configure.mode = 'HISTORY'
        AuditService.getByCode(code as string)
                    .then((response) => {
                      if (response.status && response.data) {
                        const active = editors.value[activeEditor.value as string]
                        active.content = response.data.content
                        const full = `${ response.data.source.id }:${ response.data.source.type }:${ response.data.source.code }`
                        selectSource.full = full
                        onChange(full)
                      }
                    })
                    .finally(() => (loading.froming = false))
      }
    }
  }
}

const onPlusEditor = () => {
  createEditor()
}

const onEditTabs = (targetKey: any, action: string) => {
  if (action === 'remove') {
    const keys = Object.keys(editors.value)
    if (keys.length <= 1) {
      return
    }
    const next = { ...editors.value }
    delete next[targetKey]
    editors.value = next
    if (activeEditor.value === targetKey) {
      activeEditor.value = Object.keys(editors.value)[0]
    }
  }
}

const onClearEditor = () => {
  const active = editors.value[activeEditor.value as string]
  if (active) {
    active.content = ''
  }
}

const onRun = () => {
  responseConfigure.response = null
  responseConfigure.message = null
  responseConfigure.query = ''
  queryConfigure.queryType = [HelpType.ANALYSIS, HelpType.OPTIMIZE]
  queryConfigure.cancelToken = axios.CancelToken.source()
  queryConfigure.configure!.name = selectSource.code as string
  const editor = editors.value[activeEditor.value as string]
  queryConfigure.configure!.content = editor.content

  loading.running = true
  ExecuteService.execute(queryConfigure.configure!, queryConfigure.cancelToken.token)
                .then((response) => {
                  if (response.status) {
                    responseConfigure.response = response
                    responseConfigure.query = editor.content
                    editor.content = response.data.content
                  }
                  else {
                    MfMessage.error(response.message)
                    responseConfigure.message = response.message
                    responseConfigure.response = response
                    responseConfigure.query = editor.content
                    queryConfigure.queryType.push(HelpType.FIXEDBUGS)
                  }
                })
                .finally(() => {
                  loading.running = false
                  sidePanelRefresh.value += 1
                })
}

const onCancel = () => {
  queryConfigure.cancelToken.cancel('Cancel query')
  loading.running = false
}

const onRunPlan = () => {
  responseConfigure.response = null
  responseConfigure.message = null
  responseConfigure.query = ''
  queryConfigure.queryType = [HelpType.ANALYSIS, HelpType.OPTIMIZE]
  queryConfigure.configure!.name = selectSource.code as string
  const editor = editors.value[activeEditor.value as string]
  queryConfigure.configure!.content = editor.content

  // The editor keeps the user SQL; the plan rows render in the result grid.
  loading.planning = true
  ExecuteService.plan(queryConfigure.configure!)
                .then((response) => {
                  responseConfigure.query = editor.content
                  if (response.status) {
                    responseConfigure.response = response
                  }
                  else {
                    MfMessage.error(response.message)
                    responseConfigure.message = response.message
                    responseConfigure.response = response
                    queryConfigure.queryType.push(HelpType.FIXEDBUGS)
                  }
                })
                .finally(() => {
                  loading.planning = false
                  sidePanelRefresh.value += 1
                })
}

const onFormat = () => {
  loading.formatting = true

  const active = editors.value[activeEditor.value as string]
  const configure = { sql: active.content }
  FormatService.formatSql(configure)
               .then((response) => {
                 if (response.status) {
                   active.content = response.data
                 }
                 else {
                   MfMessage.error(response.message)
                 }
               })
               .finally(() => (loading.formatting = false))
}

const onSelectTable = (value: { database: string; table: string } | null) => {
  selectedTable.value = value
}

const onSideLoadSql = (sql: string) => {
  const active = editors.value[activeEditor.value as string]
  if (active) {
    active.content = sql
  }
}

const onSideRunSql = (sql: string) => {
  onSideLoadSql(sql)
  onRun()
}

const onAddDataset = () => {
  const active = editors.value[activeEditor.value as string]
  if (!active || !selectSource.code) {
    return
  }
  const id = Math.random().toString(36).substring(2)
  localStorage.setItem(`QueryContent_${ id }`, active.content)
  router.push(`/dataset/info/source/${ selectSource.code }?tempId=${ id }`)
}

const visibleQueryHelp = (value: boolean) => {
  visibility.queryHelp = value
}

const visibleSnippet = (opened: boolean) => {
  const active = editors.value[activeEditor.value as string]
  if (opened && active) {
    dataInfo.value = SnippetRequest.of()
    dataInfo.value.context = active.content
  }
  dataInfoVisible.value = opened
}

const onEditSnippet = (snippet: SnippetModel) => {
  dataInfo.value = snippet
  dataInfoVisible.value = true
}

const onSnippetClosed = () => {
  dataInfoVisible.value = false
  sidePanelRefresh.value += 1
}

const onSearchHotkey = (event: KeyboardEvent) => {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    treeSearchRef.value?.focus()
  }
  else if ((event.metaKey || event.ctrlKey) && event.key === 'Enter') {
    event.preventDefault()
    if (selectSource.id && !loading.running) {
      onRun()
    }
  }
}

const loadEditorConfigure = () => {
  try {
    const raw = localStorage.getItem(Common.userEditorConfigure)
    if (raw) {
      const parsed = JSON.parse(raw) as UserEditor
      if (parsed.fontSize) {
        editorConfigure.fontSize = parsed.fontSize
      }
      if (parsed.theme) {
        editorConfigure.theme = parsed.theme
      }
    }
  }
  catch {
    editorConfigure.fontSize = 12
    editorConfigure.theme = 'chrome'
  }
}

const onFontSizeSelect = (size: number) => {
  editorConfigure.fontSize = size
  persistEditorConfigure()
}

const onThemeSelect = (theme: string) => {
  editorConfigure.theme = theme
  persistEditorConfigure()
}

/** Persist through the backend user editor API and remount the editor to apply. */
const persistEditorConfigure = () => {
  const configure: UserEditor = { fontSize: editorConfigure.fontSize, theme: editorConfigure.theme }
  localStorage.setItem(Common.userEditorConfigure, JSON.stringify(configure))
  UserService.changeEditor(configure)
  editorConfigureKey.value += 1
}

onMounted(() => window.addEventListener('keydown', onSearchHotkey))
onBeforeUnmount(() => window.removeEventListener('keydown', onSearchHotkey))

loadEditorConfigure()
handlerInitialize()
</script>
