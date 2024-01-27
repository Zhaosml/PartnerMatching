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
    private String userAccount;
    private String userPassword;
    private String checkPassword;
    private String planetCode;

}
