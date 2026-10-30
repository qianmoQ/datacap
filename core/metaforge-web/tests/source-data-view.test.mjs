import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-data-view.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { INDEX_COLUMN, defaultColumnView, readColumnView } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const fields = [{ name: 'id', kind: 'number' }, { name: 'name', kind: 'text' }, { name: 'created_at', kind: 'datetime' }]

test('defaults put the pinned row number first and size columns by type', () => {
  const view = defaultColumnView(fields)
  assert.deepEqual(view.columns.map(column => [column.name, column.width, column.align, column.fixed]),
    [[INDEX_COLUMN, 60, 'center', true], ['id', 100, 'center', false], ['name', 160, 'left', false], ['created_at', 170, 'center', false]])
  assert.equal(view.stickyHeader, true)
})

test('saved views keep their order, drop removed columns and append new ones', () => {
  const saved = JSON.stringify({ columns: [{ name: 'name', visible: false, width: 9999, align: 'right', fixed: true }, { name: 'gone' }, { name: 'id', width: 'x' }], bordered: true })
  const view = readColumnView(saved, fields)
  assert.deepEqual(view.columns.map(column => column.name), ['name', 'id', INDEX_COLUMN, 'created_at'])
  assert.deepEqual(view.columns[0], { name: 'name', visible: false, width: 800, align: 'right', fixed: true })
  assert.equal(view.columns[1].width, 100)
  assert.equal(view.bordered, true)
  assert.deepEqual(readColumnView('not json', fields), defaultColumnView(fields))
})
