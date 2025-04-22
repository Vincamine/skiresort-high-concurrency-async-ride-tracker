import lombok.Getter;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisClient {
    @Getter
    private static final JedisPool pool;

    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(Config.REDIS_MAX_TOTAL);
        poolConfig.setMaxIdle(Config.REDIS_MAX_IDLE);
        poolConfig.setMinIdle(Config.REDIS_MIN_IDLE);

        // Initialize the pool using Config values
        pool = new JedisPool(poolConfig, Config.REDIS_HOST, Config.REDIS_PORT);
    }
}
