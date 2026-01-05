package com.berdachuk.expertmatch.query.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that SGR pattern combinations are valid.
 * Rules:
 * - Cascade and Cycle patterns are mutually exclusive (Cascade requires 1 expert, Cycle requires >1 expert)
 * - Routing pattern can be used with either Cascade or Cycle
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

