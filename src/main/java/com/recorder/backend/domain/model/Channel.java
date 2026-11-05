package com.recorder.backend.domain.model;

import java.util.List;

public record Channel(
    Long id,
    String name,
    String sourceUrl,
    StreamProtocol protocol,
    boolean enabled,
    OutputFormat format,
    List<ExclusionRule> exclusionList1,
    List<ExclusionRule> exclusionList2,
    List<ExclusionRule> allowedWindows,
    int blockDurationMinutes
) {

    public Channel(Long id, String name, String sourceUrl, StreamProtocol protocol, boolean enabled) {
        this(id, name, sourceUrl, protocol, enabled, OutputFormat.MKV, List.of(), List.of(), List.of(), 60);
    }

    public Channel(Long id, String name, String sourceUrl, StreamProtocol protocol, boolean enabled, OutputFormat format) {
        this(id, name, sourceUrl, protocol, enabled, format, List.of(), List.of(), List.of(), 60);
    }

    public Channel(Long id, String name, String sourceUrl, StreamProtocol protocol, boolean enabled,
                   OutputFormat format, List<ExclusionRule> exclusionList1, List<ExclusionRule> exclusionList2) {
        this(id, name, sourceUrl, protocol, enabled, format, exclusionList1, exclusionList2, List.of(), 60);
    }

    public Channel(Long id, String name, String sourceUrl, StreamProtocol protocol, boolean enabled,
                   OutputFormat format, List<ExclusionRule> exclusionList1, List<ExclusionRule> exclusionList2,
                   List<ExclusionRule> allowedWindows) {
        this(id, name, sourceUrl, protocol, enabled, format, exclusionList1, exclusionList2, allowedWindows, 60);
    }
}