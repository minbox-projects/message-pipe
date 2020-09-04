package org.minbox.framework.message.pipe.core.transport;

import org.minbox.framework.sequence.Sequence;

/**
 * Use minbox {@link Sequence} to generate request id
 * <pre>
 *     &#64;Bean
 *     public RequestIdSequenceGenerator requestIdGenerator(){
 *         return new RequestIdSequenceGenerator();
 *     }
 * </pre>
 *
 * @author 恒宇少年
 */
public class RequestIdSequenceGenerator implements RequestIdGenerator {
    private static final long DATA_CENTER_ID = 1L;
    private Sequence sequence;

    public RequestIdSequenceGenerator() {
        this.sequence = new Sequence(DATA_CENTER_ID);
    }

    @Override
    public String generate() {
        return String.valueOf(sequence.nextId());
    }
}
