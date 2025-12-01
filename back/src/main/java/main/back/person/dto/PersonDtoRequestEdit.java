package main.back.person.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import main.back.person.model.Color;
import main.back.person.model.Country;
import main.back.person.validation.UniqueName;


import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PersonDtoRequestEdit {
    @NotNull
    private long id;

    @NotBlank
    @UniqueName
    private String name;

    private Integer coordinatesId;

    @NotNull
    private Double coordinateX;

    @NotNull
    @Min(-791)
    private Double coordinateY;

    private Integer locationId;

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
    private String owner;
}