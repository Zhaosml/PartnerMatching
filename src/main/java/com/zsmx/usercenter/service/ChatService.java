package com.zsmx.usercenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zsmx.usercenter.model.Chat;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.request.ChatRequest;
import com.zsmx.usercenter.model.vo.MessageVo;

import java.util.List;

/**
* @author ikun
* @description 针对表【chat(聊天消息表)】的数据库操作Service
* @createDate 2024-02-29 21:42:58
*/
public interface ChatService extends IService<Chat> {
    /**
     * 获取私聊聊天内容
     *
     * @param chatRequest
     * @param chatType
     * @param loginUser
     * @return
     */
    List<MessageVo> getPrivateChat(ChatRequest chatRequest, int chatType, User loginUser);

    /**
     * 获取大厅聊天纪录
     *
     * @param chatType
     * @param loginUser
     * @return
     */
    List<MessageVo> getHallChat(int chatType, User loginUser);

    /**
     * 聊天记录映射
     *
     * @param fromId
     * @param toId
     * @param text
     * @return
     */
    MessageVo chatResult(Long fromId, Long toId, String text,Integer chatType);

    /**
     * 队伍聊天室
     *
     * @param chatRequest
     * @param chatType
     * @param loginUser
     * @return
     */
    List<MessageVo> getTeamChat(ChatRequest chatRequest, int chatType, User loginUser);

    /**
     * 消息处理
     *
     * @param userId
     * @param chatLambdaQueryWrapper
     * @return
     */
    List<MessageVo> returnMessage(Long userId, LambdaQueryWrapper<Chat> chatLambdaQueryWrapper);

}
