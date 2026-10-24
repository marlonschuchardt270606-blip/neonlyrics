package com.neonlyrics;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConfigScreen extends Screen {
    static final String[] TABS = {"Sky", "Lyrics", "Style", "Spotify", "Display"};
    static int tab = 0;
    int px, py, cx, cw = 290;
    static final int PW = 420, PH = 250;

    public ConfigScreen() { super(Text.literal("NeonLyrics")); }

    @Override
    protected void init() {
        clearChildren();
        px = (width - PW) / 2; py = (height - PH) / 2; cx = px + 110;
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            addDrawableChild(ButtonWidget.builder(Text.literal((tab == i ? "> " : "") + TABS[i]), b -> { tab = t; init(); })
                .dimensions(px + 8, py + 30 + i * 24, 84, 20).build());
        }
        int y = py + 34;
        Config c = Config.I;
        switch (tab) {
            case 0 -> {
                btn(y, () -> "Nacht erzwingen: " + (c.forceNight ? "AN" : "AUS"), () -> c.forceNight = !c.forceNight);
                slider(y + 26, c.nightTime / 24000.0, v -> Text.literal("Tageszeit: " + (int) (v * 24000)), v -> c.nightTime = (int) (v * 24000));
            }
            case 1 -> {
                btn(y, () -> "Modus: " + new String[]{"Floating", "Sky", "Aus"}[c.lyricsMode], () -> c.lyricsMode = (c.lyricsMode + 1) % 3);
                btn(y + 26, () -> "Farbe: " + Config.COLOR_NAMES[c.accent % Config.PALETTE.length], () -> c.accent = (c.accent + 1) % Config.PALETTE.length);
                slider(y + 52, (c.worldScale - 0.02) / 0.06, v -> Text.literal("Groesse: " + String.format("%.3f", 0.02 + v * 0.06)), v -> c.worldScale = (float) (0.02 + v * 0.06));
                slider(y + 78, (c.worldDistance - 3) / 7, v -> Text.literal("Abstand: " + String.format("%.1f", 3 + v * 7)), v -> c.worldDistance = (float) (3 + v * 7));
            }
            case 2 -> {
                btn(y, () -> "Accessoire: " + new String[]{"Keins", "Halo", "Krone", "Kopfhoerer"}[c.accessory], () -> c.accessory = (c.accessory + 1) % 4);
                btn(y + 26, () -> "Crosshair: " + (c.crosshair == 1 ? "Neon-Ring" : "Aus"), () -> c.crosshair = 1 - c.crosshair);
                btn(y + 52, () -> "Farbe: " + Config.COLOR_NAMES[c.accent % Config.PALETTE.length], () -> c.accent = (c.accent + 1) % Config.PALETTE.length);
            }
            case 3 -> {
                TextFieldWidget f = new TextFieldWidget(textRenderer, cx, y + 12, cw, 20, Text.literal("Client-ID"));
                f.setMaxLength(64); f.setText(c.clientId); f.setChangedListener(s -> c.clientId = s.trim());
                addDrawableChild(f);
                btn(y + 40, () -> "Mit Spotify verbinden", SpotifyClient::connect);
                btn(y + 66, () -> "Trennen", SpotifyClient::disconnect);
                btn(y + 92, () -> "Demo-Lyrics: " + (c.demo ? "AN" : "AUS"), () -> { c.demo = !c.demo; TrackState.trackKey = ""; });
            }
            case 4 -> {
                btn(y, () -> "HUD: " + (c.showHud ? "AN" : "AUS"), () -> c.showHud = !c.showHud);
                btn(y + 26, () -> "Album-Cover: " + (c.hudCover ? "AN" : "AUS"), () -> c.hudCover = !c.hudCover);
                slider(y + 52, (c.hudScale - 0.5) / 1.5, v -> Text.literal("HUD-Groesse: " + String.format("%.2f", 0.5 + v * 1.5)), v -> c.hudScale = (float) (0.5 + v * 1.5));
                slider(y + 78, c.hudOpacity, v -> Text.literal("Deckkraft: " + (int) (v * 100) + "%"), v -> c.hudOpacity = (float) Math.max(0.2, v));
                slider(y + 104, c.hudY / 60.0, v -> Text.literal("Abstand oben: " + (int) (v * 60)), v -> c.hudY = (float) (v * 60));
            }
        }
    }

    void btn(int y, Supplier<String> label, Runnable action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label.get()), b -> { action.run(); b.setMessage(Text.literal(label.get())); })
            .dimensions(cx, y, cw, 20).build());
    }

    void slider(int y, double v, Function<Double, Text> label, Consumer<Double> apply) {
        addDrawableChild(new Slider(cx, y, cw, 20, Math.max(0, Math.min(1, v)), label, apply));
    }

    static class Slider extends SliderWidget {
        final Function<Double, Text> label; final Consumer<Double> apply;
        Slider(int x, int y, int w, int h, double v, Function<Double, Text> label, Consumer<Double> apply) {
            super(x, y, w, h, Text.empty(), v);
            this.label = label; this.apply = apply; updateMessage();
        }
        @Override protected void updateMessage() { setMessage(label.apply(value)); }
        @Override protected void applyValue() { apply.accept(value); }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xB0000000);
        HudRenderer.rounded(ctx, px, py, px + PW, py + PH, 10, 0xF0121220);
        ctx.fill(px + 100, py + 12, px + 101, py + PH - 12, 0x33FFFFFF);
        ctx.drawText(textRenderer, "NeonLyrics", px + 10, py + 10, 0xFF000000 | Config.I.accentRgb(), false);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        if (tab == 3) {
            ctx.drawText(textRenderer, "Spotify Client-ID (developer.spotify.com):", cx, py + 34, 0xFFB0B0C8, false);
            ctx.drawText(textRenderer, "Status: " + SpotifyClient.status, cx, py + 34 + 122, 0xFFFFFFFF, false);
            ctx.drawText(textRenderer, "Redirect-URI: http://127.0.0.1:8898/callback", cx, py + 34 + 136, 0xFF7A7A90, false);
        }
    }

    @Override public void removed() { Config.save(); }
    @Override public boolean shouldPause() { return false; }
}
