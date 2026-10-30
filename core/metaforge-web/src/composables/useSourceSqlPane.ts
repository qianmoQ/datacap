import { inject, onScopeDispose, ref } from 'vue'
import axios from 'axios'
import ExecuteService from '@/services/execute'
import type { ResponseModel } from '@/model/response'
import { useSourceManageRoute } from './useSourceManageRoute'

export function useSourceSqlPane() {
  const route = useSourceManageRoute()
  const can = inject<(capability: string) => boolean>('metadataCan', () => false)
  const content = ref('')
  const response = ref<ResponseModel | null>(null)
  const running = ref(false)
  const executedQuery = ref('')
  const editorRoot = ref<HTMLElement | null>(null)
  let revision = 0
  let cancel: ReturnType<typeof axios.CancelToken.source> | undefined
  let observer: ResizeObserver | undefined
  function onEditorCreated(editor: { resize: () => void }) {
    observer?.disconnect()
    if (typeof ResizeObserver !== 'undefined' && editorRoot.value) {
      observer = new ResizeObserver(() => editor.resize())
      observer.observe(editorRoot.value)
    }
  }
  async function run() {
    if (running.value || !content.value.trim() || !can('query:execute')) return
    const request = ++revision
    running.value = true
    response.value = null
    executedQuery.value = content.value
    cancel = axios.CancelToken.source()
    try {
      const result = await ExecuteService.execute({ name: String(route.params.source), content: content.value, mode: 'ADHOC' }, cancel.token)
      if (request !== revision) return
      response.value = result.data?.isSuccessful === false ? { ...result, status: false, message: result.data.message || result.message } : result
    } finally { if (request === revision) running.value = false }
  }
  onScopeDispose(() => { revision++; cancel?.cancel(); observer?.disconnect() })
  return { content, response, running, executedQuery, editorRoot, onEditorCreated, run, sourceCode: String(route.params.source) }
}
