package com.openqualityhub.service.impl;

import com.openqualityhub.entity.Defect;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.repository.DefectRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

    private final TestRunRepository testRunRepository;
    private final DefectRepository defectRepository;

    @Override
    public byte[] exportTestRunsCsv(UUID projectId) {
        List<TestRun> runs;
        if (projectId != null) {
            runs = testRunRepository.findByProjectIdAndDeletedFalse(projectId);
        } else {
            runs = testRunRepository.findByDeletedFalse();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Name,Source,Project,Total Tests,Passed,Failed,Skipped,Pass Rate,Duration (ms),Environment,Commit SHA,Branch,Created At\n");

        for (TestRun run : runs) {
            sb.append(escapeCsv(run.getId().toString())).append(',');
            sb.append(escapeCsv(run.getName())).append(',');
            sb.append(escapeCsv(run.getSource().name())).append(',');
            sb.append(escapeCsv(run.getProject() != null ? run.getProject().getName() : "")).append(',');
            sb.append(run.getTotalTests()).append(',');
            sb.append(run.getPassed()).append(',');
            sb.append(run.getFailed()).append(',');
            sb.append(run.getSkipped()).append(',');
            sb.append(run.getPassRate() != null ? run.getPassRate() : "").append(',');
            sb.append(run.getDurationMs() != null ? run.getDurationMs() : "").append(',');
            sb.append(escapeCsv(run.getEnvironment() != null ? run.getEnvironment() : "")).append(',');
            sb.append(escapeCsv(run.getCommitSha() != null ? run.getCommitSha() : "")).append(',');
            sb.append(escapeCsv(run.getBranch() != null ? run.getBranch() : "")).append(',');
            sb.append(escapeCsv(run.getCreatedAt() != null ? run.getCreatedAt().toString() : "")).append('\n');
        }

        log.info("Exported {} test runs as CSV (projectId={})", runs.size(), projectId);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportDefectsCsv(UUID projectId, String status) {
        List<Defect> defects;
        if (projectId != null && status != null) {
            Defect.DefectStatus defectStatus = Defect.DefectStatus.valueOf(status.toUpperCase());
            defects = defectRepository.findAllByProjectIdAndStatusAndDeletedFalse(projectId, defectStatus);
        } else if (projectId != null) {
            defects = defectRepository.findAllByProjectIdAndDeletedFalse(projectId);
        } else if (status != null) {
            Defect.DefectStatus defectStatus = Defect.DefectStatus.valueOf(status.toUpperCase());
            defects = defectRepository.findAllByStatusAndDeletedFalse(defectStatus);
        } else {
            defects = defectRepository.findAllByDeletedFalse();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Title,Severity,Status,Project,Reported By,Assigned To,Created At,Updated At\n");

        for (Defect defect : defects) {
            sb.append(escapeCsv(defect.getId().toString())).append(',');
            sb.append(escapeCsv(defect.getTitle())).append(',');
            sb.append(escapeCsv(defect.getSeverity().name())).append(',');
            sb.append(escapeCsv(defect.getStatus().name())).append(',');
            sb.append(escapeCsv(defect.getProject() != null ? defect.getProject().getName() : "")).append(',');
            sb.append(escapeCsv(defect.getReportedBy() != null ? defect.getReportedBy().getDisplayName() : "")).append(',');
            sb.append(escapeCsv(defect.getAssignedTo() != null ? defect.getAssignedTo().getDisplayName() : "")).append(',');
            sb.append(escapeCsv(defect.getCreatedAt() != null ? defect.getCreatedAt().toString() : "")).append(',');
            sb.append(escapeCsv(defect.getUpdatedAt() != null ? defect.getUpdatedAt().toString() : "")).append('\n');
        }

        log.info("Exported {} defects as CSV (projectId={}, status={})", defects.size(), projectId, status);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
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
