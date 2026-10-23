package com.recorder.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.recorder.backend.domain.port.out.StreamCapturePort;
import com.recorder.backend.infrastructure.adapter.out.streaming.StreamSupervisorPool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RecorderApplicationTests {

    @Autowired
    private StreamCapturePort streamCapturePort;

    @Autowired
    private StreamSupervisorPool streamSupervisorPool;

    @Test
    void cargaElContextoYRegistraLosBeansDeIngesta() {
        assertThat(streamCapturePort).isNotNull();
        assertThat(streamSupervisorPool).isNotNull();
    }
}
