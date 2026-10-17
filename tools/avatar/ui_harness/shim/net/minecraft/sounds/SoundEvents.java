package net.minecraft.sounds;

import net.minecraft.core.Holder;

/** Why: harness shim: sounds are only counted, never played. */
public final class SoundEvents {
    private SoundEvents() {}
    public static final Holder<SoundEvent> UI_BUTTON_CLICK = () -> new SoundEvent("ui.button.click");
}
