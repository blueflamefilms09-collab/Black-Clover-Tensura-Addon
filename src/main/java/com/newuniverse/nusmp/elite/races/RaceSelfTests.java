package com.newuniverse.nusmp.elite.races;

import com.newuniverse.nusmp.elite.races.data.RaceAttributeKeys;
import com.newuniverse.nusmp.elite.races.data.RaceTemplate;
import com.newuniverse.nusmp.elite.races.data.RaceTemplateLoader;
import com.newuniverse.nusmp.elite.races.data.RaceTemplates;
import com.newuniverse.nusmp.elite.races.race.DivineSaiyan;
import com.newuniverse.nusmp.elite.races.race.EliteRace;
import com.newuniverse.nusmp.elite.races.race.HighSaiyan;
import com.newuniverse.nusmp.elite.races.race.LesserSaiyan;
import com.newuniverse.nusmp.elite.races.race.MediumSaiyan;
import com.newuniverse.nusmp.elite.races.race.PrimordialRace;
import com.newuniverse.nusmp.elite.races.rule.EvolutionRules;
import com.newuniverse.nusmp.elite.races.rule.RaceEvolutionRule;
import com.newuniverse.nusmp.elite.races.rule.RaceRuleSet;
import com.newuniverse.nusmp.elite.races.runtime.PlayerRaceCtx;
import com.newuniverse.nusmp.elite.races.runtime.RaceAttributes;
import com.newuniverse.nusmp.elite.races.runtime.RaceEnergy;
import com.newuniverse.nusmp.elite.races.runtime.RaceEvolution;
import com.newuniverse.nusmp.elite.races.runtime.RaceLedger;
import com.newuniverse.nusmp.elite.races.runtime.RaceSkills;
import com.newuniverse.nusmp.elite.races.runtime.RaceTransforms;
import com.newuniverse.nusmp.elite.races.state.Coexistence;
import com.newuniverse.nusmp.elite.races.state.StateModifier;
import com.newuniverse.nusmp.elite.races.state.TransformMachine;
import com.newuniverse.nusmp.elite.races.state.TransformSpec;
import com.newuniverse.nusmp.elite.races.state.TransformState;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Why: nothing here can be played by its author, so each part of the elite races has a SELF-TEST that the CI server smoke test runs in the real game ({@code /multiverse mega
 * selftest}, keys {@code elite.races.*}; add the prefix {@code elite.races} to the smoke script's expected list). {@code registry}: the five races are in ManasCore's race registry
 * with the right classes, Tensura's default-race block carries the template numbers, the evolution links are right and each race has an icon file. {@code templates}: the key
 * tables agree, every attribute, skill and item named in a race file exists in the running game, the datapack reader finds the five files. {@code race}: a stand-in player can be
 * set to a race and gets its health. {@code transform}: a form is entered, applies its transient modifiers, ends, rests and refuses to start during the rest. {@code evolve}: the
 * rules refuse a weak player, an administrator's forced evolution moves the player along the line, the Primordial Rite refuses without its rules. {@code coexist}: the Kaioken
 * overlap rule on the real Saiyan files. A test that changes the stand-in's race puts it back.
 */
public final class RaceSelfTests {
    private RaceSelfTests() {}

    private static final String PLAYER = "elite_races_tester";

    public static void register() {
        SelfTests.register("elite.races.registry", RaceSelfTests::registry);
        SelfTests.register("elite.races.templates", RaceSelfTests::templates);
        SelfTests.register("elite.races.race", RaceSelfTests::race);
        SelfTests.register("elite.races.transform", RaceSelfTests::transform);
        SelfTests.register("elite.races.evolve", RaceSelfTests::evolve);
        SelfTests.register("elite.races.coexist", RaceSelfTests::coexist);
    }

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    private static SelfTests.Result problems(List<String> p) { return fail(p.size() + " problem(s): " + String.join(" | ", p.subList(0, Math.min(6, p.size())))); }

    private static FakePlayer tester(MinecraftServer server) {
        ServerLevel level = server.overworld();
        return SelfTests.player(level, PLAYER, 0.5, level.getSharedSpawnPos().getY() + 1.0, 0.5);
    }

    private static void setRace(ServerPlayer p, String id) {
        ManasRace r = EliteRaces.race(id);
        if (r == null) throw new IllegalStateException("race " + id + " is not registered");
        if (!RaceAPI.getRaceFrom(p).setRace(r, true)) throw new IllegalStateException("Tensura refused setRace(" + id + ")");
    }

    private static String raceId(ServerPlayer p) {
        Optional<ManasRaceInstance> o = RaceAPI.getRaceFrom(p).getRace();
        return o.map(i -> i.getRaceId().toString()).orElse("none");
    }

    // ================================================================ registry
    private static SelfTests.Result registry(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        FakePlayer p = tester(server);
        Class<?>[] classes = {LesserSaiyan.class, MediumSaiyan.class, HighSaiyan.class, DivineSaiyan.class, PrimordialRace.class};
        String[] ids = {LesserSaiyan.ID, MediumSaiyan.ID, HighSaiyan.ID, DivineSaiyan.ID, PrimordialRace.ID};
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            ManasRace r = EliteRaces.race(id);
            if (r == null) { bad.add(id + " is not registered"); continue; }
            if (!classes[i].isInstance(r)) bad.add(id + " is a " + r.getClass().getSimpleName() + ", expected " + classes[i].getSimpleName());
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("nusmp", id);
            ManasRace viaRegistry = RaceAPI.getRaceRegistry().get(rl);
            if (viaRegistry != r) bad.add(id + " is not what ManasCore's race registry holds under " + rl);
            RaceTemplate t = RaceTemplates.get(id);
            if (t == null) { bad.add(id + " has no template"); continue; }
            if (!(r instanceof EliteRace er)) { bad.add(id + " is not an EliteRace"); continue; }
            var cfg = er.getDefaultConfig();
            if (cfg.getMaxHealth() != t.stats().maxHealth() || cfg.getMinAura() != t.stats().auraMin() || cfg.getMaxMagicule() != t.stats().magiculeMax() || cfg.getAttack() != t.stats().attack())
                bad.add(id + ": Tensura's default-race block does not carry the template's numbers");
            if (r.getAttributeModifiers().isEmpty()) bad.add(id + " has no attribute modifiers (applyDefaultAttributeModifiers did not run)");
            if (r.getName().getString().isEmpty()) bad.add(id + " has no name");
            if (EliteRaces.class.getResourceAsStream("/assets/nusmp/textures/race/" + id + ".png") == null) bad.add(id + " has no icon at assets/nusmp/textures/race/" + id + ".png");
            // evolution links, read through Tensura's own methods
            ManasRaceInstance inst = r.createDefaultInstance();
            List<ManasRace> next = r.getNextEvolutions(inst, p);
            if (t.evolvesTo().isEmpty()) {
                if (!next.isEmpty()) bad.add(id + " should be the top of its line but lists " + next.size() + " next evolution(s)");
            } else {
                ManasRace want = EliteRaces.race(t.evolvesTo());
                if (next.size() != 1 || next.get(0) != want) bad.add(id + " does not list " + t.evolvesTo() + " as its next evolution");
                else if (!want.getPreviousEvolutions(want.createDefaultInstance(), p).contains(r)) bad.add(t.evolvesTo() + " does not list " + id + " as its previous evolution");
                if (er.getEvolutionRequirements(inst, p).size() != t.evolution().rules().size()) bad.add(id + " exposes a different number of requirements than its rules");
            }
        }
        if (!bad.isEmpty()) return problems(bad);
        return ok("5 races registered under nusmp:*, right classes, the default-race block carries the template numbers, the Saiyan line links up, icons exist");
    }

    // ================================================================ templates
    private static SelfTests.Result templates(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        // the two key tables must name exactly the same attributes
        Set<String> mapped = new LinkedHashSet<>(RaceAttributes.keys());
        for (String k : RaceAttributeKeys.KNOWN) if (!mapped.contains(k)) bad.add("attribute key '" + k + "' is accepted by the files but has no attribute in RaceAttributes");
        for (String k : mapped) if (!RaceAttributeKeys.KNOWN.contains(k)) bad.add("RaceAttributes maps '" + k + "', which the file parser does not accept");
        for (String k : mapped) if (RaceAttributes.get(k) == null) bad.add("attribute '" + k + "' is not bound in the running game");
        // the datapack reader finds the shipped files
        new RaceTemplateLoader().onResourceManagerReload(server.getResourceManager());
        for (String p : RaceTemplates.problems()) bad.add("race file problem: " + p);
        for (String p : RaceTemplates.checkLines(RaceTemplates.all())) bad.add("line problem: " + p);
        int skills = 0, items = 0;
        Set<String> missingSkills = new LinkedHashSet<>(), missingItems = new LinkedHashSet<>();
        for (RaceTemplate t : RaceTemplates.all().values()) {
            for (String s : t.intrinsic()) { skills++; if (RaceSkills.find(s) == null) missingSkills.add(s); }
            for (String s : t.resistances()) { skills++; if (RaceSkills.find(s) == null) missingSkills.add(s); }
            for (RaceTemplate.Extra e : t.attributes()) if (RaceAttributes.get(e.attribute()) == null) bad.add(t.id() + ": attribute " + e.attribute() + " does not resolve");
            for (TransformSpec sp : t.transformations().values()) {
                for (StateModifier m : sp.modifiers()) if (RaceAttributes.get(m.attribute()) == null) bad.add(t.id() + "." + sp.state().id + ": attribute " + m.attribute() + " does not resolve");
                sp.entries().forEach(e -> { if (!e.item().isEmpty() && !PlayerRaceCtx.itemExists(e.item())) missingItems.add(e.item()); });
            }
            for (RaceRuleSet set : List.of(t.evolution(), t.entry())) {
                for (RaceEvolutionRule r : set.rules()) {
                    List<RaceEvolutionRule> flat = r instanceof EvolutionRules.AnyOf a ? a.rules() : List.of(r);
                    for (RaceEvolutionRule f : flat) {
                        if (f instanceof EvolutionRules.Mastery m) { skills++; if (RaceSkills.find(m.skill()) == null) missingSkills.add(m.skill()); }
                        if (f instanceof EvolutionRules.Item it) { items++; if (!PlayerRaceCtx.itemExists(it.item())) missingItems.add(it.item()); }
                    }
                }
            }
        }
        if (!missingSkills.isEmpty()) bad.add("skills that do not exist in this game (renamed, or from an add-on that is not loaded): " + missingSkills);
        if (!missingItems.isEmpty()) bad.add("items that do not exist: " + missingItems);
        if (!bad.isEmpty()) return problems(bad);
        return ok(RaceTemplates.all().size() + " race files read through the data-pack reader, " + mapped.size() + " attribute keys match, " + skills + " skill ids and " + items + " item ids resolve");
    }

    // ================================================================ race
    private static SelfTests.Result race(MinecraftServer server) {
        FakePlayer p = tester(server);
        String before = raceId(p);
        try {
            float baseHealth = p.getMaxHealth();
            setRace(p, LesserSaiyan.ID);
            if (!raceId(p).equals("nusmp:lesser_saiyan")) return fail("setRace did not take: the player is " + raceId(p));
            if (RaceTransforms.raceOf(p) == null) return fail("raceOf does not see the elite race on the player");
            float after = p.getMaxHealth();
            if (after <= baseHealth) return fail("max health did not rise with the race (" + baseHealth + " -> " + after + "): the default-race attribute modifiers were not applied");
            setRace(p, PrimordialRace.ID);
            if (RaceTransforms.raceOf(p) == null || !PrimordialRace.ID.equals(RaceTransforms.raceOf(p).templateId())) return fail("the Primordial race did not take");
            return ok("a stand-in became a Lesser Saiyan (max health " + baseHealth + " -> " + after + ") and then a Primordial; raceOf follows");
        } finally {
            restore(p, before);
        }
    }

    private static void restore(ServerPlayer p, String before) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(before);
            ManasRace r = rl == null ? null : RaceAPI.getRaceRegistry().get(rl);
            if (r != null) RaceAPI.getRaceFrom(p).setRace(r, true);
        } catch (RuntimeException ignored) {
            // the stand-in is throw-away
        }
        RaceTransforms.onRespawn(p);
    }

    // ================================================================ transform
    private static boolean hasStateModifier(ServerPlayer p, String attribute) {
        var h = RaceAttributes.get(attribute);
        var inst = h == null ? null : p.getAttribute(h);
        return inst != null && inst.hasModifier(ResourceLocation.fromNamespaceAndPath("nusmp", "elite_state_" + attribute.replace(':', '_')));
    }

    private static SelfTests.Result transform(MinecraftServer server) {
        FakePlayer p = tester(server);
        String before = raceId(p);
        try {
            setRace(p, HighSaiyan.ID);
            RaceTransforms.onRespawn(p);
            RaceEnergy.setAuraFraction(p, 1.0);
            List<String> bad = new ArrayList<>();
            double atk = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
            RaceTransforms.Outcome in = RaceTransforms.request(p, RaceTransforms.Mode.STATE, TransformState.BOOSTED, false);
            if (!in.ok()) return fail("the ki boost was refused: " + in.message().getString());
            TransformMachine m = RaceLedger.machine(p);
            if (m.state() != TransformState.BOOSTED) bad.add("the machine is " + m.state() + " after entering the ki boost");
            if (!hasStateModifier(p, "attack_damage")) bad.add("the ki boost added no attack modifier");
            if (p.getAttributeValue(Attributes.ATTACK_DAMAGE) <= atk) bad.add("attack did not rise in the ki boost");
            RaceTransforms.Outcome again = RaceTransforms.request(p, RaceTransforms.Mode.STATE, TransformState.BOOSTED, false);
            if (again.ok()) bad.add("the same form was entered twice");
            RaceTransforms.Outcome out = RaceTransforms.request(p, RaceTransforms.Mode.OFF, null, false);
            if (!out.ok()) bad.add("leaving the form failed: " + out.message().getString());
            if (m.state() != TransformState.BASE) bad.add("the machine did not return to base");
            if (hasStateModifier(p, "attack_damage")) bad.add("the attack modifier stayed after the form ended");
            if (Math.abs(p.getAttributeValue(Attributes.ATTACK_DAMAGE) - atk) > 1e-6) bad.add("attack is not back to " + atk);
            if (EliteRacesConfig.cooldownScale() > 0 && m.restLeft(TransformState.BOOSTED, server.overworld().getGameTime()) <= 0) bad.add("the ki boost has no rest after it ended");
            if (EliteRacesConfig.cooldownScale() > 0 && RaceTransforms.request(p, RaceTransforms.Mode.STATE, TransformState.BOOSTED, false).ok()) bad.add("the ki boost was entered during its rest");
            // an administrator can force a form past its conditions (the ascension needs a rage and 90,000 EP), and a forced form is still a transient modifier
            RaceTransforms.Outcome forced = RaceTransforms.request(p, RaceTransforms.Mode.STATE, TransformState.GREAT_APE, true);
            if (!forced.ok()) bad.add("a forced Great Ape was refused: " + forced.message().getString());
            else {
                if (!hasStateModifier(p, "scale")) bad.add("the Great Ape added no scale modifier");
                RaceTransforms.request(p, RaceTransforms.Mode.OFF, null, false);
                if (hasStateModifier(p, "scale")) bad.add("the scale modifier stayed after the Great Ape");
            }
            if (RaceTransforms.request(p, RaceTransforms.Mode.STATE, TransformState.DIVINE, false).ok()) bad.add("a High Saiyan entered the Divine form");
            if (!bad.isEmpty()) return problems(bad);
            return ok("a High Saiyan stand-in entered the ki boost (attack " + atk + " -> up), left it (modifier gone, rest running, re-entry refused), took a forced Great Ape (scale modifier on and off) and was refused the Divine form");
        } finally {
            restore(p, before);
        }
    }

    // ================================================================ evolve
    private static SelfTests.Result evolve(MinecraftServer server) {
        FakePlayer p = tester(server);
        String before = raceId(p);
        try {
            List<String> bad = new ArrayList<>();
            setRace(p, LesserSaiyan.ID);
            RaceTransforms.onRespawn(p);
            RaceLedger.setKills(p, "any", 0);
            RaceEvolution.Outcome weak = RaceEvolution.evolve(p, false);
            if (weak.ok()) bad.add("a fresh Lesser Saiyan evolved by the rules");
            if (!"nusmp:lesser_saiyan".equals(raceId(p))) bad.add("the refused evolution still changed the race to " + raceId(p));
            RaceTemplate lesser = RaceTemplates.get(LesserSaiyan.ID);
            double bar = lesser.evolution().percent(PlayerRaceCtx.of(p));
            RaceLedger.setKills(p, "any", 100000);
            double bar2 = lesser.evolution().percent(PlayerRaceCtx.of(p));
            if (bar2 <= bar) bad.add("the kill counter did not move the evolution bar (" + bar + " -> " + bar2 + ")");
            RaceLedger.setKills(p, "any", 0);
            RaceEvolution.Outcome forced = RaceEvolution.evolve(p, true);
            if (!forced.ok()) bad.add("a forced evolution failed: " + forced.message().getString());
            else if (!"nusmp:medium_saiyan".equals(raceId(p))) bad.add("the forced evolution left the player as " + raceId(p));
            RaceEvolution.Outcome rite = RaceEvolution.rite(p, PrimordialRace.ID, false);
            if (rite.ok()) bad.add("the Primordial Rite worked with no rules met");
            RaceEvolution.Outcome saiyanRite = RaceEvolution.rite(p, HighSaiyan.ID, false);
            if (saiyanRite.ok()) bad.add("a Saiyan tier was entered by a rite, which only the Primordial has");
            RaceEvolution.Outcome forcedRite = RaceEvolution.rite(p, PrimordialRace.ID, true);
            if (!forcedRite.ok()) bad.add("a forced rite failed: " + forcedRite.message().getString());
            else if (!"nusmp:primordial".equals(raceId(p))) bad.add("the forced rite left the player as " + raceId(p));
            if (RaceEvolution.evolve(p, true).ok()) bad.add("the top of a line evolved");
            if (!bad.isEmpty()) return problems(bad);
            return ok("rules refused a fresh Lesser Saiyan, the kill counter moves the bar, a forced evolution reached Medium Saiyan, the rite refuses without its rules and a forced rite makes a Primordial");
        } finally {
            RaceLedger.setKills(p, "any", 0);
            restore(p, before);
        }
    }

    // ================================================================ coexist
    private static SelfTests.Result coexist(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        RaceTemplate t = RaceTemplates.get(HighSaiyan.ID);
        TransformSpec boost = t.spec(TransformState.BOOSTED);
        double alone = amount(Coexistence.effective(boost, false, EliteRacesConfig.kaiokenOverlap()), "attack_damage");
        double both = amount(Coexistence.effective(boost, true, EliteRacesConfig.kaiokenOverlap()), "attack_damage");
        if (Math.abs(alone - amount(boost.modifiers(), "attack_damage")) > 1e-9) bad.add("the ki boost alone is not at full strength");
        if (Math.abs(both - alone * EliteRacesConfig.kaiokenOverlap()) > 1e-9) bad.add("with Kaioken on the boost is " + both + ", expected " + alone * EliteRacesConfig.kaiokenOverlap());
        TransformSpec divine = RaceTemplates.get(DivineSaiyan.ID).spec(TransformState.DIVINE);
        if (!Coexistence.effective(divine, true, 0.1).equals(divine.modifiers())) bad.add("the Divine form is scaled by Kaioken, which it must not be");
        if (!bad.isEmpty()) return problems(bad);
        String found = "none of " + EliteRacesConfig.kaiokenSkillIds() + " is registered (the overlap rule is inert until the Kaioken skill exists)";
        for (String id : EliteRacesConfig.kaiokenSkillIds()) if (RaceSkills.find(id) != null) { found = id + " is registered"; break; }
        return ok("overlap factor " + EliteRacesConfig.kaiokenOverlap() + ": boost attack " + alone + " alone, " + both + " beside Kaioken; Divine unscaled; " + found);
    }

    private static double amount(List<StateModifier> mods, String attribute) {
        for (StateModifier m : mods) if (m.attribute().equals(attribute)) return m.amount();
        return 0.0;
    }
}
