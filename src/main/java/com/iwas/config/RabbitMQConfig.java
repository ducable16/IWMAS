//package com.iwas.config;
//
//import com.iwas.auth.EmailConsumer;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
//import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RabbitMQConfig {
//    public static final String EMAIL_QUEUE = "email.verification";
//
//    @Bean
//    public Queue emailQueue() {
//        // true = durable, queue tồn tại khi RabbitMQ restart
//        return new Queue(EMAIL_QUEUE, true);
//    }
//
//    @Bean
//    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
//        return new Jackson2JsonMessageConverter();
//    }
//
//    @Bean
//    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//        RabbitTemplate template = new RabbitTemplate(connectionFactory);
//        template.setMessageConverter(jackson2JsonMessageConverter());
//        return template;
//    }
//
//    @Bean
//    public SimpleMessageListenerContainer listenerContainer(
//            ConnectionFactory connectionFactory,
//            MessageListenerAdapter listenerAdapter
//    ) {
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//        container.setQueues(emailQueue());
//        container.setMessageListener(listenerAdapter);
//        return container;
//    }
//
//    @Bean
//    public MessageListenerAdapter listenerAdapter(EmailConsumer consumer) {
//        MessageListenerAdapter adapter = new MessageListenerAdapter(consumer, "consume");
//        adapter.setMessageConverter(jackson2JsonMessageConverter());
//        return adapter;
//    }
//
//}
