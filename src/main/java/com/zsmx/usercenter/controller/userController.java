package com.zsmx.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zsmx.usercenter.common.BaseResponse;
import com.zsmx.usercenter.common.ErrorCode;
import com.zsmx.usercenter.common.ResultUtils;
import com.zsmx.usercenter.exception.BusinessException;
import com.zsmx.usercenter.model.Team;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.request.UserLoginRequest;
import com.zsmx.usercenter.model.request.UserQueryRequest;
import com.zsmx.usercenter.model.request.UserRegisterRequest;
import com.zsmx.usercenter.model.vo.UserVO;
import com.zsmx.usercenter.service.UserService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.jws.soap.SOAPBinding;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zsmx.usercenter.constant.UserConstant.USER_LOGIN_STATE;


/**
 *
 * 用户接口
 *
 *
 * @author xiaopang
 */
@RestController
@RequestMapping("/user")
//@CrossOrigin( origins = "http://localhost:5173", allowCredentials = "true",allowedHeaders = "true")
@Slf4j
public class userController {

    @Autowired
    private UserService userService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 校验 账户、密码、校验密码 是否为空
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String username = userRegisterRequest.getUsername();
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userRegister(username,userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前用户
     *
     * @param request
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        // TODO 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    // https://yupi.icu/

    /**
     * 搜索用户
     * @param username
     * @param request
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 搜索推荐用户
     * @param pageNum
     * @param pageSize
     * @param request
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageNum,long pageSize,HttpServletRequest request) {
        // 1.获取当前登录用户信息
        User loginUser = userService.getLoginUser(request);
        // 2.将当前登录用户信息的id保存为redis的key
        String key = String.format("zsmx:user:recommend:%s", loginUser.getId());
        // 3.redis String类型的操作
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 4.如果有缓存，直接读缓存
        Page<User> page = (Page<User>) valueOperations.get(key);
        //判空，如果不为空则返回成功
        if(page != null){
            return ResultUtils.success(page);
        }
        // 5.如果没缓存，查询数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        page = userService.page(new Page<>(pageNum,pageSize),queryWrapper);
        // 6.写缓存     30秒一次
        try {
            valueOperations.set(key,page,30000, TimeUnit.MICROSECONDS);
        }
        catch (Exception e){
            log.error("redis set key error", e);
        }
        return ResultUtils.success(page);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user,HttpServletRequest request){
        // 1.校验参数是否为空
        if (user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.校验权限        获取当前登录信息里的登录状态
        User loginUser = userService.getLoginUser(request);

        // 3.触发更新    修改参数
        int result = userService.updateUser(user,loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 搜索标签
     * @param tagNameList
     * @return
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUserbyTags(@RequestParam(required = false) List<String> tagNameList){
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    @GetMapping("/{id}")
    public BaseResponse<User> getUserById(@PathVariable("id") Integer id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = this.userService.getById(id);
        return ResultUtils.success(user);
    }

    /**
     * 获取最匹配的用户
     *
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUsers(num,user));
    }

    @GetMapping("tagss")
    public BaseResponse<List<String>> tagss(HttpServletRequest request){
        User userLogin = userService.getLoginUser(request);
        // 获取用户的标签
        String tags = userLogin.getTags();
        Gson gson = new Gson();
        List<String> userTagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());

        return ResultUtils.success(userTagList);
    }
    /**
     * 添加标签
     */
    @PostMapping("addTags")
    public BaseResponse<Boolean> addTags(@RequestBody String tags, HttpServletRequest request){
        if(tags == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = userService.addTags(loginUser,tags);
        return  ResultUtils.success(result);
    }



    @GetMapping("/friends")
    public BaseResponse<List<User>> getFriends(HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        List<User> getUser = userService.getFriendsById(currentUser);
        return ResultUtils.success(getUser);
    }

    @PostMapping("/addUser/{id}")
    public BaseResponse<Boolean> addUser(@PathVariable("id") Long id, HttpServletRequest request) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        User userLogin = userService.getLoginUser(request);
        boolean addUser = userService.addUser(userLogin, id);
        return ResultUtils.success(addUser);
    }

    @PostMapping("/deleteFriend/{id}")
    public BaseResponse<Boolean> deleteFriend(@PathVariable("id") Long id, HttpServletRequest request) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "好友不存在");
        }
        User currentUser = userService.getLoginUser(request);
        boolean deleteFriend = userService.deleteFriend(currentUser, id);
        return ResultUtils.success(deleteFriend);
    }

    @PostMapping("/searchFriend")
    public BaseResponse<List<User>> searchFriend(@RequestBody UserQueryRequest userQueryRequest, HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        User currentUser = userService.getLoginUser(request);
        List<User> searchFriend = userService.searchFriend(userQueryRequest, currentUser);
        return ResultUtils.success(searchFriend);
    }

    @GetMapping("/isFriend/{id}")
    public BaseResponse<Boolean> isFriend(@PathVariable("id") Long id,HttpServletRequest request) {
        if(id<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        User loginUser = userService.getLoginUser(request);
        boolean friend = userService.isFriend(id, loginUser);

        return ResultUtils.success(friend);

    }



}
