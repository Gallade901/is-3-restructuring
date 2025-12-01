package main.back.person.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import main.back.person.model.Color;
import main.back.person.model.Country;
import lombok.Getter;
import lombok.Setter;
import main.back.person.validation.UniqueName;

import java.time.LocalDateTime;

@Getter
@Setter
public class PersonDtoImport {
    @NotBlank
    @NotNull
    private String name;

    @NotNull
    private Double coordinateX;

    @NotNull
    @Min(value = -791, message = "Y must be greater than -792")
    private Double coordinateY;

    private Color eyeColor;

    @NotNull
    private Color hairColor;

    @NotNull
    private Float locationX;

    private double locationY;

    private int locationZ;

    @Min(1)
    private long height;

    @NotNull
    private LocalDateTime birthday;

    @Min(1)
    private Integer weight;

    private Country nationality;
}