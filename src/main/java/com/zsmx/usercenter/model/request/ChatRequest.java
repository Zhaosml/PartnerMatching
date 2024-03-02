package com.zsmx.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;
@Data
public class ChatRequest implements Serializable {
    private static final long serialVersionUID = -2549525575099822962L;
    /**
     * 发送消息id
     */
    private Long fromId;

    /**
     * 接收消息id
     */
    private Long toId;
}
