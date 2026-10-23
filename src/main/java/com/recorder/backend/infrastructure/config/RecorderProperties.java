package com.recorder.backend.infrastructure.config;

import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.StreamProtocol;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recorder")
public class RecorderProperties {

    private String outputDir = "recordings";
    private final Cors cors = new Cors();
    private final Reconnect reconnect = new Reconnect();
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
    }
}
