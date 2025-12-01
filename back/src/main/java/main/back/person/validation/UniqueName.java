package main.back.person.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniquePersonNameValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueName {
    String message() default "Person with this name already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}