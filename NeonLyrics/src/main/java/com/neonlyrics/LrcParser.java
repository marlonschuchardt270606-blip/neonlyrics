package com.neonlyrics;

import java.util.*;
import java.util.regex.*;

public class LrcParser {
    public record Line(long ms, String text) {}
    static final Pattern TS = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]");

    public static List<Line> parse(String lrc) {
        List<Line> out = new ArrayList<>();
        if (lrc == null) return out;
        for (String raw : lrc.split("\\R")) {
            Matcher m = TS.matcher(raw);
            List<Long> times = new ArrayList<>();
            int end = 0;
            while (m.find() && m.start() == end) {
                long min = Long.parseLong(m.group(1)), sec = Long.parseLong(m.group(2));
                String f = m.group(3);
                long ms = f == null ? 0 : (f.length() == 1 ? Long.parseLong(f) * 100 : f.length() == 2 ? Long.parseLong(f) * 10 : Long.parseLong(f.substring(0, 3)));
                times.add(min * 60000 + sec * 1000 + ms);
                end = m.end();
            }
            String text = raw.substring(end).trim();
            for (long t : times) out.add(new Line(t, text));
        }
        out.sort(Comparator.comparingLong(Line::ms));
        return out;
    }
}
