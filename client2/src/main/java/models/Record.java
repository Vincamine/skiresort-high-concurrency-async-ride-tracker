package models;

import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Record {
    private long startTime;
    private String requestType;
    private long latency;
    private int statusCode;
}
