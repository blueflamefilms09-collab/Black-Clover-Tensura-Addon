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

    /**
     * Reanimation: raises the newest of the dead of the last minute; when there are none, the earth gives up new corpses that fit the place
     * (1.46.0, config corpse.conjure). Every outcome shows a line on the action bar so the player always sees why something did or did not happen.
     */
    public static boolean reanimate(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!CorpseConfig.enabled()) { GrimoireBook.fail(p, "Soul Corpse Magic is switched off on this server."); return false; }
        Corpses.Result r = Corpses.reanimateOrConjure(p, CorpseConfig.raiseMax(), b.masteryFrac(i));
        switch (r.outcome()) {
            case RAISED_DEAD -> {
                b.castCircle(p, 0.8f);
                p.displayClientMessage(Component.literal(r.made() == 1 ? "The dead answer: a Soul Corpse stands up." : "The dead answer: " + r.made() + " Soul Corpses stand up.")
                        .withStyle(ChatFormatting.GRAY), true);
                return true;
            }
            case CONJURED -> {
                b.castCircle(p, 0.8f);
                String line = r.made() == 1 ? "The earth gives up a corpse" : "The earth gives up " + r.made() + " corpses";
                p.displayClientMessage(Component.literal(line + (r.partial() ? " (not enough mana for more)." : ".")).withStyle(ChatFormatting.GRAY), true);
                return true;
            }
            case NO_MANA -> GrimoireBook.fail(p, "Nothing to raise: not enough mana.");
            case NO_ROOM -> GrimoireBook.fail(p, "Nothing to raise: there is no room for a body to rise here.");
            case NOTHING -> GrimoireBook.fail(p, "Nothing to raise: no one has died near you in the last minute.");
            case OFF -> GrimoireBook.fail(p, "Soul Corpse Magic is switched off on this server.");
        }
        return false;
    }

    /** One of Rades's numbered undead: No.4 Jimmy, No.3 David, No.2 Alfredo, No.1 Carl or No.0 Michael Caesal (the elite: only one at a time). */
    public static boolean named(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, NamedCorpses kind) {
        if (!CorpseConfig.enabled()) { refuse(p, kind, RadesCastDiagnosis.Refusal.SWITCHED_OFF); return false; }
        if (kind.elite() && Corpses.liveNamed(p, kind) != null) { refuse(p, kind, RadesCastDiagnosis.Refusal.ELITE_STANDS); return false; }
        Corpses.NamedSummon made = Corpses.summonNamedChecked(p, kind);      // 3.7.1: a refusal has a reason: it is shown to the player and written to the server log
        if (made.body() == null) { GrimoireBook.fail(p, made.why().playerLine(kind.title())); return false; }
        b.castCircle(p, kind.elite() ? 1.4f : 1.0f);
        p.displayClientMessage(Component.literal(kind.title() + " answers your call.").withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    /** A refusal before any body is made: the server log line (rate limited) and the player's line (summonNamedChecked logs its own refusals). */
    private static void refuse(ServerPlayer p, NamedCorpses kind, RadesCastDiagnosis.Refusal why) {
        Corpses.diag(p.getUUID(), p.level().getGameTime(), RadesCastDiagnosis.refusedLine(p.getGameProfile().getName(), kind.id, why));
        GrimoireBook.fail(p, why.playerLine(kind.title()));
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
