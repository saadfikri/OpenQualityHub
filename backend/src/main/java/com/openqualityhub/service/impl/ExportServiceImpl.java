package com.openqualityhub.service.impl;

import com.openqualityhub.entity.Defect;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.repository.DefectRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

    private static final int PAGE_SIZE = 500;

    private final TestRunRepository testRunRepository;
    private final DefectRepository defectRepository;

    @Override
    public void exportTestRunsCsv(UUID projectId, OutputStream out) {
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write("ID,Name,Source,Project,Total Tests,Passed,Failed,Skipped,Pass Rate,Duration (ms),Environment,Commit SHA,Branch,Created At\n");

            int page = 0;
            long total = 0;
            Page<TestRun> slice;
            do {
                PageRequest pageRequest = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt"));
                if (projectId != null) {
                    slice = testRunRepository.findByProjectIdAndDeletedFalseOrderByExecutedAtDesc(projectId, pageRequest);
                } else {
                    slice = testRunRepository.findByDeletedFalse(pageRequest);
                }

                for (TestRun run : slice.getContent()) {
                    writer.write(escapeCsv(run.getId().toString())); writer.write(',');
                    writer.write(escapeCsv(run.getName())); writer.write(',');
                    writer.write(escapeCsv(run.getSource().name())); writer.write(',');
                    writer.write(escapeCsv(run.getProject() != null ? run.getProject().getName() : "")); writer.write(',');
                    writer.write(String.valueOf(run.getTotalTests())); writer.write(',');
                    writer.write(String.valueOf(run.getPassed())); writer.write(',');
                    writer.write(String.valueOf(run.getFailed())); writer.write(',');
                    writer.write(String.valueOf(run.getSkipped())); writer.write(',');
                    writer.write(run.getPassRate() != null ? run.getPassRate().toString() : ""); writer.write(',');
                    writer.write(run.getDurationMs() != null ? run.getDurationMs().toString() : ""); writer.write(',');
                    writer.write(escapeCsv(run.getEnvironment() != null ? run.getEnvironment() : "")); writer.write(',');
                    writer.write(escapeCsv(run.getCommitSha() != null ? run.getCommitSha() : "")); writer.write(',');
                    writer.write(escapeCsv(run.getBranch() != null ? run.getBranch() : "")); writer.write(',');
                    writer.write(escapeCsv(run.getCreatedAt() != null ? run.getCreatedAt().toString() : "")); writer.write('\n');
                }

                total += slice.getNumberOfElements();
                page++;
                writer.flush();
            } while (slice.hasNext());

            log.info("Streamed {} test runs as CSV (projectId={})", total, projectId);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export test runs CSV", e);
        }
    }

    @Override
    public void exportDefectsCsv(UUID projectId, String status, OutputStream out) {
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write("ID,Title,Severity,Status,Project,Reported By,Assigned To,Created At,Updated At\n");

            int page = 0;
            long total = 0;
            Page<Defect> slice;
            do {
                PageRequest pageRequest = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt"));
                if (projectId != null && status != null) {
                    Defect.DefectStatus defectStatus = Defect.DefectStatus.valueOf(status.toUpperCase());
                    slice = defectRepository.findByProjectIdAndStatusAndDeletedFalse(projectId, defectStatus, pageRequest);
                } else if (projectId != null) {
                    slice = defectRepository.findByProjectIdAndDeletedFalse(projectId, pageRequest);
                } else if (status != null) {
                    Defect.DefectStatus defectStatus = Defect.DefectStatus.valueOf(status.toUpperCase());
                    slice = defectRepository.findByStatusAndDeletedFalse(defectStatus, pageRequest);
                } else {
                    slice = defectRepository.findByDeletedFalse(pageRequest);
                }

                for (Defect defect : slice.getContent()) {
                    writer.write(escapeCsv(defect.getId().toString())); writer.write(',');
                    writer.write(escapeCsv(defect.getTitle())); writer.write(',');
                    writer.write(escapeCsv(defect.getSeverity().name())); writer.write(',');
                    writer.write(escapeCsv(defect.getStatus().name())); writer.write(',');
                    writer.write(escapeCsv(defect.getProject() != null ? defect.getProject().getName() : "")); writer.write(',');
                    writer.write(escapeCsv(defect.getReportedBy() != null ? defect.getReportedBy().getDisplayName() : "")); writer.write(',');
                    writer.write(escapeCsv(defect.getAssignedTo() != null ? defect.getAssignedTo().getDisplayName() : "")); writer.write(',');
                    writer.write(escapeCsv(defect.getCreatedAt() != null ? defect.getCreatedAt().toString() : "")); writer.write(',');
                    writer.write(escapeCsv(defect.getUpdatedAt() != null ? defect.getUpdatedAt().toString() : "")); writer.write('\n');
                }

                total += slice.getNumberOfElements();
                page++;
                writer.flush();
            } while (slice.hasNext());

            log.info("Streamed {} defects as CSV (projectId={}, status={})", total, projectId, status);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export defects CSV", e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
