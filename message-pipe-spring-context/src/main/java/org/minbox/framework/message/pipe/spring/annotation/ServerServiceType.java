package org.minbox.framework.message.pipe.spring.annotation;

import lombok.Getter;
import org.minbox.framework.message.pipe.client.registrar.RegistrarService;

/**
 * Corresponding to the type definition of {@link RegistrarService}
 *
 * @author 恒宇少年
 * @see org.minbox.framework.message.pipe.client.registrar.support.GRpcRegistrarService
 * @see org.minbox.framework.message.pipe.client.registrar.support.NacosRegistrarService
 */
@Getter
public enum ServerServiceType {
    /**
     * Use grpc register to server
     */
    GRPC,
    /**
     * Use nacos client register to server
     */
    NACOS;
}
