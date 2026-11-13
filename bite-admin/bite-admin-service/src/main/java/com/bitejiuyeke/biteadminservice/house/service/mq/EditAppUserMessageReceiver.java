package com.bitejiuyeke.biteadminservice.house.service.mq;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserDTO;
import com.bitejiuyeke.biteadminservice.house.domain.entity.House;
import com.bitejiuyeke.biteadminservice.house.service.IHouseService;
import com.bitejiuyeke.biteadminservice.user.config.RabbitConfig;
import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: yibo
 */
@Component
@Slf4j
@RabbitListener(bindings = {
        @QueueBinding(
                value = @Queue(),
                exchange = @Exchange(value = RabbitConfig.EXCHANGE_NAME, type = ExchangeTypes.FANOUT,durable = "true", autoDelete = "false"))
})
public class EditAppUserMessageReceiver {
    @Autowired
    private IHouseService houseService;

    @RabbitHandler
    public void process(AppUserDTO appUserDTO) {
        if (null == appUserDTO || null == appUserDTO.getUserId()) {
            log.error("MQ接收到的用户修改消息为空或用户id为空！");
            return;
        }

        log.info("MQ成功接收到消息，message:{}", JsonUtil.obj2String(appUserDTO));

        try {
            // 1. 获取用户下房源id列表
            List<Long> houseIds = houseService.listByUserId(appUserDTO.getUserId());

            // 2. 更新用户下全量房源列表的缓存
            for (Long houseId : houseIds) {
                houseService.cacheHouse(houseId);
            }
        } catch (Exception e) {
            log.error("处理用户更新时，更新房源缓存异常，appUserDTO:{}",
                    JsonUtil.obj2String(appUserDTO), e);
        }

    }

}
