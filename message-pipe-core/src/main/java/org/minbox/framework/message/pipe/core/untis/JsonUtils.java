package org.minbox.framework.message.pipe.core.untis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Json converter utils
 *
 * @author 恒宇少年
 */
public class JsonUtils {
    /**
     * The object convert to json string
     *
     * @param object wait convert object
     * @return json string
     */
    public static String objectToJson(Object object) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * The json string convert to object
     *
     * @param json  wait convert json string
     * @param clazz object type
     * @param <T>
     * @return object instance
     */
    public static <T> T jsonToObject(String json, Class<T> clazz) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
