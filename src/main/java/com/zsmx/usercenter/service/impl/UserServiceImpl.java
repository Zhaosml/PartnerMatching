package com.zsmx.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zsmx.usercenter.common.ErrorCode;
import com.zsmx.usercenter.exception.BusinessException;
import com.zsmx.usercenter.mapper.UserMapper;
import com.zsmx.usercenter.model.Team;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.request.UserQueryRequest;
import com.zsmx.usercenter.service.UserService;
import com.zsmx.usercenter.utils.AlgorithmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.validation.BindException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zsmx.usercenter.constant.UserConstant.ADMIN_ROLE;
import static com.zsmx.usercenter.constant.UserConstant.USER_LOGIN_STATE;
import static com.zsmx.usercenter.utils.StringUtils.stringJsonListToLongSet;

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

    @Override
    public long userRegister(String username, String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 1.1. 账户长度 **不小于** 4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        // 1.2. 密码就 **不小于** 8 位
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 1.3. 账户不能包含特殊字符
        String accountRegex = "[`~!#\\$%^&*()+=|{}':;',\\\\\\\\[\\\\\\\\].<>/?~！@#￥%……&*（）9——+|{}【】\\\\\"‘；：”“’。，、？]";

        Matcher matcher = Pattern.compile(accountRegex).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }

        // 1.5. 密码和校验密码相同
        if(!userPassword.equals(checkPassword)){
            return -1;
        }

        // 1.4. 账户不能重复
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("userAccount",userAccount);
         long count = this.count(wrapper);
        //        long count = userMapper.selectCount(wrapper);
        if(count>0){
            return -1;
        }

        // 2. 加密
        // final String SALT = "zsmx";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUsername(username);
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
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
        String accountRegex = "[`~!#\\$%^&*()+=|{}':;',\\\\\\\\[\\\\\\\\].<>/?~！@#￥%……&*（）9——+|{}【】\\\\\"‘；：”“’。，、？]";
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
            safetyUser.setProfile(user.getProfile());
            safetyUser.setUsername(user.getUsername());
            safetyUser.setUserAccount(user.getUserAccount());
            safetyUser.setAvatarUrl(user.getAvatarUrl());
            safetyUser.setGender(user.getGender());
            safetyUser.setPhone(user.getPhone());
            safetyUser.setEmail(user.getEmail());
            safetyUser.setUserRole(user.getUserRole());
            safetyUser.setUserIds(user.getUserIds());
            safetyUser.setUserStatus(user.getUserStatus());
            safetyUser.setCreateTime(user.getCreateTime());
            safetyUser.setPlanetCode(user.getPlanetCode());
            safetyUser.setTags(user.getTags());
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

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList  用户要拥有的标签
     * @return  s
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList){
        //  1. 判断参数是否为空
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 查询所有的用户
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(wrapper);
        // 使用gson  数组转换为json
        Gson gson = new Gson();
        // 3. 获取用户列表里的标签
        return userList.stream().filter(user -> { String tags = user.getTags();
            // 如果标签为null 或者 是空白字符串 返回false
            if (tags == null || StringUtils.isBlank(tags)) {
                return false;
            }
            // 通过 Gson 将 JSON 字符串 tags 解析为Set<String>对象，其中Set包含了用户的标签信息
            Set<String> tempTagNameSet = gson.fromJson(tags, new TypeToken<Set<String>>(){
                }.getType());
                    // 4. 遍历 tagNameList 中的每个标签
            for (String tagName : tagNameList){
                // 如果当前的标签不在用户的标签集合中
                if(!tempTagNameSet.contains(tagName)){
                    // 返回 false，表示当前用户不符合筛选条件
                    return false;
                }
            }
                    return  true;

            }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 更新用户信息
     * @param user
     * @return
     */
    @Override
    public int updateUser(User user,User loginUser) {
        //判断权限      仅管理员和自己
        long userId = user.getId();
        if(userId<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //如果是管理员，允许更新任何用户
        //如果不是管理员，只允许更新当前信息
        if(!isAdmin(loginUser)&& userId!=loginUser.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(userId);
        if(oldUser == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        return userMapper.updateById(user);
    }

    /**
     * 获取当前登录用户信息
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if(request == null){
            return null;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if(userObj == null){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User) userObj;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        // 仅管理员可查询
        return loginUser != null && loginUser.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public List<User> matchUsers(long num, User loginUser) {
        // 查询 id和标签  标签不能为空
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list(queryWrapper);
        // 获取当前登录用户的标签
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        // 给当前登录用户的string类型的标签 转换成 json类型
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 用户列表的下标 => 相似度
        List<Pair<User, Long>> list = new ArrayList<>();
        // 依次计算所有用户和当前用户的相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            // 无标签或者为当前用户自己     就继续执行
            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()) {
                continue;
            }
            // 将遍历User全部的 userTags 标签转换成json类型
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            // 把计算的分数放到 Pair 列表里
            /**
             * 用户：user 分数：distance
             */
            list.add(new Pair<>(user, distance));
        }
        // 按编辑距离由小到大排序
        List<Pair<User, Long>> topUserPairList =
                // list就是 Pair列表
                list.stream()
                        // getValue 就是每个索引的 分数：distance
                        .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                        .limit(num)
                        .collect(Collectors.toList());
        // 原本顺序的 userId 列表      再遍历一次 Pair 列表 给他新的变量名
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());

        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList);
        // 1, 3, 2
        // User1、User2、User3
        // 1 => User1, 2 => User2, 3 => User3
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));

        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }

    @Override
    public List<User> tags(List<String> tagList) {
        return null;
    }

    @Override
    public List<User> getFriendsById(User currentUser) {
        // 查询当前id的全部数据
        User loginUser = this.getById(currentUser.getId());
        // 全部数据里的好友列表(userIds) 转换成long集合
        Set<Long> friendsId = stringJsonListToLongSet(loginUser.getUserIds());
        return friendsId.stream().map(user -> this.getSafetyUser(this.getById(user))).collect(Collectors.toList());
    }

    @Override
    public boolean addUser(User userLogin, Long id) {
        User loginUser = this.getById(userLogin.getId());

        User friendUser = this.getById(id);

        Set<Long> friendsId = stringJsonListToLongSet(loginUser.getUserIds());
        Set<Long> fid = stringJsonListToLongSet(friendUser.getUserIds());
        // TODO 添加好友需要对方同意
        if (friendsId.contains(id) && fid.contains(loginUser.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复添加好友");
        }
        friendsId.add(id);
        fid.add(loginUser.getId());

        String friends = new Gson().toJson(friendsId);
        String fids = new Gson().toJson(fid);

        loginUser.setUserIds(friends);
        friendUser.setUserIds(fids);
        return this.updateById(loginUser) && this.updateById(friendUser);
    }

    @Override
    public boolean deleteFriend(User currentUser, Long id) {
        User loginUser = this.getById(currentUser.getId());
        User friendUser = this.getById(id);

        Set<Long> friendsId = stringJsonListToLongSet(loginUser.getUserIds());
        Set<Long> fid = stringJsonListToLongSet(friendUser.getUserIds());

        friendsId.remove(id);
        fid.remove(loginUser.getId());

        String friends = new Gson().toJson(friendsId);
        String fids = new Gson().toJson(fid);
        loginUser.setUserIds(friends);
        friendUser.setUserIds(fids);
        return this.updateById(loginUser) && this.updateById(friendUser);
    }

    @Override
    public List<User> searchFriend(UserQueryRequest userQueryRequest, User currentUser) {
        String searchText = userQueryRequest.getSearchText();

        User user = this.getById(currentUser.getId());
        Set<Long> friendsId = stringJsonListToLongSet(user.getUserIds());
        List<User> users = new ArrayList<>();
        friendsId.forEach(id -> {
            User u = this.getById(id);
            if (u.getUsername().contains(searchText)) {
                users.add(u);
            }
        });
        return users;
    }

    /**
     * 添加标签
     */
    @Override
    public boolean addTags(User loginUser, String tags) {
        // 根据当前id用户 查询该id的全部内容
        User userId = this.getById(loginUser.getId());
        // 获取当前用户的标签
        String tag = userId.getTags();

        Gson gson = new Gson();
        Set<String> tagList;

        if (tag == null || tag.isEmpty()) {
            tagList = new HashSet<>(); // 创建新的标签集合
        } else {
            tagList = gson.fromJson(tag, new TypeToken<Set<String>>() {}.getType());
        }
//            Set<String> tagList = gson.fromJson(tag,new TypeToken<Set<String>>() {
//            }.getType());

        if(tagList.size() >= 10){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"最多设置10个标签");
        }

        tagList.add(tags);

        String tagsList = gson.toJson(tagList);

        userId.setTags(tagsList);


        boolean result = this.updateById(userId);
        return result;
    }

    /**
     * 是否为好友
     * @param id
     * @param loginUser
     * @return
     */
    @Override
    public boolean isFriend(Long id, User loginUser) {
        User userId = this.getById(loginUser);
        String userIds = userId.getUserIds();
        if(userIds==null){
            return false;
        }
        Gson gson = new Gson();
        Set<Long> userList = stringJsonListToLongSet(userIds);
//        Set<String> userList = gson.fromJson(userIds,new TypeToken<Set<String>>(){}.getType());
        return (userList.contains(id));

    }
}






