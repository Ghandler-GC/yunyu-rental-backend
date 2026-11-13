package com.bitejiuyeke.bitechatservice.config;


import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

/**
 * 定义将 Java 对象编码为 WebSocket 协议中的文本消息
 */
public class ServerEncoder implements Encoder.Text<Object> {

    @Override
    public void destroy() {
        // 清理资源，如关闭文件等
    }

    @Override
    public void init(EndpointConfig arg0) {
        // 初始化编码器，可以读取配置参数
    }

    @Override
    public String encode(Object obj) {
        return JsonUtil.obj2String(obj);
    }
}