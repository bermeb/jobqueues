package dev.bermeb.jobqueues.infrastructure.config;

import dev.bermeb.jobqueues.infrastructure.adapter.in.RedisStreamListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.concurrent.Executors;

@Configuration
public class RedisConfig {

    @Value("${app.queue.stream-name:jobs:stream}")
    private String streamName;
    @Value("${app.queue.group-name:jobs:group}")
    private String groupName;
    @Value("${app.queue.consumer-name:worker-0}")
    private String consumerName;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(stringRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        RedisStreamListenerAdapter adapter
    ) {
        initializeStreamAndGroup(connectionFactory);

        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .batchSize(10)
                .pollTimeout(Duration.ofSeconds(2))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();

        var container = StreamMessageListenerContainer.create(connectionFactory, options);

        container.receive(
                Consumer.from(groupName, consumerName),
                StreamOffset.create(streamName, ReadOffset.lastConsumed()),
                adapter
        );

        container.start();
        return container;
    }

    private void initializeStreamAndGroup(RedisConnectionFactory connectionFactory) {
        try (var conn = connectionFactory.getConnection()) {
            conn.streamCommands().xGroupCreate(
                    streamName.getBytes(),
                    groupName,
                    ReadOffset.from("0"),
                    true
            );
        } catch (RedisSystemException e) {
            if (e.getRootCause() != null) {
                if (!e.getRootCause().getMessage().contains("BUSYGROUP")) {
                    throw e;
                }
            }
        }
    }

}