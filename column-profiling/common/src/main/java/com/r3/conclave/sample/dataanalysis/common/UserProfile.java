package com.r3.conclave.sample.dataanalysis.common;

import java.io.Serializable;

public class UserProfile implements Serializable {
    private final String name;
    private final Integer age;
    private final String country;
    private final String gender;

    public UserProfile(String name, Integer age, String country, String gender) {
        this.name = name;
        this.age = age;
        this.country = country;
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public String getCountry() {
        return country;
    }

    public String getGender() {
        return gender;
    }

}

