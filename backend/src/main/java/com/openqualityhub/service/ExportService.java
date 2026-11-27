package com.openqualityhub.service;

import java.io.OutputStream;
import java.util.UUID;

public interface ExportService {

    void exportTestRunsCsv(UUID projectId, OutputStream out);

    void exportDefectsCsv(UUID projectId, String status, OutputStream out);
}
