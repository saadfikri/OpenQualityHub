package com.openqualityhub.service;

import java.util.UUID;

public interface ExportService {

    byte[] exportTestRunsCsv(UUID projectId);

    byte[] exportDefectsCsv(UUID projectId, String status);
}
