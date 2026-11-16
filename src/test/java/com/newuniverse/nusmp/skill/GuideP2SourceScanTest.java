package com.newuniverse.nusmp.skill;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guide P2 (docs/guide_fixes_p2.md), read from the source: the combined class damage cap is called by SkillDamage; the two legacy grimoire skill families pay through Tensura behind the flatCosts
 * switch; the grimoire cast event and its built-in listener; the one page damage block; the volatile page caches; the dirty-only-on-change gates; the Quenched bridge (data-driven, no class of the other
 * mod, off by default); the translatable messages; the smoke list. Pure (file reads); the working directory is the project root.
 */
class GuideP2SourceScanTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(MAIN.resolve(rel), StandardCharsets.UTF_8); }

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }

    @Test
    void skillDamagePassesTheClassToTheBalanceLaw() throws IOException {
        String d = code(read("skill/SkillDamage.java"));
        assertTrue(d.contains("instance.getSkill() instanceof NUSkill s ? s.ultimateClass() : null"), "the class of the skill instance");
        assertTrue(d.contains("BalanceLaw.damage(caster, target, (float) scaled, masteryFrac, cls)"), "the class-aware damage");
        // the old path stays behind the existing classDamage switch (and a null class is the plain law)
        String law = code(read("balance/BalanceLaw.java"));
        assertTrue(law.contains("if (cls == null || !NUConfig.ucClassDamage()) return damage(caster, target, raw, masteryFrac);"), "classDamage=false and an unclassified skill take the plain law");
    }

    @Test
    void theLegacyGrimoireSkillsPayThroughTensuraBehindTheFlatCostsSwitch() throws IOException {
        String lp = code(read("skill/LegacyPay.java"));
        assertTrue(lp.contains("EnergyHelper.isOutOfEnergy(player, instance, mode)"), "Tensura checks and takes the price");
        assertTrue(lp.contains("NUConfig.ucFlatCosts()") && lp.contains("SkillUtil.spendMagicules(player, flat)"), "flatCosts = the old flat write");
        assertTrue(lp.contains("PAYING.remove()"), "the scope is cleared in finally");
        assertEquals(1, count(lp, "isOutOfEnergy("), "one Tensura payment per cast");
        String page = code(read("skill/GrimoirePageSkill.java"));
        assertTrue(page.contains("public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode)") && page.contains("LegacyPay.paying()"), "the page skill answers the price");
        assertEquals(1, count(page, "SkillUtil.spendMagicules("), "only the old static pay(player, base) keeps the direct write");
        String magic = code(read("skill/GrimoireMagicSkill.java"));
        assertEquals(0, count(magic, "SkillUtil.spendMagicules("), "the five grimoire_<magic> casts pay through LegacyPay");
        assertEquals(5, count(magic, "LegacyPay.charge(instance, player, mode,"));
        assertTrue(magic.contains("public double getMagiculeCost(") && magic.contains("LegacyPay.paying()"), "the magic skill answers the price");
        String time = code(read("skill/TimePages.java"));
        assertFalse(Pattern.compile("\\bpay\\(p, ").matcher(time).find(), "every Time page pay passes its skill instance");
        assertEquals(7, count(time, "pay(i, p, "));
    }

    @Test
    void theGrimoireCastEventIsPostedAndHeardBehindItsSwitch() throws IOException {
        String ev = code(read("book/GrimoireCastEvent.java"));
        assertTrue(ev.contains("class Pre extends GrimoireCastEvent implements ICancellableEvent") && ev.contains("class Post extends GrimoireCastEvent"));
        String gb = code(read("book/GrimoireBook.java"));
        assertTrue(gb.contains("NUConfig.gfCastEvent() && net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new GrimoireCastEvent.Pre(this, player, instance, p, mode)).isCanceled()) return;"), "a Pre listener can refuse a cast");
        assertTrue(gb.contains("new GrimoireCastEvent.Post(this, player, instance, p, mode)") && gb.contains("else GrimoireCastListeners.afterCast(this, player, instance, p, mode);"), "the old direct calls stay as the switch's other branch");
        int pre = gb.indexOf("GrimoireCastEvent.Pre"), pay = gb.indexOf("EnergyHelper.isOutOfEnergy(player, instance, mode)");
        assertTrue(pre > 0 && pre < pay, "Pre comes before the payment");
        String l = code(read("book/GrimoireCastListeners.java"));
        for (String hook : new String[]{"CharacterSets.onCast", "CharacterFlourishRest.after", "CharacterFlairBulls.onCast", "QuestHooks.onCast", "FlowCasts.onCast", "MagicGear.onCast",
                "Wave6Events.onSpellCast", "SpiritDark.onCast", "Wave16.onSpellCast", "DreamWorld.onCast", "MirrorWorks.echo", "ManaLaw.onCast", "FranchiseEvents.onSpellCast", "SolomonCasts.record"})
            assertTrue(l.contains(hook), hook + " moved out of the cast tail");
        assertFalse(gb.contains("SolomonCasts.record") || gb.contains("Wave16.onSpellCast"), "castPage no longer calls the features directly");
        assertTrue(code(read("NUSMP.java")).contains("addListener(com.newuniverse.nusmp.book.GrimoireCastListeners::onPost)"), "registered");
        String cfg = code(read("NUConfig.java"));
        assertTrue(cfg.contains("define(\"grimoireCastEvent\", true)") && cfg.contains("define(\"markDirtyOnlyOnChange\", true)"), "SERVER switches, default on");
    }

    @Test
    void thePageDamageBlockIsOneAndThePageCachesArePublishedThroughVolatileHolders() throws IOException {
        String gb = code(read("book/GrimoireBook.java"));
        assertEquals(1, count(gb, "ArmourState.spellBoost(caster, magic)"), "the multiplier block exists once (pageDamage)");
        assertEquals(1, count(gb, "BalanceLaw.damage(caster, target, r, masteryFrac(i))"));
        assertTrue(gb.contains("private volatile PageCache cache;") && gb.contains("private volatile FamilyCache family;"), "one volatile field publishes each finished list");
        assertFalse(gb.contains("builtVersion") || gb.contains("restAll"), "the loose per-field caches are gone");
    }

    @Test
    void dirtyIsMarkedOnlyOnChangeBehindTheSwitch() throws IOException {
        String gb = code(read("book/GrimoireBook.java"));
        assertTrue(gb.contains("if (changed || !com.newuniverse.nusmp.NUConfig.gfDirtyOnChange()) instance.markDirty();"));
        String am = code(read("antimagic/AntiMagic.java"));
        assertTrue(am.contains("if (changed || !com.newuniverse.nusmp.NUConfig.gfDirtyOnChange()) i.markDirty();"));
    }

    @Test
    void theQuenchedBridgeIsDataDrivenOffByDefaultAndTouchesNoClassOfTheOtherMod() throws IOException {
        String b = code(read("labyrinth/mega/cores/QuenchedBridge.java"));
        assertFalse(b.contains("tensuranoindex."), "no class of the other mod");
        assertTrue(b.contains("BossCoreConfig.quenchedBridge()") && b.contains("QuenchedRules.render("), "behind the switch, through the pure rules");
        assertTrue(b.contains("performPrefixedCommand"), "the owner's command is what runs");
        String cfg = code(read("labyrinth/mega/cores/BossCoreConfig.java"));
        assertTrue(cfg.contains("bool(b, \"bridge\", false,") && cfg.contains("define(\"telesmaCommand\", \"\")") && cfg.contains("define(\"maliceCommand\", \"\")"), "off, blank commands");
        String rules = code(read("labyrinth/mega/cores/QuenchedRules.java"));
        assertFalse(rules.contains("net.minecraft") || rules.contains("net.neoforged"), "the rules are pure");
        String ev = code(read("labyrinth/mega/cores/BossCoreEvents.java"));
        assertTrue(ev.contains("QuenchedBridge.forget(id)") && ev.contains("QuenchedBridge.clear()"), "no state outlives the player or the server");
    }

    @Test
    void theMessagesAreTranslatableAndTheFragmentHasTheirKeysAndTheSmokeListHasThePrefix() throws IOException {
        String frag = Files.readString(Path.of("tools/lang/guide_p2.json"), StandardCharsets.UTF_8);
        String magic = read("skill/GrimoireMagicSkill.java") + read("skill/GrimoirePageSkill.java");
        var m = Pattern.compile("\"(anime_multiverse\\.skill\\.(?:mode|msg)\\.(?:grimoire_[a-z_]+|guide_p2\\.[a-z_]+))\"").matcher(magic);
        int keys = 0;
        while (m.find()) { keys++; assertTrue(frag.contains("\"" + m.group(1) + "\""), "lang fragment lacks " + m.group(1)); }
        m = Pattern.compile("SkillMsg\\.t\\(\"(guide_p2\\.[a-z_]+)\"").matcher(magic);
        while (m.find()) { keys++; assertTrue(frag.contains("\"anime_multiverse.skill.msg." + m.group(1) + "\""), "lang fragment lacks " + m.group(1)); }
        assertTrue(keys >= 10, "the converted strings moved (" + keys + ")");
        assertFalse(code(read("skill/GrimoireMagicSkill.java")).contains("\" has entered Devil Union with \""), "the broadcast is translatable");
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"), StandardCharsets.UTF_8);
        assertTrue(smoke.contains(",ultclass,guide.p2,"), "guide.p2 right after ultclass");
    }
}
