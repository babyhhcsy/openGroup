package com.thero.worktile.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;

import java.util.List;

public class FastJsonUtil {
    /**
     * fastJOSN 转换对象
     * @param body
     * @param filed
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T parseObject(String body, String filed, Class<T> clazz){
        JSONObject jsonObject = JSON.parseObject(body);
        if(jsonObject==null || !jsonObject.containsKey(filed)){
            return null;
        }
        try {
            return JSON.parseObject(jsonObject.get(filed).toString(), clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将json中的某个数据转化为对象
     * @param body
     * @param filed
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> parseArray(String body, String filed, Class<T> clazz){
        JSONObject jsonObject = JSON.parseObject(body);
        if(jsonObject==null || !jsonObject.containsKey(filed)){
            return null;
        }
        try {
            return JSON.parseArray(jsonObject.get(filed).toString(), clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static <T> List<T> parseArray(String body, Class<T> clazz){
        try {
            return JSON.parseArray(body, clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 字符串转对象
     * @param paramStr
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T parseObject(String paramStr, Class<T> clazz){
        return JSON.parseObject(paramStr,clazz);
    }

    /**
     * 获取json body中的filed内容
     * @param body
     * @param filed
     * @return
     */
    public static final String parseString(String body, String filed){
        JSONObject result = JSONObject.parseObject(body);
        if(result==null || !result.containsKey(filed)){
            return null;
        }
        if(Strings.isNullOrEmpty(filed)){
            return null;
        }
        return result.getString(filed);
    }
}
