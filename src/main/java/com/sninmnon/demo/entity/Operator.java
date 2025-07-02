package com.sninmnon.demo.entity;

import lombok.Data;

import java.util.Date;
@Data
public class Operator {
    private Long id;
    private String name;
    private String role;
    private Integer rarity;
    private String gender;
    private String faction;
    private String position;
    private Date release;
}
