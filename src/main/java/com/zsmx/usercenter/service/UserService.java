package com.zsmx.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zsmx.usercenter.model.Team;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.request.UserQueryRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
    long userRegister(String username,String userAccount,String userPassword,String checkPassword );

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

    /**
     * 根据标签搜索用户
     * @param tagNameList
     * @return
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 更新用户信息
     * @param user
     * @return
     */
    int updateUser(User user,User loginUser);

    /**
     * 获取当前登录用户信息
     * @return
     */
     User getLoginUser(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
     boolean isAdmin(HttpServletRequest request);

    boolean isAdmin(User loginUser);
    /**
     * 匹配用户
     * @param num
     * @param loginUser
     * @return
     */
    List<User> matchUsers(long num, User loginUser);

    List<User> tags(List<String> tagList);


    /**
     * 根据id获取好友列表
     *
     * @param currentUser
     * @return
     */
    List<User> getFriendsById(User currentUser);

    /**
     * 添加好友
     *
     * @param userLogin
     * @param id
     * @return
     */
    boolean addUser(User userLogin, Long id);

    /**
     * 删除好友
     *
     * @param currentUser
     * @param id
     * @return
     */
    boolean deleteFriend(User currentUser, Long id);

    /**
     * 搜索好友
     *
     * @param userQueryRequest
     * @param currentUser
     * @return
     */
    List<User> searchFriend(UserQueryRequest userQueryRequest, User currentUser);

    /**
     * 添加标签
     */
    boolean addTags(User loginUser, String tags);

    boolean isFriend(Long id, User loginUser);
}
