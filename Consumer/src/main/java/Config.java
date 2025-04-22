public class Config {
    // RabbitMQ
    public static final String HOST = "44.225.254.46";
    public static final String USERNAME = "admin";
    public static final String PASSWORD = "050709";
    public static final int PORT = 5672;

    // Redis
    public static final String REDIS_HOST = "54.244.202.209";
    public static final int REDIS_PORT = 6379;
    public static final int REDIS_MAX_TOTAL = 100;
    public static final int REDIS_MAX_IDLE = 20;
    public static final int REDIS_MIN_IDLE = 10;

    // Consumer
    public static final int TOTAL_MESSAGES = 200_000;
    public static final int THREAD_COUNT = 8;
}