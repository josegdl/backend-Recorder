package com.recorder.backend.infrastructure.config;

import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.StreamProtocol;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recorder")
public class RecorderProperties {

    private String outputDir = "recordings";
    private String timezone = "system";
    private final Cors cors = new Cors();
    private final Reconnect reconnect = new Reconnect();
    private final Scheduler scheduler = new Scheduler();
    private final List<ChannelProperties> channels = new ArrayList<>();

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public Cors getCors() {
        return cors;
    }

    public Reconnect getReconnect() {
        return reconnect;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public List<ChannelProperties> getChannels() {
        return channels;
    }

    public static class Reconnect {

        private int maxAttempts = 0;
        private Duration initialDelay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofSeconds(30);

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
        }

        public Duration getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
        }
    }

    public static class Scheduler {

        private boolean enabled = false;
        private int poolSize = 2;
        private int tickSeconds = 1;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            if (poolSize <= 0) {
                throw new IllegalArgumentException("poolSize debe ser positivo");
            }
            this.poolSize = poolSize;
        }

        public int getTickSeconds() {
            return tickSeconds;
        }

        public void setTickSeconds(int tickSeconds) {
            if (tickSeconds <= 0) {
                throw new IllegalArgumentException("tickSeconds debe ser positivo");
            }
            this.tickSeconds = tickSeconds;
        }
    }

    public static class Cors {

        private List<String> allowedOriginPatterns =
                List.of("http://localhost:[*]", "http://127.0.0.1:[*]");

        public List<String> getAllowedOriginPatterns() {
            return allowedOriginPatterns;
        }

        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = allowedOriginPatterns;
        }
    }

    public static class ChannelProperties {

        private Long id;
        private String name;
        private String sourceUrl;
        private StreamProtocol protocol;
        private boolean enabled = true;
        private OutputFormat format = OutputFormat.MKV;
        private int blockDurationMinutes = 60;
        private String scheduleWindow;
        private List<ExclusionRuleProperties> exclusionList1 = new ArrayList<>();
        private List<ExclusionRuleProperties> exclusionList2 = new ArrayList<>();
        private List<ExclusionRuleProperties> allowedWindows = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        public void setSourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
        }

        public StreamProtocol getProtocol() {
            return protocol;
        }

        public void setProtocol(StreamProtocol protocol) {
            this.protocol = protocol;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public OutputFormat getFormat() {
            return format;
        }

        public void setFormat(OutputFormat format) {
            this.format = format;
        }

        public int getBlockDurationMinutes() {
            return blockDurationMinutes;
        }

        public void setBlockDurationMinutes(int blockDurationMinutes) {
            if (blockDurationMinutes <= 0) {
                throw new IllegalArgumentException("blockDurationMinutes debe ser mayor a 0");
            }
            this.blockDurationMinutes = blockDurationMinutes;
        }

        public String getScheduleWindow() {
            return scheduleWindow;
        }

        public void setScheduleWindow(String scheduleWindow) {
            this.scheduleWindow = scheduleWindow;
        }

        public List<ExclusionRuleProperties> getExclusionList1() {
            return exclusionList1;
        }

        public void setExclusionList1(List<ExclusionRuleProperties> exclusionList1) {
            this.exclusionList1 = exclusionList1;
        }

        public List<ExclusionRuleProperties> getExclusionList2() {
            return exclusionList2;
        }

        public void setExclusionList2(List<ExclusionRuleProperties> exclusionList2) {
            this.exclusionList2 = exclusionList2;
        }

        public List<ExclusionRuleProperties> getAllowedWindows() {
            return allowedWindows;
        }

        public void setAllowedWindows(List<ExclusionRuleProperties> allowedWindows) {
            this.allowedWindows = allowedWindows;
        }
    }

    public static class ExclusionRuleProperties {

        private java.time.DayOfWeek dayOfWeek;
        private LocalTime start;
        private LocalTime end;

        public java.time.DayOfWeek getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(java.time.DayOfWeek dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public LocalTime getStart() {
            return start;
        }

        public void setStart(LocalTime start) {
            this.start = start;
        }

        public LocalTime getEnd() {
            return end;
        }

        public void setEnd(LocalTime end) {
            this.end = end;
        }
    }
}
