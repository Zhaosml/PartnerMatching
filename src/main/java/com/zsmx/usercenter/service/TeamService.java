package com.zsmx.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zsmx.usercenter.model.Team;
import com.zsmx.usercenter.model.User;

/**
* @author ikun
* @description 针对表【team(队伍表 )】的数据库操作Service
* @createDate 2024-02-15 14:32:12
*/
public interface TeamService extends IService<Team> {
    /**
     * 创建队伍
     * @param team
     * @param loginUser
     * @return
     */

    long addTeam(Team team, User loginUser);
}
