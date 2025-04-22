package models;

import io.swagger.client.model.LiftRide;
import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LiftEvent {

    private LiftRide ride;
    private int skierID;
    private int resortID;
    private int seasonID;
    private int dayID;
}
