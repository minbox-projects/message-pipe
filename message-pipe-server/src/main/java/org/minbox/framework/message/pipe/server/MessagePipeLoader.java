package org.minbox.framework.message.pipe.server;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.server.manager.MessagePipeManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.ObjectUtils;

import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Load all message pipelines at startup
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipeLoader implements InitializingBean {
    /**
     * The name of {@link MessagePipeLoader}
     */
    public static final String BEAN_NAME = "messagePipeLoader";
    private static final String ALL_PATTERN = "*";
    private static final String PIPE_NAME_PATTERN = "(.*?).queue";
    private RedisTemplate redisTemplate;
    private RedisSerializer sourceKeySerializer;
    private MessagePipeManager messagePipeManager;

    public MessagePipeLoader(RedisTemplate redisTemplate, MessagePipeManager messagePipeManager) {
        this.redisTemplate = redisTemplate;
        this.messagePipeManager = messagePipeManager;
        if (this.redisTemplate.getKeySerializer() != null) {
            this.sourceKeySerializer = this.redisTemplate.getKeySerializer();
        }
        redisTemplate.setKeySerializer(new StringRedisSerializer());
    }

    /**
     * loading message pipes
     */
    private void loadPipes() {
        String allKeyPattern = LockNames.MESSAGE_QUEUE.format(ALL_PATTERN);
        Set keySet = redisTemplate.keys(allKeyPattern);
        if (ObjectUtils.isEmpty(keySet)) {
            return;
        }
        log.info("Loading message pipes from redis，size：{}.", keySet.size());
        Iterator iterator = keySet.iterator();
        while (iterator.hasNext()) {
            try {
                String pipeKey = String.valueOf(iterator.next());
                Pattern pipeKeyPattern = Pattern.compile(PIPE_NAME_PATTERN);
                Matcher matcher = pipeKeyPattern.matcher(pipeKey);
                if (matcher.find()) {
                    String pipeName = matcher.group(1);
                    // Create Message Pipe
                    messagePipeManager.createMessagePipe(pipeName);
                    log.debug("Message Pipe：{}，Create successfully.", pipeName);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Load message pipes
        this.loadPipes();
        // Restore configured KeySerializer
        this.redisTemplate.setKeySerializer(this.sourceKeySerializer);
    }
}
