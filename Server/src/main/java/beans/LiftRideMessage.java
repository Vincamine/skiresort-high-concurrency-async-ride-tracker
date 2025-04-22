package beans;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LiftRideMessage {
    private Integer resortID;
    private Integer seasonID;
    private Integer dayID;
    private Integer skierID;
    private LiftRide liftRide;
}
