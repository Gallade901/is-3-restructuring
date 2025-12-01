package main.back.person.validation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import main.back.person.model.Person;

@ApplicationScoped
public class UniquePersonNameValidator implements ConstraintValidator<UniqueName, String> {

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {

        if (name == null || name.trim().isEmpty()) {
            return true; // Let @NotBlank handle empty values
        }
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(p) FROM Person p WHERE p.name = :name", Long.class);
        query.setParameter("name", name.trim());
        Long count = query.getSingleResult();
        return count == 0;

    }
}