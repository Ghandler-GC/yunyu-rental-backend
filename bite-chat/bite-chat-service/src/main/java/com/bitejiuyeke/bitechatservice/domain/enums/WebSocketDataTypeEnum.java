package com.bitejiuyeke.bitechatservice.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: yibo
 */
@Getter
@AllArgsConstructor
public enum WebSocketDataTypeEnum {

    TEXT("text", "文本消息"),
    HEART_BEAT("heart_beat", "心跳检测"),
    CHAT("chat", "聊天消息");

    private final String type;
    private final String desc;

    public static WebSocketDataTypeEnum getByType(String type) {
        for (WebSocketDataTypeEnum webSocketDataTypeEnum : WebSocketDataTypeEnum.values()) {
            if (webSocketDataTypeEnum.getType().equalsIgnoreCase(type)) {
                return webSocketDataTypeEnum;
            }
        }
        return null;
    }

}