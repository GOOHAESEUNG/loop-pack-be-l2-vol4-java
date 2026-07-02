package com.loopers.support.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    public static final String CATALOG_EVENTS = "catalog-events";
    public static final String ORDER_EVENTS = "order-events";
    public static final String COUPON_ISSUE_REQUESTS = "coupon-issue-requests";

    @Bean
    public NewTopic catalogEvents() {
        return TopicBuilder.name(CATALOG_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderEvents() {
        return TopicBuilder.name(ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic couponIssueRequests() {
        return TopicBuilder.name(COUPON_ISSUE_REQUESTS).partitions(3).replicas(1).build();
    }
}
