package com.zsmx.usercenter.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessageVo implements Serializable {
    private static final long serialVersionUID = 8594295339609130386L;
    private WebSocketVo formUser;
    private WebSocketVo toUser;
    private Long teamId;
    private String text;


    //11day
    private Boolean isMy = false;
    private Integer chatType;
    private Boolean isAdmin = false;
    private String createTime;
}
