package main.back.location.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationDtoRequest {
    @NotNull(message = "X cannot be null")
    private Float x;

    private double y;

    private int z;

    @NotNull(message = "Login cannot be null")
    private String login;
}