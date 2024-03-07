package com.zsmx.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zsmx.usercenter.model.Team;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.dto.TeamQuery;
import com.zsmx.usercenter.model.request.TeamJoinRequest;
import com.zsmx.usercenter.model.request.TeamQuitRequest;
import com.zsmx.usercenter.model.request.TeamUpdateRequest;
import com.zsmx.usercenter.model.vo.TeamUserVO;
import com.zsmx.usercenter.model.vo.UserVO;

import java.util.List;

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

    /**
     * 搜索队伍
     * @param teamQuery
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 修改队伍
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param userLogin
     * @return
     */
    boolean joinTime(TeamJoinRequest teamJoinRequest, User userLogin);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 解散队伍
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteTeam(long id, User loginUser);
    /**
     * 获得团队
     *
     * @param teamId 团队id
     * @param userId 用户id
     * @return
     */
    TeamUserVO getTeam(Long teamId,  Long userId);
    /**
     * 获取团队成员
     *
     * @param teamId 团队id
     * @return {@link List}<{@link UserVO}>
     */
    List<UserVO> getTeamMember(Long teamId);
    /**
     * 列出我所有加入
     *
     * @param id id
     * @return {@link List}<{@link TeamUserVO}>
     */
    List<TeamUserVO> listAllMyJoin(long id);


    /**
     * 删除过期队伍
     */

//    void deleteDissolveTeam();
}
