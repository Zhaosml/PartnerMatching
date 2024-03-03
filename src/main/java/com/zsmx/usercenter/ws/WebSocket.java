package com.zsmx.usercenter.ws;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;

import com.zsmx.usercenter.model.Chat;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.request.MessageRequest;
import com.zsmx.usercenter.model.vo.MessageVo;
import com.zsmx.usercenter.model.vo.WebSocketVo;
import com.zsmx.usercenter.service.ChatService;
import com.zsmx.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;


import static com.zsmx.usercenter.constant.ChatConstant.PRIVATE_CHAT;
import static com.zsmx.usercenter.constant.ChatConstant.TEAM_CHAT;
import static com.zsmx.usercenter.utils.StringUtils.stringJsonListToLongSet;
/**
 * WebSocket服务端
 */
@Component
@Slf4j
@ServerEndpoint("/websocket/{userId}/{teamId}") // WebSocket端点
public class WebSocket {

    private static UserService userService;
    private static ChatService chatService;

    @Resource
    public void setHeatMapService(UserService userService) {
        WebSocket.userService = userService;
    }

    @Resource
    public void setHeatMapService(ChatService chatService) {
        WebSocket.chatService = chatService;
    }

    /**
     * 线程安全的无序的集合，存储所有会话
     */
    private static final CopyOnWriteArraySet<Session> SESSIONS = new CopyOnWriteArraySet<>();

    /**
     * 存储在线连接数  存储在线用户的会话
     */
    private static final Map<String, Session> SESSION_POOL = new HashMap<>(0);

    /**
     * 保存队伍的连接信息
     */
    private static final Map<String, ConcurrentHashMap<String, WebSocket>> rooms = new ConcurrentHashMap();
    /**
    /**
     * 当WebSocket建立连接成功后会触发这个注解修饰的方法
     * @param session
     * @param userId
     */

    private Session session;

