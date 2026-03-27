package com.example.demo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class NumberUtils {
    public static String stringDivideFloor(String strNum1, String strNum2) {
        // 转成 BigDecimal（精度最高，不丢精度）
        BigDecimal num1 = new BigDecimal(strNum1);
        BigDecimal num2 = new BigDecimal(strNum2);

        // 1. 相除
        // 2. 向下取整（RoundingMode.FLOOR）
        // 3. 保留 2 位小数
        BigDecimal result = num1.divide(num2, 2, RoundingMode.FLOOR);

        // 格式化为 0.00 格式（确保一定有两位小数）
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(result);
    }

    public static float stringDivideToFloat(String str1, String str2) {
        // 空值/除数为0防护
        if (str1 == null || str2 == null || str2.trim().isEmpty()) {
            return 0.0f;
        }

        BigDecimal dividend = new BigDecimal(str1.trim());
        BigDecimal divisor = new BigDecimal(str2.trim());

        // 除数不能为0
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0f;
        }

        // 核心：相除 + 保留2位小数 + 向下取整
        BigDecimal result = dividend.divide(divisor, 2, RoundingMode.FLOOR);

        // 返回 float
        return result.floatValue();
    }


    /**
     * 两个 String 数字相乘
     * 向下取整，保留两位小数，返回格式化后的字符串
     *
     * @param str1 数字字符串1
     * @param str2 数字字符串2
     * @return 保留两位小数的结果，如 "3.14"、"5.00"
     */
    public static String stringMultiply(String str1, String str2) {
        // 空值防护
        if (str1 == null || str2 == null) {
            return "0.00";
        }

        try {
            BigDecimal num1 = new BigDecimal(str1.trim());
            BigDecimal num2 = new BigDecimal(str2.trim());

            // 相乘
            BigDecimal multiplyResult = num1.multiply(num2);

            // 向下取整，保留 2 位小数
            BigDecimal result = multiplyResult.setScale(2, RoundingMode.FLOOR);

            // 格式化为固定两位小数，确保输出 0.00 格式
            DecimalFormat df = new DecimalFormat("0.00");
            return df.format(result);

        } catch (Exception e) {
            // 格式错误返回默认值
            return "0.00";
        }
    }

    /**
     * 两个 String 数字相乘，保留2位小数，向下取整，返回 float
     */
    public static float stringMultiplyToFloat(String str1, String str2) {
        // 空值判断
        if (str1 == null || str2 == null) {
            return 0.0f;
        }

        try {
            BigDecimal num1 = new BigDecimal(str1.trim());
            BigDecimal num2 = new BigDecimal(str2.trim());

            // 相乘
            BigDecimal result = num1.multiply(num2);

            // 保留2位小数 + 向下取整
            result = result.setScale(2, RoundingMode.FLOOR);

            // 转 float 返回
            return result.floatValue();

        } catch (Exception e) {
            return 0.0f;
        }
    }
}
