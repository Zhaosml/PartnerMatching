package com.zsmx.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户退出队伍请求体
 */
@Data
public class TeamQuitRequest implements Serializable {
    private static final long serialVersionUID = -5990617424872616459L;
    private Long teamId;
    private String password;
}
