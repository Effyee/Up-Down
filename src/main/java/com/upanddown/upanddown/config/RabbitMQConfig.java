package com.upanddown.upanddown.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // === 실시간 주가 데이터 수신을 위한 Fanout Exchange 설정 (Python 프로듀서와 연동) ===
    public static final String REALTIME_QUEUE_NAME = "stock_price_queue"; // Python과 약속된 큐 이름
    public static final String REALTIME_EXCHANGE_NAME = "stock_exchange"; // Python과 약속된 Fanout Exchange 이름

    @Bean
    public Queue realtimeStockQueue() {
        // durable=true: RabbitMQ가 재시작되어도 큐가 사라지지 않음
        return new Queue(REALTIME_QUEUE_NAME, true);
    }

    @Bean
    public FanoutExchange realtimeStockFanoutExchange() {
        return new FanoutExchange(REALTIME_EXCHANGE_NAME, true, false);
    }

    @Bean
    public Binding realtimeStockFanoutBinding() {
        // realtimeStockQueue()를 realtimeStockFanoutExchange()에 바인딩
        return BindingBuilder.bind(realtimeStockQueue())
                .to(realtimeStockFanoutExchange());
    }


    /*

    public static final String TOPIC_QUEUE_NAME = "stock_update_event_queue"; // 큐 이름 분리!
    public static final String TOPIC_EXCHANGE_NAME = "stock_event_exchange";
    public static final String ROUTING_KEY = "stock.event.#";

    @Bean
    public Queue topicStockQueue() {
        return new Queue(TOPIC_QUEUE_NAME, true);
    }

    @Bean
    public TopicExchange topicStockExchange() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
    }

    @Bean
    public Binding topicStockBinding() {
        return BindingBuilder.bind(topicStockQueue())
                .to(topicStockExchange())
                .with(ROUTING_KEY);
    }
    */
}