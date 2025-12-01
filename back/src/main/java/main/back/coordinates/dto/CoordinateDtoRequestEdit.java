package main.back.coordinates.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoordinateDtoRequestEdit {
    @NotNull
    private Integer id;
    @NotNull
    private Double x;
    @NotNull
    @Min(-791)
    private Double y;
    @NotNull
    private String owner;
}