@echo off
setlocal enabledelayedexpansion

echo Creando estructura de carpetas...
set BASE=src\main\java\com\recorder\backend

mkdir "%BASE%\config" 2>nul
mkdir "%BASE%\domain\model" 2>nul
mkdir "%BASE%\domain\port\in" 2>nul
mkdir "%BASE%\domain\port\out" 2>nul
mkdir "%BASE%\domain\service" 2>nul
mkdir "%BASE%\infrastructure\adapter\in\rest" 2>nul
mkdir "%BASE%\infrastructure\adapter\in\scheduler" 2>nul
mkdir "%BASE%\infrastructure\adapter\out\persistence\entity" 2>nul
mkdir "%BASE%\infrastructure\adapter\out\persistence\repository" 2>nul
mkdir "%BASE%\infrastructure\adapter\out\streaming\adapter" 2>nul
mkdir "%BASE%\infrastructure\exception" 2>nul

echo Generando archivos del Dominio...

(
echo package com.recorder.backend.domain.model;
echo.
echo public enum StreamProtocol {
echo     M3U8_HLS,
echo     RTMP,
echo     YOUTUBE,
echo     FACEBOOK,
echo     TV_TUNER
echo }
) > "%BASE%\domain\model\StreamProtocol.java"

(
echo package com.recorder.backend.domain.model;
echo.
echo public enum RecordingStatus {
echo     IDLE,
echo     CONNECTING,
echo     RECORDING,
echo     RECONNECTING,
echo     COMPLETED,
echo     FAILED
echo }
) > "%BASE%\domain\model\RecordingStatus.java"

(
echo package com.recorder.backend.domain.model;
echo.
echo public record Channel(
echo     Long id,
echo     String name,
echo     String sourceUrl,
echo     StreamProtocol protocol,
echo     boolean enabled
echo ^) {}
) > "%BASE%\domain\model\Channel.java"

(
echo package com.recorder.backend.domain.model;
echo.
echo import java.time.LocalDateTime;
echo.
echo public record RecordingTask(
echo     Long id,
echo     Channel channel,
echo     LocalDateTime scheduledStart,
echo     LocalDateTime scheduledEnd,
echo     String outputFilePath,
echo     RecordingStatus status
echo ^) {}
) > "%BASE%\domain\model\RecordingTask.java"

echo Generando Puertos...

(
echo package com.recorder.backend.domain.port.out;
echo.
echo import com.recorder.backend.domain.model.Channel;
echo import com.recorder.backend.domain.model.StreamProtocol;
echo import java.io.File;
echo.
echo public interface StreamCapturePort {
echo     boolean supports(StreamProtocol protocol^);
echo     Process startCapture(Channel channel, File outputFile^);
echo     void stopCapture(Long channelId^);
echo }
) > "%BASE%\domain\port\out\StreamCapturePort.java"

(
echo package com.recorder.backend.domain.port.in;
echo.
echo import com.recorder.backend.domain.model.RecordingStatus;
echo.
echo public interface RecordingServicePort {
echo     void startChannelRecording(Long channelId^);
echo     void stopChannelRecording(Long channelId^);
echo     RecordingStatus getChannelStatus(Long channelId^);
echo }
) > "%BASE%\domain\port\in\RecordingServicePort.java"

echo Generando Infraestructura y Concurrencia...

