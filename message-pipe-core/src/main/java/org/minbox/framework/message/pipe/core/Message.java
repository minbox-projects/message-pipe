package org.minbox.framework.message.pipe.core;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Message entity in the pipeline
 *
 * @author 恒宇少年
 */
@Getter
@NoArgsConstructor
@ToString
public class Message implements Serializable {
    private static final String DEFAULT_ENCODING = Charset.defaultCharset().name();
    /**
     * The unique identifier of the message
     */
    private String messageId;
    private String bodyEncoding;
    private byte[] body;
    private final Map<String, Object> metadata = new HashMap<>();

    public Message(byte[] body) {
        this(body, DEFAULT_ENCODING);
    }

    public Message(byte[] body, String bodyEncoding) {
        this.messageId = UUID.randomUUID().toString();
        this.bodyEncoding = bodyEncoding;
        this.body = body;
    }
}
