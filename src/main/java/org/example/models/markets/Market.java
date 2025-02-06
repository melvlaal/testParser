package org.example.models.markets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Market {
    private long id;
    private String name;
    private List<Runner> runners;


}
