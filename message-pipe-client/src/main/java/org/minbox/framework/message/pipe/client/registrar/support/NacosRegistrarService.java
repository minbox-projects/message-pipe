package org.minbox.framework.message.pipe.client.registrar.support;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.client.config.ClientConfiguration;
import org.minbox.framework.message.pipe.client.process.MessageProcessorManager;
import org.minbox.framework.message.pipe.client.registrar.RegistrarService;
import org.minbox.framework.message.pipe.core.PipeConstants;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Register Client to Nacos Server
 *
 * @author 恒宇少年
 */
@Slf4j
public class NacosRegistrarService implements RegistrarService, InitializingBean, DisposableBean, BeanFactoryAware {
    private static final String NACOS_SERVER_ADDRESS_PATTERN = "%s:%d";
    private BeanFactory beanFactory;
    private NamingService namingService;
    private final ClientConfiguration configuration;
    private final String pipeNames;

    public NacosRegistrarService(ClientConfiguration configuration,
                                 MessageProcessorManager messageProcessorManager) {
        this.configuration = configuration;
        this.pipeNames = messageProcessorManager.getBindingPipeNameString();
        if (configuration.getServerPort() <= 0 || configuration.getServerPort() > 65535) {
            throw new MessagePipeException("MessagePipe Server port must be greater than 0 and less than 65535");
        }
        if (ObjectUtils.isEmpty(configuration.getServerAddress())) {
            throw new MessagePipeException("Registration target server address cannot be empty.");
        }
        if (ObjectUtils.isEmpty(this.pipeNames)) {
            throw new MessagePipeException("At least one message pipe is bound.");
        }
    }

    /**
     * Create a new {@link NamingService} instance
     *
     * @param serverAddress The nacos server address
     * @param serverPort    The nacos server port
     * @return The {@link NamingService} new instance
     */
    private NamingService createNamingService(String serverAddress, int serverPort) {
        try {
            String nacoServerAddress = String.format(NACOS_SERVER_ADDRESS_PATTERN, serverAddress, serverPort);
            return NacosFactory.createNamingService(nacoServerAddress);
        } catch (NacosException e) {
            log.error(e.getMessage(), e);
        }
        throw new MessagePipeException("Initializing Nacos NamingService encountered an exception.");
    }

    /**
     * Register client to nacos server
     * <p>
     * If there is an instance of {@link NamingService} in the running project,
     * use it directly, otherwise create a new instance
     *
     * @param serverAddress The server address
     * @param serverPort    The server port
     */
    @Override
    public void register(String serverAddress, int serverPort) {
        try {
            if (this.namingService == null) {
                this.namingService = this.createNamingService(serverAddress, serverPort);
            }
            Instance instance = new Instance();
            instance.setIp(this.configuration.getLocalHost());
            instance.setPort(this.configuration.getLocalPort());
            // metadata map
            Map<String, String> metadata = new HashMap<>();
            metadata.put(PipeConstants.PIPE_NAMES_METADATA_KEY, this.pipeNames);
            instance.setMetadata(metadata);
            // register to nacos server
            this.namingService.registerInstance(PipeConstants.CLIENT_SERVICE_NAME, instance);
            log.info("Current client registered to nacos server successfully.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            this.namingService = this.beanFactory.getBean(NamingService.class);
        } catch (BeansException e) {
            log.warn("No NamingService instance is provided, a new instance will be automatically created for use");
        }
    }

    @Override
    public void destroy() throws Exception {
        this.namingService.deregisterInstance(PipeConstants.CLIENT_SERVICE_NAME, this.configuration.getLocalHost(),
                this.configuration.getLocalPort());
        log.info("The client is successfully offline from the nacos server.");
    }
}
