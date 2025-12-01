package main.back.location.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LocationDtoResponse {
    @NotNull
    private Integer id;
    @NotNull
    private Float x;
    private double y;
    private int z;
    @NotNull
    private String owner;
}