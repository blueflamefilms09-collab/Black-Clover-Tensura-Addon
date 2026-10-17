package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.AlignmentPolicy;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Directional Lock must be obeyed by three doors written by three different builders (the Hub's polarity gates, the generator's chain travel, and any
 * old Gate or Maw that now leads into the dungeon), and none of them may import the alignment code. So the rule is one object behind the contract's one-method
 * {@link AlignmentPolicy}; every door asks {@code MegaDungeon.mayEnter} and this class answers from the player's {@link PlayerDungeonAlignment} through the pure
 * {@link AlignmentRules}. Operators pass when {@code floors.opsBypass} is on, so an admin can test both roads. A refusal tells the player which road is
 * theirs and plays a dull thud, but fires no quest and changes nothing; because a gate block asks again every tick a player stands in it, the message and
 * the sound repeat at most once every two seconds per player (the answer is still "no" every time). A policy must never trap anybody because of its own bug,
 * so any exception is logged and the answer is "yes".
 */
public final class LockPolicy implements AlignmentPolicy {
    private static final Logger LOG = LogUtils.getLogger();
    /** Ticks between two spoken refusals to one player. */
    private static final long SPEAK_EVERY = 40L;
    private static final Map<UUID, Long> SPOKEN = new HashMap<>();

    @Override
    public boolean mayEnter(ServerPlayer player, Polarity polarity) {
        try {
            if (allows(player, polarity)) return true;
            refuse(player, polarity);
            return false;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The Directional Lock failed and let a player through: {}", e.toString());
            return true;
        }
    }

    /** Would the lock let this player take the road? No message, no sound, no state change: the question alone (the self-tests and the command use it). */
    public static boolean allows(ServerPlayer player, Polarity polarity) {
        if (player == null || polarity == null) return true;
        if (!FloorsConfig.get(FloorsConfig.LOCK_ENABLED)) return true;
        if (bypasses(player)) return true;
        PlayerDungeonAlignment a = AlignmentAttachments.get(player);
        return AlignmentRules.mayEnter(a.chosen(), a.locked(), polarity, true);
    }

    /** Does this player pass the lock because they are an operator and {@code floors.opsBypass} is on? */
    public static boolean bypasses(ServerPlayer player) {
        return FloorsConfig.get(FloorsConfig.OPS_BYPASS) && player.hasPermissions(2);
    }

    private static void refuse(ServerPlayer player, Polarity wanted) {
        long now = player.level().getGameTime();
        Long last = SPOKEN.get(player.getUUID());
        if (last != null && now >= last && now - last < SPEAK_EVERY) return;
        SPOKEN.put(player.getUUID(), now);
        if (!FloorsText.real(player)) return;
        Polarity own = AlignmentAttachments.get(player).chosen();
        player.sendSystemMessage(FloorsText.text("refused", "The %s will not open for you: you are bound to the %s. The way back to Floor 0 is always open.", FloorsText.road(wanted), FloorsText.road(own))
                .withStyle(ChatFormatting.DARK_PURPLE));
        player.playNotifySound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 0.9f, 0.6f);
    }

    /** Forgets what was said to one player (logout). */
    static void forget(UUID id) { SPOKEN.remove(id); }

    /** Forgets everything (the server stopped). */
    static void clear() { SPOKEN.clear(); }
}
