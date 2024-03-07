package com.zsmx.usercenter.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class WebSocketVo implements Serializable {
    private static final long serialVersionUID = -3510278262450115509L;
    private long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 添加的好友
     */
    private String userIds;
}
