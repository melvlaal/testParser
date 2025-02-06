package org.example.models.matches;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchEvent {
    private long id;
    private String name;
    private long kickoff;
}

