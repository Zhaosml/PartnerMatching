package com.zsmx.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
;
import com.zsmx.usercenter.common.ErrorCode;
import com.zsmx.usercenter.exception.BusinessException;
import com.zsmx.usercenter.mapper.ChatMapper;
import com.zsmx.usercenter.model.Chat;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.request.ChatRequest;
import com.zsmx.usercenter.model.vo.MessageVo;
import com.zsmx.usercenter.model.vo.WebSocketVo;
import com.zsmx.usercenter.service.ChatService;
import com.zsmx.usercenter.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author ikun
* @description 针对表【chat(聊天消息表)】的数据库操作Service实现
* @createDate 2024-02-29 21:42:58
*/
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat>
    implements ChatService {

    @Resource
    private UserService userService;

    @Override
    public List<MessageVo> getPrivateChat(ChatRequest chatRequest, Integer chatType, User loginUser) {
        Long fromId = chatRequest.getFromId();
        Long toId = chatRequest.getToId();
        if (fromId == null || toId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态异常请重试");
        }
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.
                and(privateChat -> privateChat.eq(Chat::getFromId, fromId).eq(Chat::getToId, toId)
                        .or().
                        eq(Chat::getToId, fromId).eq(Chat::getFromId, toId)
                ).eq(Chat::getChatType, chatType);
        List<Chat> list = this.list(chatLambdaQueryWrapper);
        return list.stream().map(chat -> {
            MessageVo messageVo = chatResult(fromId, toId, chat.getText());
            if (chat.getFromId().equals(loginUser.getId())) {
                messageVo.setIsMy(true);
            }
            return messageVo;
        }).collect(Collectors.toList());
    }

    @Override
    public MessageVo chatResult(Long fromId, Long toId, String text) {
        MessageVo messageVo = new MessageVo();
        User fromUser = userService.getById(fromId);
        User toUser = userService.getById(toId);
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        WebSocketVo toWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        BeanUtils.copyProperties(toUser, toWebSocketVo);
        messageVo.setFormUser(fromWebSocketVo);
        messageVo.setToUser(toWebSocketVo);
        messageVo.setText(text);
        return messageVo;
    }
}




