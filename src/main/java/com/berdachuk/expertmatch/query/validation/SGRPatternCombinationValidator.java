package com.berdachuk.expertmatch.query.validation;

import com.berdachuk.expertmatch.query.domain.QueryRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates SGR pattern combinations in com.berdachuk.expertmatch.query.domain.QueryRequest.QueryOptions.
 * <p>
 * Rules:
 * - Cascade and Cycle patterns are mutually exclusive
 * (Cascade requires exactly 1 expert, Cycle requires >1 expert)
 * - Routing pattern can be used with either Cascade or Cycle
 */
public class SGRPatternCombinationValidator implements ConstraintValidator<ValidSGRPatternCombination, QueryRequest> {

    @Override
    public void initialize(ValidSGRPatternCombination constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(QueryRequest queryRequest, ConstraintValidatorContext context) {
        if (queryRequest == null || queryRequest.options() == null) {
            return true; // Let other validators handle null checks
        }

        com.berdachuk.expertmatch.query.domain.QueryRequest.QueryOptions options = queryRequest.options();
        Boolean useCascade = options.useCascadePattern();
        Boolean useCycle = options.useCyclePattern();

        // Both are null or false - valid
        if ((useCascade == null || !useCascade) && (useCycle == null || !useCycle)) {
            return true;
        }

        // Both are true - invalid (mutually exclusive)
        if (Boolean.TRUE.equals(useCascade) && Boolean.TRUE.equals(useCycle)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid SGR pattern combination: Cascade and Cycle patterns cannot be enabled simultaneously. " +
                            "Cascade pattern requires exactly 1 expert result, while Cycle pattern requires multiple expert results (>1). " +
                            "Please enable only one of them."
            ).addConstraintViolation();
            return false;
        }

        // Only one is enabled - valid
        return true;
    }
}

