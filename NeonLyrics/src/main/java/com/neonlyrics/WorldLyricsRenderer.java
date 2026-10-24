package com.neonlyrics;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class WorldLyricsRenderer {
    public static void render(WorldRenderContext ctx) {
        Config c = Config.I;
        if (c.lyricsMode == 2 || TrackState.title == null || TrackState.lines.isEmpty()) return;
        MatrixStack m = ctx.matrixStack();
        VertexConsumerProvider vc = ctx.consumers();
        if (m == null || vc == null) return;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();

        double scale = c.worldScale;
        Vec3d target;
        if (c.lyricsMode == 0) { // Floating: folgt der Blickrichtung
            target = cp.add(Vec3d.fromPolar(cam.getPitch(), cam.getYaw()).multiply(c.worldDistance)).add(0, 0.9, 0);
        } else {                 // Sky: gross am Himmel in Blickrichtung (nur Yaw)
            target = cp.add(Vec3d.fromPolar(0, cam.getYaw()).multiply(c.worldDistance * 6)).add(0, c.worldDistance * 3, 0);
            scale *= 6;
        }
        long p = TrackState.pos();
        int i = TrackState.idx(p);
        int acc = c.accentRgb();

        m.push();
        m.translate(target.x - cp.x, target.y - cp.y, target.z - cp.z);
        m.multiply(cam.getRotation());
        m.scale((float) -scale, (float) -scale, (float) scale);
        Matrix4f mat = m.peek().getPositionMatrix();

        line(tr, mat, vc, TrackState.text(i - 1), -16, 0x8896A0, 0x80, 0f, acc);
        line(tr, mat, vc, TrackState.text(i), 0, 0xC8C8E0, 0xFF, TrackState.frac(i, p), acc);
        line(tr, mat, vc, TrackState.text(i + 1), 16, 0x8896A0, 0x80, 0f, acc);
        m.pop();
        if (vc instanceof VertexConsumerProvider.Immediate imm) imm.draw();
    }

    static void line(TextRenderer tr, Matrix4f mat, VertexConsumerProvider vc, String s, float y, int rgb, int alpha, float frac, int acc) {
        if (s == null || s.isEmpty()) return;
        float x = -tr.getWidth(s) / 2f;
        int glow = (0x40 << 24) | acc;
        float[][] o = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (float[] d : o) tr.draw(s, x + d[0], y + d[1], glow, false, mat, vc, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
        tr.draw(s, x, y, (alpha << 24) | rgb, false, mat, vc, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
        int n = (int) (s.length() * frac);
        if (n > 0) tr.draw(s.substring(0, n), x, y, 0xFF000000 | acc, false, mat, vc, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
    }
}
