package io.edurt.datacap.test.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public abstract class RedisJdbcBaseTest
{
    private static final Logger log = LoggerFactory.getLogger(RedisJdbcBaseTest.class);
    @ClassRule
    public static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort()
                    .withStartupTimeout(Duration.ofSeconds(30)));

    protected Statement statement;
    protected Connection connection;
    protected RedissonClient redissonClient;

    @Before
    public void init()
    {
        try {
            initializeRedisData();
            initializeJdbcConnection();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to initialize test environment", e);
        }
    }

    private void initializeRedisData()
    {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(String.format("redis://%s:%d",
                        REDIS_CONTAINER.getHost(),
                        REDIS_CONTAINER.getFirstMappedPort()));
        redissonClient = Redisson.create(config);

        redissonClient.getKeys().flushdb();

        RBucket<String> stringKey = redissonClient.getBucket("test:string");
        stringKey.set("hello world");

        RList<String> listKey = redissonClient.getList("test:list");
        listKey.addAll(List.of("item1", "item2", "item3"));

        RSet<String> setKey = redissonClient.getSet("test:set");
        setKey.addAll(List.of("member1", "member2", "member3"));

        RMap<String, String> hashKey = redissonClient.getMap("test:hash");
        hashKey.put("field1", "value1");
        hashKey.put("field2", "value2");
    }

    private void initializeJdbcConnection()
            throws Exception
    {
        Class.forName("io.edurt.datacap.driver.RedisJdbcDriver");
        Properties props = new Properties();
        props.setProperty("database", "0");

        String jdbcUrl = String.format("jdbc:redis://%s:%d",
                REDIS_CONTAINER.getHost(),
                REDIS_CONTAINER.getFirstMappedPort()
        );
        connection = DriverManager.getConnection(jdbcUrl, props);
        statement = connection.createStatement();
    }

    @After
    public void cleanup()
    {
        try {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (redissonClient != null) {
                redissonClient.shutdown();
            }
        }
        catch (Exception e) {
            log.error("Failed to cleanup test environment", e);
        }
    }

    static {
        REDIS_CONTAINER.setPortBindings(List.of("6379:6379"));
        REDIS_CONTAINER.start();
    }
}
