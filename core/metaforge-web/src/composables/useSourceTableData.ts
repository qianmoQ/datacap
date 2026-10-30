import { computed, inject, onScopeDispose, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { mapSourceColumns } from '@/model/source-column'
import { buildDataFilters, dataFields, dataKind, defaultOperator, emptyValue, formToJson, formToRow, jsonToRow, rowToForm, toCsv, toJson, validateForm,
  type DataCondition, type DataField, type DataOperator, type FieldIssue, type FormValues } from '@/model/source-data'
import { useRowSelection } from './useRowSelection'
import { usePanelState } from './usePanelState'

export type DataRow = Record<string, unknown> & { __key: string }
export type RowEditorMode = 'add' | 'edit'

/** Data preview of one table: server-side filters, sort and paging, plus row insert/update/delete. */
export function useSourceTableData() {
  const { t } = useI18n()
  const route = useRoute()
  const can = inject<(capability: string) => boolean>('metadataCan', () => false)
  const sharedRows = inject<Ref<any[]>>('metadataColumnRows', ref([]))
  const fieldPanelOpen = inject<Ref<boolean>>('metadataFieldPanelOpen', ref(false))
  const fieldPanelHost = inject<Ref<HTMLElement | null>>('metadataFieldPanelHost', ref(null))
  const fieldPanelWidth = inject<Ref<number>>('metadataFieldPanelWidth', ref(324))

  const loading = ref(false)
  const saving = ref(false)
  const headers = ref<string[]>([])
  const rows = ref<DataRow[]>([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(10)
  const sql = ref('')
  const hidden = ref(new Set<string>())
  const order = ref<{ name: string; order: 'asc' | 'desc' } | null>(null)
  let nextConditionId = 1
  const conditions = ref<DataCondition[]>([])
  let applied: DataCondition[] = []
  const sqlVisible = ref(false)
  const deleteTargets = ref<DataRow[]>([])
  const copyTargets = ref<DataRow[]>([])
  let revision = 0
  let alive = true

  const columnMeta = computed(() => mapSourceColumns(sharedRows.value || []))
  const typeOf = (name: string) => columnMeta.value.find(column => column.name === name)?.type || ''
  /** Columns known from metadata keep their table order; the query headers fill in when metadata is unavailable. */
  const columnNames = computed(() => columnMeta.value.length ? columnMeta.value.map(column => column.name) : headers.value)
  const visibleColumns = computed(() => columnNames.value.filter(name => !hidden.value.has(name)))
  const selection = useRowSelection(() => rows.value, row => row.__key)
  const { selected, allSelected, partialSelected, toggleAll, toggle, select } = selection
  const selectedRows = computed(() => rows.value.filter(row => selected.value.has(row.__key)))
  // Shift-click selects from the last row clicked or checked; plain clicks replace the selection.
  let anchor: number | null = null
  function onRowClick(row: DataRow, index: number, event: MouseEvent) {
    if (event.shiftKey && anchor !== null) {
      const [from, to] = anchor < index ? [anchor, index] : [index, anchor]
      select(rows.value.slice(from, to + 1).map(item => item.__key))
      return
    }
    anchor = index
    if (event.metaKey || event.ctrlKey) toggle(row.__key, !selected.value.has(row.__key))
    else select([row.__key])
  }
  function onCheck(row: DataRow, index: number, checked: boolean) {
    anchor = index
    toggle(row.__key, checked)
  }
  const rowClass = (row: DataRow) => selected.value.has(row.__key) ? 'bg-surface-hover [&>td]:bg-surface-hover' : ''
  const editor = usePanelState<DataRow>()
  const editorMode = ref<RowEditorMode>('add')
  const editorValues = ref<FormValues>({})
  const editorOriginal = ref<FormValues>({})
  const editorTab = ref<'form' | 'json'>('form')
  const editorJson = ref('')
  const editorIssues = ref<Record<string, FieldIssue>>({})
  const jsonError = ref('')
  const continueAdding = ref(false)

  const identity = () => [route.params.source, route.params.database, route.params.table].join('/')
  const errorMessage = (error: unknown) => error instanceof Error ? error.message : String(error)
  function assertSuccess(response: any) {
    if (!response.status || response.data?.isSuccessful === false) throw new Error(response.data?.message || response.message || t('common.loadingFailed'))
  }
  /** Rows sent back to the server for WHERE clauses must not carry the client-side row key. */
  const plain = (row: DataRow) => Object.fromEntries(Object.entries(row).filter(([key]) => key !== '__key'))

  async function load() {
    const request = ++revision
    const [source, database, table] = [route.params.source, route.params.database, route.params.table].map(String)
    if (!can('table:read') || !source || !database || !table) { rows.value = []; total.value = 0; return }
    loading.value = true
    try {
      const response = await MetadataService.queryTable(source, database, table, {
        pagination: { page: page.value, size: size.value },
        filters: buildDataFilters(applied),
        orders: order.value ? [order.value] : [],
      })
      if (request !== revision || !alive) return
      assertSuccess(response)
      const data = response.data || {}
      headers.value = data.headers || []
      rows.value = (data.columns || []).map((row: Record<string, unknown>, index: number) => ({ ...row, __key: String(index) }))
      total.value = Number(data.pagination?.total ?? rows.value.length)
      sql.value = data.content || ''
      select([])
      anchor = null
    } catch (error) {
      if (request === revision && alive) { rows.value = []; total.value = 0; MfMessage.error(errorMessage(error)) }
    } finally {
      if (request === revision && alive) loading.value = false
    }
  }

  // Filter rows
  function addCondition(column = '') {
    const operator = defaultOperator(dataKind(typeOf(column)))
    conditions.value = [...conditions.value, { id: nextConditionId++, column, operator, value: emptyValue(operator) }]
  }
  function changeColumn(condition: DataCondition, column: string) {
    condition.column = column
    condition.operator = defaultOperator(dataKind(typeOf(column)))
    condition.value = emptyValue(condition.operator)
  }
  function changeOperator(condition: DataCondition, operator: DataOperator) {
    const previous = condition.operator
    condition.operator = operator
    if (Array.isArray(emptyValue(previous)) !== Array.isArray(emptyValue(operator)) || previous === 'BETWEEN' || operator === 'BETWEEN') condition.value = emptyValue(operator)
  }
  const removeCondition = (id: number) => { conditions.value = conditions.value.filter(condition => condition.id !== id) }
  const clearConditions = () => { conditions.value = [] }
  function onSearch() {
    applied = conditions.value.map(condition => ({ ...condition, value: Array.isArray(condition.value) ? [...condition.value] : condition.value }))
    page.value = 1
    load()
  }
  function onReset() {
    conditions.value = []
    addCondition()
    onSearch()
  }

  // Paging, sort and columns
  function onPageChange(next: number, nextSize: number) {
    if (nextSize !== size.value) { size.value = nextSize; page.value = 1 } else page.value = next
    load()
  }
  function setOrder(name: string | null, direction: 'asc' | 'desc' = 'asc') {
    order.value = name ? { name, order: direction } : null
    page.value = 1
    load()
  }
  function toggleColumn(name: string, visible: boolean) {
    const next = new Set(hidden.value)
    if (visible) next.delete(name); else next.add(name)
    hidden.value = next
  }

  // Row editor: a typed form or raw JSON, kept in sync when the user switches between them.
  const fields = computed<DataField[]>(() => {
    const list = dataFields(sharedRows.value || [])
    return list.length ? list : headers.value.map(name => ({ name, type: '', kind: 'text' as const, options: [], nullable: true, hasDefault: false, autoIncrement: false, required: false }))
  })
  function openEditor(mode: RowEditorMode, row: DataRow | null = null) {
    if (saving.value || !can(mode === 'edit' ? 'data:modify' : 'data:insert')) return
    editorMode.value = mode
    editorTab.value = 'form'
    editorValues.value = rowToForm(fields.value, row)
    editorOriginal.value = { ...editorValues.value }
    editorIssues.value = {}
    jsonError.value = ''
    editor.open(row)
  }
  const closeEditor = (force = false) => { if (!saving.value || force) editor.close() }
  /** Parses the JSON editor into row objects; a single object or an array of objects. */
  function parseJsonRows(): Record<string, unknown>[] | null {
    let parsed: unknown
    try { parsed = JSON.parse(editorJson.value) } catch (error) {
      jsonError.value = t('source.data.jsonInvalid', { message: errorMessage(error) })
      return null
    }
    const list = Array.isArray(parsed) ? parsed : [parsed]
    if (!list.length || list.some(item => item === null || typeof item !== 'object' || Array.isArray(item))) {
      jsonError.value = t('source.data.jsonObjectRequired')
      return null
    }
    jsonError.value = ''
    return list as Record<string, unknown>[]
  }
  function setEditorTab(tab: 'form' | 'json') {
    if (tab === editorTab.value) return
    if (tab === 'json') {
      editorJson.value = JSON.stringify(formToJson(fields.value, editorValues.value), null, 2)
      jsonError.value = ''
      editorTab.value = 'json'
      return
    }
    const parsed = parseJsonRows()
    if (!parsed) return
    if (parsed.length !== 1) { jsonError.value = t('source.data.formSingleRow'); return }
    editorValues.value = rowToForm(fields.value, parsed[0])
    editorIssues.value = {}
    editorTab.value = 'form'
  }
  /** Rows to send: typed form values, or the parsed JSON. Edits keep only the fields that changed. */
  function editorRows(): Record<string, unknown>[] | null {
    const editing = editorMode.value === 'edit'
    if (editorTab.value === 'form') {
      editorIssues.value = validateForm(fields.value, editorValues.value)
      if (Object.keys(editorIssues.value).length) return null
      if (!editing) return [formToRow(fields.value, editorValues.value)]
      const changed = fields.value.filter(field => editorValues.value[field.name] !== editorOriginal.value[field.name])
      return [formToRow(changed, editorValues.value, false)]
    }
    const parsed = parseJsonRows()
    if (!parsed) return null
    if (!editing) return parsed.map(row => jsonToRow(fields.value, row))
    if (parsed.length !== 1) { jsonError.value = t('source.data.formSingleRow'); return null }
    const original = formToJson(fields.value, editorOriginal.value)
    const changed = Object.fromEntries(Object.entries(parsed[0]).filter(([name, value]) => JSON.stringify(value) !== JSON.stringify(original[name])))
    return [jsonToRow(fields.value, changed)]
  }
  async function submitEditor() {
    if (saving.value) return
    const rowsToSave = editorRows()
    if (!rowsToSave) return
    const [source, database, table] = [route.params.source, route.params.database, route.params.table].map(String)
    const target = editor.target.value
    if (editorMode.value === 'edit') {
      if (!target || !Object.keys(rowsToSave[0]).length) { closeEditor(); return }
      await mutate(MetadataService.updateData(source, database, table, { preview: false, rows: [{ row: rowsToSave[0], original: plain(target) }] }), () => closeEditor(true))
      return
    }
    await mutate(MetadataService.insertData(source, database, table, { preview: false, rows: rowsToSave }), () => {
      // "Continue adding" keeps the panel open with a blank form for the next row.
      if (continueAdding.value && editorMode.value === 'add') {
        editorValues.value = rowToForm(fields.value, null)
        editorJson.value = JSON.stringify(formToJson(fields.value, editorValues.value), null, 2)
        editorIssues.value = {}
      } else closeEditor(true)
    })
  }
  async function mutate(request: Promise<any>, onSuccess: () => void) {
    const key = identity()
    saving.value = true
    try {
      assertSuccess(await request)
      if (!alive || identity() !== key) return
      MfMessage.success(t('source.tip.updateSuccess'))
      onSuccess()
      await load()
    } catch (error) {
      if (alive && identity() === key) MfMessage.error(errorMessage(error))
    } finally {
      saving.value = false
    }
  }

  // Delete and copy go through confirm dialogs that preview the generated SQL.
  const requestDelete = (targets: DataRow[]) => { if (targets.length && can('data:delete')) deleteTargets.value = targets }
  const requestCopy = () => { if (selectedRows.value.length && can('data:insert')) copyTargets.value = selectedRows.value }
  const deleteRows = computed(() => deleteTargets.value.map(plain))
  const copyRows = computed(() => copyTargets.value.map(plain))
  function closeDelete() { deleteTargets.value = []; load() }
  function closeCopy() { copyTargets.value = []; load() }

  // Export the rows on screen with the visible columns.
  function exportRows(format: 'csv' | 'json') {
    const data = (selectedRows.value.length ? selectedRows.value : rows.value).map(plain)
    const content = format === 'csv' ? '﻿' + toCsv(visibleColumns.value, data) : toJson(visibleColumns.value, data)
    const url = URL.createObjectURL(new Blob([content], { type: format === 'csv' ? 'text/csv;charset=utf-8' : 'application/json' }))
    const link = document.createElement('a')
    link.href = url
    link.download = `${route.params.table}.${format}`
    link.click()
    URL.revokeObjectURL(url)
  }

  watch(editor.visible, value => { fieldPanelOpen.value = value }, { immediate: true })
  watch(identity, () => {
    closeEditor(true)
    hidden.value = new Set()
    order.value = null
    conditions.value = []
    addCondition()
    applied = []
    page.value = 1
    load()
  }, { immediate: true })
  onScopeDispose(() => { alive = false; revision++; fieldPanelOpen.value = false })

  return {
    can, loading, saving, rows, total, page, size, sql, sqlVisible, columnNames, visibleColumns, hidden, order, typeOf,
    conditions, addCondition, changeColumn, changeOperator, removeCondition, clearConditions, onSearch, onReset,
    onPageChange, setOrder, toggleColumn, selected, selectedRows, allSelected, partialSelected, toggleAll, onRowClick, onCheck, rowClass,
    editorVisible: editor.visible, editorMode, editorValues, editorTab, editorJson, editorIssues, jsonError, continueAdding, fields,
    openEditor, closeEditor, setEditorTab, submitEditor, fieldPanelHost, fieldPanelWidth,
    deleteTargets, deleteRows, requestDelete, closeDelete, copyTargets, copyRows, requestCopy, closeCopy, exportRows, refresh: load,
  }
}
