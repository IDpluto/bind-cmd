package net.immortaldevs.bindcmd;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.immortaldevs.bindcmd.config.Config;
import net.minecraft.client.MinecraftClient;

public final class BindCmdClient implements ClientModInitializer {
    private static long lastKeyPress = 0L;
    private static final long COOLDOWN = 200L;
    private static SkillComboTracker skillComboTracker;
    private static boolean attackWasPressed = false;

    @Override
    public void onInitializeClient() {
        skillComboTracker = new SkillComboTracker();
        PayloadTypeRegistry.playS2C().register(
                ConfigS2CPayload.ID, ConfigS2CPayload.CODEC
        );

        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(BindCmdClient::onEndClientTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onPlayerJoin(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onPlayerDisconnect());

        ClientPlayNetworking.registerGlobalReceiver(
                ConfigS2CPayload.ID,
                (payload, context) -> Config.setServerBindings(payload.config())
        );
    }

    private static void onPlayerJoin(MinecraftClient client) {
        Config.clearServerBindings();
        if (client.getServer() != null && client.getServer().isSingleplayer()) {
            var server = client.getServer();
            var dir = server.session.getDirectory().path();
            Config.loadWorldConfig(dir);
        }
    }

    private static void onPlayerDisconnect() {
        Config.clearServerBindings();
    }

    private static void onEndClientTick(MinecraftClient client) {
        // Update skill combo tracker
        if (skillComboTracker != null) {
            skillComboTracker.tick();
            handleSkillCasting(client);
        }

        // Handle regular command bindings
        if (System.currentTimeMillis() - lastKeyPress < COOLDOWN) return;
        for (CommandBinding binding : Config.getBindings()) {
            handleBinding(client, binding);
        }
    }

    /**
     * Handles MMOCore skill casting with key combos
     */
    private static void handleSkillCasting(MinecraftClient client) {
        var networkHandler = client.getNetworkHandler();
        if (networkHandler == null || client.player == null) return;

        // Check for left click (attack key)
        boolean attackPressed = client.options.attackKey.isPressed();

        // Detect left click press (rising edge)
        if (attackPressed && !attackWasPressed) {
            // Check if we have an active combo
            if (skillComboTracker.hasActiveCombo()) {
                int slot = skillComboTracker.getSkillSlot();
                if (slot > 0) {
                    String playerName = client.player.getName().getString();
                    String command = "mmocore cast specific " + playerName + " " + slot;
                    networkHandler.sendChatCommand(command);

                    // Reset combo state after casting
                    skillComboTracker.reset();
                }
            }
        }

        attackWasPressed = attackPressed;
    }

    private static void handleBinding(MinecraftClient client, CommandBinding binding) {
        var networkHandler = client.getNetworkHandler();

        if (binding.isUnknown() || networkHandler == null) {
            return;
        }

        if (binding.isPressed() && !binding.wasPressed) {
            var commands = Config.getBindings().stream()
                    .filter(b -> b.getTranslationKey().equals(binding.getTranslationKey()))
                    .map(b -> b.command)
                    .toList();

            for (String command : commands) {
                Command cmd = new Command(command);
                switch (cmd.getType()) {
                    case COMMAND -> networkHandler.sendChatCommand(cmd.getCommand());
                    case MESSAGE -> networkHandler.sendChatMessage(cmd.getCommand());
                    case NONE -> {
                        return;
                    }
                }
            }
            lastKeyPress = System.currentTimeMillis();
            binding.wasPressed = true;
        }

        if (!binding.isPressed() && binding.wasPressed) {
            binding.wasPressed = false;
        }
    }
}
