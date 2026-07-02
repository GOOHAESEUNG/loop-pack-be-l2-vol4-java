package com.loopers.testcontainers;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTestContainersConfigTest {

    @DisplayName("Kafka 컨테이너가 기동되고, bootstrap-servers 시스템 프로퍼티로 브로커에 접속할 수 있다.")
    @Test
    void kafkaContainerStartsAndBrokerResponds() throws Exception {
        // arrange
        new KafkaTestContainersConfig();
        String bootstrapServers = System.getProperty("spring.kafka.bootstrap-servers");
        assertThat(bootstrapServers).isNotBlank();

        // act
        try (AdminClient adminClient = AdminClient.create(
            Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        )) {
            String clusterId = adminClient.describeCluster()
                .clusterId()
                .get(10, TimeUnit.SECONDS);

            // assert
            assertThat(clusterId).isNotBlank();
        }
    }
}
