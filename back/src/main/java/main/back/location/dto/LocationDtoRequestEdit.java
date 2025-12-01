package main.back.location.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationDtoRequestEdit {
    @NotNull
    private Integer id;

    @NotNull(message = "X cannot be null")
    private Float x;

    private double y;

    private int z;

    @NotNull(message = "Owner cannot be null")
    private String owner;
}