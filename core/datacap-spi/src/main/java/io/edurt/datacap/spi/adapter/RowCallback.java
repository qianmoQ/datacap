package io.edurt.datacap.spi.adapter;

import java.util.List;

public interface RowCallback
{
    /**
     * 在第一行之前回调一次，传入列元数据。默认空实现。
     * Called once before any row, carrying the column metadata of the source result.
     */
    default void onSchema(List<String> headers, List<String> types) {}

    /**
     * 每一行回调一次。
     * Called per row. Values are aligned to {@code headers} from {@link #onSchema}.
     */
    void onRow(List<Object> row);
}
