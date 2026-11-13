package com.bitejiuyeke.biteportalservice.homepage.service;

import com.bitejiuyeke.biteportalservice.homepage.domain.dto.CityDescDTO;

import java.util.List;

/**
 * @author: yibo
 */
public interface IRegionService {

    List<CityDescDTO> regionChildren(Long parentId);

}
