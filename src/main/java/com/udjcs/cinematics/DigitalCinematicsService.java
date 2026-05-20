package com.udjcs.cinematics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;
import java.util.stream.Stream;

@Service
public class DigitalCinematicsService {

    private static final double PHOTO_DUR   = 5.0;
    private static final double XFADE_DUR   = 1.2;
    private static final String[] XFADE_TYPES = {
        "smoothleft","smoothright","smoothup","smoothdown","circleopen","circleclose",
        "radial","zoomin","diagtr","diagbl","horzopen","vertopen","dissolve","fadeblack"
    };
    private static final String[] KB_Z = {
        "min(zoom+0.0015,1.5)",
        "if(lte(zoom,1.0),1.5,max(1.001,zoom-0.0015))",
        "1.4","1.4","1.4","1.4",
        "min(zoom+0.0015,1.5)","min(zoom+0.0015,1.5)",
        "min(zoom+0.0015,1.5)","min(zoom+0.0015,1.5)",
        "min(zoom+0.0018,1.6)","1.05"
    };
    private static final String[] KB_X = {
        "iw/2-(iw/zoom/2)","iw/2-(iw/zoom/2)",
        "if(lte(on,1),0,min(iw-(iw/zoom),x+3))",
        "if(lte(on,1),iw-(iw/zoom),max(0,x-3))",
        "iw/2-(iw/zoom/2)","iw/2-(iw/zoom/2)",
        "0","iw-(iw/zoom)","0","iw-(iw/zoom)",
        "min(iw-(iw/zoom),on*2)","iw/2-(iw/zoom/2)"
    };
    private static final String[] KB_Y = {
        "ih/2-(ih/zoom/2)","ih/2-(ih/zoom/2)",
        "ih/2-(ih/zoom/2)","ih/2-(ih/zoom/2)",
        "if(lte(on,1),0,min(ih-(ih/zoom),y+3))",
        "if(lte(on,1),ih-(ih/zoom),max(0,y-3))",
        "0","0","ih-(ih/zoom)","ih-(ih/zoom)",
        "min(ih-(ih/zoom),on*2)","ih/2-(ih/zoom/2)"
    };

    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${app.youtube.client-secrets:client_secrets.json}")
    private String clientSecretsPath;

    @Value("${server.port:8082}")
    private int serverPort;

    public DigitalCinematicsService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String createJob() {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jobs.put(id, new JobState());
        return id;
    }

    public JobState getJob(String id) {
        return jobs.get(id);
    }

    // ── Compress Audio ──────────────────────────────────────────────────────────
    public void compressAudio(String jobId, String inputPath, String outputPath) {
        JobState job = jobs.get(jobId);
        new Thread(() -> {
            try {
                if (!Files.exists(Path.of(inputPath))) {
                    job.finish(true, "File not found: " + inputPath); return;
                }
                String out = resolveOutput(outputPath, inputPath, ".m4a");
                job.setProgress(2, "Probing duration…");
                double dur = probeDuration(inputPath);
                job.setProgress(5, "Encoding AAC VBR…");
                List<String> cmd = new ArrayList<>(List.of(
                    "ffmpeg","-y","-i",inputPath,
                    "-progress","pipe:1","-nostats",
                    "-c:a","aac","-q:a","2", out));
                if (runFfmpeg(job, cmd, dur)) job.finish(false, out);
            } catch (Exception e) { job.finish(true, e.getMessage()); }
        }).start();
    }

