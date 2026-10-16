package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.book.GrimoireBook;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Why: the page casts of Rades Spirito's Soul Corpse Magic, kept apart from {@link Corpses} (which is what a body IS) so a page is
 * three lines: check, ask the manager, show the cast. Each cast follows the grimoire contract: return false (so no cooldown) when
 * nothing happened, and send a short reason to the player. Mana for a page is taken by the book through the page's cost tier; the
 * extra payment of the pact at the moment it saves someone is {@link CorpseMana}.
 */
public final class SoulCorpseSpells {
    private SoulCorpseSpells() {}

    /** Reanimation: raises the newest of the dead of the last minute. */
    public static boolean reanimate(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!CorpseConfig.enabled()) { GrimoireBook.fail(p, "Soul Corpse Magic is switched off on this server."); return false; }
        int made = Corpses.reanimate(p, CorpseConfig.raiseMax());
        if (made <= 0) { GrimoireBook.fail(p, "No one worth raising has died near you in the last minute."); return false; }
        b.castCircle(p, 0.8f);
        p.displayClientMessage(Component.literal(made == 1 ? "A Soul Corpse stands up." : made + " Soul Corpses stand up.").withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    /** One of the named puppets: Jimmy, Alfredo or Carl. */
    public static boolean named(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, NamedCorpses kind) {
        if (!CorpseConfig.enabled()) { GrimoireBook.fail(p, "Soul Corpse Magic is switched off on this server."); return false; }
        if (Corpses.summonNamed(p, kind) == null) { GrimoireBook.fail(p, kind.display + " will not come here."); return false; }
        b.castCircle(p, 1.0f);
        p.displayClientMessage(Component.literal(kind.display + " answers your call.").withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    /** True Soul Corpse Magic: arms the pact (ten minutes), re-raises the pets that fell shortly before, and starts the long cooldown itself. */
    public static boolean truePact(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!CorpseConfig.enabled()) { GrimoireBook.fail(p, "Soul Corpse Magic is switched off on this server."); return false; }
        if (Pacts.armed(p)) { GrimoireBook.fail(p, Pacts.status(p)); return false; }
        if (!Pacts.arm(p)) { GrimoireBook.fail(p, "Your pact is already running."); return false; }
        int pets = Corpses.reraisePets(p);
        int ticks = (int) (CorpseConfig.trueCooldownSeconds() * 20 * com.newuniverse.nusmp.mana.ManaLaw.cooldownMult(p)
                * com.newuniverse.nusmp.item.MagicGear.cooldownMult(p) * NUGameRules.spellCooldown(p.level()));
        i.setCoolDown(NUSMP.ticksToSeconds(ticks), mode);
        b.castCircle(p, 1.2f);
        CorpseVfx.pact(p.serverLevel(), p, 100, false);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5f, 0.5f);
        p.displayClientMessage(Component.literal("The pact is armed for " + CorpseConfig.pactTicks() / 1200 + " minutes. Sneak and use on up to 3 players to bind them."
                + (pets > 0 ? " " + pets + (pets == 1 ? " fallen companion rises." : " fallen companions rise.") : "")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        return true;
    }
}
