package com.example.aicopilot.service;

import com.example.aicopilot.agent.DataModeler;
import com.example.aicopilot.agent.FormUXDesigner;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.event.ProcessGeneratedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Asynchronous Artifact Generator.
 * Subscribes to ProcessGeneratedEvent to perform data modeling and form design in the background.
 */
@Service
@RequiredArgsConstructor
public class AsyncArtifactGenerator {

    private final DataModeler dataModeler;
    private final FormUXDesigner formUXDesigner;
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Async // Run in a separate thread (prevent blocking main flow)
    @EventListener
    public void handleProcessGenerated(ProcessGeneratedEvent event) {
        try {
            String jobId = event.getJobId();
            String userRequest = event.getUserRequest();
            String processJson = objectMapper.writeValueAsString(event.getProcessResponse());

            // ---------------------------------------------------------
            // Step 2: Data Modeling
            // ---------------------------------------------------------
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Step 2/3: Defining Data Model...");
            long startData = System.currentTimeMillis();

            // Call Data Modeler (User Request + Process Context)
            DataEntitiesResponse data = dataModeler.designDataModel(userRequest, processJson);

            long durationData = System.currentTimeMillis() - startData;
            jobRepository.saveArtifact(jobId, "DATA", data, durationData);

            // ---------------------------------------------------------
            // Step 3: Form UX Design
            // ---------------------------------------------------------
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Step 3/3: Configuring Forms and Permissions...");
            String dataJson = objectMapper.writeValueAsString(data);
            long startForm = System.currentTimeMillis();

            // Call Form Designer (Process + Data Context)
            FormResponse form = formUXDesigner.designForm(userRequest, processJson, dataJson);

            long durationForm = System.currentTimeMillis() - startForm;
            jobRepository.saveArtifact(jobId, "FORM", form, durationForm);

            // Complete all tasks
            jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "All designs completed.");

        } catch (Exception e) {
            e.printStackTrace();
            // Update state on error (partial failure consideration as process is already successful)
            jobRepository.updateState(event.getJobId(), JobStatus.State.FAILED, "Error during subsequent tasks: " + e.getMessage());
        }
    }
}