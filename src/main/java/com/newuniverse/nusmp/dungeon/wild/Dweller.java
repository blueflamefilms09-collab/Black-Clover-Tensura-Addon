package com.newuniverse.nusmp.dungeon.wild;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.UUID;

/**
 * What the Living Dungeon remembers about a creature that is not one of its own classes (the real mobs of Tensura and Ice and Fire): a NeoForge
 * data attachment on the entity, saved with it, like the hunger, the "dressed" mark and the home of dungeon.DungeonCreature. It is created
 * the first time such a creature joins the Dungeon (WildDriver.onJoin); the driver reads and changes it from the server tick.
 */
public final class Dweller {
    /** No home yet. */
    public static final long NO_HOME = Long.MIN_VALUE;

    public static final Codec<Dweller> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("hunger", WildRules.START).forGetter(d -> d.appetite.hunger()),
            Codec.BOOL.optionalFieldOf("dressed", false).forGetter(d -> d.dressed),
            Codec.LONG.optionalFieldOf("home", NO_HOME).forGetter(d -> d.home),
            Codec.BOOL.optionalFieldOf("asleep", false).forGetter(d -> d.asleep),
            Codec.FLOAT.optionalFieldOf("sleepHealth", 0f).forGetter(d -> d.sleepHealth)
    ).apply(i, Dweller::new));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Dweller>> DWELLER = ATTACHMENTS.register("dweller",
            () -> AttachmentType.builder(() -> new Dweller()).serialize(CODEC).build());

    public final Appetite appetite;
    /** Marked by a Butcher Cleaver's Anatomical Cut: its carcass carries an extra portion of meat. */
    public boolean dressed;
    /** Switched off by the driver because it is full and has nothing to do (Mob#setNoAi); it wakes when hungry, hurt, or disturbed. */
    public boolean asleep;
    public float sleepHealth;
    private long home;

    // not saved: the driver's own timers
    public long wokenAt = Long.MIN_VALUE, nextGraze, nextHunt, nextForage, avoidUntil;
    public UUID avoid;

    public Dweller() { this(WildRules.START, false, NO_HOME, false, 0f); }

    public Dweller(int hunger, boolean dressed, long home, boolean asleep, float sleepHealth) {
        this.appetite = new Appetite(hunger);
        this.dressed = dressed;
        this.home = home;
        this.asleep = asleep;
        this.sleepHealth = sleepHealth;
    }

    public boolean hasHome() { return home != NO_HOME; }

    public void setHome(BlockPos pos) { home = pos.asLong(); }

    /** Where it lives (only meaningful when {@link #hasHome()}). */
    public BlockPos home() { return BlockPos.of(home); }
}
