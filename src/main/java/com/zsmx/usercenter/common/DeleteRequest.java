package com.zsmx.usercenter.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用的删除请求参数
 */
@Data
public class DeleteRequest implements Serializable {
    private static final long serialVersionUID = 3600996588405355580L;
    private long id;
}
