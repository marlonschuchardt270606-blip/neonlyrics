package com.neonlyrics;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class NeonLyricsClient implements ClientModInitializer {
    static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.neonlyrics.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.neonlyrics"));
        SpotifyClient.init();
        HudRenderCallback.EVENT.register(HudRenderer::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(WorldLyricsRenderer::render);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) mc.setScreen(new ConfigScreen());
            if (mc.world != null && mc.player != null) {
                if (Config.I.forceNight) mc.world.setTimeOfDay(Config.I.nightTime);
                Cosmetics.tick(mc);
            }
        });
    }
}
