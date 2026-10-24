package com.neonlyrics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;

public class HudRenderer {
    public static void render(DrawContext ctx, RenderTickCounter tc) {
        Config c = Config.I;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        int sw = ctx.getScaledWindowWidth();
        int acc = 0xFF000000 | c.accentRgb();

        if (c.crosshair == 1) {
            int cx = sw / 2, cy = ctx.getScaledWindowHeight() / 2;
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                int x = cx + (int) Math.round(Math.cos(a) * 7), y = cy + (int) Math.round(Math.sin(a) * 7);
                ctx.fill(x, y, x + 1, y + 1, acc);
            }
        }
        if (!c.showHud || TrackState.title == null) return;

        TextRenderer tr = mc.textRenderer;
        int w = 260, h = 50;
        int x1 = -w / 2, x2 = w / 2;
        int alpha = (int) (c.hudOpacity * 255) << 24;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(sw / 2f, c.hudY, 0);
        ctx.getMatrices().scale(c.hudScale, c.hudScale, 1f);

        rounded(ctx, x1, 0, x2, h, 12, alpha | 0x0B0B14);
        int tx = x1 + 12;
        if (c.hudCover && TrackState.cover != null) {
            ctx.drawTexture(RenderLayer::getGuiTextured, TrackState.cover, x1 + 8, 8, 0f, 0f, 32, 32, 64, 64, 64, 64);
            tx = x1 + 48;
        }
        int maxW = x2 - 10 - tx;
        ctx.drawText(tr, tr.trimToWidth(TrackState.title, maxW), tx, 7, 0xFFFFFFFF, false);
        ctx.drawText(tr, tr.trimToWidth(TrackState.artist == null ? "" : TrackState.artist, maxW), tx, 17, 0xFF9A9AB0, false);

        long p = TrackState.pos();
        int i = TrackState.idx(p);
        String line = TrackState.text(i);
        if (!line.isEmpty()) {
            String shown = tr.trimToWidth(line, maxW);
            ctx.drawText(tr, shown, tx, 28, 0xFF6C6C88, false);
            int n = (int) (shown.length() * TrackState.frac(i, p));
            if (n > 0) ctx.drawText(tr, shown.substring(0, n), tx, 28, acc, false);
        }
        int bx1 = tx, bx2 = x2 - 10, by = h - 8;
        rounded(ctx, bx1, by, bx2, by + 3, 1, 0x55FFFFFF);
        if (TrackState.durationMs > 0) {
            int fw = (int) ((bx2 - bx1) * (p / (double) TrackState.durationMs));
            if (fw > 0) rounded(ctx, bx1, by, bx1 + Math.max(2, fw), by + 3, 1, acc);
        }
        ctx.getMatrices().pop();
    }

    static void rounded(DrawContext c, int x1, int y1, int x2, int y2, int r, int argb) {
        if (r < 2) { c.fill(x1, y1, x2, y2, argb); return; }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.floor(Math.sqrt(r * r - dy * dy));
            c.fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, argb);
            c.fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, argb);
        }
        c.fill(x1, y1 + r, x2, y2 - r, argb);
    }
}
