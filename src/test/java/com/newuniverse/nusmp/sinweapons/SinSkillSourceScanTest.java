package com.newuniverse.nusmp.sinweapons;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rules 9 and 11 for the five Berserk weapon skills, pinned by reading the source: no client class in the common code, no static player or per-player map, one staggered and bounded look per player, no world scan,
 * no packet of its own, the right click goes through the shared owner check, every skill is registered under the id the rules name, and the self-test is registered. Also the earlier techniques are still there.
 */
class SinSkillSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");
    private static final List<String> FILES = List.of("sinweapons/SinSkillRules.java", "sinweapons/SinSkillService.java", "sinweapons/SinSkillSelfTests.java", "skill/SinWeaponSkill.java");

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", ""); }

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel), StandardCharsets.UTF_8); }

    @Test
    void commonCodeTouchesNoClientClassAndKeepsNoPlayerState() throws IOException {
        for (String f : FILES) {
            String c = code(read(f));
            assertFalse(CLIENT.matcher(c).find(), f + " uses a client class");
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:Server)?Player\\s+\\w+\\s*[;=]").matcher(c).find(), f + " keeps a static player");
            assertFalse(Pattern.compile("static\\s+(?:final\\s+)?(?:Hash|Concurrent|Linked|Identity)?Map<").matcher(c).find(), f + " keeps a per-player map in a static");
            assertFalse(c.contains("getAllEntities") || c.contains("getEntities().getAll") || c.contains("getEntitiesOfClass"), f + " scans entities");
            assertFalse(c.contains("getChunk(") || c.contains("loadChunk") || c.contains("getBlockState("), f + " touches the world");
            assertFalse(c.contains("PacketDistributor") || c.contains("CustomPacketPayload"), f + " sends a packet of its own");
        }
    }

    @Test
    void theLookIsOncePerSecondPerPlayerAndBounded() throws IOException {
        String c = code(read("sinweapons/SinSkillService.java"));
        int tick = c.indexOf("static void onPlayerTick(");
        assertTrue(tick > 0);
        String body = c.substring(tick, c.indexOf("public static void scan("));
        assertTrue(body.indexOf("SinSkillRules.scanTurn(") > 0 && body.indexOf("SinSkillRules.scanTurn(") < body.indexOf("SafeCast.tick("), "the staggered turn is tested before any work");
        assertTrue(body.contains("PerfProbe.begin()") && body.contains("PerfProbe.end("), "the look is inside a probe section");
        String scan = c.substring(c.indexOf("public static void scan("), c.indexOf("public static void technique("));
        assertTrue(scan.contains("SinSkillRules.slotsScanned(inv.getContainerSize())"), "at most 41 slots");
        assertEquals(2, scan.split("for \\(int i = 0; i < n; i\\+\\+\\)", -1).length - 1, "two passes over the slots, no more");
        assertTrue(read("sinweapons/SinWeaponBoot.java").contains("SinSkillService.register(net.neoforged.neoforge.common.NeoForge.EVENT_BUS)"), "listens on the game bus");
    }

    @Test
    void theRightClickGoesThroughTheSharedCheckAndTheOldTechniquesStay() throws IOException {
        String item = code(read("sinweapons/SinWeaponItem.java"));
        int use = item.indexOf("public InteractionResultHolder<ItemStack> use(");
        String body = item.substring(use, item.indexOf("public boolean fire("));
        assertTrue(body.contains("SinSkillService.mayUse(sp, stack)") && body.contains("SinSkillService.refuse(sp, this)"), "the right click asks the shared owner check");
        assertTrue(body.indexOf("isOnCooldown(this)") < body.indexOf("SinSkillService.mayUse"), "a refusal is throttled by the cooldown it sets");
        assertTrue(body.contains("if (technique(sp)) sp.getCooldowns().addCooldown(this, kind.cooldown);"), "the technique and its cooldown are as before");
        assertTrue(item.contains("boolean technique(ServerPlayer p)") && item.contains("hurtEnemy("), "the technique code and the plain hits are kept");
        String boot = code(read("sinweapons/SinWeaponBoot.java"));
        assertTrue(boot.contains("SinSkillService.register(") && boot.contains("SinSkillSelfTests.register();") && boot.contains("SinWeaponSelfTests.register();"), "wired into the one entry point");
        assertTrue(code(read("sinweapons/SinSkillSelfTests.java")).contains("SelfTests.register(\"berserk.skills\""), "the self-test is registered");
    }

    @Test
    void theSettingIsAServerSettingWithTheOldBehaviourOneFlagAway() throws IOException {
        String cfg = read("sinweapons/SinWeaponConfig.java");
        assertTrue(cfg.contains("define(\"soulBound\", true)") && cfg.contains("public static boolean soulBound()"));
        assertTrue(read("sinweapons/SinWeaponBoot.java").contains("ModConfig.Type.SERVER, SinWeaponConfig.SPEC, \"nusmp-sinweapons-server.toml\""), "still the one server config");
    }

    @Test
    void everySkillIsRegisteredUnderTheIdOfTheRulesAndNamedInTheClass() throws IOException {
        String reg = read("skill/NUSkills.java"), cls = read("skill/SinWeaponSkill.java");
        for (SinRules.Kind k : SinRules.Kind.values()) {
            String id = SinSkillRules.skillId(k);
            assertTrue(reg.contains("register(\"" + id + "\", () -> new SinWeaponSkill(com.newuniverse.nusmp.sinweapons.SinRules.Kind." + k.name() + "))"), id + " is not registered for " + k);
            assertTrue(cls.contains("SkillText.of(\"" + id + "\""), id + " has no text in SinWeaponSkill");
            assertTrue(cls.contains("case " + k.name() + " ->"), k + " missing in SinWeaponSkill.skillText");
        }
        String c = code(cls);
        assertTrue(c.contains("super(SkillType.ULTIMATE, skillText(kind), true)") && c.contains("ultimateClass() { return UltimateClass.LORD; }"), "an Ultimate of the Lord class, like Greed's");
        assertTrue(c.contains("getModes(ManasSkillInstance instance) { return IDS.length; }") && c.contains("{\"technique\", \"manifest\"}"), "two modes");
    }

    @Test
    void theModesDoWhatTheRightClickDoesAndForgeOneBoundWeapon() throws IOException {
        String c = code(read("sinweapons/SinSkillService.java"));
        assertTrue(c.contains("w.fire(p)"), "Technique uses the item's own technique and cooldown");
        assertTrue(c.contains("SinSkillRules.manifest(holds, carries, now, p.getPersistentData().getLong(key))"), "Manifest decides through the pure rule");
        assertTrue(c.contains("bindStack(fresh, p)") && c.contains("SinSkillRules.nextForgeAt(now)"), "a forged stack is bound and starts the cooldown");
        assertTrue(c.contains("p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)"), "a stored stack is removed from the hand");
        assertFalse(c.contains("forgetSkill") || c.contains(".discard()") || c.contains("shrink("), "no skill is taken away and no other player's item is removed");
    }
}
