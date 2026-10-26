package com.newuniverse.nusmp.entity.veldanava;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the loot guide gives every survivor of Veldanava one boon from the Will of the Creator, once, forever. This is the table of the three boons and the one
 * place that applies one: a permanent attribute change on the player (so it works on a dedicated server, needs no client code and survives death and relogging).
 * The choice comes in as a number from a command the player runs from a clickable chat line, so it is validated here ({@link #valid}) and the "already
 * received" flag is the player's own NBT key {@code hasReceivedVeldanavaBoon} (set once, kept through death).
 *
 * <p>These are this mod's own boons named after the guide's ultimate skills; they are not Tensura skills (the mod has no API to grant those safely).
 *
 * <p>Since 3.13.0 the Will of the Creator offers real ultimate skills instead (see {@code skills.VeldanavaSkills}; Sandalphon first). The boons stay here, working, for old saves and for
 * the server setting {@code rewardScreen = false}, but they are no longer offered by default.
 */
public final class VeldanavaBoons {
    private VeldanavaBoons() {}

    /** The player's NBT key from the guide, and the choice made. */
    public static final String TAG_RECEIVED = "hasReceivedVeldanavaBoon", TAG_CHOICE = "nusmp_veldanava_boon";
    public static final int COUNT = 3;

    /** One attribute change of a boon: its attribute key, the amount and whether it multiplies the base value. */
    public record Change(String attribute, double amount, boolean multiply) {}

    public record Boon(int id, String name, String text, List<Change> changes) {}

    private static final List<Boon> TABLE = List.of(
            new Boon(1, "Magic King Solomon", "Twelve more hearts and a steadier hand: +24 maximum health, +3 armor toughness.",
                    List.of(new Change("max_health", 24.0, false), new Change("armor_toughness", 3.0, false))),
            new Boon(2, "Martial King Hercules", "The strength of a hero: +8 attack damage and 10% faster on your feet.",
                    List.of(new Change("attack_damage", 8.0, false), new Change("movement_speed", 0.10, true))),
            new Boon(3, "Wisdom King Raphael", "Calm and sure: +6 armor, +0.5 attack speed and +2 luck.",
                    List.of(new Change("armor", 6.0, false), new Change("attack_speed", 0.5, false), new Change("luck", 2.0, false))));

    public static boolean valid(int choice) { return choice >= 1 && choice <= COUNT; }

    public static Boon boon(int choice) { return TABLE.get(valid(choice) ? choice - 1 : 0); }

    public static List<Boon> all() { return TABLE; }

    /** The boon that suits a player best: the magic king for a magic user, the martial king for everybody else (a hint only, all three are always offered). */
    public static int recommended(boolean magicUser) { return magicUser ? 1 : 2; }

    private static Holder<Attribute> holder(String key) {
        return switch (key) {
            case "max_health" -> Attributes.MAX_HEALTH;
            case "armor_toughness" -> Attributes.ARMOR_TOUGHNESS;
            case "attack_damage" -> Attributes.ATTACK_DAMAGE;
            case "movement_speed" -> Attributes.MOVEMENT_SPEED;
            case "attack_speed" -> Attributes.ATTACK_SPEED;
            case "armor" -> Attributes.ARMOR;
            default -> Attributes.LUCK;
        };
    }

    private static ResourceLocation id(int choice, String key) { return ResourceLocation.fromNamespaceAndPath("nusmp", "veldanava_boon_" + choice + "_" + key); }

    /** True once the player has taken their boon. */
    public static boolean received(Player p) { return p.getPersistentData().getBoolean(TAG_RECEIVED); }

    /** Gives the boon, once. False if the choice is invalid or the player already has theirs. Server side. */
    public static boolean grant(ServerPlayer p, int choice) {
        if (!valid(choice) || received(p)) return false;
        p.getPersistentData().putBoolean(TAG_RECEIVED, true);
        p.getPersistentData().putInt(TAG_CHOICE, choice);
        apply(p);
        p.displayClientMessage(Component.literal("The Will of the Creator grants you " + boon(choice).name() + ".").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        return true;
    }

    /** Puts the player's chosen boon on them (idempotent: the same ids are replaced). Called on grant, login and respawn. */
    public static void apply(ServerPlayer p) {
        int choice = p.getPersistentData().getInt(TAG_CHOICE);
        if (!valid(choice) || !received(p)) return;
        float healthBefore = p.getHealth();
        for (Change c : boon(choice).changes()) {
            AttributeInstance a = p.getAttribute(holder(c.attribute()));
            if (a == null) continue;
            a.addOrReplacePermanentModifier(new AttributeModifier(id(choice, c.attribute()), c.amount(),
                    c.multiply() ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE));
        }
        if (p.getHealth() < healthBefore) p.setHealth(healthBefore);
    }

    /** Takes a player's boon back (an admin tool and the self-test's cleanup): its attribute changes, the flag and the choice. */
    public static void revoke(ServerPlayer p) {
        for (Boon b : TABLE) {
            for (Change c : b.changes()) {
                AttributeInstance a = p.getAttribute(holder(c.attribute()));
                if (a != null) a.removeModifier(id(b.id(), c.attribute()));
            }
        }
        p.getPersistentData().remove(TAG_RECEIVED);
        p.getPersistentData().remove(TAG_CHOICE);
    }

    /** The chat lines of the three choices, as plain strings (the entity turns them into clickable lines). */
    public static List<String> menu(boolean magicUser) {
        List<String> out = new ArrayList<>();
        int rec = recommended(magicUser);
        for (Boon b : TABLE) out.add((b.id() == rec ? "* " : "") + b.name() + ": " + b.text());
        return out;
    }
}
