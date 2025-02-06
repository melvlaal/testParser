package org.example.models.sports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class League {
    private long id;
    private String name;
    @JsonProperty("nameDefault")
    private String nameDefault;
    private String url;
    private int weight;
    private int prematch;
    private int inplay;
    private int outright;
    private boolean top; // Поле "top"
    @JsonProperty("topOrder")
    private int topOrder;
    @JsonProperty("hasZeroMarginEvents")
    private boolean hasZeroMarginEvents;
    @JsonProperty("logoUrl")
    private String logoUrl;
    private Object background;

}
