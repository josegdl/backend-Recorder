package com.recorder.backend.domain.exception;

public class ChannelNotFoundException extends RuntimeException {

    public ChannelNotFoundException(Long channelId) {
        super("No se encontro el canal con id " + channelId);
    }
}
