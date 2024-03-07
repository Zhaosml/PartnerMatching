package com.zsmx.usercenter.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zsmx.usercenter.constant.ChatConstant.CACHE_CHAT_HALL;
import static com.zsmx.usercenter.constant.ChatConstant.CACHE_CHAT_PRIVATE;

/**
* @author ikun
* @description 针对表【chat(聊天消息表)】的数据库操作Service实现
* @createDate 2024-02-29 21:42:58
*/
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat>
    implements ChatService {

    @Resource
    private RedisTemplate<String, List<MessageVo>> redisTemplate;
    @Resource
    private UserService userService;

    public List<MessageVo> getPrivateChat(ChatRequest chatRequest, int chatType, User loginUser) {
        Long toId = chatRequest.getToId();
        if (toId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态异常请重试");
        }
        // 获取缓存
        List<MessageVo> chatRecords = getCache(CACHE_CHAT_PRIVATE, loginUser.getId() + "" + toId);
        if (chatRecords != null) {
            saveCache(CACHE_CHAT_PRIVATE, loginUser.getId() + "" + toId, chatRecords);
            return chatRecords;
        }

        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.
                and(privateChat -> privateChat
                                .eq(Chat::getFromId, loginUser.getId())
                        .eq(Chat::getToId, toId)
                        .or().
                        eq(Chat::getToId, loginUser.getId())
                        .eq(Chat::getFromId, toId)
                        ).eq(Chat::getChatType, chatType);
        // 两方共有聊天

        List<Chat> list = this.list(chatLambdaQueryWrapper);

        List<MessageVo> messageVoList = list.stream().map(chat -> {
            MessageVo messageVo = chatResult(loginUser.getId(), toId, chat.getText(), chatType, chat.getCreateTime());
            if (chat.getFromId().equals(loginUser.getId())) {
                messageVo.setIsMy(true);
            }
            return messageVo;
        }).collect(Collectors.toList());
        saveCache(CACHE_CHAT_PRIVATE, loginUser.getId() + "" + toId, messageVoList);
        return messageVoList;
    }


    /**
     * 保存缓存
     *
     * @param redisKey
     * @param id
     * @param messageVos
     */
    @Override
    public void saveCache(String redisKey, String id, List<MessageVo> messageVos) {
        try {
            ValueOperations<String, List<MessageVo>> valueOperations = redisTemplate.opsForValue();
            // 解决缓存雪崩
            int i = RandomUtil.randomInt(2, 3);
            if (redisKey.equals(CACHE_CHAT_HALL)) {
                valueOperations.set(redisKey, messageVos, 2 + i / 10, TimeUnit.MINUTES);
            } else {
                valueOperations.set(redisKey + id, messageVos, 2 + i / 10, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("redis set key error");
        }
    }

    /**
     * 获取缓存
     *
     * @param redisKey
     * @param id
     * @return
     */
    @Override
    public List<MessageVo> getCache(String redisKey, String id) {
        ValueOperations<String, List<MessageVo>> valueOperations = redisTemplate.opsForValue();
        List<MessageVo> chatRecords;
        if (redisKey.equals(CACHE_CHAT_HALL)) {
            chatRecords = valueOperations.get(redisKey);
        } else {
            chatRecords = valueOperations.get(redisKey + id);
        }
        return chatRecords;
    }

    @Override
    public MessageVo chatResult(Long userId, Long toId, String text, Integer chatType, Date createTime) {
        MessageVo messageVo = new MessageVo();
        User fromUser = userService.getById(userId);
        User toUser = userService.getById(toId);

        WebSocketVo fromWebSocketVo = new WebSocketVo();
        WebSocketVo toWebSocketVo = new WebSocketVo();

        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        BeanUtils.copyProperties(toUser, toWebSocketVo);

        messageVo.setFormUser(fromWebSocketVo);
        messageVo.setToUser(toWebSocketVo);
        messageVo.setText(text);
        messageVo.setCreateTime(DateUtil.format(createTime, "yyyy年MM月dd日 HH:mm:ss"));
        messageVo.setChatType(chatType);
        return messageVo;
    }
    @Override
    public List<MessageVo> getTeamChat(ChatRequest chatRequest, int chatType, User loginUser) {
        Long teamId = chatRequest.getTeamId();
        if (teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.eq(Chat::getChatType, chatType).eq(Chat::getTeamId, teamId);
        return returnMessage(loginUser.getId(), chatLambdaQueryWrapper);
    }

    /**
     * 获取大厅聊天纪录
     * @param chatType
     * @param loginUser
     * @return
     */
    @Override
    public List<MessageVo> getHallChat(int chatType, User loginUser) {
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.eq(Chat::getChatType, chatType);
        return returnMessage(loginUser.getId(), chatLambdaQueryWrapper);
    }

    /**
     * 消息处理
     * @param userId
     * @param chatLambdaQueryWrapper
     * @return
     */
    @Override
    public List<MessageVo> returnMessage(Long userId, LambdaQueryWrapper<Chat> chatLambdaQueryWrapper) {
        List<Chat> chatList = this.list(chatLambdaQueryWrapper);
        return chatList.stream().map(chat -> {
            MessageVo messageVo = chatResult(chat.getFromId(), chat.getText());
            if (chat.getFromId().equals(userId)) {
                messageVo.setIsMy(true);
            }
            return messageVo;
        }).collect(Collectors.toList());
    }

    /**
     * Vo映射
     *
     * @param userId
     * @param text
     * @return
     */
    public MessageVo chatResult(Long userId, String text) {
        MessageVo messageVo = new MessageVo();
        User fromUser = userService.getById(userId);
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        messageVo.setFormUser(fromWebSocketVo);
        messageVo.setText(text);
        return messageVo;
    }

    @Override
    public void deleteKey(String key, String id) {
        if (key.equals(CACHE_CHAT_HALL)) {
            redisTemplate.delete(key);
        } else {
            redisTemplate.delete(key + id);
        }
    }


}




