package org.example.models.sports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sport {
    private long id;
    private String name;
    private String family;
    private List<Region> regions;
}