package io.edurt.datacap.condor.condition;

import io.edurt.datacap.condor.metadata.RowDefinition;

public class AndCondition
        implements Condition
{
    private final Condition left;
    private final Condition right;

    public AndCondition(Condition left, Condition right)
    {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evaluate(RowDefinition row)
    {
        return left.evaluate(row) && right.evaluate(row);
    }
}
