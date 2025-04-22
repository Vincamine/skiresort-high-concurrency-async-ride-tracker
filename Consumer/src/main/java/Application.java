import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Application {

  public static void main(String[] args) throws Exception {
    // Configure the RabbitMQ connection factory
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(Config.HOST);
    factory.setPort(Config.PORT);
    factory.setUsername(Config.USERNAME);
    factory.setPassword(Config.PASSWORD);

    // Set up a fixed thread pool with the specified number of threads
    ExecutorService service = Executors.newFixedThreadPool(Config.THREAD_COUNT);
    MessageHandler messageHandler = new MessageHandler();

    // Establish a connection to RabbitMQ
    try (Connection connection = factory.newConnection()) {
      System.out.println("Consumer started!");

      // Submit consumer tasks to the thread pool
      for (int i = 0; i < Config.THREAD_COUNT; i++) {
        service.submit(() -> {
          try {
            Channel channel = connection.createChannel();
            MessageConsumer messageConsumer = new MessageConsumer(channel, messageHandler);
            messageConsumer.startConsuming();
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
      }
      service.awaitTermination(600, TimeUnit.SECONDS);
    }

  }
}
