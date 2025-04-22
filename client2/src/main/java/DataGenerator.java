import lombok.Getter;
import models.LiftEvent;
import io.swagger.client.model.LiftRide;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * DataGenerator is for generating lift ride events for skiers.
 * The generated events are placed into a BlockingQueue for further processing.
 */
public class DataGenerator implements Runnable {

    private static final int SKIER_MAX_ID = 100000;
    private static final int RESORT_MAX_ID = 10;
    private static final int LIFT_MAX_ID = 40;
    private static final int SEASON_ID = 2024;
    private static final int DAY_ID = 1;
    private static final int TIME_MAX = 360;
    private final int liftEventNum;

    @Getter
    private final BlockingQueue<LiftEvent> generatedItems;

    /**
     * Constructor to initialize the DataGenerator with the number of events to generate.
     *
     * @param liftEventNum Number of lift events to generate
     */
    public DataGenerator(int liftEventNum) {
        this.liftEventNum = liftEventNum;
        this.generatedItems = new ArrayBlockingQueue<>(liftEventNum);
    }

    /**
     * The run method generates the specified number of lift events and places them
     * in the BlockingQueue. It is called when the thread starts.
     */
    @Override
    public void run() {
        for (int i = 0; i < liftEventNum; i++) {
            try {
                LiftEvent event = generateLiftEvent();
                generatedItems.put(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Data generation interrupted: " + e.getMessage());
            }
        }
    }

    /**
     * Generates a single LiftEvent object with random attributes.
     *
     * @return A new LiftEvent object
     */
    private LiftEvent generateLiftEvent() {
        int liftID = ThreadLocalRandom.current().nextInt(1, LIFT_MAX_ID + 1);
        int time = ThreadLocalRandom.current().nextInt(1, TIME_MAX + 1);
        LiftRide liftRide = new LiftRide();
        liftRide.setTime(time);
        liftRide.setLiftID(liftID);

        int skierID = ThreadLocalRandom.current().nextInt(1, SKIER_MAX_ID + 1);
        int resortID = ThreadLocalRandom.current().nextInt(1, RESORT_MAX_ID + 1);
        return new LiftEvent(liftRide, skierID, resortID, SEASON_ID, DAY_ID);
    }
}
