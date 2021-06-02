package com.r3.conclave.sample.dataanalysis.common;

public class UserProfile {
    private String name;
    private Integer age;
    private String country;
    private String gender;

    public UserProfile(String name, Integer age, String country, String gender){
        this.name = name;
        this.age = age;
        this.country = country;
        this.gender = gender;
    }

    public String getName(){
        return name;
    }

    public Integer getAge(){
        return age;
    }

    public String getCountry(){
        return country;
    }

    public String getGender(){
        return gender;
    }

}
