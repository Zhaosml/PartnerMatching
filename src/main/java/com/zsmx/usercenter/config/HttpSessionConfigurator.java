package com.zsmx.usercenter.config;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class HttpSessionConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // 1. 获取HttpSession对象
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        // 2. 将Httpsession对象保存起来
        if(httpSession != null){
            sec.getUserProperties().put(HttpSession.class.getName(),httpSession);
        }
        super.modifyHandshake(sec, request, response);
    }
}