(
echo package com.recorder.backend.infrastructure.adapter.out.streaming;
echo.
echo import com.recorder.backend.domain.model.Channel;
echo import org.slf4j.Logger;
echo import org.slf4j.LoggerFactory;
echo import java.io.BufferedReader;
echo import java.io.File;
echo import java.io.InputStreamReader;
echo import java.util.concurrent.atomic.AtomicBoolean;
echo.
echo public class FFmpegProcessWorker implements Runnable {
echo     private static final Logger log = LoggerFactory.getLogger(FFmpegProcessWorker.class^);
echo     private final Channel channel;
echo     private final File outputDir;
echo     private final AtomicBoolean running = new AtomicBoolean(false^);
echo     private Process currentProcess;
echo.
echo     public FFmpegProcessWorker(Channel channel, File outputDir^) {
echo         this.channel = channel;
echo         this.outputDir = outputDir;
echo     }
echo.
echo     @Override
echo     public void run(^) {
echo         running.set(true^);
echo         int attempt = 0;
echo         while (running.get(^)^) {
echo             try {
echo                 attempt++;
echo                 log.info("Iniciando captura de canal {} (Intento {})", channel.name(^), attempt^);
echo                 ProcessBuilder pb = buildFFmpegProcess(^);
echo                 pb.redirectErrorStream(true^);
echo                 currentProcess = pb.start(^);
echo                 try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream(^)^)^)^) {
echo                     String line;
echo                     while ((line = reader.readLine(^)^) != null^) {
echo                         // Drenar buffer
echo                     }
echo                 }
echo                 int exitCode = currentProcess.waitFor(^);
echo                 if (exitCode != 0 ^&^& running.get(^)^) {
echo                     log.warn("FFmpeg finalizo con codigo {}. Reintentando...", exitCode^);
echo                     Thread.sleep(Math.min(1000L * (1 ^<^< Math.min(attempt, 5^)^), 30000L^)^);
echo                 } else {
echo                     break;
echo                 }
echo             } catch (InterruptedException e^) {
echo                 Thread.currentThread(^).interrupt(^);
echo                 break;
echo             } catch (Exception e^) {
echo                 log.error("Error en captura", e^);
echo             }
echo         }
echo     }
echo.
echo     private ProcessBuilder buildFFmpegProcess(^) {
echo         String outFile = new File(outputDir, channel.id(^) + "_" + System.currentTimeMillis(^) + ".mkv"^).getAbsolutePath(^);
echo         return new ProcessBuilder("ffmpeg", "-y", "-reconnect", "1", "-i", channel.sourceUrl(^), "-c", "copy", outFile^);
echo     }
echo.
echo     public void terminate(^) {
echo         running.set(false^);
echo         if (currentProcess != null ^&^& currentProcess.isAlive(^)^) {
echo             currentProcess.destroy(^);
echo         }
echo     }
echo }
) > "%BASE%\infrastructure\adapter\out\streaming\FFmpegProcessWorker.java"

(
echo package com.recorder.backend.infrastructure.adapter.out.streaming;
echo.
echo import com.recorder.backend.domain.model.Channel;
echo import org.springframework.stereotype.Component;
echo import java.io.File;
echo import java.util.concurrent.ConcurrentHashMap;
echo import java.util.concurrent.ExecutorService;
echo import java.util.concurrent.Executors;
echo import java.util.concurrent.Future;
echo.
echo @Component
echo public class StreamSupervisorPool {
echo     private final ConcurrentHashMap^<Long, FFmpegProcessWorker^> activeWorkers = new ConcurrentHashMap^<^>(^);
echo     private final ConcurrentHashMap^<Long, Future^<?^>^> runningTasks = new ConcurrentHashMap^<^>(^);
echo     private final ExecutorService executor = Executors.newCachedThreadPool(^);
echo.
echo     public synchronized void spawnWorker(Channel channel, File outputDir^) {
echo         if (activeWorkers.containsKey(channel.id(^)^)^) return;
echo         FFmpegProcessWorker worker = new FFmpegProcessWorker(channel, outputDir^);
echo         activeWorkers.put(channel.id(^), worker^);
echo         runningTasks.put(channel.id(^), executor.submit(worker^)^);
echo     }
echo.
echo     public synchronized void killWorker(Long channelId^) {
echo         FFmpegProcessWorker worker = activeWorkers.remove(channelId^);
echo         if (worker != null^) worker.terminate(^);
echo         Future^<?^> task = runningTasks.remove(channelId^);
echo         if (task != null^) task.cancel(true^);
echo     }
echo }
) > "%BASE%\infrastructure\adapter\out\streaming\StreamSupervisorPool.java"

echo Generando adaptadores REST...

(
echo package com.recorder.backend.infrastructure.adapter.in.rest;
echo.
echo import org.springframework.web.bind.annotation.RequestMapping;
echo import org.springframework.web.bind.annotation.RestController;
echo.
echo @RestController
echo @RequestMapping("/api/v1/channels"^)
echo public class ChannelController {
echo }
) > "%BASE%\infrastructure\adapter\in\rest\ChannelController.java"

(
echo package com.recorder.backend.infrastructure.adapter.in.rest;
echo.
echo import org.springframework.web.bind.annotation.RequestMapping;
echo import org.springframework.web.bind.annotation.RestController;
echo.
echo @RestController
echo @RequestMapping("/api/v1/monitoring"^)
echo public class MonitorController {
echo }
) > "%BASE%\infrastructure\adapter\in\rest\MonitorController.java"

echo Generando .gitignore...
(
echo target/
echo !.mvn/wrapper/maven-wrapper.jar
echo *.class
echo .classpath
echo .project
echo .settings/
echo .idea/
echo *.iml
echo *.log
echo *.mkv
echo *.mp4
) > .gitignore

echo Estructura generada correctamente.