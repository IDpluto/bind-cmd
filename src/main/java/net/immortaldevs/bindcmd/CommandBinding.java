package net.immortaldevs.bindcmd;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class CommandBinding {
    private static final String CATEGORY = "key.categories.bindcmd";

    public boolean wasPressed = false;
    public String command;

    private BindSource source = BindSource.CLIENT;
    private KeyBinding key;

    public CommandBinding(String command, KeyBinding key) {
        this.command = command;
        this.key = key;
    }

    public CommandBinding(String command) {
        this(command, new KeyBinding("key.keyboard.unknown", -1, CATEGORY));
    }

    public CommandBinding(String command, String translationKey, BindSource source) {
        this(command);
        this.command = command;
        this.source = source;
        InputUtil.Key keyFromTranslation = InputUtil.fromTranslationKey(translationKey);
        int keyCode = keyFromTranslation.getCode();
        InputUtil.Type type = translationKey.startsWith("key.mouse") ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM;
        this.key = new KeyBinding(translationKey, type, keyCode, CATEGORY);
    }

    public KeyBinding getKey() {
        return this.key;
    }

    public BindSource getSource() {
        return this.source;
    }

    public String getTranslationKey() {
        return this.key.getBoundKeyTranslationKey();
    }

    public boolean isUnknown() {
        return getTranslationKey().endsWith("unknown");
    }

    public boolean isPressed() {
        return key.isPressed();
    }

    public void setBoundKey(int keyCode, int scanCode) {
        unbind();
        InputUtil.Key inputKey = InputUtil.fromKeyCode(keyCode, scanCode);
        if (inputKey == InputUtil.UNKNOWN_KEY) {
            key = new KeyBinding("key.keyboard.unknown", -1, CATEGORY);
        } else {
            key = new KeyBinding(inputKey.getTranslationKey(), InputUtil.Type.KEYSYM, inputKey.getCode(), CATEGORY);
        }
    }

    public void setBoundMouse(int button) {
        unbind();
        InputUtil.Key inputKey = InputUtil.Type.MOUSE.createFromCode(button);
        key = new KeyBinding(inputKey.getTranslationKey(), InputUtil.Type.MOUSE, inputKey.getCode(), CATEGORY);
    }

    public void unbind() {
        KeyBinding.KEYS_BY_ID.remove(getTranslationKey());
        KeyBinding.updateKeysByCode();
    }
}
