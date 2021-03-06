/*
 * Copyright (c) 2010. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.vaadin.validation;

import com.vaadin.data.validator.IntegerValidator;
import org.apache.commons.lang3.StringUtils;

public class MinMaxIntegerValidator extends IntegerValidator {

    private int myMinValue;
    private int myMaxValue;

    public MinMaxIntegerValidator(String errorMessage, int minValue, int maxValue) {
        super(errorMessage);
        myMinValue = minValue;
        myMaxValue = maxValue;
    }

    @Override
    public boolean isValid(Object value) {
        if (value instanceof Number) {
            long numVal = ((Number)value).longValue();
            return numVal >= myMinValue && numVal <= myMaxValue;
        }
        return super.isValid(value);
    }

    @Override
    protected boolean isValidString(String value) {
        if (StringUtils.isNotBlank(value) && super.isValidString(value)) {
            int intValue = Integer.parseInt(value);
            return intValue >= myMinValue && intValue <= myMaxValue;
        }
        return false;
    }
}
