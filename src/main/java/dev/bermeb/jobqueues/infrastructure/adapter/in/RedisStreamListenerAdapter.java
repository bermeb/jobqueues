package dev.bermeb.jobqueues.infrastructure.adapter.in;

import dev.bermeb.jobqueues.domain.model.Job;
import dev.bermeb.jobqueues.domain.port.in.ProcessJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamListenerAdapter implements StreamListener<String, MapRecord<String, String, String>> {

    private final ProcessJob processJob;
    private final JsonMapper jsonMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        String messageId = record.getId().getValue();
        String payload = record.getValue().get("payload");

        if (payload == null) {
            log.error("Message {} has no payload. Skipping.", messageId);
            return;
        }

        try {
            Job job = jsonMapper.readValue(payload, Job.class);

            processJob.process(job, messageId);
        } catch (JacksonException e) {
            log.error("Error while deserialising message {}.", messageId, e);
        }
     }
}
