package com.ds.bonus.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String BONUS_EXCHANGE = "ds.bonus.exchange";
    public static final String BONUS_QUEUE = "ds.bonus.calculate.queue";
    public static final String BONUS_ROUTING_KEY = "ds.bonus.calculate";
    public static final String BONUS_ORDER_PAID_QUEUE = "ds.bonus.order.paid.queue";
    public static final String BONUS_ORDER_PAID_ROUTING_KEY = "ds.bonus.order.paid";
    public static final String BONUS_ORDER_RETURNED_QUEUE = "ds.bonus.order.returned.queue";
    public static final String BONUS_ORDER_RETURNED_ROUTING_KEY = "ds.bonus.order.returned";

    @Bean
    public TopicExchange bonusExchange() {
        return new TopicExchange(BONUS_EXCHANGE, true, false);
    }

    @Bean
    public Queue bonusQueue() {
        return new Queue(BONUS_QUEUE, true);
    }

    @Bean
    public Binding bonusBinding() {
        return BindingBuilder.bind(bonusQueue()).to(bonusExchange()).with(BONUS_ROUTING_KEY);
    }

    @Bean
    public Queue bonusOrderPaidQueue() {
        return new Queue(BONUS_ORDER_PAID_QUEUE, true);
    }

    @Bean
    public Binding bonusOrderPaidBinding() {
        return BindingBuilder.bind(bonusOrderPaidQueue()).to(bonusExchange()).with(BONUS_ORDER_PAID_ROUTING_KEY);
    }

    @Bean
    public Queue bonusOrderReturnedQueue() {
        return new Queue(BONUS_ORDER_RETURNED_QUEUE, true);
    }

    @Bean
    public Binding bonusOrderReturnedBinding() {
        return BindingBuilder.bind(bonusOrderReturnedQueue()).to(bonusExchange()).with(BONUS_ORDER_RETURNED_ROUTING_KEY);
    }
}