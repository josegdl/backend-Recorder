package com.recorder.backend.domain.model;

public enum OutputFormat {
    MKV(".mkv", "video"),
    MP3(".mp3", "audio");

    private final String extension;
    private final String mediaType;

    OutputFormat(String extension, String mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() {
        return extension;
    }

    public String mediaType() {
        return mediaType;
    }
}
