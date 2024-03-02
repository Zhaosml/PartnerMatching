package com.zsmx.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessageRequest implements Serializable {
    private static final long serialVersionUID = -1168539355778140190L;
    private Long toId;
    private Long teamId;
    private String text;
    private Integer chatType;
    private boolean isAdmin;


}
