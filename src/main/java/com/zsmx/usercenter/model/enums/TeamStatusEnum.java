package com.zsmx.usercenter.model.enums;
public enum TeamStatusEnum {
    PUBLIC(0,"公开"),
    PRIVATE(1,"私有的"),
    SECRET(2, "加密");

    private int value;
    private String test;

    public static TeamStatusEnum getTeamStatusEnum(Integer value){
        if(value == null){
            return null;
        }
        //获取 TeamStatusEnum 枚举中所有可能的枚举值。
        TeamStatusEnum[] values = TeamStatusEnum.values();
        //对 TeamStatusEnum 中的每个枚举值进行迭代。
        for(TeamStatusEnum teamStatusEnum : values){
            //对比枚举值的整数表示与传入的整数值是否相等。
            if(teamStatusEnum.getValue() == value){
                //如果相等，则表示找到了对应的枚举值，直接返回该枚举值。
                return teamStatusEnum;
            }
        }
        return null;
    }
    TeamStatusEnum(int value, String test) {
        this.value = value;
        this.test = test;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }
}