    // ── Compress Video ──────────────────────────────────────────────────────────
    public void compressVideo(String jobId, String inputPath, String outputPath, String quality) {
        JobState job = jobs.get(jobId);
        new Thread(() -> {
            try {
                if (!Files.exists(Path.of(inputPath))) {
                    job.finish(true, "File not found: " + inputPath); return;
                }
                String out  = resolveOutput(outputPath, inputPath, ".mp4");
                boolean h265 = "2".equals(quality);
                job.setProgress(2, "Probing duration…");
                double dur = probeDuration(inputPath);
                job.setProgress(5, "Encoding " + (h265 ? "H.265" : "H.264") + "…");
                List<String> cmd = new ArrayList<>(List.of(
                    "ffmpeg","-y","-i",inputPath,
                    "-progress","pipe:1","-nostats",
                    "-c:v", h265 ? "libx265" : "libx264",
                    "-crf", h265 ? "24" : "23",
                    "-preset","medium"));
                if (h265) cmd.addAll(List.of("-tag:v","hvc1"));
                cmd.addAll(List.of("-c:a","copy","-movflags","+faststart", out));
                if (runFfmpeg(job, cmd, dur)) job.finish(false, out);
            } catch (Exception e) { job.finish(true, e.getMessage()); }
        }).start();
    }

