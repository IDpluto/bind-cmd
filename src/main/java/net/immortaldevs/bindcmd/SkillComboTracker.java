package net.immortaldevs.bindcmd;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Tracks skill combo key states for MMOCore skill casting.
 * Supports F, R, C keys with single/double press combos.
 */
public class SkillComboTracker {

    private static final String CATEGORY = "category.bindcmd.skills";

    public enum ComboState {
        NONE,
        SINGLE,
        DOUBLE
    }

    private ComboState fState = ComboState.NONE;
    private ComboState rState = ComboState.NONE;
    private ComboState cState = ComboState.NONE;

    // Key bindings for F, R, C
    public final KeyBinding fKey;
    public final KeyBinding rKey;
    public final KeyBinding cKey;

    private boolean fWasPressed = false;
    private boolean rWasPressed = false;
    private boolean cWasPressed = false;

    public SkillComboTracker() {
        // Register key bindings
        this.fKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bindcmd.skill_f",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                CATEGORY
        ));

        this.rKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bindcmd.skill_r",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                CATEGORY
        ));

        this.cKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bindcmd.skill_c",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                CATEGORY
        ));
    }

    /**
     * Called every client tick to update key states
     */
    public void tick() {
        updateKeyState(fKey, fWasPressed, pressed -> fWasPressed = pressed,
                       fState, state -> fState = state);
        updateKeyState(rKey, rWasPressed, pressed -> rWasPressed = pressed,
                       rState, state -> rState = state);
        updateKeyState(cKey, cWasPressed, pressed -> cWasPressed = pressed,
                       cState, state -> cState = state);
    }

    private void updateKeyState(KeyBinding key, boolean wasPressed,
                               java.util.function.Consumer<Boolean> setWasPressed,
                               ComboState currentState,
                               java.util.function.Consumer<ComboState> setState) {
        boolean isPressed = key.isPressed();

        // Key just pressed (rising edge)
        if (isPressed && !wasPressed) {
            if (currentState == ComboState.NONE) {
                setState.accept(ComboState.SINGLE);
            } else if (currentState == ComboState.SINGLE) {
                setState.accept(ComboState.DOUBLE);
            }
            // If already DOUBLE, stay DOUBLE
        }

        setWasPressed.accept(isPressed);
    }

    /**
     * Get the skill slot based on current combo state and left click.
     * Returns -1 if no valid combo.
     *
     * Mapping:
     * - F + Left Click = Slot 2
     * - F F + Left Click = Slot 3
     * - R + Left Click = Slot 4
     * - R R + Left Click = Slot 5
     * - C + Left Click = Slot 6
     * - C C + Left Click = Slot 7
     */
    public int getSkillSlot() {
        // Priority: Check in order F, R, C
        // If multiple keys are pressed, F takes priority

        if (fState == ComboState.DOUBLE) {
            return 3;
        } else if (fState == ComboState.SINGLE) {
            return 2;
        }

        if (rState == ComboState.DOUBLE) {
            return 5;
        } else if (rState == ComboState.SINGLE) {
            return 4;
        }

        if (cState == ComboState.DOUBLE) {
            return 7;
        } else if (cState == ComboState.SINGLE) {
            return 6;
        }

        return -1; // No valid combo
    }

    /**
     * Reset all combo states
     */
    public void reset() {
        fState = ComboState.NONE;
        rState = ComboState.NONE;
        cState = ComboState.NONE;
    }

    /**
     * Check if any combo is active
     */
    public boolean hasActiveCombo() {
        return fState != ComboState.NONE ||
               rState != ComboState.NONE ||
               cState != ComboState.NONE;
    }
}
