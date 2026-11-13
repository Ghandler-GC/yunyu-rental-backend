package com.bitejiuyeke.biteportalservice.homepage.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: yibo
 */
@Data
public class CityDescVO implements Serializable {
    private Long id;
    private String name;
    private String fullName;
}