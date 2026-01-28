package io.edurt.datacap.test.redis;

import io.edurt.datacap.driver.parser.RedisParser;
import io.edurt.datacap.driver.parser.RedisSelectParser;
import io.edurt.datacap.driver.parser.RedisShowParser;
import io.edurt.datacap.sql.statement.ShowStatement;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class RedisParserTest
{
    @Test
    public void testParseShowDatabases()
    {
        RedisParser parser = RedisParser.createParser("SHOW DATABASES");
        assertTrue("Parser should be RedisShowParser", parser instanceof RedisShowParser);
        assertEquals("Show type should be DATABASES", ShowStatement.ShowType.DATABASES, parser.getShowType());
    }

    @Test
    public void testParseShowTables()
    {
        RedisParser parser = RedisParser.createParser("SHOW TABLES");
        assertTrue("Parser should be RedisShowParser", parser instanceof RedisShowParser);
        assertEquals("Show type should be TABLES", ShowStatement.ShowType.TABLES, parser.getShowType());
    }

    @Test
    public void testParseShowTablesWithPattern()
    {
        RedisParser parser = RedisParser.createParser("SHOW TABLES LIKE 'test%'");
        assertTrue("Parser should be RedisShowParser", parser instanceof RedisShowParser);
        RedisShowParser showParser = (RedisShowParser) parser;
        assertNotNull("Pattern should not be null", showParser.getPattern());
        log.info("Pattern: {}", showParser.getPattern());
    }

    @Test
    public void testParseShowColumns()
    {
        RedisParser parser = RedisParser.createParser("SHOW COLUMNS FROM test:hash");
        assertTrue("Parser should be RedisShowParser", parser instanceof RedisShowParser);
        assertEquals("Show type should be COLUMNS", ShowStatement.ShowType.COLUMNS, parser.getShowType());
        assertEquals("Collection should be test:hash", "test:hash", parser.getCollection());
    }

    @Test
    public void testParseSelect()
    {
        RedisParser parser = RedisParser.createParser("SELECT * FROM test:*");
        assertTrue("Parser should be RedisSelectParser", parser instanceof RedisSelectParser);
        assertEquals("Collection should be test:*", "test:*", parser.getCollection());
    }

    @Test
    public void testParseSelectAll()
    {
        RedisParser parser = RedisParser.createParser("SELECT * FROM test");
        assertTrue("Parser should be RedisSelectParser", parser instanceof RedisSelectParser);
        assertEquals("Collection should be test", "test", parser.getCollection());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidSql()
    {
        RedisParser.createParser("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseNullSql()
    {
        RedisParser.createParser(null);
    }
}
