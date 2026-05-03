package com.iwas.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EMAIL_QUEUE = "email.verification";
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_ROUTING_KEY = "email.verify";

    public static final String SEARCH_USER_QUEUE = "search.user.sync";
    public static final String SEARCH_USER_EXCHANGE = "search.user.exchange";
    public static final String SEARCH_USER_ROUTING_KEY = "search.user";

    public static final String SEARCH_PROJECT_QUEUE = "search.project.sync";
    public static final String SEARCH_PROJECT_EXCHANGE = "search.project.exchange";
    public static final String SEARCH_PROJECT_ROUTING_KEY = "search.project";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(emailExchange())
                .with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Queue searchUserQueue() {
        return new Queue(SEARCH_USER_QUEUE, true);
    }

    @Bean
    public DirectExchange searchExchange() {
        return new DirectExchange(SEARCH_USER_EXCHANGE);
    }

    @Bean
    public Binding searchBinding() {
        return BindingBuilder
                .bind(searchUserQueue())
                .to(searchExchange())
                .with(SEARCH_USER_ROUTING_KEY);
    }

    @Bean
    public Queue searchProjectQueue() {
        return new Queue(SEARCH_PROJECT_QUEUE, true);
    }

    @Bean
    public DirectExchange searchProjectExchange() {
        return new DirectExchange(SEARCH_PROJECT_EXCHANGE);
    }

    @Bean
    public Binding searchProjectBinding() {
        return BindingBuilder
                .bind(searchProjectQueue())
                .to(searchProjectExchange())
                .with(SEARCH_PROJECT_ROUTING_KEY);
    }

}
