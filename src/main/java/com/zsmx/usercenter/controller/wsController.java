package com.zsmx.usercenter.controller;

import com.zsmx.usercenter.common.BaseResponse;
import com.zsmx.usercenter.common.ResultUtils;
import com.zsmx.usercenter.ws.WebSocket;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.websocket.Session;


@RestController
@RequestMapping("/websocket")
public class wsController {
    @Autowired
    private WebSocket webSocket;

    @ApiOperation(value = "发送广播消息")
    @PostMapping("/broadcast/send")
    public BaseResponse sendBroadcastMessage(@RequestParam String message) {
        webSocket.sendAllMessage(message);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "发送单点消息")
    @PostMapping("/single/send")
    public BaseResponse sendSingleMessage(@RequestParam String userId, @RequestParam String message) {
        webSocket.sendOneMessage(userId, message);
        return ResultUtils.success(null);
    }
    @ApiOperation(value = "发送单点消息")
    @PostMapping("/onOpen")
    public BaseResponse onOpen(@RequestParam String userId, Session session) {
        webSocket.onOpen(session,userId);
        return ResultUtils.success(null);
    }



}
