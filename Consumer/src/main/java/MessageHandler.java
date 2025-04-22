import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class MessageHandler {

  public void processMessage(String message) {
    Gson gson = new Gson();
    LiftRideMessage liftRideMessage = gson.fromJson(message, LiftRideMessage.class);

    int skierID = liftRideMessage.getSkierID();
    int liftID = liftRideMessage.getLiftRide().getLiftID();
    int vertical = liftID * 10;
    String dayID = String.valueOf(liftRideMessage.getDayID());
    String seasonID = String.valueOf(liftRideMessage.getSeasonID());
    int resortID = liftRideMessage.getResortID();

    try (Jedis jedis = RedisClient.getPool().getResource()) {
      Pipeline pipeline = jedis.pipelined(); // Start the pipeline

      // Batch Redis commands
      String resortKey = "resort:" + resortID + ":season:" + seasonID + ":day:" + dayID + ":skiers";
      pipeline.sadd(resortKey, String.valueOf(skierID));

      String skierDayKey = "resort:" + resortID + ":season:" + seasonID + ":day:" + dayID + ":skier:" + skierID;
      pipeline.hincrBy(skierDayKey, "vertical", vertical);

      String skierVerticalKey = "resort:" + resortID + ":skier:" + skierID + ":vertical";
      pipeline.hincrBy(skierVerticalKey, seasonID, vertical);
      pipeline.hincrBy(skierVerticalKey, "all", vertical);


//      String skierLiftsKey = "resort:" + resortID + ":season:" + seasonID + ":day:" + dayID + ":lifts:" + skierID;
//      pipeline.sadd(skierLiftsKey, String.valueOf(liftID));
//
//      String skierDaysKey = "skier:" + skierID + ":season:" + seasonID + ":days";
//      pipeline.sadd(skierDaysKey, String.valueOf(dayID));

      System.out.println("Queued Redis write:" + liftID);

      // Execute all queued commands in the pipeline
      pipeline.sync();
      System.out.println("Redis pipeline execution successful.");
    } catch (Exception e) {
      System.err.println("Redis pipeline execution failed: " + e.getMessage());
    }
  }
}
