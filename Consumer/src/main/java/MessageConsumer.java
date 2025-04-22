import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class MessageConsumer {
  private static final String QUEUE_NAME = "assignment2_queue";

  private final Channel channel;
  private final MessageHandler messageHandler;

  public MessageConsumer(Channel channel, MessageHandler messageHandler) {
    this.channel = channel;
    this.messageHandler = messageHandler;
  }

  public void startConsuming() throws IOException {
    // Declare the queue. If it doesn't exist, it will be created.
    channel.queueDeclare(QUEUE_NAME, false, false, false, null);

    // Begin consuming messages from the queue, with automatic message acknowledgment (auto-ack enabled)
    channel.basicConsume(QUEUE_NAME, true, new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope,
          AMQP.BasicProperties properties, byte[] body) throws IOException {

        // Convert the message content to a string
        String message = new String(body, "UTF-8");

        // Process the message asynchronously using CompletableFuture to handle it in a separate thread
        CompletableFuture.runAsync(() -> {
          try {
            // Pass the received message to the message handler for processing
            messageHandler.processMessage(message);
          } catch (Exception e) {
            System.err.println("Exception occurred during message processing: " + e);
            throw new RuntimeException(e);
          }
        }).exceptionally(ex -> {
          System.err.println("Exception occurred during CompletableFuture processing: " + ex);
          return null;
        });
      }
    });
  }
}
