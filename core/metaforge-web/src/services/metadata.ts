import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import type { NativeRequest } from '@/model/source-native'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/metadata'

class MetadataService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    nativeNamespaces(code: string, request: NativeRequest): Promise<ResponseModel>
    {
        return new HttpUtils().post(`/api/v1/source-management/${ encodeURIComponent(code) }/namespaces`, request)
    }

    nativeBrowse(code: string, request: NativeRequest): Promise<ResponseModel>
    {
        return new HttpUtils().post(`/api/v1/source-management/${ encodeURIComponent(code) }/browse`, request)
    }

    nativeDetail(code: string, request: NativeRequest): Promise<ResponseModel>
    {
        return new HttpUtils().post(`/api/v1/source-management/${ encodeURIComponent(code) }/detail`, request)
    }

    nativeDownload(code: string, request: NativeRequest): Promise<ResponseModel>
    {
        return new HttpUtils().post(`/api/v1/source-management/${ encodeURIComponent(code) }/download`, request)
    }

    getCapabilities(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`/api/v1/source-management/${ encodeURIComponent(code) }/capabilities`)
    }

    getEngines(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/engines`)
    }

    getDataTypes(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/data-types`)
    }

    getDatabaseBySource(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/databases`)
    }

    getTablesByDatabase(code: string, database: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/tables`)
    }

    getColumnsByTable(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/columns`)
    }

    getDatabase(code: string, database: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }`)
    }

    getTable(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }`)
    }

    getTableStatement(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/statement`)
    }

    updateAutoIncrement(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/auto-increment`, configure)
    }

    createTable(code: string, database: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/create-table`, configure)
    }

    dropTable(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/drop-table`, configure)
    }

    truncateTable(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/truncate-table`, configure)
    }

    queryTable(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/query-table`, configure)
    }

    /** Indexes, foreign keys, triggers, views and generated columns that use the given columns. */
    getColumnDependencies(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/column-dependencies`, configure)
    }

    deleteData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/delete-data`, configure)
    }

    updateData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/update-data`, configure)
    }

    exportData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/export-data`, configure)
    }

    createColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/create-column`, configure)
    }

    dropColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/drop-column`, configure)
    }

    getIndexes(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/indexes`)
    }

    getForeignKeys(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/foreign-keys`)
    }

    /** One row per index column, with order, prefix, cardinality, type, comment, key length and usage. */
    getIndexDetails(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/index-details`)
    }

    /** One row per foreign key column of the table, with its reference, rules and whether the referenced table exists. */
    getForeignKeyDetails(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/foreign-key-details`)
    }

    createForeignKey(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/create-foreign-key`, configure)
    }

    /** Changes or rebuilds foreign keys: each one is dropped, then added with the new definition. */
    alterForeignKey(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/alter-foreign-key`, configure)
    }

    dropForeignKey(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/drop-foreign-key`, configure)
    }

    /** Changes or rebuilds indexes: each one is dropped and added again in one statement. */
    alterIndex(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/alter-index`, configure)
    }

    createIndex(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/create-index`, configure)
    }

    dropIndex(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/drop-index`, configure)
    }

    getColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/get-column`, configure)
    }

    changeColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/change-column`, configure)
    }

    insertData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/insert-data`, configure)
    }
}

export default new MetadataService()