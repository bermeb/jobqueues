package dev.bermeb.jobqueues.domain.service;

import dev.bermeb.jobqueues.domain.model.Job;
import dev.bermeb.jobqueues.domain.port.in.ProcessJob;
import dev.bermeb.jobqueues.domain.port.out.JobStateStorePort;
import dev.bermeb.jobqueues.domain.port.out.ProcessJobPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ProcessJobService implements ProcessJob {

    private final JobStateStorePort stateStore;
    private final List<ProcessJobPort> processors;

    @Override
    public void process(Job job, String messageId) {
        UUID jobId = job.jobId();
        Optional<Job.JobState> currentState = stateStore.getJobState(jobId);

        if (currentState.isPresent()) {
            Job.JobState jobState = currentState.get();
            if (jobState == Job.JobState.COMPLETED) {
                log.info("Job {} has already been completed.", jobId);
                return;
            }
            if (jobState == Job.JobState.FAILED) {
                log.error("Job {} has failed.", jobId);
                return;
            }
            if (jobState == Job.JobState.PROCESSING) {
                log.info("Job {} is currently being processed.", jobId);
                return;
            }
        }

        stateStore.setState(jobId, Job.JobState.PROCESSING, Duration.ofMinutes(5));

        try {
            ProcessJobPort processor = processors.stream()
                    .filter(p -> p.getType().equalsIgnoreCase(job.type()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No processor for the job type '" + job.type() + "' registered."));

            processor.process(job);

            stateStore.setState(jobId, Job.JobState.COMPLETED, Duration.ofHours(24));
            log.info("Job {} has been completed.", jobId);
        } catch (Exception e) {
            stateStore.setState(jobId, Job.JobState.RETRY, Duration.ofHours(24));
            log.error("Error while processing job {}: {}. Retrying.", jobId, e.getMessage());
        }
    }
}