package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Why: client feedback without a renderer: when a delver walks into the area of a mechanic they see its name and, if it can be beaten, a one-line hint of how, in the action bar;
 * when it is overcome they get a title. Every string is a lang key with an English fallback in code ({@code Component.translatableWithFallback}), so the messages read fine before
 * the lang fragment ({@code tools/mechanics/lang_fragment.json}) is merged. The names and descriptions of the thousand mechanics are {@code mechanic.nusmp.<id>.name / .desc}.
 */
public final class Messages {
    private Messages() {}

    /** Hints by counter kind: the one line a player gets about how to beat the mechanic. */
    static final Map<String, String> HINT = Map.ofEntries(
            Map.entry("kill_all", "Its wardens hold it up: kill them all."),
            Map.entry("kill_marked", "One marked warden holds it up: kill the keystone."),
            Map.entry("item_offer", "An altar waits for an offering."),
            Map.entry("stand_plate", "Stand on the plate and hold your ground."),
            Map.entry("sneak_through", "Sneak through it."),
            Map.entry("hold_light", "Carry a light through it."),
            Map.entry("sequence", "The pedestals have an order. Remember what you saw."),
            Map.entry("lever", "A lever somewhere in the room ends it."),
            Map.entry("survive", "Hold out until it passes."),
            Map.entry("all_present", "The whole party must stand on the plate together."),
            Map.entry("split_plates", "Two of you must hold the plates at the same time."),
            Map.entry("carry", "Carry the key from one pedestal to the other."),
            Map.entry("no_damage", "Take no damage and it lets go."),
            Map.entry("quiet", "Do not run or jump."),
            Map.entry("pay_xp", "The altar wants experience."),
            Map.entry("pay_hp", "The altar wants blood."),
            Map.entry("pay_item", "The altar wants a price in items."),
            Map.entry("pillars_sync", "Light every pillar at nearly the same moment."),
            Map.entry("mirror", "Repeat the pattern it shows you."));

    public static MutableComponent name(MechanicDef d) { return Component.translatableWithFallback(d.nameKey(), d.name()); }

    public static MutableComponent text(String key, String english, Object... args) { return Component.translatableWithFallback("message.nusmp.mega.mech." + key, english, args); }

    public static MutableComponent hint(String counterKind) {
        String h = HINT.get(counterKind);
        return h == null ? Component.empty() : text("hint." + counterKind, h);
    }

    /** The action-bar line when a player first steps into the area of an armed mechanic. */
    static void entered(ServerPlayer p, Instance in) {
        MutableComponent line = text("enter", "Mechanic: %s", name(in.def)).withStyle(ChatFormatting.GOLD);
        if (!in.def.counter().none()) line = line.append(Component.literal("  ")).append(hint(in.def.counter().kind()).withStyle(ChatFormatting.GRAY));
        p.displayClientMessage(line, true);
    }

    /** The title when a mechanic has been overcome. */
    static void solved(Instance in) {
        for (ServerPlayer p : in.inside) {
            if (p.connection == null) continue;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10));
            p.connection.send(new ClientboundSetTitleTextPacket(text("solved", "Overcome").withStyle(ChatFormatting.GREEN)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(name(in.def).withStyle(ChatFormatting.GRAY)));
        }
    }

    /** A free-form line to the players of the area (the {@code title} primitive). */
    static void say(Instance in, String text, String mode) {
        MutableComponent c = text.isEmpty() ? name(in.def) : Component.translatableWithFallback(text, text);
        for (ServerPlayer p : in.inside) {
            if (!Safety.affects(p) || p.connection == null) continue;
            switch (mode) {
                case "title" -> {
                    p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 30, 10));
                    p.connection.send(new ClientboundSetTitleTextPacket(c));
                }
                case "chat" -> p.sendSystemMessage(c);
                default -> p.displayClientMessage(c, true);
            }
        }
    }
}
