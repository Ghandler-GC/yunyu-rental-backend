package com.bitejiuyeke.biteportalservice.homepage.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: yibo
 */
@Data
public class DictsVO implements Serializable {
    private Long id;
    private String key;
    private String name;
}