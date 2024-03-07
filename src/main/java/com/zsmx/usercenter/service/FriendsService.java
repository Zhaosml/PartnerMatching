package com.zsmx.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zsmx.usercenter.model.Friends;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.request.FriendAddRequest;
import com.zsmx.usercenter.model.vo.FriendsRecordVO;

import java.util.List;
import java.util.Set;

/**
* @author ikun
* @description 针对表【friends(好友申请管理表)】的数据库操作Service
* @createDate 2024-03-03 15:30:56
*/
public interface FriendsService extends IService<Friends> {

    /**
     * 好友申请
     *
     * @param loginUser
     * @param friendAddRequest
     * @return
     */
    boolean addFriendRecords(User loginUser, FriendAddRequest friendAddRequest);

    /**
     * 查询出所有申请、同意记录
     *
     * @param loginUser
     * @return
     */
    List<FriendsRecordVO> obtainFriendApplicationRecords(User loginUser);

    /**
     * 同意好友
     *
     * @param loginUser
     * @param fromId
     * @return
     */
    boolean agreeToApply(User loginUser, Long fromId);

    /**
     * 撤销好友申请
     *
     * @param id        申请记录id
     * @param loginUser 登录用户
     * @return
     */
    boolean canceledApply(Long id, User loginUser);

    /**
     * 获取我申请的记录
     *
     * @param loginUser
     * @return
     */
    List<FriendsRecordVO> getMyRecords(User loginUser);

    /**
     * 获取未读记录条数
     *
     * @param loginUser
     * @return
     */
    int getRecordCount(User loginUser);

    /**
     * 读取纪录
     *
     * @param loginUser
     * @param ids
     * @return
     */
    boolean toRead(User loginUser, Set<Long> ids);
}
