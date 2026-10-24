package com.neonlyrics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.DustParticleEffect;

/** Client-seitige Partikel-Accessoires (nur du siehst sie). */
public class Cosmetics {
    static int tick = 0;

    public static void tick(MinecraftClient mc) {
        Config c = Config.I;
        if (c.accessory == 0 || ++tick % 3 != 0) return;
        ClientPlayerEntity p = mc.player;
        DustParticleEffect fx = new DustParticleEffect(c.accentRgb(), 0.6f);
        double x = p.getX(), z = p.getZ(), top = p.getY() + p.getHeight();
        double yaw = Math.toRadians(p.getYaw());
        double sx = Math.cos(yaw), sz = Math.sin(yaw); // Seitenvektor
        switch (c.accessory) {
            case 1 -> { // Halo
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    mc.world.addParticle(fx, x + Math.cos(a) * 0.3, top + 0.4, z + Math.sin(a) * 0.3, 0, 0, 0);
                }
            }
            case 2 -> { // Krone
                for (int i = 0; i < 15; i++) {
                    double a = i * Math.PI * 2 / 15;
                    double spike = (i % 3 == 0) ? 0.25 : 0.0;
                    mc.world.addParticle(fx, x + Math.cos(a) * 0.28, top + 0.1 + spike, z + Math.sin(a) * 0.28, 0, 0, 0);
                }
            }
            case 3 -> { // Kopfhoerer: Buegel ueber dem Kopf + Ohrmuscheln
                for (int i = 0; i <= 10; i++) {
                    double a = i * Math.PI / 10;
                    double side = Math.cos(a) * 0.36, up = Math.sin(a) * 0.3;
                    mc.world.addParticle(fx, x + sx * side, top - 0.2 + up, z + sz * side, 0, 0, 0);
                }
                for (int s = -1; s <= 1; s += 2) for (int k = 0; k < 4; k++)
                    mc.world.addParticle(fx, x + sx * 0.38 * s, top - 0.25 + k * 0.05, z + sz * 0.38 * s, 0, 0, 0);
            }
            default -> {}
        }
    }
}
