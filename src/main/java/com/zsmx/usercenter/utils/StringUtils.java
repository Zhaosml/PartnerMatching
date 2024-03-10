package com.zsmx.usercenter.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @Author: QiMu
 * @Date: 2023年03月21日 08:02
 * @Version: 1.0
 * @Description: 字符串工具类
 */
public class StringUtils {
    /**
     * 字符串json数组转Long类型set集合
     *
     * @param jsonList
     * @return Set<Long>
     */
    // 将一个字符串类型的 JSON 数组转换为一个长整型的集合
    public static Set<Long> stringJsonListToLongSet(String jsonList) {
        // 使用 Gson 将 JSON 字符串转换为 Set<Long> 类型
        Set<Long> set = new Gson().fromJson(jsonList, new TypeToken<Set<Long>>() {
        }.getType());
        // 使用 Optional 来处理可能为空的情况，如果 set 为 null，则返回一个空的 HashSet
        return Optional.ofNullable(set).orElse(new HashSet<>());
    }
}
