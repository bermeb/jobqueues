package dev.bermeb.jobqueues.infrastructure.adapter.out;

import dev.bermeb.jobqueues.domain.model.Job;
import dev.bermeb.jobqueues.domain.port.out.ProcessJobPort;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReminderJobProcessorAdapter implements ProcessJobPort {

    @Override
    public String getType() {
        return "REMINDER";
    }

    @Override
    public void process(Job job) {
        String userId = (String) job.data().getOrDefault("userId", "test-user");
        String reminderMessage = (String) job.data().getOrDefault("message", "Reminder Job");

        log.info("Starting Reminder-Job {}: Sending reminder to '{}' with message '{}'", job.jobId(), userId, reminderMessage);
    }
}