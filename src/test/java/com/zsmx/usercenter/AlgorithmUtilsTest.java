package com.zsmx.usercenter;

import com.zsmx.usercenter.utils.AlgorithmUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

/**
 * 算法工具测试类
 */
@SpringBootTest
public class AlgorithmUtilsTest {
    @Test
    void test() {
        List<String> str1 = Arrays.asList("java", "大一", "男");
        List<String> str2 = Arrays.asList("java", "大二", "女");
        List<String> str3 = Arrays.asList("Python", "大二", "女");
        int score1 = AlgorithmUtils.minDistance(str1, str2);
        int score2 = AlgorithmUtils.minDistance(str1, str3);
        int score3 = AlgorithmUtils.minDistance(str2, str3);
        System.out.println(score1);
        System.out.println(score2);
        System.out.println(score3);
    }
}
