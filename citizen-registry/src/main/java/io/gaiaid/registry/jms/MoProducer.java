package io.gaiaid.registry.jms;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.twentysixty.sa.client.jms.AbstractProducer;
import io.twentysixty.sa.client.jms.ProducerInterface;
import io.twentysixty.sa.client.model.message.BaseMessage;


@ApplicationScoped
public class MoProducer extends AbstractProducer implements ProducerInterface {

	@Inject
    ConnectionFactory _connectionFactory;

	
	@ConfigProperty(name = "io.gaiaid.jms.ex.delay")
	Long _exDelay;
	
	@ConfigProperty(name = "io.gaiaid.jms.mo.queue.name")
	String _queueName;
	
	@ConfigProperty(name = "io.gaiaid.jms.mo.producer.threads")
	Integer _threads;
	
	@ConfigProperty(name = "io.gaiaid.debug")
	Boolean _debug;
	
	
	
	private static final Logger logger = Logger.getLogger(MoProducer.class);
	
    
    void onStart(@Observes StartupEvent ev) {
    	logger.info("onStart: SaProducer");
    	
    	this.setExDelay(_exDelay);
		this.setDebug(_debug);
		this.setQueueName(_queueName);
		this.setThreads(_threads);
		this.setConnectionFactory(_connectionFactory);
    	this.setProducerCount(_threads);
    	
    }

    void onStop(@Observes ShutdownEvent ev) {
    	logger.info("onStop: SaProducer");
    }
 
 
    @Override
    public void sendMessage(BaseMessage message) throws Exception {
    	this.spool(message, 0);
    }

    
	
    
    
    

}