package main.back.location.model;

import jakarta.persistence.*;
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
public class Location {
    public Location(Float x, double y, int z, User user, List<Person> people) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.user = user;
        this.people = people;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotNull
    private Float x;

    private double y;

    private int z;

    @OneToMany(mappedBy = "location")
    private List<Person> people;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}