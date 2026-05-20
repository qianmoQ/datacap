package io.edurt.datacap.spi.adapter;

import java.util.List;

public interface BatchWriter
        extends AutoCloseable
{
    void addRow(List<?> row);

    long writtenCount();

    @Override
    void close();
}
