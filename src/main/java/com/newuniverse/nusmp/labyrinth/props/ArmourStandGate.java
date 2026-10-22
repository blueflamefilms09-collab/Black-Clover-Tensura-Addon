package com.newuniverse.nusmp.labyrinth.props;

import com.newuniverse.nusmp.labyrinth.loot.GearStacks;
import com.newuniverse.nusmp.labyrinth.loot.LootGear;
import com.newuniverse.nusmp.labyrinth.loot.LootRarity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.List;

/**
 * Why: the armour stands the dungeon's templates carry were scenery (a hall could hold twelve), and the owner wants them back only as a RARE prop. They are born when the sector generator places
 * a piece (the template's entities join the world through the normal entity join), so this is where they are decided, with no per-tick work and no extra entity: the placer opens a {@link Scope}
 * around each piece ({@link #run}) that says whether the plan ({@link ArmourStandRarity}) gave this room its one stand; {@link #onJoin} (high priority, game bus) then meets every armour stand of
 * that piece that has the template's signature (invulnerable, no gravity, not invisible: an invisible stand is a marker) and either cancels it (it never exists) or, for the first stand of a
 * chosen room, keeps it and makes it the rare stand: breakable and lootable like any armour stand (the template's lock on its slots and its invulnerability are removed), wearing two to four
 * pieces of the dungeon's armour for the floor's depth ({@link RareStandSet}, rolled by the loot system), tagged so the old rack path never touches it. A stand outside a generator scope (a
 * player's, one loaded from disk, one another system made, the Hub's) is left alone. The stand is part of its chunk: it is saved and removed with it, nothing is cleaned up by hand.
 */
public final class ArmourStandGate {
    private ArmourStandGate() {}

    /** The tag of the rare stand (so other code can tell it apart; the legacy rack path skips it). */
    public static final String RARE_TAG = "nusmp_rare_stand";

    /** What the placer says about the piece being placed: does its room hold the rare stand, on which floor depth, with which seed; {@code left} is the stands still to allow in it (1 or 0). */
    public record Scope(boolean allowed, int depth, long seed, int[] left) {
        public static Scope of(boolean allowed, int depth, long seed) { return new Scope(allowed, depth, seed, new int[] {allowed ? 1 : 0}); }
    }

    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();
    private static final java.util.concurrent.atomic.AtomicInteger KEPT = new java.util.concurrent.atomic.AtomicInteger(), DROPPED = new java.util.concurrent.atomic.AtomicInteger();

    /** Stands made rare and stands dropped since the game started (for the self-test and the log). */
    public static int kept() { return KEPT.get(); }

    public static int dropped() { return DROPPED.get(); }

    /** The scope of the piece being placed on this thread, or null. */
    public static Scope current() { return CURRENT.get(); }

    /** Runs {@code work} with the scope set, restoring the previous one afterwards (nests). */
    public static void run(Scope scope, Runnable work) {
        Scope before = CURRENT.get();
        CURRENT.set(scope);
        try { work.run(); } finally { if (before == null) CURRENT.remove(); else CURRENT.set(before); }
    }

    /** Does this stand carry the signature the generator's templates give their stands? */
    public static boolean templateSignature(ArmorStand stand) { return stand.isNoGravity() && stand.isInvulnerable() && !stand.isInvisible(); }

    public static void onJoin(EntityJoinLevelEvent e) {
        if (!(e.getEntity() instanceof ArmorStand stand) || !(e.getLevel() instanceof ServerLevel level) || e.loadedFromDisk()) return;
        Scope s = CURRENT.get();
        if (s == null || !templateSignature(stand)) return;
        if (s.allowed() && s.left()[0] > 0) {
            s.left()[0]--;
            makeRare(level, stand, s);
            KEPT.incrementAndGet();
            return;
        }
        DROPPED.incrementAndGet();
        e.setCanceled(true);
    }

    /** Turns a template stand into the rare stand: unlocked, breakable, dressed for the floor's depth, tagged. */
    public static void makeRare(ServerLevel level, ArmorStand stand, Scope s) {
        CompoundTag tag = new CompoundTag();
        stand.saveWithoutId(tag);
        tag.putBoolean("Invulnerable", false);
        tag.putInt("DisabledSlots", 0);
        stand.load(tag);
        stand.addTag(RARE_TAG);
        List<RareStandSet.Piece> pieces = RareStandSet.pieces(s.depth(), s.seed());
        for (RareStandSet.Piece p : pieces) {
            ItemStack stack = stackOf(p, s);
            if (stack.isEmpty()) continue;
            stand.setItemSlot(slot(p.slot()), stack);
        }
    }

    /** The stack of one piece: the loot system's rolled gear for its own items, a plain stack for vanilla ones; empty when the item does not exist. */
    static ItemStack stackOf(RareStandSet.Piece p, Scope s) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(p.item())).orElse(Items.AIR);
        if (item == Items.AIR) return ItemStack.EMPTY;
        LootRarity rarity = LootRarity.fromId(p.rarity());
        if (rarity != null && item instanceof LootGear) return GearStacks.create(item, rarity, s.seed() * 31L + p.slot().hashCode(), s.depth());
        return new ItemStack(item);
    }

    static EquipmentSlot slot(String name) {
        return switch (name) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            default -> EquipmentSlot.FEET;
        };
    }
}
