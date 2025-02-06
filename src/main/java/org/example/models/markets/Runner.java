package org.example.models.markets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Runner {
    private long id;
    private String name;
    private double price;
}
