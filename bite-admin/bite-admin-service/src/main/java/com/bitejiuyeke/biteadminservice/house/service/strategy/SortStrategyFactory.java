package com.bitejiuyeke.biteadminservice.house.service.strategy;

import com.bitejiuyeke.biteadminservice.house.domain.enums.HouseSortEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 排序策略工厂类
 *
 * @author: yibo
 */
@Slf4j
public class SortStrategyFactory {

    /**
     * 根据排序规则返回对应的排序策略
     */
    public static ISortStrategy getSortStrategy(String sort) {

        if (StringUtils.isNotEmpty(sort)) {
            if (sort.equalsIgnoreCase(HouseSortEnum.DISTANCE.name())) {
                return DistanceSortStrategy.getInstance();
            } else if (sort.equalsIgnoreCase(HouseSortEnum.PRICE_ASC.name())) {
                return PriceSortStrategy.getInstance(true);
            } else if (sort.equalsIgnoreCase(HouseSortEnum.PRICE_DESC.name())) {
                return PriceSortStrategy.getInstance(false);
            } else {
                log.error("不存在的排序规则，将按照距离排序！");
                return DistanceSortStrategy.getInstance();
            }
        }
        return DistanceSortStrategy.getInstance();

        // 问题：高并发场景下，请求多少次，new多少次策略类。
        // 要求：每种策列只需要一个类即可
        // 解决：单例模式
//        if (StringUtils.isNotEmpty(sort)) {
//            if (sort.equalsIgnoreCase(HouseSortEnum.DISTANCE.name())) {
//                return new DistanceSortStrategy();
//            } else if (sort.equalsIgnoreCase(HouseSortEnum.PRICE_ASC.name())) {
//                return new PriceSortStrategy(true);
//            } else if (sort.equalsIgnoreCase(HouseSortEnum.PRICE_DESC.name())) {
//                return new PriceSortStrategy(false);
//            } else {
//                log.error("不存在的排序规则，将按照距离排序！");
//                return new DistanceSortStrategy();
//            }
//        }
//        return new DistanceSortStrategy();
    }
}
