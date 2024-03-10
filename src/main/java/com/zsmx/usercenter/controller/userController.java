package com.zsmx.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zsmx.usercenter.common.BaseResponse;
import com.zsmx.usercenter.common.ErrorCode;
import com.zsmx.usercenter.common.ResultUtils;
import com.zsmx.usercenter.exception.BusinessException;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.request.UserLoginRequest;
import com.zsmx.usercenter.model.request.UserQueryRequest;
import com.zsmx.usercenter.model.request.UserRegisterRequest;
import com.zsmx.usercenter.service.UserService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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
@Api(tags = "用户接口管理")
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
    @ApiOperation(value = "用户注册",notes = "注册  注册用户，账号不能重复 最低四位，密码最低8位，确认密码和密码相同")
    //todo 账号不能重复注册 待测试
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 校验 账户、密码、校验密码 是否为空
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取用户注册请求中的用户名、账户、密码、校验密码
        String username = userRegisterRequest.getUsername();
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        // 检查用户名、账户、密码、校验密码是否有任何一个为空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }

        // 调用 userService 的用户注册方法，获取注册结果
        long result = userService.userRegister(username,userAccount, userPassword, checkPassword);

        // 返回成功响应并携带注册结果
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
    @ApiOperation(value = "用户登录",notes = "登录  登录账号，账号 最低四位，密码最低8位，确认密码和密码相同")
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
    @ApiOperation(value = "用户注销",notes = "个人中心-退出登录 注销用户，并且删除 '登录态'")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前用户数据
     *
     * @param request
     * @return
     */
    @GetMapping("/current")
    @ApiOperation(value = "获取当前用户数据",notes = "全局 查询当前登录用户id的全部数据（进行了用户脱敏）")
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

    /**
     * 根据用户名，简介 搜索用户
     * @param username
     * @param request
     * @return
     */
    @GetMapping("/search")
    @ApiOperation(value = "条件查询用户",notes = "首页-顶部 根据用户名，简介 搜索用户（需要登录）")
    public BaseResponse<List<User>> searchUsers(String username,String profile, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long id = loginUser.getId();
        if (id>0 && id == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        if(StringUtils.isNotBlank(profile)){
            queryWrapper.like("profile",profile);
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
    @ApiOperation(value = "查询用户(分页)" ,notes = "首页-查询用户 查询用户数据并且保存到redis")
    public BaseResponse<Page<User>> recommendUsers(long pageNum,long pageSize,HttpServletRequest request) {
        // 1. 获取当前登录用户信息
        User loginUser = userService.getLoginUser(request);
        // 2. 将当前登录用户信息的id保存为redis的key
        String key = String.format("zsmx:user:recommend:%s", loginUser.getId());
        // 3. redis String类型的操作
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 4. 如果有缓存，直接读缓存
        Page<User> page = (Page<User>) valueOperations.get(key);
        // 判空，如果不为空则返回成功
        if(page != null){
            return ResultUtils.success(page);
        }
        // 5. 如果没缓存，查询数据库
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

    /**
     * 修改用户信息
     * @param user
     * @param request
     * @return
     */
    @PostMapping("/update")
    @ApiOperation(value = "修改当前用户信息",notes = "修改当前用户信息")
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

    /**
     * 删除指定用户
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @ApiOperation(value = "删除指定用户" ,notes = "根据id删除用户")
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

    /**
     * 根据指定id查询用户信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "根据指定id查询用户信息" ,notes = "根据指定id查询用户信息")
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
    //todo 待分页 默认30条
    @GetMapping("/match")
    @ApiOperation(value = "心动模式匹配",notes = "首页-心动匹配  使用编辑距离算法，查询与自己标签相似度优先级最高的用户")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUsers(num,user));
    }

    /**
     * 添加标签
     */
    @PostMapping("addTags")
    @ApiOperation(value = "添加-标签",notes = "个人页面-修改信息  在类型为数组的标签字段末尾进行追加标签，如果数组为空则是添加数组元素为一 的标签")
    public BaseResponse<Boolean> addTags(@RequestBody String tags, HttpServletRequest request){
        if(tags == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = userService.addTags(loginUser,tags);
        return  ResultUtils.success(result);
    }

    /**
     * 根据id获取好友列表
     * @param request
     * @return
     */
    @GetMapping("/friends")
    @ApiOperation(value ="根据id获取好友列表")
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
    @ApiOperation(value = "查询是否为好友", notes = "查询是否为好友，判断查看的用户是否在userIds好友列表里，是就是true，不是就是flase")
    public BaseResponse<Boolean> isFriend(@PathVariable("id") Long id,HttpServletRequest request) {
        if(id<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        User loginUser = userService.getLoginUser(request);
        boolean friend = userService.isFriend(id, loginUser);

        return ResultUtils.success(friend);

    }



}
