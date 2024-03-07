package com.zsmx.usercenter.ws;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;

import com.zsmx.usercenter.config.HttpSessionConfigurator;
import com.zsmx.usercenter.model.Chat;
import com.zsmx.usercenter.model.Team;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.model.request.MessageRequest;
import com.zsmx.usercenter.model.vo.MessageVo;
import com.zsmx.usercenter.model.vo.WebSocketVo;
import com.zsmx.usercenter.service.ChatService;
import com.zsmx.usercenter.service.TeamService;
import com.zsmx.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;


import static com.zsmx.usercenter.constant.ChatConstant.*;
import static com.zsmx.usercenter.constant.UserConstant.ADMIN_ROLE;
import static com.zsmx.usercenter.constant.UserConstant.USER_LOGIN_STATE;
import static com.zsmx.usercenter.utils.StringUtils.stringJsonListToLongSet;
/**
 * WebSocket服务端
 */
@Component
@Slf4j
@ServerEndpoint(value = "/websocket/{userId}/{teamId}",configurator = HttpSessionConfigurator.class) // WebSocket端点
public class WebSocket {

    /**
     * 保存队伍的连接信息
     */
    private static final Map<String, ConcurrentHashMap<String, WebSocket>> ROOMS = new HashMap<>();
    /**
     * 线程安全的无序的集合
     */
    private static final CopyOnWriteArraySet<Session> SESSIONS = new CopyOnWriteArraySet<>();
    /**
     * 存储在线连接数
     */
    private static final Map<String, Session> SESSION_POOL = new HashMap<>(0);
    private static UserService userService;
    private static ChatService chatService;
    private static TeamService teamService;
    /**
     * 房间在线人数
     */
    private static int onlineCount = 0;
    /**
     * 当前信息
     */
    private Session session;
    private HttpSession httpSession;

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocket.onlineCount--;
    }

    @Resource
    public void setHeatMapService(UserService userService) {
        WebSocket.userService = userService;
    }

    @Resource
    public void setHeatMapService(ChatService chatService) {
        WebSocket.chatService = chatService;
    }

    @Resource
    public void setHeatMapService(TeamService teamService) {
        WebSocket.teamService = teamService;
    }


    /**
     * 队伍内群发消息
     *
     * @param teamId
     * @param msg
     * @throws Exception
     */
    public static void broadcast(String teamId, String msg) throws Exception {
        ConcurrentHashMap<String, WebSocket> map = ROOMS.get(teamId);
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

    /**
     * 发送消息
     *
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId, @PathParam(value = "teamId") String teamId, EndpointConfig config) {
        try {
            // 检查用户ID是否为空或未定义
            if (StringUtils.isBlank(userId) || "undefined".equals(userId)) {
                // 如果是，则发送参数错误的错误消息给用户
                sendError(userId, "参数有误");
                return;
            }

            HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
            User user = (User) httpSession.getAttribute(USER_LOGIN_STATE);

            if (user != null) {
                this.session = session;
                this.httpSession = httpSession;
            }
            if (!"NaN".equals(teamId)) {
                if (!ROOMS.containsKey(teamId)) {
                    ConcurrentHashMap<String, WebSocket> room = new ConcurrentHashMap<>();
                    room.put(userId, this);
                    ROOMS.put(String.valueOf(teamId), room);
                    // 在线数加1
                    addOnlineCount();
                } else {
                    // 房间已存在，直接添加用户到相应的房间
                    if (!ROOMS.get(teamId).containsKey(userId)) {
                        ROOMS.get(teamId).put(userId, this);
                        // 在线数加1
                        addOnlineCount();
                    }
                }
            } else {
                // 1. 添加新会话到集合中
                SESSIONS.add(session);
                // 2. 将用户ID和会话存入会话池
                SESSION_POOL.put(userId, session);
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
                ROOMS.remove(teamId).remove(userId);
                if (getOnlineCount() > 0) {
                    subOnlineCount();
                }
                log.info("用户退出:当前在线人数为:" + getOnlineCount());
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
            sendOneMessage(userId,"pong");
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
        User fromUser = userService.getById(userId);
        Team team = teamService.getById(teamId);
        // 如果是私聊消息
        if (chatType == PRIVATE_CHAT) {
            // 私聊
            privateChat(fromUser, toId, text, chatType);
        } else if (chatType == TEAM_CHAT) {
            // 队伍内聊天
            teamChat(fromUser, text, team, chatType);
        } else {
            // 群聊
            hallChat(fromUser, text, chatType);
        }
    }



    /**
     * 队伍聊天
     *
     * @param user
     * @param text
     * @param team
     * @param chatType
     */
    private void teamChat(User user, String text, Team team, Integer chatType) {
        MessageVo messageVo = new MessageVo();
        WebSocketVo fromWebSocketVo = new WebSocketVo();

        BeanUtils.copyProperties(user, fromWebSocketVo);
        messageVo.setFormUser(fromWebSocketVo);

        messageVo.setText(text);
        messageVo.setTeamId(team.getId());
        messageVo.setChatType(chatType);
        messageVo.setCreateTime(DateUtil.format(new Date(), "yyyy年MM月dd日 HH:mm:ss"));

        if (user.getId() == team.getUserId() || user.getUserRole() == ADMIN_ROLE) {
            messageVo.setIsAdmin(true);
        }
        User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
        if (loginUser.getId() == user.getId()) {
            messageVo.setIsMy(true);
        }
        String toJson = new Gson().toJson(messageVo);
        try {
            broadcast(String.valueOf(team.getId()), toJson);
            savaChat(user.getId(), null, text, team.getId(), chatType);
            chatService.deleteKey(CACHE_CHAT_TEAM, String.valueOf(team.getId()));
            log.error("队伍聊天，发送给={},队伍={},在线:{}人", user.getId(), team.getId(), getOnlineCount());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 大厅聊天
     *
     * @param user
     * @param text
     */
    private void hallChat(User user, String text, Integer chatType) {
        MessageVo messageVo = new MessageVo();
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(user, fromWebSocketVo);
        messageVo.setFormUser(fromWebSocketVo);
        messageVo.setText(text);
        messageVo.setChatType(chatType);
        messageVo.setCreateTime(DateUtil.format(new Date(), "yyyy年MM月dd日 HH:mm:ss"));
        if (user.getUserRole() == ADMIN_ROLE) {
            messageVo.setIsAdmin(true);
        }
        User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
        if (loginUser.getId() == user.getId()) {
            messageVo.setIsMy(true);
        }
        String toJson = new Gson().toJson(messageVo);
        sendAllMessage(toJson);
        savaChat(user.getId(), null, text, null, chatType);
        chatService.deleteKey(CACHE_CHAT_HALL, String.valueOf(user.getId()));
    }

    /**
     * 私人聊天
     *
     * @param user     使用者
     * @param toId     至id
     * @param text     文本
     * @param chatType 聊天类型
     */
    private void privateChat(User user, Long toId, String text, Integer chatType) {
        Session toSession = SESSION_POOL.get(toId.toString());
        if (toSession != null) {
            MessageVo messageVo = chatService.chatResult(user.getId(), toId, text, chatType, DateUtil.date(System.currentTimeMillis()));
            User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
            if (loginUser.getId() == user.getId()) {
                messageVo.setIsMy(true);
            }
            String toJson = new Gson().toJson(messageVo);
            sendOneMessage(toId.toString(), toJson);
            log.info("发送给用户username={}，消息：{}", messageVo.getToUser(), toJson);
        } else {
            log.info("用户不在线username={}的session", toId);
        }
        savaChat(user.getId(), toId, text, null, chatType);
        chatService.deleteKey(CACHE_CHAT_PRIVATE, user.getId() + "" + toId);
        chatService.deleteKey(CACHE_CHAT_PRIVATE, toId + "" + user.getId());
    }

    /**
     * 保存聊天
     *
     * @param userId
     * @param toId
     * @param text
     */
    private void savaChat(Long userId, Long toId, String text, Long teamId, Integer chatType) {
//        if (chatType == PRIVATE_CHAT) {
//            User user = userService.getById(userId);
//            Set<Long> userIds = stringJsonListToLongSet(user.getUserIds());
//            if (!userIds.contains(toId)) {
//                sendError(String.valueOf(userId), "该用户不是你的好友");
//                return;
//            }
//        }
        Chat chat = new Chat();
        chat.setFromId(userId);
        chat.setText(String.valueOf(text));
        chat.setChatType(chatType);
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
        for (Session userSession : SESSIONS) {
            try {
                if (userSession.isOpen()) {
                    synchronized (userSession) {
                        userSession.getBasicRemote().sendText(message);
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
        HashMap<String, List<WebSocketVo>> stringListHashMap = new HashMap<>(0);
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
