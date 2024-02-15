package com.zsmx.usercenter.once;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class DemoData {
    @ExcelProperty("成员编号")
    private String id;
    @ExcelProperty("用户名称")

    private String userName;
}