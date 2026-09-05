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

    public SystemMetrics(Map<String, Object> jvmMetrics, Map<String, Object> systemMetrics) {
        this.timestamp = System.currentTimeMillis();
        this.jvmMetrics = jvmMetrics != null ? jvmMetrics : Map.of();
        this.systemMetrics = systemMetrics != null ? systemMetrics : Map.of();
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
        double systemLoadAverage = osBean.getSystemLoadAverage();
        double systemCpuLoad = -1.0d;
        double processCpuLoad = -1.0d;

        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            systemCpuLoad = sunOsBean.getCpuLoad();
            processCpuLoad = sunOsBean.getProcessCpuLoad();
            if (systemCpuLoad < 0.0d) {
                systemCpuLoad = sunOsBean.getCpuLoad();
            }
            if (processCpuLoad < 0.0d) {
                processCpuLoad = sunOsBean.getProcessCpuLoad();
            }
        }

        Map<String, Object> os = new HashMap<>();
        os.put("name", osBean.getName());
        os.put("version", osBean.getVersion());
        os.put("arch", osBean.getArch());
        os.put("availableProcessors", osBean.getAvailableProcessors());
        os.put("systemLoadAverage", systemLoadAverage);
        os.put("systemCpuLoad", systemCpuLoad);
        os.put("processCpuLoad", processCpuLoad);
        metrics.put("os", os);

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