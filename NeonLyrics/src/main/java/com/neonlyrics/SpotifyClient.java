package com.neonlyrics;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

public final class SpotifyClient {
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final String REDIRECT = "http://127.0.0.1:8898/callback";
    static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    public static volatile String status = "Nicht verbunden";
    static String verifier;
    static HttpServer server;
    static int coverCounter = 0;
    static final long DEMO_LEN = 40000;
    static long demoStart = System.currentTimeMillis();
    static final ScheduledExecutorService EX = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "NeonLyrics-IO"); t.setDaemon(true); return t;
    });

    public static void init() { EX.scheduleWithFixedDelay(SpotifyClient::poll, 1, 2, TimeUnit.SECONDS); }

    // ---------- OAuth (PKCE, kein Client-Secret noetig) ----------
    public static void connect() {
        Config c = Config.I;
        if (c.clientId.isBlank()) { status = "Client-ID fehlt"; return; }
        try {
            byte[] b = new byte[64]; new SecureRandom().nextBytes(b);
            verifier = B64.encodeToString(b);
            String challenge = B64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
            if (server != null) server.stop(0);
            server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8898), 0);
            server.createContext("/callback", ex -> {
                String code = null;
                String q = ex.getRequestURI().getRawQuery();
                if (q != null) for (String kv : q.split("&")) if (kv.startsWith("code=")) code = URLDecoder.decode(kv.substring(5), StandardCharsets.UTF_8);
                byte[] body = "<html><body style='font-family:sans-serif;background:#0b0b14;color:#ff4fd8'><h2>NeonLyrics verbunden. Du kannst dieses Fenster schliessen.</h2></body></html>".getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body); ex.close();
                final String fc = code;
                new Thread(() -> { exchange(fc); server.stop(0); }).start();
            });
            server.start();
            String url = "https://accounts.spotify.com/authorize?response_type=code&client_id=" + enc(c.clientId)
                + "&scope=" + enc("user-read-currently-playing user-read-playback-state")
                + "&redirect_uri=" + enc(REDIRECT) + "&code_challenge_method=S256&code_challenge=" + challenge;
            Util.getOperatingSystem().open(URI.create(url));
            status = "Warte auf Browser-Login ...";
        } catch (Exception e) { status = "Fehler: " + e.getMessage(); }
    }

    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    static void exchange(String code) {
        if (code == null) { status = "Login abgebrochen"; return; }
        tokenRequest("grant_type=authorization_code&code=" + enc(code) + "&redirect_uri=" + enc(REDIRECT)
            + "&client_id=" + enc(Config.I.clientId) + "&code_verifier=" + enc(verifier));
    }

    static void refresh() {
        tokenRequest("grant_type=refresh_token&refresh_token=" + enc(Config.I.refreshToken) + "&client_id=" + enc(Config.I.clientId));
    }

    static void tokenRequest(String form) {
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) { status = "Token-Fehler " + r.statusCode(); return; }
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            Config c = Config.I;
            c.accessToken = o.get("access_token").getAsString();
            if (o.has("refresh_token")) c.refreshToken = o.get("refresh_token").getAsString();
            c.expiresAt = System.currentTimeMillis() + o.get("expires_in").getAsLong() * 1000 - 60000;
            Config.save();
            status = "Verbunden";
        } catch (Exception e) { status = "Fehler: " + e.getMessage(); }
    }

    public static void disconnect() {
        Config.I.accessToken = ""; Config.I.refreshToken = ""; Config.save();
        status = "Nicht verbunden"; TrackState.title = null; TrackState.trackKey = "";
    }

    // ---------- Polling ----------
    static void poll() {
        try {
            Config c = Config.I;
            if (c.demo) { demo(); return; }
            if (c.accessToken.isBlank()) return;
            if (System.currentTimeMillis() > c.expiresAt && !c.refreshToken.isBlank()) refresh();
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
                .header("Authorization", "Bearer " + c.accessToken).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 204) { TrackState.playing = false; status = "Verbunden (nichts spielt)"; return; }
            if (r.statusCode() != 200) { status = "API-Fehler " + r.statusCode(); return; }
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            if (!o.has("item") || o.get("item").isJsonNull()) return;
            JsonObject it = o.getAsJsonObject("item");
            String id = it.get("id").getAsString();
            TrackState.baseMs = o.get("progress_ms").getAsLong();
            TrackState.sampledAt = System.currentTimeMillis();
            TrackState.playing = o.get("is_playing").getAsBoolean();
            status = "Verbunden";
            if (!id.equals(TrackState.trackKey)) {
                String name = it.get("name").getAsString();
                List<String> arts = new ArrayList<>();
                for (JsonElement a : it.getAsJsonArray("artists")) arts.add(a.getAsJsonObject().get("name").getAsString());
                String artist = String.join(", ", arts);
                long dur = it.get("duration_ms").getAsLong();
                TrackState.trackKey = id; TrackState.title = name; TrackState.artist = artist; TrackState.durationMs = dur;
                TrackState.lines = List.of(); TrackState.cover = null;
                JsonArray imgs = it.getAsJsonObject("album").getAsJsonArray("images");
                if (imgs.size() > 0) loadCover(imgs.get(imgs.size() - 1).getAsJsonObject().get("url").getAsString());
                loadLyrics(name, arts.isEmpty() ? "" : arts.get(0), dur);
            }
        } catch (Exception e) { status = "Fehler: " + e.getMessage(); }
    }

    static void demo() {
        if (!"demo".equals(TrackState.trackKey)) {
            TrackState.trackKey = "demo"; TrackState.title = "Demo Song"; TrackState.artist = "NeonLyrics";
            TrackState.durationMs = DEMO_LEN; TrackState.cover = null;
            TrackState.lines = LrcParser.parse("[00:01.00]We found love\n[00:05.00]in a hopeless place\n[00:10.00]Neon lights over snow\n[00:15.00]Dancing under aurora skies\n[00:20.00]Hate me, or hold me close\n[00:26.00]Every word will glow\n[00:32.00]Until the night is gone");
            demoStart = System.currentTimeMillis();
        }
        TrackState.playing = true;
        TrackState.baseMs = (System.currentTimeMillis() - demoStart) % DEMO_LEN;
        TrackState.sampledAt = System.currentTimeMillis();
        status = "Demo-Modus";
    }

    // ---------- Lyrics (LRCLIB) ----------
    static void loadLyrics(String title, String artist, long durMs) {
        EX.submit(() -> {
            try {
                String url = "https://lrclib.net/api/get?track_name=" + enc(title) + "&artist_name=" + enc(artist) + "&duration=" + (durMs / 1000);
                HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "NeonLyrics/1.0").GET().build(), HttpResponse.BodyHandlers.ofString());
                String lrc = null;
                if (r.statusCode() == 200) {
                    JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
                    if (o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull()) lrc = o.get("syncedLyrics").getAsString();
                }
                if (lrc == null) {
                    r = HTTP.send(HttpRequest.newBuilder(URI.create("https://lrclib.net/api/search?q=" + enc(title + " " + artist))).header("User-Agent", "NeonLyrics/1.0").GET().build(), HttpResponse.BodyHandlers.ofString());
                    if (r.statusCode() == 200) for (JsonElement e : JsonParser.parseString(r.body()).getAsJsonArray()) {
                        JsonObject o = e.getAsJsonObject();
                        if (o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull()) { lrc = o.get("syncedLyrics").getAsString(); break; }
                    }
                }
                if (lrc != null && title.equals(TrackState.title)) TrackState.lines = LrcParser.parse(lrc);
            } catch (Exception ignored) {}
        });
    }

    // ---------- Cover ----------
    static void loadCover(String url) {
        EX.submit(() -> {
            try {
                HttpResponse<byte[]> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
                BufferedImage src = ImageIO.read(new ByteArrayInputStream(r.body()));
                BufferedImage dst = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = dst.createGraphics(); g.drawImage(src, 0, 0, 64, 64, null); g.dispose();
                ByteArrayOutputStream bo = new ByteArrayOutputStream(); ImageIO.write(dst, "png", bo);
                NativeImage img = NativeImage.read(new ByteArrayInputStream(bo.toByteArray()));
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    Identifier id = Identifier.of("neonlyrics", "cover/" + (coverCounter++));
                    mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
                    TrackState.cover = id;
                });
            } catch (Exception ignored) {}
        });
    }
}
