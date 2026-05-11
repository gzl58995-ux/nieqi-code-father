package com.nieqi.nieqicodefather.model.enums;

public enum UserRoleEnum {

    USER("user"),
    ADMIN("admin");

    private final String value;

    UserRoleEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

