package dev.hamilo.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric Loader entrypoint. This is the reliable path for loading Hamilo into
 * the Fabric game instance that Pojav launches; it does not depend on a
 * separate Execute .jar process.
 */
public final class HamiloFabricClient implements ClientModInitializer {
    private boolean rightShiftWasDown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        System.out.println("[Hamilo] Fabric client entrypoint loaded. Press Right Shift to toggle the GUI.");
    }

    private void onClientTick(MinecraftClient client) {
        if (client.getWindow() == null) return;
        boolean down = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT)
                == GLFW.GLFW_PRESS;
        if (down && !rightShiftWasDown) {
            client.setScreen(client.currentScreen instanceof HamiloClickGuiScreen
                    ? null : new HamiloClickGuiScreen());
        }
        rightShiftWasDown = down;
    }
}
