package com.zsmx.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zsmx.usercenter.common.ErrorCode;
import com.zsmx.usercenter.exception.BusinessException;
import com.zsmx.usercenter.mapper.TeamMapper;
import com.zsmx.usercenter.model.Team;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.UserTeam;
import com.zsmx.usercenter.model.dto.TeamQuery;
import com.zsmx.usercenter.model.enums.TeamStatusEnum;
import com.zsmx.usercenter.model.request.TeamJoinRequest;
import com.zsmx.usercenter.model.request.TeamQuitRequest;
import com.zsmx.usercenter.model.request.TeamUpdateRequest;
import com.zsmx.usercenter.model.vo.TeamUserVO;
import com.zsmx.usercenter.model.vo.UserVO;
import com.zsmx.usercenter.service.TeamService;
import com.zsmx.usercenter.service.UserService;
import com.zsmx.usercenter.service.UserTeamService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
* @author ikun
* @description 针对表【team(队伍表 )】的数据库操作Service实现
* @createDate 2024-02-15 14:32:12
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Resource
    private TeamService teamService;
    @Resource
    private UserTeamService userTeamService;
    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        // 1. 请求参数是否为空？
        if(team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        // 2. 是否登录，未登录不允许创建
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId = loginUser.getId();
        // 3. 校验信息
        //      1. 队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if(maxNum<1 || maxNum >= 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //      2. 队伍标题 <= 20   ||  为空
        String name = team.getName();
        if (StringUtils.isBlank(name) && name.length() > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //      3. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isBlank(description) || description.length() > 512){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //      4. status 是否公开（int）不传默认为 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getTeamStatusEnum(status);
        if(statusEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍状态不满足要求");
        }
        //      5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if(TeamStatusEnum.SECRET.equals(statusEnum)){
                if(StringUtils.isBlank(password) || password.length() > 32){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码设置不正确");
            }
        }
        //      6. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if(new Date().after(expireTime)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"超时时间 > 当前时间");
        }
        //      7. 校验用户最多创建 5 个队伍
        //todo 有bug，有可能同时创建100个队伍
        QueryWrapper<Team> wrapper = new QueryWrapper<>();
        wrapper.eq("userId", userId);
        long hasTeamNum = this.count(wrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }
        // 4. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if(!result || teamId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"创建队伍失败");
        }
        // 5. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if(!result){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    /**
     * 搜索队伍
     * @param teamQuery
     * @return
     */
    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                //todo 新知识点
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getTeamStatusEnum(status);
            // 显示加密队伍
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }
        // 不展示已过期的队伍
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        // 关联查询创建人的用户信息
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    /**
     * 修改队伍
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if(teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if(id == null && id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        // 只有管理员或者队伍的创建者可以修改
        if(oldTeam.getUserId() != loginUser.getId() && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getTeamStatusEnum(teamUpdateRequest.getStatus());
        //如果队伍是加密状态
        if(teamStatusEnum.equals(TeamStatusEnum.SECRET)){
            //如果密码为空
            if(StringUtils.isBlank(teamUpdateRequest.getPassword())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"创建房间必须要设置密码");
            }
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest,updateTeam);
        return this.updateById(updateTeam);
    }

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param userLogin
     * @return
     */
    @Override
    public boolean joinTime(TeamJoinRequest teamJoinRequest, User userLogin) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //队伍必须存在
        Long teamId = teamJoinRequest.getTeamId();
        if (teamId == null && teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 根据 id 获取队伍信息
        Team team = getTeamById(teamId);
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getTeamStatusEnum(status);
        if (teamStatusEnum.equals(TeamStatusEnum.PRIVATE)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "禁止加入私有的");
        }
        String password = teamJoinRequest.getPassword();
        if (teamStatusEnum.equals(TeamStatusEnum.SECRET)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.NULL_ERROR, "密码错误");
            }
        }
        // 该用户已加入队伍的数量
        long userId = userLogin.getId();

        RLock lock = redissonClient.getLock("zsmx:join_Team");
        try {
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    QueryWrapper<UserTeam> wrapper = new QueryWrapper<>();
                    wrapper.eq("userId", userId);
                    long hasJoinNum = userTeamService.count(wrapper);
                    if (hasJoinNum > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入5个队伍");
                    }
                    // 不能重复加入已加入的队伍
                    wrapper = new QueryWrapper<>();
                    wrapper.eq("userId", userId);
                    wrapper.eq("teamId", teamId);
                    long hasUserJoinTeam = userTeamService.count(wrapper);
                    if (hasUserJoinTeam > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
                    }

                    // 已加入队伍的人数
                    long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
                    if (teamHasJoinNum >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数已满");
                    }
                    //修改队伍信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }

            }
        }
        catch(InterruptedException e){
                log.error("doCacheRecommendUser error", e);
                return false;
            } finally{
                //只能释放自己的锁
                if (lock.isHeldByCurrentThread()) {
                    System.out.println("unLock: " + Thread.currentThread().getId());
                    lock.unlock();
                }
            }
    }


    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if(teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        // 根据 id 获取队伍信息
        Team team = getTeamById(teamId);
        long userId = loginUser.getId();
        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setUserId(userId);
        queryUserTeam.setTeamId(teamId);
        QueryWrapper<UserTeam> wrapper = new QueryWrapper<>(queryUserTeam);
        long count = userTeamService.count(wrapper);
        if(count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未加入队伍");
        }
        // 已加入队伍的人数
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        // 队伍只剩一人，解散
        if (teamHasJoinNum == 1){
            // 删除队伍和所有加入队伍的关系
            this.removeById(teamId);
            // 查询当前队伍的人
            QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.eq("teamId",teamId);
//            return userTeamService.remove(userTeamQueryWrapper);
        }
        else {
            // 是否为队长
            if(team.getUserId() == userId){
                // 把队伍转移最早加入的用户
                // 1. 查询已加入队伍的所有用户和加入时间
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId",teamId);
                // todo QueryWrapper新知识
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if(CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                // 更新当前队伍队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if(!result){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队伍队长失败");
                }
                // 退出队伍，移除关系
//                return userTeamService.remove(wrapper);
            }

        }
        return userTeamService.remove(wrapper);
    }
    /**
     * 解散队伍
     * @param id
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteTeam(long id, User loginUser) {
        // 1. 校验请求参数;
        // 2. 校验队伍是否存在
        Team team = this.getTeamById(id);
        long teamId = team.getId();
        // 3. 校验你是不是队伍的队长
        if(team.getUserId() != loginUser.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH, "无访问权限");
        }
        // 4. 移除所有加入队伍的关联信息
        QueryWrapper<UserTeam> wrapper = new QueryWrapper<>();
        wrapper.eq("teamId",teamId);
        boolean result = userTeamService.remove(wrapper);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 5. 删除队伍
        return this.removeById(teamId);
    }

    /**
     * 根据 id 获取队伍信息
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId) {
        if(teamId == 0 || teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if(team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        return team;
    }

    /**
     * 获取某队伍当前人数
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId){
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId",teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }




}




