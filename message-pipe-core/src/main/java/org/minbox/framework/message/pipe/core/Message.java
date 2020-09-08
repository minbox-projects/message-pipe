package org.minbox.framework.message.pipe.core;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Message entity in the pipeline
 *
 * @author 恒宇少年
 */
@Getter
@NoArgsConstructor
public class Message implements Serializable {
    private static final String DEFAULT_ENCODING = Charset.defaultCharset().name();
    private String bodyEncoding;
    private byte[] body;
    private Map<String, Object> metadata = new HashMap<>();

    public Message(byte[] body) {
        this(body, DEFAULT_ENCODING);
    }

    public Message(byte[] body, String bodyEncoding) {
        this.bodyEncoding = bodyEncoding;
        this.body = body;
    }
}
