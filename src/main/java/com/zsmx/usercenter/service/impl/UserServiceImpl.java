package com.zsmx.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zsmx.usercenter.common.ErrorCode;
import com.zsmx.usercenter.constant.UserConstant;
import com.zsmx.usercenter.exception.BusinessException;
import com.zsmx.usercenter.mapper.UserMapper;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zsmx.usercenter.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author ikun
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2024-01-21 10:59:03
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    @Resource
    private UserMapper userMapper;
    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "zsmx";
//    private static final String USER_LOGIN_STATE = "userLoginState";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword,String planetCode) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        // 账户不能包含特殊字符
        String accountRegex = "[`~!#\\$%^&*()+=|{}':;',\\\\\\\\[\\\\\\\\].<>/?~！@#￥%……&*（）9——+|{}【】\\\\\"‘；：”“’。，、？]";

//        String accountRegex = "[`~!#\\$%^&*()+=|{}'Aa:;',\\\\\\\\[\\\\\\\\].<>/?~！@#￥%……&*（）9——+|{}【】\\\\\"‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(accountRegex).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }

        // 密码和校验密码相同
        if(!userPassword.equals(checkPassword)){
            return -1;
        }

        // 账户不能重复
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("userAccount",userAccount);
        // long count = this.count(wrapper);
        long count = userMapper.selectCount(wrapper);
        if(count>0){
            return -1;
        }
        // 星球id不能重复
        wrapper = new QueryWrapper<>();
        wrapper.eq("userAccount",userAccount);
        // long count = this.count(wrapper);
        count = userMapper.selectCount(wrapper);
        if(count>0){
            return -1;
        }
        // 2. 加密
        // final String SALT = "zsmx";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if(!saveResult){
            return -1;
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if(StringUtils.isAllBlank(userAccount,userPassword)){
            return null;
        }
        if(userAccount.length()<4){
            return null;
        }
        if(userPassword.length()<8){
            return null;
        }
        // 账户不能包含特殊字符
        String accountRegex = "[`~!#\\$%^&*()+=|{}'Aa:;',\\\\\\\\[\\\\\\\\].<>/?~！@#￥%……&*（）9——+|{}【】\\\\\"‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(accountRegex).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }

        // 2. 加密
        // final String SALT = "zsmx";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在     这里会不会查出删除状态1的数据
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("userAccount",userAccount);
        wrapper.eq("userPassword",encryptPassword);
        User user = userMapper.selectOne(wrapper);
        // 用户不存在
        if(user == null){
            log.info("user login failed，userAccount cannot match userPassword");
            return null;
        }
        //  3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录状态
        request.getSession().setAttribute(USER_LOGIN_STATE,safetyUser);
        return safetyUser;

    }

    /**
     * 用户脱敏
     * @param user 用户信息
     * @return  脱敏后的用户信息
     */
    @Override
    public User getSafetyUser(User user){
        if(user==null){
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUsername(user.getUsername());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setAvatarUrl(user.getAvatarUrl());
        safetyUser.setGender(user.getGender());
        // safetyUser.setUserPassword("");
        safetyUser.setPhone(user.getPhone());
        safetyUser.setEmail(user.getEmail());
        safetyUser.setUserRole(user.getUserRole());
        safetyUser.setUserStatus(user.getUserStatus());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setPlanetCode(user.getPlanetCode());
        // safetyUser.setUpdateTime(new Date());
        // safetyUser.setIsDelete(0);
        return safetyUser;
    }

    /**
     * 注销用户
     * @param request  登录态
     * @return 1
     */
    @Override
    public int userLogout(HttpServletRequest request){
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }
}