    /**
     * 队伍内群发消息
     *
     * @param teamId
     * @param msg
     * @throws Exception
     */
    public static void broadcast(String teamId, String msg) throws Exception {
        ConcurrentHashMap<String, WebSocket> map = rooms.get(teamId);
        for (String key : map.keySet()) {
            // keySet获取map集合key的集合  然后在遍历key即可
            try {
                WebSocket webSocket = map.get(key);
                webSocket.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId, @PathParam(value = "teamId") String teamId) {
        try {
            // 检查用户ID是否为空或未定义
            if (StringUtils.isBlank(userId) || "undefined".equals(userId)) {
                // 如果是，则发送参数错误的错误消息给用户
                sendError(userId, "参数有误");
                return;
            }
            this.session = session;
            if (!"NaN".equals(teamId)) {
                if (!rooms.containsKey(teamId)) {
                    ConcurrentHashMap<String, WebSocket> room = new ConcurrentHashMap<>();
                    room.put(userId, this);
                    rooms.put(teamId, room);
                } else {
                    // 房间已存在，直接添加用户到相应的房间
                    rooms.get(teamId).put(userId, this);
                }
            } else {
                // 1. 添加新会话到集合中
                SESSIONS.add(session);
                // 2. 将用户ID和会话存入会话池
                SESSION_POOL.put(userId, session);
                log.info("有新用户加入，userId={}, 当前在线人数为：{}", userId, SESSION_POOL.size());
                // 3. 发送所有在线用户信息给客户端
                sendAllUsers();
            }
        } catch (Exception e) {
            // 处理异常情况
            e.printStackTrace();
        }
    }

    /**
     * 发送错误消息给指定用户
     * @param userId 用户ID
     * @param errorMessage 错误消息内容
     */
    private void sendError(String userId, String errorMessage) {
        // 1. 创建一个JSON对象
        JSONObject obj = new JSONObject();
        // 2. 设置JSON对象的"error"字段为错误消息内容
        obj.set("error", errorMessage);
        // 3. 将JSON对象转换为字符串，并通过WebSocket发送给指定用户
        sendOneMessage(userId, obj.toString());
    }

    /**
     * WebSocket连接关闭时执行的方法
     * @param userId 用户ID
     * @param session 关闭的会话对象
     */
    @OnClose
    public void onClose(@PathParam("userId") String userId,@PathParam("teamId") String teamId, Session session) {
        try {
            if (!"NaN".equals(teamId)) {
                rooms.remove(teamId);
            } else {
                // 如果会话池不为空
                if (!SESSION_POOL.isEmpty()) {
                    // 从会话池中移除关闭的会话
                    SESSION_POOL.remove(userId);
                    // 从会话集合中移除关闭的会话
                    SESSIONS.remove(session);
                }
                log.info("【WebSocket消息】连接断开，总数为：" + SESSION_POOL.size());
                sendAllUsers();
            }
            // 发送更新后的所有在线用户信息给客户端
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收客户端发送的消息
     * @param message 客户端发送的消息内容
     * @param userId 发送消息的用户ID
     */
    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        // 如果收到PING消息，则回复pong
        if ("PING".equals(message)) {
            sendAllMessage("pong");
            return;
        }
        // 记录服务端收到的消息
        log.info("服务端收到用户username={}的消息:{}", userId, message);
        // 将收到的消息解析为MessageRequest对象
        MessageRequest messageRequest = new Gson().fromJson(message, MessageRequest.class);
        Long toId = messageRequest.getToId();
        Long teamId = messageRequest.getTeamId();
        String text = messageRequest.getText();
        Integer chatType = messageRequest.getChatType();
        // 如果是私聊消息
        if (chatType == PRIVATE_CHAT) {
            // 私聊
            privateChat(userId, toId, text, chatType);
        } else if (chatType == TEAM_CHAT) {
            // 队伍内聊天
            teamChat(userId, text, teamId, chatType);
        } else {
            // 群聊
            hallChat(userId, text, chatType);
        }
    }


    /**
     * 发送消息
     *
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }
    /**
     * 队伍聊天
     *
     * @param userId
     * @param text
     * @param teamId
     * @param chatType
     */
    private void teamChat(String userId, String text, Long teamId, Integer chatType) {
        savaChat(userId, null, text, teamId, chatType);
        MessageVo messageVo = new MessageVo();
        User fromUser = userService.getById(userId);
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        messageVo.setFormUser(fromWebSocketVo);
        messageVo.setText(text);
        messageVo.setTeamId(teamId);
        String toJson = new Gson().toJson(messageVo);
        try {
            broadcast(String.valueOf(teamId), toJson);
            log.error("队伍聊天，发送给==={}", teamId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 大厅聊天
     *
     * @param userId
     * @param text
     */
    private void hallChat(String userId, String text, Integer chatType) {
        savaChat(userId, null, text, null, chatType);
        MessageVo messageVo = new MessageVo();
        User fromUser = userService.getById(userId);
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        messageVo.setFormUser(fromWebSocketVo);
        messageVo.setText(text);
        messageVo.setChatType(chatType);
        String toJson = new Gson().toJson(messageVo);
        sendAllMessage(toJson);
    }

    /**
     * 私聊
     *
     * @param userId
     * @param text
     */
    private void privateChat(String userId, Long toId, String text, Integer chatType) {
        savaChat(userId, toId, text, null, chatType);
        Session toSession = SESSION_POOL.get(toId.toString());
        if (toSession != null) {
            MessageVo messageVo = chatService.chatResult(Long.parseLong(userId), toId, text,chatType);
            String toJson = new Gson().toJson(messageVo);
            sendOneMessage(toId.toString(), toJson);
            log.info("发送给用户username={}，消息：{}", messageVo.getToUser(), toJson);
        } else {
            log.info("发送失败，未找到用户username={}的session", toId);
        }
    }

    /**
     * 保存聊天
     *
     * @param userId
     * @param toId
     * @param text
     */
    private void savaChat(String userId, Long toId, String text, Long teamId, Integer chatType) {
        if (chatType == PRIVATE_CHAT) {
            User user = userService.getById(userId);
            Set<Long> userIds = stringJsonListToLongSet(user.getUserIds());
            if (!userIds.contains(toId)) {
                sendError(userId, "该用户不是你的好友");
                return;
            }
        }
        Chat chat = new Chat();
        chat.setFromId(Long.parseLong(userId));
        chat.setToId(toId);
        chat.setText(text);
        chat.setChatType(PRIVATE_CHAT);
        chat.setCreateTime(new Date());
        if (toId != null && toId > 0) {
            chat.setToId(toId);
        }
        if (teamId != null && teamId > 0) {
            chat.setTeamId(teamId);
        }
        chatService.save(chat);
    }

    /**
     * 此为广播消息
     *
     * @param message 消息
     */
    public void sendAllMessage(String message) {
        log.info("【WebSocket消息】广播消息：" + message);
        for (Session session : SESSIONS) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.getBasicRemote().sendText(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 此为单点消息
     *
     * @param userId  用户编号
     * @param message 消息
     */
    public void sendOneMessage(String userId, String message) {
        Session session = SESSION_POOL.get(userId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    log.info("【WebSocket消息】单点消息：" + message);
                    session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送所有在线用户信息
     */
    public void sendAllUsers() {
        log.info("【WebSocket消息】发送所有在线用户信息");
        HashMap<String, List<WebSocketVo>> stringListHashMap = new HashMap<>();
        List<WebSocketVo> webSocketVos = new ArrayList<>();
        stringListHashMap.put("users", webSocketVos);
        for (Serializable key : SESSION_POOL.keySet()) {
            User user = userService.getById(key);
            WebSocketVo webSocketVo = new WebSocketVo();
            BeanUtils.copyProperties(user, webSocketVo);
            webSocketVos.add(webSocketVo);
        }
        sendAllMessage(JSONUtil.toJsonStr(stringListHashMap));
    }
}
