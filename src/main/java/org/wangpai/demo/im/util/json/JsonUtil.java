package org.wangpai.demo.im.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {
    /**
     * 此对象的方法是线程安全的
     *
     * @since 2022-1-4
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String pojo2Json(Object obj) throws JsonProcessingException {
        return MAPPER.writeValueAsString(obj);
    }

    public static <T> T json2Pojo(String json, Class<T> realType) throws JsonProcessingException {
        return MAPPER.readValue(json, realType);
    }
}
