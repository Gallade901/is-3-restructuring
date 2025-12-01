package main.back.coordinates.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import main.back.person.model.Person;
import main.back.user.model.User;

import java.util.List;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
public class Coordinates {
    public Coordinates(Double x, Double y, User user, List<Person> people) {
        this.x = x;
        this.y = y;
        this.user = user;
        this.people = people;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotNull
    private Double x;

    @NotNull
    @Min(-791)
    private Double y;

    @OneToMany(mappedBy = "coordinates")
    private List<Person> people;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

