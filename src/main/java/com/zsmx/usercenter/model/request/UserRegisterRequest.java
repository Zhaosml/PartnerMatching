package com.zsmx.usercenter.model.request;


import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 *
 * @author xiaopang
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = -7052350412788015417L;
    //用户账号
    private String userAccount;
    //用户密码
    private String userPassword;
    //校验密码
    private String checkPassword;
    //星球编号
    private String planetCode;

}
