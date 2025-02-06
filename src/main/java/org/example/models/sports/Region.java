package org.example.models.sports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Region {
    private long id;
    private String name;
    private List<League> leagues;
}
