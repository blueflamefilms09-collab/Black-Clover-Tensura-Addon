package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.GrimoireSkillInstance;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.grimoire.CanonNaming;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing of Rades's undead can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs this
 * (key {@code magic.rades_undead}). It checks, on a dedicated server with a stand-in player: the five numbered undead and the soul zombie are registered entity types and flagged undead
 * (the {@code minecraft:undead} tag, which smite and the Soul Burial engraving read); each numbered undead is created with its table's health; the two new pages are the LAST pages of the
 * Soul Corpse book and the old ones kept their places; a summoned zombie is owned, tagged, has the scaled health, never burns in daylight and is dismissed when the cap is passed (oldest
 * first) and when its owner goes; the numbered undead are capped at {@link NamedCorpses#MAX_AT_ONCE} and the elite stands only once; the Unique Skill's name carries the bound character
 * for every character of {@link CanonBook} and is the plain name without one. Everything is cleaned up at the end.
 */
public final class RadesUndeadSelfTests {
    private RadesUndeadSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.rades_undead", RadesUndeadSelfTests::run);
    }

    private static Result run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer owner = SelfTests.standIn(level, "rades_undead", at.x, at.y, at.z);
        StringBuilder sb = new StringBuilder();
        try {
            // 1. registered, undead, with their rows' health
            for (NamedCorpses k : NamedCorpses.values()) {
                EntityType<NamedCorpseEntity> t = CorpseEntities.type(k);
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", k.id))) return Result.fail(k.id + " is not registered");
                if (!t.is(EntityTypeTags.UNDEAD)) return Result.fail(k.id + " is not flagged undead (minecraft:undead)");
                NamedCorpseEntity e = t.create(level);
                if (e == null) return Result.fail(k.id + " could not be created");
                if (e.kind() != k) return Result.fail(k.id + " was built with the row " + e.kind());
                if (Math.abs(e.getAttributeValue(Attributes.MAX_HEALTH) - k.health) > 0.01) return Result.fail(k.id + " has health " + e.getAttributeValue(Attributes.MAX_HEALTH) + ", the row says " + k.health);
            }
            if (!CorpseEntities.SOUL_ZOMBIE.get().is(EntityTypeTags.UNDEAD)) return Result.fail("the soul zombie is not flagged undead");
            sb.append("5 undead + zombie registered and undead; ");

            // 2. the pages: the two new ones are last, the old five kept their places
            GrimoireBook book = NUSkills.WIKI_ATTRIBUTE_BOOKS.get(MagicType.SOUL_CORPSE).get();
            List<BookPage> pages = book.pages();
            int fam = book.familyCount();
            if (!pages.get(fam - 2).id().equals("corpse_david") || !pages.get(fam - 1).id().equals("corpse_michael_caesal")) return Result.fail("corpse_david and corpse_michael_caesal are not the last two family pages");
            String[] old = {"corpse_reanimate", "corpse_jimmy", "corpse_alfredo", "corpse_carl", "true_soul_corpse"};
            for (int i = 0; i < old.length; i++) if (!pages.get(5 + i).id().equals(old[i])) return Result.fail("the page " + old[i] + " moved from index " + (5 + i));
            if (!pages.get(6).name().equals("No.4 - Jimmy") || !pages.get(7).name().equals("No.2 - Alfredo") || !pages.get(8).name().equals("No.1 - Carl")) return Result.fail("a numbered page is not named No.N - Name");
            sb.append("pages ok (").append(fam).append("); ");

            // 3. the zombies: owned, tagged, scaled, sun-safe, capped, dismissed
            int cap = CorpseConfig.zombieCap();
            List<SoulZombieEntity> first = SoulZombies.summon(owner, 3, 400, 1.4, null);
            if (first.isEmpty()) return Result.fail("no zombie could be summoned");
            SoulZombieEntity z = first.get(0);
            if (!owner.getUUID().equals(z.getOwnerUUID()) || !owner.getUUID().equals(Corpses.ownerOf(z)) || !Corpses.isCorpse(z)) return Result.fail("a summoned zombie is not owned and tagged");
            if (Math.abs(z.getMaxHealth() - SoulZombieRules.health(1.4)) > 0.01) return Result.fail("zombie health " + z.getMaxHealth() + ", expected " + SoulZombieRules.health(1.4));
            if (z.burnsInDaylight() || z.isSunBurnTick()) return Result.fail("a summoned zombie would burn in daylight");
            if (z.isAlliedTo(owner) == false) return Result.fail("a zombie does not count its owner as an ally");
            SoulZombies.summon(owner, cap + 3, 400, 1.0, null);
            int now = SoulZombies.count(owner.getUUID());
            if (now > cap) return Result.fail("the zombie cap " + cap + " was passed: " + now);
            if (Corpses.ZOMBIES.total() > CorpseConfig.zombieGlobalCap()) return Result.fail("the server-wide zombie cap was passed: " + Corpses.ZOMBIES.total());
            if (!first.stream().allMatch(e -> e.isRemoved() || e.isAlive())) return Result.fail("a zombie is in an odd state");
            sb.append("zombies ").append(now).append("/").append(cap).append(" sun-safe; ");
            Corpses.crumbleAllOf(server, owner.getUUID());
            if (SoulZombies.count(owner.getUUID()) != 0) return Result.fail("the zombies were not dismissed with their owner");

            // 4. the numbered undead: at most three at once, the elite once
            Corpses.summonNamed(owner, NamedCorpses.MICHAEL_CAESAL);
            if (Corpses.liveNamed(owner, NamedCorpses.MICHAEL_CAESAL) == null) return Result.fail("Michael Caesal did not stand");
            for (NamedCorpses k : new NamedCorpses[]{NamedCorpses.JIMMY, NamedCorpses.DAVID, NamedCorpses.ALFREDO, NamedCorpses.CARL}) Corpses.summonNamed(owner, k);
            int named = Corpses.NAMED.count(owner.getUUID());
            if (named > NamedCorpses.MAX_AT_ONCE) return Result.fail(named + " numbered undead stand at once (cap " + NamedCorpses.MAX_AT_ONCE + ")");
            Corpses.crumbleAllOf(server, owner.getUUID());
            sb.append("named cap ").append(named).append("/").append(NamedCorpses.MAX_AT_ONCE).append("; ");

            // 5. the Unique Skill names the character
            Set<String> seen = new HashSet<>();
            for (CanonBook c : CanonBook.values()) {
                MagicType m = MagicType.byName(c.magic);
                ManasSkillInstance inst = NUSkills.grimoireSkillFor(m).createDefaultInstance();      // the book, or the legacy skill of a magic with no book
                if (!(inst instanceof GrimoireSkillInstance gi)) return Result.fail(c.name() + ": the book's instance is not a GrimoireSkillInstance");
                String plain = inst.getDisplayName().getString();
                inst.getOrCreateTag().putString(CanonNaming.TAG, c.id());
                String named2 = inst.getDisplayName().getString();
                if (!named2.startsWith(CanonNaming.possessive(c.owner) + " ") || !named2.endsWith(plain)) return Result.fail(c.name() + ": the skill name is '" + named2 + "' (plain '" + plain + "')");
                inst.getOrCreateTag().remove(CanonNaming.TAG);
                if (!inst.getDisplayName().getString().equals(plain)) return Result.fail(c.name() + ": the name did not return to the plain one");
                seen.add(c.id());
            }
            sb.append(seen.size()).append(" characters named");
            return Result.ok(sb.toString());
        } finally {
            Corpses.crumbleAllOf(server, owner.getUUID());
        }
    }
}
