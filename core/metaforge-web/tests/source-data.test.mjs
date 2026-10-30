import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-data.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { buildDataFilters, dataKind, defaultOperator, operatorsFor, toCsv, toJson } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('column types pick the filter kind and default operator', () => {
  assert.equal(dataKind('DATETIME'), 'date')
  assert.equal(dataKind('BIGINT'), 'number')
  assert.equal(dataKind('VARCHAR'), 'text')
  assert.equal(defaultOperator('date'), 'BETWEEN')
  assert.equal(defaultOperator('text'), 'LIKE')
  assert.ok(!operatorsFor('number').includes('LIKE'))
})

test('conditions become server filters and incomplete rows are skipped', () => {
  const filters = buildDataFilters([
    { id: 1, column: 'created_at', operator: 'BETWEEN', value: ['2026-10-01', '2026-10-31'] },
    { id: 2, column: 'type', operator: 'IN', value: ['SYSTEM', '', 'AI'] },
    { id: 3, column: 'name', operator: 'LIKE', value: 'flow' },
    { id: 4, column: 'code', operator: 'LIKE', value: 'user_%' },
    { id: 5, column: 'deleted', operator: 'IS NULL', value: '' },
    { id: 6, column: 'status', operator: '=', value: '' },
    { id: 7, column: '', operator: '=', value: 'x' },
    { id: 8, column: 'updated_at', operator: 'BETWEEN', value: ['', '2026-10-31'] },
  ])
  assert.deepEqual(filters, [
    { name: 'created_at', operator: 'GE', value: '2026-10-01 00:00:00' },
    { name: 'created_at', operator: 'LE', value: '2026-10-31 23:59:59' },
    { name: 'type', operator: 'IN', value: ['SYSTEM', 'AI'] },
    { name: 'name', operator: 'LIKE', value: '%flow%' },
    { name: 'code', operator: 'LIKE', value: 'user_%' },
    { name: 'deleted', operator: 'IS_NULL' },
    { name: 'updated_at', operator: 'LE', value: '2026-10-31 23:59:59' },
  ])
})

test('exports keep column order, quote CSV cells and write nulls', () => {
  const rows = [{ id: 1, name: 'a,"b"', note: null }]
  assert.equal(toCsv(['id', 'name', 'note'], rows), 'id,name,note\r\n1,"a,""b""",')
  assert.deepEqual(JSON.parse(toJson(['name', 'id'], rows)), [{ name: 'a,"b"', id: 1 }])
})
