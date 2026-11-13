package com.bitejiuyeke.biteadminapi.house.feign;

import com.bitejiuyeke.biteadminapi.house.domain.dto.SearchHouseListReqDTO;
import com.bitejiuyeke.biteadminapi.house.domain.vo.HouseDetailVO;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author: yibo
 */
@FeignClient(contextId = "houseFeignClient", value = "bite-admin", path = "/house")
public interface HouseFeignClient {

    /**
     * 查询房源列表，支持筛选、排序、翻页
     */
    @PostMapping("/list/search")
    R<BasePageVO<HouseDetailVO>> searchList(@Validated @RequestBody SearchHouseListReqDTO searchHouseListReqDTO);


    /**
     * 查询房源详情（带缓存）
     */
    @GetMapping("/detail")
    R<HouseDetailVO> detail(@RequestParam Long houseId);

}
