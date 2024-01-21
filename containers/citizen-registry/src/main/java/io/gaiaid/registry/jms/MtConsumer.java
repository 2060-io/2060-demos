package io.gaiaid.registry.jms;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.twentysixty.sa.client.jms.AbstractConsumer;
import io.twentysixty.sa.client.jms.ConsumerInterface;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.res.c.MessageResource;

@ApplicationScoped
public class MtConsumer extends AbstractConsumer implements ConsumerInterface {

	@RestClient
	@Inject
	MessageResource messageResource;
	

	@Inject
    ConnectionFactory _connectionFactory;

	
	@ConfigProperty(name = "io.gaiaid.jms.ex.delay")
	Long _exDelay;
	
	
	@ConfigProperty(name = "io.gaiaid.jms.mt.queue.name")
	String _queueName;
	
	@ConfigProperty(name = "io.gaiaid.jms.mt.consumer.threads")
	Integer _threads;
	
	@ConfigProperty(name = "io.gaiaid.debug")
	Boolean _debug;
	
	
	private static final Logger logger = Logger.getLogger(MtConsumer.class);

	
	
	void onStart(@Observes StartupEvent ev) {
    	
		logger.info("onStart: BeConsumer queueName: " + _queueName);
		
		this.setExDelay(_exDelay);
		this.setDebug(_debug);
		this.setQueueName(_queueName);
		this.setThreads(_threads);
		this.setConnectionFactory(_connectionFactory);
		super._onStart();
		
    }

    void onStop(@Observes ShutdownEvent ev) {
    	
    	logger.info("onStop: BeConsumer");
		
    	
    	super._onStop();
    	
    }
	
    @Override
	public void receiveMessage(BaseMessage message) throws Exception {
		
		messageResource.sendMessage(message);
		
	}

	
}
