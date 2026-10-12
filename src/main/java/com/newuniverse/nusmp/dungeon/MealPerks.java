package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.SpellRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * The second effect of a cooked monster meal ({@link MealTable}): on eating, the skill it lends ({@link MealSkills}) and the magic it
 * attunes the eater to ({@link MealAttunement}), announced in the action bar a moment after the meal's own message; in the tooltip, what
 * the meal will do. Both are looked up at runtime and say nothing about a skill or magic the game does not have.
 */
final class MealPerks {
    private MealPerks() {}

    /** Called on the server when the player has eaten a meal that carries this perk. */
    static void eat(Player p, MealTable.Perk perk) {
        MealSkills.Outcome got = MealSkills.grant(p, perk.skill(), perk.skillTicks());
        MagicType magic = MealAttunement.parse(perk.magic());
        if (magic != null) MealAttunement.give(p, magic, perk.magicTicks());
        String skill = got == MealSkills.Outcome.GRANTED || got == MealSkills.Outcome.EXTENDED ? perk.skillName() : null;
        String line = MealTable.eatLine(magic == null ? null : magic.displayName, magic == null ? null : magic.hit.name(), skill);
        if (line.isEmpty() || !(p.level() instanceof ServerLevel sl)) return;
        SpellRuntime.later(sl, 60, () -> p.displayClientMessage(Component.literal(line).withStyle(ChatFormatting.AQUA), true));
    }

    /** The perk's lines under a meal's tooltip. */
    static void tooltip(MealTable.Perk perk, List<Component> tip) {
        MagicType magic = MealAttunement.parse(perk.magic());
        if (magic != null) tip.add(Component.literal(MealTable.attuneLine(magic.displayName, magic.hit.name(), perk.magicTicks())).withStyle(ChatFormatting.AQUA));
        if (MealSkills.available(perk.skill())) tip.add(Component.literal(MealTable.skillLine(perk.skillName(), perk.skillTicks())).withStyle(ChatFormatting.AQUA));
    }
}
