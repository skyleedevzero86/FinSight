package com.sleekydz86.finsight.core.health.domain.vo;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

public class SystemMetrics {
    private final long timestamp;
    private final Map<String, Object> jvmMetrics;
    private final Map<String, Object> systemMetrics;

    public SystemMetrics() {
        this.timestamp = System.currentTimeMillis();
        this.jvmMetrics = collectJvmMetrics();
        this.systemMetrics = collectSystemMetrics();
    }

    private Map<String, Object> collectJvmMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        metrics.put("memory", Map.of(
                "total", runtime.totalMemory(),
                "free", runtime.freeMemory(),
                "used", runtime.totalMemory() - runtime.freeMemory(),
                "max", runtime.maxMemory(),
                "heapUsed", memoryBean.getHeapMemoryUsage().getUsed(),
                "heapMax", memoryBean.getHeapMemoryUsage().getMax(),
                "nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed()
        ));

        metrics.put("threads", Map.of(
                "count", threadBean.getThreadCount(),
                "peakCount", threadBean.getPeakThreadCount(),
                "daemonCount", threadBean.getDaemonThreadCount()
        ));

        metrics.put("processors", runtime.availableProcessors());

        return metrics;
    }

    private Map<String, Object> collectSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        metrics.put("os", Map.of(
                "name", osBean.getName(),
                "version", osBean.getVersion(),
                "arch", osBean.getArch(),
                "availableProcessors", osBean.getAvailableProcessors(),
                "systemLoadAverage", osBean.getSystemLoadAverage()
        ));

        return metrics;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getJvmMetrics() {
        return jvmMetrics;
    }

    public Map<String, Object> getSystemMetrics() {
        return systemMetrics;
    }

    @Override
    public String toString() {
        return "SystemMetrics{" +
                "timestamp=" + timestamp +
                ", jvmMetrics=" + jvmMetrics +
                ", systemMetrics=" + systemMetrics +
                '}';
    }
}