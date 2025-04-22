package models;

import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ThroughputData {
    private double timeInSeconds;
    private double throughput;
}
