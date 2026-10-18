package com.newuniverse.nusmp.client.skilltree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Why: the owner wants the tree to feel like the other anime menus, which means a click you can hear and a reward you can hear: a plain UI click when something is selected, and a
 * short rising chime when a purchase really went through. The chime is played when the SYNCED level rises, not when the button is pressed, so it is never a promise the server did not
 * keep. It is three notes (a major chord climbing), which cannot be one sound event with a pitch, so the notes are queued and played one by one from the screen's tick. Sounds go
 * through the sound manager as UI sounds (not positioned in the world), and a failure to play one is swallowed: a missing sound must never close the screen.
 */
final class TreeSounds {
    private final Deque<float[]> queue = new ArrayDeque<>();   // {ticks to wait, pitch}

    /** The click of selecting something. */
    static void click() {
        try {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        } catch (RuntimeException noSound) {
            // a sound that cannot play never stops the screen
        }
    }

    /** Queues the rising chime: three notes a few ticks apart. */
    void chime() {
        queue.clear();
        queue.add(new float[]{0f, 1.00f});
        queue.add(new float[]{2f, 1.26f});
        queue.add(new float[]{2f, 1.50f});
    }

    /** Plays the queued notes that are due; called once per client tick. */
    void tick() {
        if (queue.isEmpty()) return;
        float[] head = queue.peekFirst();
        if (head[0] > 0f) {
            head[0] -= 1f;
            return;
        }
        queue.pollFirst();
        try {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, head[1]));
        } catch (RuntimeException noSound) {
            // see click()
        }
    }
}
