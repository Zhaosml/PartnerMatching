package com.zsmx.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zsmx.usercenter.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
* @author ikun
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2024-01-21 10:59:03
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount,String userPassword,String checkPassword,String planetCode);

    /**
     * 用户登录
     *
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @return  脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     * @param user 用户信息
     * @return 用户脱敏后的信息
     */
    User getSafetyUser(User user);

    /**
     * 用户注销
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);
}
