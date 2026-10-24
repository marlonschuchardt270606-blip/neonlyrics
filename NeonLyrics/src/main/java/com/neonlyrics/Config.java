package com.neonlyrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

public class Config {
    public static Config I = new Config();
    public static final int[] PALETTE = {0xFF4FD8, 0xB66BFF, 0xFFFFFF, 0x4FE3FF, 0x6BFFB0};
    public static final String[] COLOR_NAMES = {"Neon-Pink", "Violett", "Weiss", "Cyan", "Mint"};

    public int accent = 0;
    public int lyricsMode = 0;      // 0 Floating, 1 Sky, 2 Aus
    public int accessory = 0;       // 0 keine, 1 Halo, 2 Krone, 3 Kopfhoerer
    public int crosshair = 1;       // 0 Aus, 1 Neon-Ring
    public boolean showHud = true, hudCover = true, forceNight = false, demo = false;
    public float hudScale = 1f, hudOpacity = 0.85f, hudY = 6f;
    public float worldScale = 0.04f, worldDistance = 5f;
    public int nightTime = 18000;
    public String clientId = "", accessToken = "", refreshToken = "";
    public long expiresAt = 0;

    public int accentRgb() { return PALETTE[Math.floorMod(accent, PALETTE.length)]; }

    static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("neonlyrics.json"); }
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        try { if (Files.exists(file())) I = GSON.fromJson(Files.readString(file()), Config.class); }
        catch (Exception e) { I = new Config(); }
        if (I == null) I = new Config();
    }
    public static synchronized void save() {
        try { Files.writeString(file(), GSON.toJson(I)); } catch (Exception ignored) {}
    }
}
