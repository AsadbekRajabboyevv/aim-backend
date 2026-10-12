package com.onlinemenu.setting.domain;

import java.util.function.Predicate;

public enum SettingType {
    STRING(value -> true),
    INTEGER(value -> value.matches("-?\\d{1,9}")),
    BOOLEAN(value -> value.equals("true") || value.equals("false"));

    private final Predicate<String> validator;

    SettingType(Predicate<String> validator) {
        this.validator = validator;
    }

    public boolean isValid(String value) {
        return value != null && validator.test(value);
    }
}
