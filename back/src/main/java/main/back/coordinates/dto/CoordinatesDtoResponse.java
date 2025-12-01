package main.back.coordinates.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CoordinatesDtoResponse {
    @NotNull
    private int id;
    @NotNull
    private Double x;
    @Max(965)
    private Double y;
    @NotNull
    private String owner;

}