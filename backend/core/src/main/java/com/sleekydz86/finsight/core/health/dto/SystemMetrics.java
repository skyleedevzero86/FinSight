package com.sleekydz86.finsight.core.health.dto;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;

public class SystemMetrics {
    private LocalDateTime timestamp;
    private MemoryMetrics memory;
    private CpuMetrics cpu;
    private ThreadMetrics threads;
    private JvmMetrics jvm;
    private OperatingSystemMetrics os;

    public SystemMetrics() {
        this.timestamp = LocalDateTime.now();
        this.memory = new MemoryMetrics();
        this.cpu = new CpuMetrics();
        this.threads = new ThreadMetrics();
        this.jvm = new JvmMetrics();
        this.os = new OperatingSystemMetrics();
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public MemoryMetrics getMemory() { return memory; }
    public void setMemory(MemoryMetrics memory) { this.memory = memory; }

    public CpuMetrics getCpu() { return cpu; }
    public void setCpu(CpuMetrics cpu) { this.cpu = cpu; }

    public ThreadMetrics getThreads() { return threads; }
    public void setThreads(ThreadMetrics threads) { this.threads = threads; }

    public JvmMetrics getJvm() { return jvm; }
    public void setJvm(JvmMetrics jvm) { this.jvm = jvm; }

    public OperatingSystemMetrics getOs() { return os; }
    public void setOs(OperatingSystemMetrics os) { this.os = os; }

    public static class MemoryMetrics {
        private long totalMemory;
        private long freeMemory;
        private long usedMemory;
        private double memoryUsagePercentage;
        private long maxMemory;

        public MemoryMetrics() {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            this.totalMemory = memoryBean.getHeapMemoryUsage().getCommitted();
            this.freeMemory = memoryBean.getHeapMemoryUsage().getMax() - memoryBean.getHeapMemoryUsage().getUsed();
            this.usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            this.maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            this.memoryUsagePercentage = (double) usedMemory / maxMemory * 100;
        }

        public long getTotalMemory() { return totalMemory; }
        public void setTotalMemory(long totalMemory) { this.totalMemory = totalMemory; }

        public long getFreeMemory() { return freeMemory; }
        public void setFreeMemory(long freeMemory) { this.freeMemory = freeMemory; }

        public long getUsedMemory() { return usedMemory; }
        public void setUsedMemory(long usedMemory) { this.usedMemory = usedMemory; }

        public double getMemoryUsagePercentage() { return memoryUsagePercentage; }
        public void setMemoryUsagePercentage(double memoryUsagePercentage) { this.memoryUsagePercentage = memoryUsagePercentage; }

        public long getMaxMemory() { return maxMemory; }
        public void setMaxMemory(long maxMemory) { this.maxMemory = maxMemory; }
    }

    public static class CpuMetrics {
        private double systemCpuLoad;
        private double processCpuLoad;
        private int availableProcessors;

        public CpuMetrics() {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            this.availableProcessors = osBean.getAvailableProcessors();
            // CPU 로드는 별도 계산 필요
        }

        public double getSystemCpuLoad() { return systemCpuLoad; }
        public void setSystemCpuLoad(double systemCpuLoad) { this.systemCpuLoad = systemCpuLoad; }

        public double getProcessCpuLoad() { return processCpuLoad; }
        public void setProcessCpuLoad(double processCpuLoad) { this.processCpuLoad = processCpuLoad; }

        public int getAvailableProcessors() { return availableProcessors; }
        public void setAvailableProcessors(int availableProcessors) { this.availableProcessors = availableProcessors; }
    }

    public static class ThreadMetrics {
        private int threadCount;
        private int daemonThreadCount;
        private int peakThreadCount;
        private long totalStartedThreadCount;

        public ThreadMetrics() {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            this.threadCount = threadBean.getThreadCount();
            this.daemonThreadCount = threadBean.getDaemonThreadCount();
            this.peakThreadCount = threadBean.getPeakThreadCount();
            this.totalStartedThreadCount = threadBean.getTotalStartedThreadCount();
        }

        public int getThreadCount() { return threadCount; }
        public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

        public int getDaemonThreadCount() { return daemonThreadCount; }
        public void setDaemonThreadCount(int daemonThreadCount) { this.daemonThreadCount = daemonThreadCount; }

        public int getPeakThreadCount() { return peakThreadCount; }
        public void setPeakThreadCount(int peakThreadCount) { this.peakThreadCount = peakThreadCount; }

        public long getTotalStartedThreadCount() { return totalStartedThreadCount; }
        public void setTotalStartedThreadCount(long totalStartedThreadCount) { this.totalStartedThreadCount = totalStartedThreadCount; }
    }

    public static class JvmMetrics {
        private String version;
        private String vendor;
        private long startTime;
        private long uptime;

        public JvmMetrics() {
            Runtime runtime = Runtime.getRuntime();
            this.version = System.getProperty("java.version");
            this.vendor = System.getProperty("java.vendor");
            this.startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
            this.uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getVendor() { return vendor; }
        public void setVendor(String vendor) { this.vendor = vendor; }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public long getUptime() { return uptime; }
        public void setUptime(long uptime) { this.uptime = uptime; }
    }

    public static class OperatingSystemMetrics {
        private String name;
        private String version;
        private String architecture;
        private int availableProcessors;

        public OperatingSystemMetrics() {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            this.name = osBean.getName();
            this.version = osBean.getVersion();
            this.architecture = osBean.getArch();
            this.availableProcessors = osBean.getAvailableProcessors();
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getArchitecture() { return architecture; }
        public void setArchitecture(String architecture) { this.architecture = architecture; }

        public int getAvailableProcessors() { return availableProcessors; }
        public void setAvailableProcessors(int availableProcessors) { this.availableProcessors = availableProcessors; }
    }
}