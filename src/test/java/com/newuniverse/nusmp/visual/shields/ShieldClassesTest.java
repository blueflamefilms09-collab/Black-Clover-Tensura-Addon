package com.newuniverse.nusmp.visual.shields;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 1.110.0: a source scan of the item classes behind the shields: each answers vanilla's shield contract ({@code UseAnim.BLOCK}, a use duration of 72000 so the blocking
 * state of LivingEntity#isBlocking holds after its five ticks, {@code SHIELD_BLOCK}, and a {@code use} that starts the hand), in either hand, and the client registers vanilla's
 * {@code blocking} property for every id of {@link ShieldSet}. Every relic of the shield look is in the set, so a shield added later must join the preset.
 */
class ShieldClassesTest {
    static final Path JAVA = Path.of("src", "main", "java", "com", "newuniverse", "nusmp");

    static String read(String rel) throws IOException { return Files.readString(JAVA.resolve(rel)); }

    @Test
    void theMagebaneShieldClassIsAVanillaShield() throws IOException {
        String s = read("item/MagicWeaponItem.java");
        assertTrue(s.contains("kind == Kind.MAGEBANE_SHIELD ? UseAnim.BLOCK"), "UseAnim.BLOCK");
        assertTrue(s.contains("kind == Kind.MAGEBANE_SHIELD ? 72000"), "use duration 72000");
        assertTrue(s.contains("kind == Kind.MAGEBANE_SHIELD && ability == ItemAbilities.SHIELD_BLOCK"), "SHIELD_BLOCK");
        int use = s.indexOf("if (kind == Kind.MAGEBANE_SHIELD) {");
        assertTrue(use > 0 && s.indexOf("player.startUsingItem(hand)", use) > use, "use() starts the hand it was given (either hand)");
        assertFalse(s.substring(use, s.indexOf("player.startUsingItem(hand)", use)).contains("MAIN_HAND"), "no main-hand-only gate before it");
    }

    @Test
    void theLegendaryShieldClassIsAVanillaShield() throws IOException {
        String s = read("heroweapon/HeroWeaponItem.java");
        assertTrue(s.contains("case BLOCK -> UseAnim.BLOCK"), "UseAnim.BLOCK");
        assertTrue(s.contains("raised() ? 72000 : 0"), "use duration 72000");
        assertTrue(s.contains("kind.style() == HeroStyle.BLOCK && ability == ItemAbilities.SHIELD_BLOCK"), "SHIELD_BLOCK");
        int off = s.indexOf("kind.style() == HeroStyle.BLOCK && hand == InteractionHand.OFF_HAND");
        assertTrue(off > 0 && s.indexOf("player.startUsingItem(hand)", off) > off, "the off hand raises the shield too");
        assertTrue(s.indexOf("if (hand != InteractionHand.MAIN_HAND) return", off) > off, "the main-hand-only techniques come after the off-hand branch");
    }

    @Test
    void theRelicShieldClassIsAVanillaShield() throws IOException {
        String s = read("relics/RelicWeaponItem.java");
        assertTrue(s.contains("shield() ? UseAnim.BLOCK : UseAnim.NONE"), "UseAnim.BLOCK for the shield look");
        assertTrue(s.contains("shield() && ability == ItemAbilities.SHIELD_BLOCK"), "SHIELD_BLOCK");
        assertTrue(read("relics/RelicItemSupport.java").contains("USE_TICKS = 72000"), "use duration 72000");
        assertTrue(s.contains("getUseDuration(ItemStack stack, LivingEntity user) { return RelicItemSupport.USE_TICKS; }"));
        int use = s.indexOf("public InteractionResultHolder<ItemStack> use(");
        int guard = s.indexOf("if (shield()) {", use), level = s.indexOf("level.isClientSide || !(player instanceof ServerPlayer", use);
        assertTrue(guard > use && guard < level, "the shield branch runs before the server-only relic click, so the client starts the hand too");
        assertTrue(s.indexOf("player.startUsingItem(hand)", guard) < level, "it starts the hand it was given");
        assertTrue(s.contains("ShieldUseRules.decide("), "the decision is the pure rule");
    }

