// Person.java
package main.back.person.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import main.back.coordinates.model.Coordinates;
import main.back.location.model.Location;
import main.back.user.model.User;



import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "person")
public class Person {
    public Person(String name, Coordinates coordinates, Color eyeColor,
                  Color hairColor, Location location, long height,
                  LocalDateTime birthday, Integer weight, Country nationality, User user) {
        this.name = name;
        this.coordinates = coordinates;
        this.eyeColor = eyeColor;
        this.hairColor = hairColor;
        this.location = location;
        this.height = height;
        this.birthday = birthday;
        this.weight = weight;
        this.nationality = nationality;
        this.user = user;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank
    @NotNull
    private String name;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates;

    @Column(updatable = false)
    @NotNull
    private LocalDate creationDate;

    @Enumerated(EnumType.STRING)
    private Color eyeColor;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Color hairColor;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Min(1)
    private long height;

    @NotNull
    private LocalDateTime birthday;

    @Min(1)
    private Integer weight;

    @Enumerated(EnumType.STRING)
    private Country nationality;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        creationDate = LocalDate.now();
    }
}