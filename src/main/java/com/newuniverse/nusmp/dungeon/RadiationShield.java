package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.mana.ManaLaw;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Protection against the Golden City's magicule radiation (docs/living_dungeon.md section 3). A Mana Zone or a Golden Ward shuts the
 * radiation out completely; Tensura's Magic Sense does not, but it feels the magicules pressing in and dulls them, so they build only
 * every other second. The City itself lends the Golden Ward ({@link #ward}); {@link #step} is the one-second pass DungeonEcology runs
 * on whoever stands in the City, and the counting is {@link RadiationCount}.
 */
public final class RadiationShield {
    private RadiationShield() {}

    private static final ResourceLocation MAGIC_SENSE = ResourceLocation.fromNamespaceAndPath("tensura", "magic_sense");
    private static final String WARD = "WardUntil", PASS = "RadiationPass", HINTED = "SenseHinted";

    /** Lends the Golden Ward: full protection from the radiation for {@code ticks}. A ward already running is extended, never shortened. */
    public static void ward(Player p, int ticks) {
        if (ticks <= 0) return;
        CompoundTag d = SkillsData.of(p);
        d.putLong(WARD, Math.max(d.getLong(WARD), p.level().getGameTime() + ticks));
    }

    public static boolean warded(Player p) { return wardLeft(p) > 0; }

    /** Ticks of the Golden Ward left, 0 if there is none. */
    public static long wardLeft(Player p) {
        CompoundTag d = SkillsData.peek(p);
        return d == null ? 0L : Math.max(0L, d.getLong(WARD) - p.level().getGameTime());
    }

    /** Does the player have Tensura's Magic Sense (found the way DungeonFood finds Predator and Gluttony)? */
    public static boolean hasMagicSense(Player p) {
        try {
            return SkillAPI.getSkillsFrom(p).getSkill(MAGIC_SENSE).isPresent();
        } catch (Throwable noSkills) { /* Tensura's skill list is not there: nobody has it */ }
        return false;
    }

    /**
     * What the radiation does to this player on this one-second pass in the Golden City: +1 it builds (exposed), 0 it is held (Magic Sense
     * dulls it), -1 it ebbs (a Mana Zone or a Golden Ward shuts it out).
     */
    public static int step(ServerPlayer p) {
        boolean shielded = ManaLaw.zoneActive(p) || warded(p);
        boolean sense = !shielded && hasMagicSense(p);
        CompoundTag d = SkillsData.of(p);
        int pass = d.getInt(PASS);
        if (sense) {
            d.putInt(PASS, RadiationCount.next(pass));
            if (!d.getBoolean(HINTED)) {
                d.putBoolean(HINTED, true);
                p.displayClientMessage(Component.literal("Magic Sense: you feel the magicules pressing in, and dull them. The radiation builds more slowly.")
                        .withStyle(ChatFormatting.AQUA), true);
            }
        }
        return RadiationCount.step(shielded, sense, pass);
    }
}