    /** The ids that a {@code RelicDef.of("id", ...)} chain gives {@code .look(Look.SHIELD)}. */
    static Set<String> relicShieldIds() throws IOException {
        Set<String> ids = new TreeSet<>();
        Pattern of = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\"");
        try (var files = Files.walk(JAVA.resolve("relics"))) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                String t = Files.readString(p);
                Matcher m = of.matcher(t);
                List<int[]> spans = new ArrayList<>();
                List<String> names = new ArrayList<>();
                while (m.find()) {
                    names.add(m.group(1));
                    spans.add(new int[]{m.start(), t.length()});
                }
                for (int i = 0; i + 1 < spans.size(); i++) spans.get(i)[1] = spans.get(i + 1)[0];
                for (int i = 0; i < names.size(); i++) if (t.substring(spans.get(i)[0], spans.get(i)[1]).contains("Look.SHIELD")) ids.add(names.get(i));
            }
        }
        return ids;
    }

    @Test
    void everyRelicOfTheShieldLookIsInTheSet() throws IOException {
        Set<String> relics = relicShieldIds();
        assertFalse(relics.isEmpty());
        for (String id : relics) assertTrue(ShieldSet.IDS.contains(id), id + " has Look.SHIELD but is not in ShieldSet: it needs the preset, the blocking model and the predicate");
        for (String id : ShieldSet.IDS) if (!id.equals("legendary_shield")) assertTrue(relics.contains(id), id + " is in ShieldSet but is not a relic of the shield look (and is not the hero Legendary Shield)");
    }

    @Test
    void theClientRegistersTheBlockingPredicateForTheWholeSet() throws IOException {
        String c = read("client/ShieldClient.java");
        assertTrue(c.contains("ShieldSet.IDS") && c.contains("\"blocking\"") && c.contains("getUseItem() == stack"), "vanilla's blocking property for every id of the set");
        assertTrue(read("client/WeaponRenderer.java").contains("ShieldClient::registerBlocking"), "WeaponRenderer wires it at client setup");
        assertTrue(read("client/WeaponRenderer.java").contains("ShieldSet.isShieldMesh(id)"), "the renderer frames a shield mesh on vanilla's frame");
    }

    @Test
    void shieldsAreNotBetterCombatWeapons() throws IOException {
        // a shield swings nothing: the Better Combat attributes live in data/nusmp/weapon_attributes and must not exist for a shield, or Better Combat would treat it as a
        // (possibly two-handed) weapon and hide the off hand the way it does for a two-handed weapon
        Path dir = Path.of("src", "main", "resources", "data", "nusmp", "weapon_attributes");
        for (String id : ShieldSet.IDS) assertFalse(Files.exists(dir.resolve(id + ".json")), id + " must carry no Better Combat attributes");
    }

    @Test
    void noUpgradedVuMeshExistsForAShieldYet() throws IOException {
        // client/relics/item/vu/VuRenderer places its meshes on the sprite diagonal; a shield there would bypass the shield frame, so one must teach it ShieldFrame first
        Path vu = Path.of("src", "main", "resources", "assets", "nusmp", "weapon_meshes_vu");
        if (!Files.isDirectory(vu)) return;
        try (var files = Files.list(vu)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                String n = p.getFileName().toString();
                if (n.endsWith(".txt")) assertFalse(ShieldSet.isShieldMesh(n.substring(0, n.length() - 4)), n + ": VuRenderer does not know the shield frame");
            }
        }
    }

    @Test
    void theAxeRuleAndTheBootAreWired() throws IOException {
        String b = read("item/ShieldBoot.java");
        assertTrue(b.contains("canDisableShield()") && b.contains("addCooldown(used.getItem(), DISABLE_TICKS)"), "an axe puts the used shield on cooldown, whichever item it is");
        assertTrue(b.contains("visual.shields.items") && b.contains("visual.shields.use") && b.contains("visual.shields.models"), "the self-tests");
        assertTrue(read("NUSMP.java").contains("ShieldBoot.init()"), "wired from NUSMP");
    }
}
