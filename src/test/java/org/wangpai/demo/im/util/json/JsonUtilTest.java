package org.wangpai.demo.im.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.wangpai.demo.im.protocol.Message;

public class JsonUtilTest {
    public static void main(String[] args) throws JsonProcessingException {
        var message = new Message();
        message.setMsg("123456");

        var json = JsonUtil.pojo2Json(message);
        System.out.println(json);

        var msg = JsonUtil.json2Pojo(json, Message.class);
        System.out.println(msg);
    }
}