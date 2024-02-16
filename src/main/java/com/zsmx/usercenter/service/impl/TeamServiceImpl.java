package com.zsmx.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zsmx.usercenter.common.ErrorCode;
import com.zsmx.usercenter.exception.BusinessException;
import com.zsmx.usercenter.mapper.TeamMapper;
import com.zsmx.usercenter.model.Team;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.UserTeam;
import com.zsmx.usercenter.model.enums.TeamStatusEnum;
import com.zsmx.usercenter.service.TeamService;
import com.zsmx.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

/**
* @author ikun
* @description 针对表【team(队伍表 )】的数据库操作Service实现
* @createDate 2024-02-15 14:32:12
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Resource
    private UserTeamService userTeamService;
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
}




