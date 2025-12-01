package main.back.person.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import main.back.person.model.Color;
import main.back.person.model.Country;
import main.back.person.validation.UniqueName;


import java.time.LocalDateTime;

@Getter
@Setter
public class PersonDtoRequest {
    @NotBlank
    @UniqueName
    private String name;

    private Integer coordinatesId = 0;

    @NotNull
    private Double coordinateX;

    @NotNull
    @Min(-791)
    private Double coordinateY;

    private Integer locationId = 0;

    @NotNull
    private Float locationX;

    private double locationY;

    private int locationZ;

    private Color eyeColor;

    @NotNull
    private Color hairColor;

    @Min(1)
    private long height;

    @NotNull
    private LocalDateTime birthday;

    @Min(1)
    private Integer weight;

    private Country nationality;

    @NotBlank
    private String login;
}