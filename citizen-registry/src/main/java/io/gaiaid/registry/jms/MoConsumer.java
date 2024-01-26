package io.gaiaid.registry.jms;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.gaiaid.registry.svc.GaiaService;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.twentysixty.sa.client.jms.AbstractConsumer;
import io.twentysixty.sa.client.jms.ConsumerInterface;
import io.twentysixty.sa.client.model.message.BaseMessage;

@ApplicationScoped
public class MoConsumer extends AbstractConsumer implements ConsumerInterface {

	@Inject GaiaService gaiaService;

	@Inject
    ConnectionFactory _connectionFactory;

	
	@ConfigProperty(name = "io.gaiaid.jms.ex.delay")
	Long _exDelay;
	
	
	@ConfigProperty(name = "io.gaiaid.jms.mo.queue.name")
	String _queueName;
	
	@ConfigProperty(name = "io.gaiaid.jms.mo.consumer.threads")
	Integer _threads;
	
	@ConfigProperty(name = "io.gaiaid.debug")
	Boolean _debug;
	
	private static final Logger logger = Logger.getLogger(MoConsumer.class);
	
	
	void onStart(@Observes StartupEvent ev) {
    	
		logger.info("onStart: SaConsumer queueName: " + _queueName);
		
		this.setExDelay(_exDelay);
		this.setDebug(_debug);
		this.setQueueName(_queueName);
		this.setThreads(_threads);
		this.setConnectionFactory(_connectionFactory);
		super._onStart();
		
    }

    void onStop(@Observes ShutdownEvent ev) {
    	
    	logger.info("onStop: SaConsumer");
    	
    	super._onStop();
    	
    }
	
    @Override
	public void receiveMessage(BaseMessage message) throws Exception {
		
		gaiaService.userInput(message);
		
	}

	
}
