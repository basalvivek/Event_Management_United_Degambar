package com.udjcs.cinematics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Controller
@RequestMapping("/digital-cinematics")
public class DigitalCinematicsController {

    private final DigitalCinematicsService service;
    private final ObjectMapper mapper;

    public DigitalCinematicsController(DigitalCinematicsService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper  = mapper;
    }

    @GetMapping
    public String index() {
        return "digital-cinematics/index";
    }

    @PostMapping("/run")
    @ResponseBody
    public Map<String, String> run(
        @RequestParam                     String action,
        @RequestParam(defaultValue = "")  String inputPath,
        @RequestParam(defaultValue = "")  String picsFolder,
        @RequestParam(defaultValue = "")  String outputPath,
        @RequestParam(defaultValue = "1") String quality,
        @RequestParam(defaultValue = "")  String ytTitle,
        @RequestParam(defaultValue = "")  String ytChannelId,
        @RequestParam(defaultValue = "public") String ytPrivacy
    ) {
        String jobId = service.createJob();
        switch (action) {
            case "compress_audio" -> service.compressAudio(jobId, inputPath.trim(), outputPath.trim());
            case "compress_video" -> service.compressVideo(jobId, inputPath.trim(), outputPath.trim(), quality);
            case "create_video"   -> service.createVideo(jobId, inputPath.trim(), picsFolder.trim(), outputPath.trim());
            case "upload_youtube" -> service.uploadYoutube(jobId, inputPath.trim(), ytTitle.trim(), ytChannelId.trim(), ytPrivacy);
            default               -> service.getJob(jobId).finish(true, "Unknown action: " + action);
        }
        return Map.of("jobId", jobId);
    }

    @GetMapping(value = "/progress/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter progress(@PathVariable String jobId) {
        SseEmitter emitter = new SseEmitter(600_000L);
        JobState job = service.getJob(jobId);
        if (job == null) { emitter.completeWithError(new IllegalArgumentException("Unknown job")); return emitter; }
        new Thread(() -> {
            int sent = 0;
            try {
                while (true) {
                    JobState.Snapshot snap = job.snapshot(sent);
                    sent += snap.newLogs().size();
                    emitter.send(SseEmitter.event().data(mapper.writeValueAsString(Map.of(
                        "pct",     snap.pct(),
                        "message", snap.message(),
                        "logs",    snap.newLogs(),
                        "done",    snap.done(),
                        "error",   snap.error(),
                        "result",  snap.result()
                    ))));
                    if (snap.done()) { emitter.complete(); break; }
                    Thread.sleep(250);
                }
            } catch (Exception e) { emitter.completeWithError(e); }
        }).start();
        return emitter;
    }

    // ── Local filesystem browser ────────────────────────────────────────────────
    @GetMapping("/browse")
    @ResponseBody
    public Map<String, Object> browse(
        @RequestParam(required = false) String path,
        @RequestParam(required = false, defaultValue = "all") String filter
    ) {
        Path dir;
        if (path == null || path.isBlank()) {
            dir = Path.of(System.getProperty("user.home"));
        } else {
            Path p = Path.of(path);
            dir = Files.isDirectory(p) ? p : (p.getParent() != null ? p.getParent() : p);
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.sorted((a, b) -> {
                boolean ad = Files.isDirectory(a), bd = Files.isDirectory(b);
                if (ad != bd) return ad ? -1 : 1;
                return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
            }).forEach(p -> {
                String name = p.getFileName().toString();
                if (name.startsWith(".")) return;
                boolean isDir = Files.isDirectory(p);
                if (!isDir && !matchesFilter(name, filter)) return;
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("name", name); e.put("path", p.toAbsolutePath().toString()); e.put("dir", isDir);
                if (!isDir) { try { e.put("size", fmtSize(Files.size(p))); } catch (IOException ex) { e.put("size", ""); } }
                entries.add(e);
            });
        } catch (Exception ignored) {}
        List<String> roots = new ArrayList<>();
        if (dir.getParent() == null)
            for (Path r : FileSystems.getDefault().getRootDirectories()) roots.add(r.toString());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("current", dir.toAbsolutePath().toString());
        result.put("parent",  dir.getParent() != null ? dir.getParent().toAbsolutePath().toString() : null);
        result.put("roots",   roots);
        result.put("entries", entries);
        return result;
    }

    private boolean matchesFilter(String name, String filter) {
        if ("all".equals(filter)) return true;
        String n = name.toLowerCase();
        return switch (filter) {
            case "audio" -> n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".aac")
                         || n.endsWith(".m4a") || n.endsWith(".flac") || n.endsWith(".ogg") || n.endsWith(".opus");
            case "video" -> n.endsWith(".mp4") || n.endsWith(".avi") || n.endsWith(".mov")
                         || n.endsWith(".mkv") || n.endsWith(".wmv") || n.endsWith(".m4v");
            default -> true;
        };
    }

    private String fmtSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format("%.1f MB", b / (1024.0 * 1024));
        return String.format("%.2f GB", b / (1024.0 * 1024 * 1024));
    }

    // ── YouTube info & channel management ──────────────────────────────────────
    @GetMapping("/yt-info")
    @ResponseBody
    public Map<String, Object> ytInfo() {
        return service.getYtInfo();
    }

    @GetMapping("/yt-channels")
    @ResponseBody
    public List<Map<String, Object>> ytChannels() {
        return service.getChannels();
    }

    @PostMapping("/yt-channels")
    @ResponseBody
    public Map<String, Object> addChannel(@RequestParam String label) {
        try {
            String id      = service.addChannel(label.trim());
            String oauthUrl = service.getOAuthUrl(id);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", id);
            r.put("oauthUrl", oauthUrl != null ? oauthUrl : "");
            return r;
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @DeleteMapping("/yt-channels/{id}")
    @ResponseBody
    public Map<String, String> removeChannel(@PathVariable String id) {
        try {
            service.removeChannel(id);
            return Map.of("status", "ok");
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    // ── OAuth flow ──────────────────────────────────────────────────────────────
    @GetMapping("/oauth/connect")
    public String oauthConnect(@RequestParam(required = false) String channelId) {
        if (channelId == null || channelId.isBlank())
            return "redirect:/digital-cinematics?ytError=No+channel+specified";
        try {
            String url = service.getOAuthUrl(channelId);
            if (url == null) return "redirect:/digital-cinematics?ytError="
                + URLEncoder.encode("client_secrets.json not found", StandardCharsets.UTF_8);
            return "redirect:" + url;
        } catch (Exception e) {
            return "redirect:/digital-cinematics?ytError="
                + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/oauth/callback")
    public String oauthCallback(@RequestParam(required = false) String code,
                                @RequestParam(required = false) String state,
                                @RequestParam(required = false) String error) {
        if (error != null || code == null)
            return "redirect:/digital-cinematics?ytError=OAuth+was+denied+or+cancelled";
        if (state == null || state.isBlank())
            return "redirect:/digital-cinematics?ytError=Invalid+OAuth+state";
        try {
            service.handleOAuthCallback(state, code);
            return "redirect:/digital-cinematics?ytSuccess=Channel+connected+successfully";
        } catch (Exception e) {
            return "redirect:/digital-cinematics?ytError="
                + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }
}
