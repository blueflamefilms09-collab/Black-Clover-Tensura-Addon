package com.newuniverse.nusmp.multiverse;

import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Optional;

/**
 * Races (0.27): the mod no longer has its own races beside Tensura's. A player's race is their Tensura race; this adds a small
 * buff on top of it by race family, as transient attribute modifiers that follow race changes (checked every 2 s). The Devil
 * (command only) is the mod's one own race and has its own buff.
 */
public final class RaceBuffs {
    private RaceBuffs() {}

    private record Buff(Holder<Attribute> attribute, double amount, Operation op) {}

    private static Buff add(Holder<Attribute> a, double v) { return new Buff(a, v, Operation.ADD_VALUE); }
    private static Buff pct(Holder<Attribute> a, double v) { return new Buff(a, v, Operation.ADD_MULTIPLIED_BASE); }

    /** The buff per family: small, flavourful, on top of what Tensura's race already gives. */
    static List<Buff> buffs(Race r) {
        return switch (r) {
            case HUMAN -> List.of(pct(Attributes.ATTACK_DAMAGE, 0.05), add(Attributes.LUCK, 1));                       // adaptable
            case ELF -> List.of(pct(Attributes.MOVEMENT_SPEED, 0.08), add(Attributes.MAX_HEALTH, 2));                   // agile
            case DWARF -> List.of(add(Attributes.ARMOR_TOUGHNESS, 2), pct(Attributes.BLOCK_BREAK_SPEED, 0.15));         // sturdy smiths
            case BEASTFOLK -> List.of(pct(Attributes.MOVEMENT_SPEED, 0.10), add(Attributes.JUMP_STRENGTH, 0.08));       // fast hunters
            case OGRE -> List.of(pct(Attributes.ATTACK_DAMAGE, 0.10), add(Attributes.MAX_HEALTH, 2));                   // warriors
            case GOBLIN -> List.of(pct(Attributes.MOVEMENT_SPEED, 0.05), add(Attributes.LUCK, 1));                      // nimble
            case GIANT -> List.of(add(Attributes.MAX_HEALTH, 4), add(Attributes.KNOCKBACK_RESISTANCE, 0.2));            // immovable
            case ORC -> List.of(add(Attributes.MAX_HEALTH, 4), pct(Attributes.ATTACK_DAMAGE, 0.05));                    // tough
            case DRAGONEWT -> List.of(add(Attributes.ARMOR, 2), pct(Attributes.ATTACK_DAMAGE, 0.05));                   // scaled
            case MERFOLK -> List.of(add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.5), add(Attributes.OXYGEN_BONUS, 2));   // swimmers
            case SLIME -> List.of(add(Attributes.FALL_DAMAGE_MULTIPLIER, -0.5), add(Attributes.ARMOR, 2));              // squishy
            case VAMPIRE -> List.of(pct(Attributes.ATTACK_DAMAGE, 0.05), pct(Attributes.MOVEMENT_SPEED, 0.05));         // predators
            case UNDEAD -> List.of(add(Attributes.ARMOR, 2), add(Attributes.KNOCKBACK_RESISTANCE, 0.2));                // unfeeling
            case DAEMON -> List.of(pct(Attributes.ATTACK_DAMAGE, 0.08), pct(Attributes.MOVEMENT_SPEED, 0.05));          // fiends
            case DEVIL -> List.of(pct(Attributes.ATTACK_DAMAGE, 0.10), add(Attributes.ARMOR, 2), pct(Attributes.MOVEMENT_SPEED, 0.05));
        };
    }

    /** Every attribute any buff touches (so a race change can clear the old buff). */
    private static final List<Holder<Attribute>> TOUCHED = List.of(Attributes.ATTACK_DAMAGE, Attributes.LUCK, Attributes.MOVEMENT_SPEED,
            Attributes.MAX_HEALTH, Attributes.ARMOR_TOUGHNESS, Attributes.BLOCK_BREAK_SPEED, Attributes.JUMP_STRENGTH, Attributes.KNOCKBACK_RESISTANCE,
            Attributes.ARMOR, Attributes.WATER_MOVEMENT_EFFICIENCY, Attributes.OXYGEN_BONUS, Attributes.FALL_DAMAGE_MULTIPLIER);

    private static ResourceLocation id(Holder<Attribute> a) {
        String path = a.unwrapKey().map(k -> k.location().getPath()).orElse("attr").replace('.', '_');
        return ResourceLocation.fromNamespaceAndPath("nusmp", "race_buff_" + path);
    }

    /** The player's Tensura race instance, if Tensura / ManasCore report one. */
    public static Optional<ManasRaceInstance> tensuraRace(Player p) {
        try {
            return RaceAPI.getRaceFrom(p).getRace();
        } catch (RuntimeException | LinkageError e) {
            return Optional.empty();
        }
    }

    /** The family of the player's Tensura race, or null. */
    public static Race tensuraFamily(Player p) {
        return tensuraRace(p).map(i -> Race.ofTensuraRace(i.getRaceId().getPath())).orElse(null);
    }

    /** What the status panel shows: the Tensura race's own name (plus "Devil" when set). */
    public static String displayName(Player p) {
        if (MultiverseProfile.race(p) == Race.DEVIL) return Race.DEVIL.displayName;
        return tensuraRace(p).map(i -> i.getDisplayName().getString()).orElse(MultiverseProfile.race(p).displayName);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 13) return;
        Race r = MultiverseProfile.race(p);
        String applied = p.getPersistentData().getString("nusmp_race_buff");
        if (applied.equals(r.name())) return;
        for (Holder<Attribute> a : TOUCHED) {
            AttributeInstance inst = p.getAttribute(a);
            if (inst != null) inst.removeModifier(id(a));
        }
        for (Buff b : buffs(r)) {
            AttributeInstance inst = p.getAttribute(b.attribute());
            if (inst != null) inst.addTransientModifier(new AttributeModifier(id(b.attribute()), b.amount(), b.op()));
        }
        p.getPersistentData().putString("nusmp_race_buff", r.name());
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Transient modifiers are gone after a respawn or relog: apply again. */
    public static void reset(Player p) { p.getPersistentData().remove("nusmp_race_buff"); }
}
