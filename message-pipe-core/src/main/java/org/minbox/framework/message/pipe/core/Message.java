package org.minbox.framework.message.pipe.core;

import lombok.Getter;

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * Message entity in the pipeline
 *
 * @author 恒宇少年
 */
@Getter
public class Message implements Serializable {
    private static final String DEFAULT_ENCODING = Charset.defaultCharset().name();
    private String bodyEncoding;
    private byte[] body;

    public Message(byte[] body) {
        this(body, DEFAULT_ENCODING);
    }

    public Message(byte[] body, String bodyEncoding) {
        this.bodyEncoding = bodyEncoding;
        this.body = body;
    }
}
