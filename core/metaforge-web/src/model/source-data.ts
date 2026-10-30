export type DataOperator = '=' | '!=' | '>' | '>=' | '<' | '<=' | 'LIKE' | 'IN' | 'NOT IN' | 'BETWEEN' | 'IS NULL' | 'IS NOT NULL'

export interface DataCondition {
  id: number
  column: string
  operator: DataOperator
  /** A string for scalar operators, a list for IN / NOT IN, a [start, end] date pair for BETWEEN. */
  value: string | string[]
}

export interface DataFilter {
  name: string
  operator: string
  value?: unknown
}

export type DataKind = 'date' | 'number' | 'text'

/** API operator names understood by Filter.Operator on the server. */
const API_OPERATORS: Record<Exclude<DataOperator, 'BETWEEN'>, string> = {
  '=': 'EQ', '!=': 'NE', '>': 'GT', '>=': 'GE', '<': 'LT', '<=': 'LE',
  LIKE: 'LIKE', IN: 'IN', 'NOT IN': 'NOT_IN', 'IS NULL': 'IS_NULL', 'IS NOT NULL': 'IS_NOT_NULL',
}

export function dataKind(type: string): DataKind {
  const value = String(type || '').toUpperCase()
  if (/DATE|TIME|YEAR/.test(value)) return 'date'
  if (/INT|DECIMAL|NUMERIC|FLOAT|DOUBLE|REAL|BIT/.test(value)) return 'number'
  return 'text'
}

export function operatorsFor(kind: DataKind): DataOperator[] {
  if (kind === 'date') return ['BETWEEN', '=', '!=', '>', '>=', '<', '<=', 'IS NULL', 'IS NOT NULL']
  if (kind === 'number') return ['=', '!=', '>', '>=', '<', '<=', 'IN', 'NOT IN', 'IS NULL', 'IS NOT NULL']
  return ['=', '!=', 'LIKE', 'IN', 'NOT IN', 'IS NULL', 'IS NOT NULL']
}

export const defaultOperator = (kind: DataKind): DataOperator => kind === 'date' ? 'BETWEEN' : kind === 'text' ? 'LIKE' : '='

export const emptyValue = (operator: DataOperator): string | string[] => operator === 'IN' || operator === 'NOT IN' ? [] : operator === 'BETWEEN' ? ['', ''] : ''

export const needsValue = (operator: DataOperator) => operator !== 'IS NULL' && operator !== 'IS NOT NULL'

/**
 * Turns the filter rows into server filters. Incomplete rows are skipped; BETWEEN becomes an
 * inclusive day range and LIKE matches anywhere unless the user typed their own wildcards.
 */
export function buildDataFilters(conditions: DataCondition[]): DataFilter[] {
  return conditions.flatMap<DataFilter>(condition => {
    const { column: name, operator, value } = condition
    if (!name) return []
    if (!needsValue(operator)) return [{ name, operator: API_OPERATORS[operator as Exclude<DataOperator, 'BETWEEN'>] }]
    if (operator === 'BETWEEN') {
      const [start, end] = Array.isArray(value) ? value : []
      return [
        ...(start ? [{ name, operator: 'GE', value: `${start} 00:00:00` }] : []),
        ...(end ? [{ name, operator: 'LE', value: `${end} 23:59:59` }] : []),
      ]
    }
    if (operator === 'IN' || operator === 'NOT IN') {
      const values = (Array.isArray(value) ? value : []).map(String).filter(item => item !== '')
      return values.length ? [{ name, operator: API_OPERATORS[operator], value: values }] : []
    }
    const text = String(Array.isArray(value) ? '' : value)
    if (text === '') return []
    return [{ name, operator: API_OPERATORS[operator], value: operator === 'LIKE' && !/[%_]/.test(text) ? `%${text}%` : text }]
  })
}

const csvCell = (value: unknown) => {
  if (value === null || value === undefined) return ''
  const text = typeof value === 'object' ? JSON.stringify(value) : String(value)
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
}

export function toCsv(columns: string[], rows: Record<string, unknown>[]): string {
  return [columns.map(csvCell).join(','), ...rows.map(row => columns.map(column => csvCell(row[column])).join(','))].join('\r\n')
}

export function toJson(columns: string[], rows: Record<string, unknown>[]): string {
  return JSON.stringify(rows.map(row => Object.fromEntries(columns.map(column => [column, row[column] ?? null]))), null, 2)
}
