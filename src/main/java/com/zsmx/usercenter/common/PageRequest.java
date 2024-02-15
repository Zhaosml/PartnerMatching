package com.zsmx.usercenter.common;

import lombok.Data;
import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = -2519905813791025787L;

    /**
     * 页面大小
     */
    protected int pageSize = 10;

    /**
     * 当前是第几页
     */
    protected int pageNum = 1;
}
