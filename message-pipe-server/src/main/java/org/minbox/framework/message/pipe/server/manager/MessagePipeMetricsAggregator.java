package org.minbox.framework.message.pipe.server.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.server.MessagePipe;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Global metrics aggregator for MessagePipe cluster monitoring
 * <p>
 * Functionality:
 * - Collects monitoring metrics from all MessagePipe instances
 * - Calculates global aggregated statistics
 * - Outputs summary report (1 per minute)
 * - Identifies and lists problem instances
 * <p>
 * Features:
 * - Singleton design
 * - Thread-safe
 * - Configurable sampling and thresholds
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipeMetricsAggregator {

    private static final MessagePipeMetricsAggregator INSTANCE
        = new MessagePipeMetricsAggregator();

    // Configuration (public for access from Monitor)
    public volatile AggregationConfig config = new AggregationConfig();

    // Registry of all pipes
    private final ConcurrentHashMap<String, org.minbox.framework.message.pipe.server.MessagePipe> pipes
        = new ConcurrentHashMap<>();

    // Global dropped message counter
    private final AtomicLong droppedMessageCount = new AtomicLong(0);

    // Aggregation report executor
    private ScheduledExecutorService aggregationExecutor;

    // Report interval (60 seconds)
    private static final long AGGREGATION_INTERVAL = 60000;

    private MessagePipeMetricsAggregator() {
    }

    public static MessagePipeMetricsAggregator getInstance() {
        return INSTANCE;
    }

    /**
     * Start the aggregation reporting thread
     */
    public synchronized void startAggregationReporting() {
        if (aggregationExecutor != null) {
            return;
        }

        aggregationExecutor = Executors.newScheduledThreadPool(1, runnable -> {
            Thread t = new Thread(runnable, "MessagePipeMetricsAggregator");
            t.setDaemon(true);
            return t;
        });

        aggregationExecutor.scheduleAtFixedRate(
            this::outputAggregationReport,
            AGGREGATION_INTERVAL,
            AGGREGATION_INTERVAL,
            TimeUnit.MILLISECONDS
        );

        log.info("MessagePipe Metrics Aggregator started. Mode: {}. Total pipes: {}",
            config.enabled ? "AGGREGATION" : "INDIVIDUAL", pipes.size());
    }

    /**
     * Register a pipe
     */
    public void register(String pipeName, org.minbox.framework.message.pipe.server.MessagePipe pipe) {
        pipes.put(pipeName, pipe);
        log.debug("Pipe registered for metrics: {} (total: {})",
            pipeName, pipes.size());
    }

    /**
     * Unregister a pipe
     */
    public void unregister(String pipeName) {
        pipes.remove(pipeName);
        log.debug("Pipe unregistered from metrics: {} (remaining: {})",
            pipeName, pipes.size());
    }

    /**
     * Get aggregated metrics
     */
    public AggregatedMetrics getAggregatedMetrics() {
        if (pipes.isEmpty()) {
            return new AggregatedMetrics();
        }

        List<PipeMetrics> allMetrics = pipes.values()
            .stream()
            .map(pipe -> new PipeMetrics(pipe.getName(), pipe.size()))
            .collect(Collectors.toList());

        return new AggregatedMetrics(allMetrics, config);
    }

    /**
     * Output aggregation report
     */
    private void outputAggregationReport() {
        if (!config.enabled || pipes.isEmpty()) {
            return;
        }

        try {
            AggregatedMetrics aggregated = getAggregatedMetrics();
            outputReport(aggregated);
        } catch (Exception e) {
            log.error("Error generating aggregation report", e);
        }
    }

    /**
     * Format and output the report
     */
    private void outputReport(AggregatedMetrics metrics) {
        String separator = "==============================================================================";
        String dashes = "------------------------------------------------------------------------------";

        log.info(separator);
        log.info("MessagePipe Cluster Monitoring Report (60s) - Aggregated");
        log.info(dashes);

        // 1. Cluster statistics
        log.info("Cluster Statistics:");
        log.info("  Total Pipelines: {}", metrics.totalPipelines);
        log.info("  Healthy Pipelines: {} ({}%)",
            metrics.healthyCount, String.format("%.1f", metrics.healthyPercentage));
        log.info("  Caution Pipelines: {} ({}%)",
            metrics.cautionCount, String.format("%.1f", metrics.cautionPercentage));
        log.info("  Warning Pipelines: {} ({}%)",
            metrics.warningCount, String.format("%.1f", metrics.warningPercentage));
        log.info("  Critical Pipelines: {} ({}%)",
            metrics.criticalCount, String.format("%.1f", metrics.criticalPercentage));

        // 2. Aggregate metrics
        log.info(dashes);
        log.info("Aggregate Metrics:");
        log.info("  Total Queue Depth: {} (avg {})",
            metrics.totalQueueDepth, metrics.avgQueueDepth);

        // 3. Queue distribution
        log.info(dashes);
        log.info("Queue Distribution:");
        log.info("  Healthy (0-{}): {} pipelines ({}%)",
            config.queueDepthCaution,
            metrics.healthyCount, String.format("%.1f", metrics.healthyPercentage));
        log.info("  Caution ({}-{}): {} pipelines ({}%)",
            config.queueDepthCaution,
            config.queueDepthWarning,
            metrics.cautionCount, String.format("%.1f", metrics.cautionPercentage));
        log.info("  Warning (>{},<{}): {} pipelines ({}%)",
            config.queueDepthWarning,
            config.queueDepthCritical,
            metrics.warningCount, String.format("%.1f", metrics.warningPercentage));
        log.info("  Critical (>{}): {} pipelines ({}%)",
            config.queueDepthCritical,
            metrics.criticalCount, String.format("%.1f", metrics.criticalPercentage));

        // 4. Problem pipelines list
        if (!metrics.problemPipelines.isEmpty()) {
            log.info(dashes);
            log.info("Top {} Problem Pipelines:", metrics.problemPipelines.size());
            int index = 1;
            for (ProblemPipeline problem : metrics.problemPipelines) {
                log.warn("  {}. {}: Queue={}, Status={}",
                    index,
                    problem.pipeName,
                    problem.metrics.currentQueueSize,
                    problem.status
                );
                index++;
            }
        }

        log.info(separator);
    }

    /**
     * Record a dropped message for a specific pipeline
     * <p>
     * Tracks global dropped message count for monitoring purposes.
     *
     * @param pipeName the name of the pipeline that dropped the message
     */
    public void recordDroppedMessage(String pipeName) {
        droppedMessageCount.incrementAndGet();
        if (pipes.containsKey(pipeName)) {
            log.debug("Dropped message recorded for pipeline: {}, totalDropped={}",
                pipeName, droppedMessageCount.get());
        }
    }

    /**
     * Update configuration
     */
    public synchronized void setConfig(AggregationConfig config) {
        this.config = config;
        log.info("Aggregation config updated: {}", config);
    }

    /**
     * Shutdown the aggregator
     */
    public void shutdown() {
        if (aggregationExecutor != null) {
            aggregationExecutor.shutdown();
            try {
                if (!aggregationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    aggregationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                aggregationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("MessagePipe Metrics Aggregator shutdown");
    }

    // ==================== Inner Classes ====================

    /**
     * Pipe metrics data
     */
    public static class PipeMetrics {
        public final String pipeName;
        public final int currentQueueSize;

        public PipeMetrics(String pipeName, int currentQueueSize) {
            this.pipeName = pipeName;
            this.currentQueueSize = currentQueueSize;
        }
    }

    /**
     * Aggregation configuration
     */
    public static class AggregationConfig {
        // Enable aggregation reporting
        public boolean enabled = true;

        // Number of problem instances to display in summary report
        public int topPipelineCount = 10;

        // Queue depth thresholds
        public int queueDepthCaution = 10000;      // Yellow
        public int queueDepthWarning = 100000;     // Orange
        public int queueDepthCritical = 500000;    // Red

        @Override
        public String toString() {
            return "AggregationConfig{" +
                "enabled=" + enabled +
                ", topPipelineCount=" + topPipelineCount +
                ", queueDepthCaution=" + queueDepthCaution +
                ", queueDepthWarning=" + queueDepthWarning +
                ", queueDepthCritical=" + queueDepthCritical +
                '}';
        }
    }

    /**
     * Aggregated metrics data
     */
    public static class AggregatedMetrics {
        public int totalPipelines;
        public int healthyCount;
        public int cautionCount;
        public int warningCount;
        public int criticalCount;

        public double healthyPercentage;
        public double cautionPercentage;
        public double warningPercentage;
        public double criticalPercentage;

        public long totalQueueDepth;

        public double avgQueueDepth;

        public List<ProblemPipeline> problemPipelines;

        public AggregatedMetrics() {
            this.problemPipelines = new ArrayList<>();
        }

        public AggregatedMetrics(List<PipeMetrics> metrics,
                                AggregationConfig config) {
            this.totalPipelines = metrics.size();
            this.problemPipelines = new ArrayList<>();

            if (metrics.isEmpty()) {
                return;
            }

            // Count instances by level
            for (PipeMetrics m : metrics) {
                this.totalQueueDepth += m.currentQueueSize;

                // Categorize by status
                if (m.currentQueueSize > config.queueDepthCritical) {
                    this.criticalCount++;
                } else if (m.currentQueueSize > config.queueDepthWarning) {
                    this.warningCount++;
                } else if (m.currentQueueSize > config.queueDepthCaution) {
                    this.cautionCount++;
                } else {
                    this.healthyCount++;
                }
            }

            // Calculate percentages
            this.healthyPercentage = (double) healthyCount / totalPipelines * 100;
            this.cautionPercentage = (double) cautionCount / totalPipelines * 100;
            this.warningPercentage = (double) warningCount / totalPipelines * 100;
            this.criticalPercentage = (double) criticalCount / totalPipelines * 100;

            // Calculate averages
            this.avgQueueDepth = (double) totalQueueDepth / totalPipelines;

            // Extract problem pipelines
            extractProblemPipelines(metrics, config);
        }

        private void extractProblemPipelines(List<PipeMetrics> metrics,
                                             AggregationConfig config) {
            metrics.stream()
                .filter(m -> m.currentQueueSize > config.queueDepthWarning)
                .map(m -> new ProblemPipeline(m, config))
                .sorted(Comparator.comparingInt((ProblemPipeline p) -> p.severity).reversed())
                .limit(config.topPipelineCount)
                .forEach(problemPipelines::add);
        }
    }

    /**
     * Problem pipeline information
     */
    public static class ProblemPipeline {
        public String pipeName;
        public PipeMetrics metrics;
        public String status;
        public int severity;  // For sorting, higher is worse

        public ProblemPipeline(PipeMetrics metrics,
                              AggregationConfig config) {
            this.pipeName = metrics.pipeName;
            this.metrics = metrics;
            this.calculateStatus(config);
        }

        private void calculateStatus(AggregationConfig config) {
            if (metrics.currentQueueSize > config.queueDepthCritical) {
                this.status = "CRITICAL (Queue Overload)";
                this.severity = 900;
            } else if (metrics.currentQueueSize > config.queueDepthWarning) {
                this.status = "WARNING (Queue Building Up)";
                this.severity = 500;
            } else {
                this.status = "CAUTION (Monitoring)";
                this.severity = 100;
            }
        }
    }
}
