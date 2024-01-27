package com.zsmx.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 *
 * @author xiaopang
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 8123829836548422176L;
    private String userAccount;
    private String userPassword;
}
