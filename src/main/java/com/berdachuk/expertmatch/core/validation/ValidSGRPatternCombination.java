package com.berdachuk.expertmatch.core.validation;

import com.berdachuk.expertmatch.core.validation.impl.SGRPatternCombinationValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates SGR pattern combinations.
 * <p>
 * Rules: Cascade and Cycle patterns are mutually exclusive.
 * Routing pattern can be used with either Cascade or Cycle.
 */
@Documented
@Constraint(validatedBy = SGRPatternCombinationValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSGRPatternCombination {
    String message() default "Invalid SGR pattern combination: Cascade and Cycle patterns cannot be enabled simultaneously. Cascade requires exactly 1 expert result, while Cycle requires multiple expert results (>1).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}