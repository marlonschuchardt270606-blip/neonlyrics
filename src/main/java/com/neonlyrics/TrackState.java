package com.neonlyrics;

import net.minecraft.util.Identifier;
import java.util.List;

public class TrackState {
    public static volatile String title, artist, trackKey = "";
    public static volatile long durationMs, baseMs, sampledAt;
    public static volatile boolean playing;
    public static volatile Identifier cover;
    public static volatile List<LrcParser.Line> lines = List.of();

    public static long pos() {
        long p = baseMs + (playing ? System.currentTimeMillis() - sampledAt : 0);
        return Math.max(0, durationMs > 0 ? Math.min(p, durationMs) : p);
    }
    public static int idx(long p) {
        List<LrcParser.Line> l = lines; int r = -1;
        for (int i = 0; i < l.size(); i++) { if (l.get(i).ms() <= p) r = i; else break; }
        return r;
    }
    public static float frac(int i, long p) {
        List<LrcParser.Line> l = lines;
        if (i < 0 || i >= l.size()) return 0;
        long a = l.get(i).ms();
        long b = i + 1 < l.size() ? l.get(i + 1).ms() : a + 4000;
        long len = Math.min(Math.max(1, b - a), 6000);
        return Math.max(0f, Math.min(1f, (p - a) / (float) len));
    }
    public static String text(int i) {
        List<LrcParser.Line> l = lines;
        return (i >= 0 && i < l.size()) ? l.get(i).text() : "";
    }
}