    // ── Create Video (animated photo album) ─────────────────────────────────────
    public void createVideo(String jobId, String audioPath, String picsFolder, String outputPath) {
        JobState job = jobs.get(jobId);
        new Thread(() -> {
            try {
                if (!Files.exists(Path.of(audioPath))) {
                    job.finish(true, "Audio file not found: " + audioPath); return;
                }
                Path pics = Path.of(picsFolder);
                if (!Files.isDirectory(pics)) {
                    job.finish(true, "Photos folder not found: " + picsFolder); return;
                }
                job.setProgress(2, "Scanning photos…");
                List<Path> images = new ArrayList<>();
                try (Stream<Path> walk = Files.walk(pics, 1)) {
                    walk.filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".jpg") || n.endsWith(".jpeg")
                            || n.endsWith(".png")  || n.endsWith(".webp");
                    }).sorted().forEach(images::add);
                }
                if (images.isEmpty()) { job.finish(true, "No images found in: " + picsFolder); return; }
                job.setProgress(5, "Found " + images.size() + " photos. Building filter complex…");

                String defaultOut = pics.getParent() != null
                    ? pics.getParent().resolve("album.mp4").toString()
                    : pics.resolve("album.mp4").toString();
                String out = resolveOutput(outputPath, defaultOut, ".mp4");

                double totalDur = images.size() * PHOTO_DUR - (images.size() - 1) * XFADE_DUR;
                List<String> cmd = buildAlbumCmd(images, audioPath, out);
                if (runFfmpeg(job, cmd, totalDur)) job.finish(false, out);
            } catch (Exception e) { job.finish(true, e.getMessage()); }
        }).start();
    }

    private List<String> buildAlbumCmd(List<Path> images, String audio, String out) throws IOException {
        int n = images.size();
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg"); cmd.add("-y");
        for (Path img : images) {
            cmd.addAll(List.of("-loop","1","-t", String.valueOf(PHOTO_DUR), "-i", img.toString()));
        }
        cmd.addAll(List.of("-i", audio));

        StringBuilder fc = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int ki = i % KB_Z.length;
            fc.append("[").append(i).append(":v]")
              .append("scale=1920:1080:force_original_aspect_ratio=increase,crop=1920:1080,")
              .append("eq=contrast=1.08:brightness=0.03:saturation=1.15,")
              .append("zoompan=z='").append(KB_Z[ki]).append("'")
              .append(":x='").append(KB_X[ki]).append("'")
              .append(":y='").append(KB_Y[ki]).append("'")
              .append(":d=125:s=1920x1080:fps=25,")
              .append("vignette=angle=0.785")
              .append("[v").append(i).append("];");
        }
        if (n == 1) {
            fc.append("[v0]fade=type=in:duration=0.6,")
              .append("fade=type=out:start_time=").append(PHOTO_DUR - 0.6).append(":duration=0.6[vout];");
        } else {
            for (int i = 0; i < n - 1; i++) {
                String inA  = i == 0 ? "[v0]" : "[xf" + (i - 1) + "]";
                String inB  = "[v" + (i + 1) + "]";
                String outX = i == n - 2 ? "[vout]" : "[xf" + i + "]";
                double offset = (i + 1) * (PHOTO_DUR - XFADE_DUR);
                fc.append(inA).append(inB)
                  .append("xfade=transition=").append(XFADE_TYPES[i % XFADE_TYPES.length])
                  .append(":duration=").append(XFADE_DUR)
                  .append(":offset=").append(String.format("%.2f", offset))
                  .append(outX).append(";");
            }
        }

        Path scriptFile = Files.createTempFile("cine_fc_", ".txt");
        Files.writeString(scriptFile, fc.toString());
        scriptFile.toFile().deleteOnExit();

        cmd.addAll(List.of("-filter_complex_script", scriptFile.toString()));
        cmd.addAll(List.of("-map","[vout]","-map", n + ":a",
            "-progress","pipe:1","-nostats",
            "-c:v","libx264","-crf","18","-preset","fast",
            "-c:a","aac","-b:a","192k","-ar","48000",
            "-pix_fmt","yuv420p","-shortest", out));
        return cmd;
    }

    // ── Channel management ──────────────────────────────────────────────────────
    private Path channelsFile() {
        return Path.of(clientSecretsPath).toAbsolutePath().resolveSibling("yt_channels.json");
    }

    private Path tokenFile(String channelId) {
        return Path.of(clientSecretsPath).toAbsolutePath()
                   .resolveSibling("yt_token_" + channelId + ".json");
    }

    public List<Map<String, Object>> getChannels() {
        Path cf = channelsFile();
        if (!Files.exists(cf)) return List.of();
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode ch : mapper.readTree(cf.toFile())) {
                String id = ch.path("id").asText();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",        id);
                m.put("label",     ch.path("label").asText());
                m.put("connected", Files.exists(tokenFile(id)));
                result.add(m);
            }
            return result;
        } catch (Exception e) { return List.of(); }
    }

    public String addChannel(String label) throws IOException {
        String id = "ch" + Long.toHexString(System.currentTimeMillis());
        Path cf = channelsFile();
        List<Map<String, Object>> channels = new ArrayList<>();
        if (Files.exists(cf)) {
            for (JsonNode ch : mapper.readTree(cf.toFile())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",    ch.path("id").asText());
                m.put("label", ch.path("label").asText());
                channels.add(m);
            }
        }
        Map<String, Object> newCh = new LinkedHashMap<>();
        newCh.put("id", id); newCh.put("label", label);
        channels.add(newCh);
        mapper.writerWithDefaultPrettyPrinter().writeValue(cf.toFile(), channels);
        return id;
    }

    public void removeChannel(String id) throws IOException {
        Path cf = channelsFile();
        if (!Files.exists(cf)) return;
        List<Map<String, Object>> channels = new ArrayList<>();
        for (JsonNode ch : mapper.readTree(cf.toFile())) {
            if (!id.equals(ch.path("id").asText())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",    ch.path("id").asText());
                m.put("label", ch.path("label").asText());
                channels.add(m);
            }
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(cf.toFile(), channels);
        Files.deleteIfExists(tokenFile(id));
    }

    // ── Upload to YouTube ───────────────────────────────────────────────────────
    public void uploadYoutube(String jobId, String videoPath, String title,
                              String channelId, String privacy) {
        JobState job = jobs.get(jobId);
        new Thread(() -> {
            try {
                if (!Files.exists(Path.of(videoPath))) {
                    job.finish(true, "File not found: " + videoPath); return;
                }
                if (channelId == null || channelId.isBlank()) {
                    job.finish(true, "No channel selected."); return;
                }
                Path secretsFile = Path.of(clientSecretsPath);
                if (!Files.exists(secretsFile)) {
                    job.finish(true, "client_secrets.json not found. Run setup first."); return;
                }
                job.setProgress(3, "Loading credentials for channel…");
                JsonNode installed  = getInstalled(secretsFile);
                String clientId     = installed.path("client_id").asText();
                String clientSecret = installed.path("client_secret").asText();
                Path tf = tokenFile(channelId);
                if (!Files.exists(tf)) {
                    job.finish(true, "Channel not connected. Authorise it first."); return;
                }
                job.setProgress(6, "Refreshing access token…");
                String accessToken = loadOrRefreshToken(tf, clientId, clientSecret);
                if (accessToken == null) {
                    job.finish(true, "Token refresh failed. Re-connect the channel."); return;
                }
                job.setProgress(10, "Initiating resumable upload…");
                long fileSize = Files.size(Path.of(videoPath));
                String uploadUri = initiateResumableUpload(accessToken, title, privacy, fileSize);
                job.setProgress(12, "Uploading " + (fileSize / 1_048_576) + " MB…");
                String ytUrl = uploadInChunks(uploadUri, videoPath, fileSize, job);
                if (ytUrl != null) job.finish(false, ytUrl);
                else               job.finish(true, "Upload completed but no video ID returned");
            } catch (Exception e) { job.finish(true, e.getMessage()); }
        }).start();
    }

    // ── YouTube info & OAuth ────────────────────────────────────────────────────
    public Map<String, Object> getYtInfo() {
        Path secretsFile = Path.of(clientSecretsPath).toAbsolutePath();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("secretsPath",   secretsFile.toString());
        info.put("secretsExists", Files.exists(secretsFile));
        return info;
    }

    public String getOAuthUrl(String channelId) throws IOException {
        Path secretsFile = Path.of(clientSecretsPath);
        if (!Files.exists(secretsFile)) return null;
        JsonNode inst   = getInstalled(secretsFile);
        String clientId = inst.path("client_id").asText();
        String redirect = "http://localhost:" + serverPort + "/digital-cinematics/oauth/callback";
        String scope    = "https://www.googleapis.com/auth/youtube.upload";
        return "https://accounts.google.com/o/oauth2/auth"
            + "?client_id="     + enc(clientId)
            + "&redirect_uri="  + enc(redirect)
            + "&response_type=code"
            + "&scope="         + enc(scope)
            + "&access_type=offline&prompt=consent"
            + "&state="         + enc(channelId);
    }

    public void handleOAuthCallback(String channelId, String code) throws Exception {
        Path secretsFile    = Path.of(clientSecretsPath);
        JsonNode inst       = getInstalled(secretsFile);
        String clientId     = inst.path("client_id").asText();
        String clientSecret = inst.path("client_secret").asText();
        String redirect     = "http://localhost:" + serverPort + "/digital-cinematics/oauth/callback";
        String body = "code=" + enc(code)
            + "&client_id="     + enc(clientId)
            + "&client_secret=" + enc(clientSecret)
            + "&redirect_uri="  + enc(redirect)
            + "&grant_type=authorization_code";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("https://oauth2.googleapis.com/token"))
            .header("Content-Type","application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode token = mapper.readTree(resp.body());
        if (!token.has("access_token"))
            throw new RuntimeException("OAuth failed: " + resp.body());
        mapper.writerWithDefaultPrettyPrinter().writeValue(tokenFile(channelId).toFile(), token);
    }

    private String loadOrRefreshToken(Path tokenFile, String clientId, String clientSecret) throws Exception {
        if (!Files.exists(tokenFile)) return null;
        JsonNode token        = mapper.readTree(tokenFile.toFile());
        String  refreshToken  = token.path("refresh_token").asText(null);
        if (refreshToken == null) return null;
        String body = "grant_type=refresh_token"
            + "&refresh_token=" + enc(refreshToken)
            + "&client_id="    + enc(clientId)
            + "&client_secret=" + enc(clientSecret);
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("https://oauth2.googleapis.com/token"))
            .header("Content-Type","application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode newTok = mapper.readTree(resp.body());
        if (!newTok.has("access_token")) return null;
        ObjectNode merged = (ObjectNode) token.deepCopy();
        merged.put("access_token", newTok.path("access_token").asText());
        mapper.writerWithDefaultPrettyPrinter().writeValue(tokenFile.toFile(), merged);
        return newTok.path("access_token").asText();
    }

    private String initiateResumableUpload(String accessToken, String title,
                                           String privacy, long fileSize) throws Exception {
        String metadata = "{\"snippet\":{\"title\":" + mapper.writeValueAsString(title)
            + ",\"description\":\"\"},\"status\":{\"privacyStatus\":\"" + privacy + "\"}}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("https://www.googleapis.com/upload/youtube/v3/videos"
                + "?uploadType=resumable&part=snippet,status"))
            .header("Authorization","Bearer " + accessToken)
            .header("Content-Type","application/json")
            .header("X-Upload-Content-Type","video/mp4")
            .header("X-Upload-Content-Length", String.valueOf(fileSize))
            .POST(HttpRequest.BodyPublishers.ofString(metadata)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.headers().firstValue("Location")
            .orElseThrow(() -> new RuntimeException("Upload initiation failed: " + resp.body()));
    }

    private String uploadInChunks(String uploadUri, String videoPath,
                                   long fileSize, JobState job) throws Exception {
        int chunkSize = 5 * 1024 * 1024;
        long offset = 0;
        try (InputStream is = Files.newInputStream(Path.of(videoPath))) {
            while (offset < fileSize) {
                long end   = Math.min(offset + chunkSize - 1, fileSize - 1);
                int  len   = (int)(end - offset + 1);
                byte[] buf = is.readNBytes(len);
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUri))
                    .header("Content-Type","video/mp4")
                    .header("Content-Range","bytes " + offset + "-" + end + "/" + fileSize)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(buf)).build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                offset = end + 1;
                int pct = 12 + (int)(offset * 87 / fileSize);
                job.setProgress(pct, "Uploading… " + pct + "%  ("
                    + (offset / 1_048_576) + "/" + (fileSize / 1_048_576) + " MB)");
                if (resp.statusCode() == 200 || resp.statusCode() == 201) {
                    return "https://www.youtube.com/watch?v="
                        + mapper.readTree(resp.body()).path("id").asText();
                }
            }
        }
        return null;
    }

    // ── FFmpeg helpers ─────────────────────────────────────────────────────────
    private boolean runFfmpeg(JobState job, List<String> cmd, double duration)
        throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process proc = pb.start();
        new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) job.addLog(line);
            } catch (IOException ignored) {}
        }).start();
        Pattern p = Pattern.compile("out_time_ms=(\\d+)");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.find() && duration > 0) {
                    int pct = Math.min((int)(Long.parseLong(m.group(1)) / 1_000_000.0 / duration * 100), 98);
                    job.setProgress(pct, "Processing… " + pct + "%");
                }
            }
        }
        // stdout EOF — all frames encoded; waiting for muxer to flush
        job.setProgress(99, "Finalizing output file…");
        int rc = proc.waitFor();
        if (rc != 0) job.finish(true, "FFmpeg exited with code " + rc);
        return rc == 0;
    }

    private double probeDuration(String path) {
        try {
            Process p = new ProcessBuilder(
                "ffprobe","-v","error","-show_entries","format=duration",
                "-of","default=noprint_wrappers=1:nokey=1", path).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return Double.parseDouble(out);
        } catch (Exception e) { return -1; }
    }

    private JsonNode getInstalled(Path secretsFile) throws IOException {
        JsonNode root = mapper.readTree(secretsFile.toFile());
        return root.path("installed").isMissingNode() ? root.path("web") : root.path("installed");
    }

    private String resolveOutput(String outputPath, String inputPath, String ext) {
        if (outputPath == null || outputPath.isBlank()) {
            return stripExt(inputPath) + "_compressed" + ext;
        }
        Path out = Path.of(outputPath);
        if (Files.isDirectory(out)) {
            String baseName = stripExt(Path.of(inputPath).getFileName().toString());
            return out.resolve(baseName + "_compressed" + ext).toString();
        }
        // Ensure correct extension
        return outputPath.toLowerCase().endsWith(ext) ? outputPath : outputPath + ext;
    }

    private String stripExt(String path) {
        int dot = path.lastIndexOf('.');
        return dot > 0 ? path.substring(0, dot) : path;
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
