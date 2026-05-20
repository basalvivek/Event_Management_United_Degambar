package com.udjcs.cinematics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class JobState {

    private final ReentrantLock lock = new ReentrantLock();
    private int pct;
    private String message = "Starting…";
    private final List<String> logLines = new ArrayList<>();
    private boolean done;
    private boolean error;
    private String result = "";

    public void addLog(String line) {
        if (line == null || line.isBlank()) return;
        lock.lock();
        try { logLines.add(line); message = line; }
        finally { lock.unlock(); }
    }

    public void setProgress(int pct, String msg) {
        lock.lock();
        try {
            this.pct = pct;
            if (msg != null && !msg.isBlank()) { this.message = msg; logLines.add(msg); }
        } finally { lock.unlock(); }
    }

    public void finish(boolean error, String result) {
        lock.lock();
        try {
            if (!error) this.pct = 100;
            this.done   = true;
            this.error  = error;
            this.result = result != null ? result : "";
            String msg  = error ? "ERROR: " + this.result : "Done. " + this.result;
            logLines.add(msg);
            this.message = msg;
        } finally { lock.unlock(); }
    }

    public Snapshot snapshot(int from) {
        lock.lock();
        try {
            List<String> newLogs = from < logLines.size()
                ? new ArrayList<>(logLines.subList(from, logLines.size()))
                : List.of();
            return new Snapshot(pct, message, newLogs, done, error, result);
        } finally { lock.unlock(); }
    }

    public record Snapshot(int pct, String message, List<String> newLogs,
                           boolean done, boolean error, String result) {}
}
