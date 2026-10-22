package com.newuniverse.nusmp.spiritcontrol;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the rules of the owner's brief that no unit test of numbers can see are read from the real sources: NO spirit key registration is left (no KeyMapping of any spirit, no key packet, no client key
 * class), the control plane is server only and bounded (one entity search in the whole package, no chunk access, no block reads, the tick handler is guarded by an empty-map check, state is per UUID), every
 * spirit package registers its handle in its Boot, the grimoire appends the six pages after everything and asks the spirit gate before the "instant page" rule, the Spirit Lord's free casts give way only
 * while combat mode is on, the config is a SERVER config with its own file and the documented defaults, and every spirit doc has the 3.0.0 pointer.
 */
class SpiritControlSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path CTL = SRC.resolve("spiritcontrol");

    private static String code(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    private static List<Path> javaIn(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    // ------------------------------------------------------------------ the keys are gone
    @Test
    void noSpiritKeyRegistrationIsLeftAnywhere() throws IOException {
        List<String> bad = new ArrayList<>();
        Pattern spiritKey = Pattern.compile("key\\.nusmp\\.(sylph|undine_link|gnome|fire_salamander|spirit_(light|dark|space)_\\w+)");
        for (Path p : javaIn(SRC)) {
            String c = code(p);
            if (spiritKey.matcher(c).find()) bad.add(SRC.relativize(p) + " registers or names a spirit key");
        }
        for (String gone : new String[]{"client/sylph/SylphClient", "client/undine/UndineClient", "client/gnome/GnomeClient", "client/firecast/FireCastClient", "client/spiritdark/SpiritDarkClient",
                "client/spiritspace/SpiritSpaceClient", "sylph/SylphNet", "undine/UndineNet", "gnome/GnomeNet", "firecast/FireCastNet", "spiritdark/SpiritDarkNet", "spiritspace/SpiritSpaceNet"})
            if (Files.exists(SRC.resolve(gone + ".java"))) bad.add(gone + " still exists");
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theSpiritLightClientIsOnlyTheVoiceHookAndItsNetOnlyCarriesTheVoice() throws IOException {
        String client = code(SRC.resolve("client/spiritlight/SpiritLightClient.java"));
        assertFalse(client.contains("KeyMapping") || client.contains("RegisterKeyMappingsEvent") || client.contains("consumeClick") || client.contains("sendToServer"), "no key, no packet to the server");
        assertTrue(client.contains("SpiritLightNet.CLIENT_VOICE = SpiritLightVoiceClient::play"), "the seraph's voice is still played");
        String net = code(SRC.resolve("spiritlight/SpiritLightNet.java"));
        assertFalse(net.contains("playToServer") || net.contains("record Key"), "no key packet");
    }

    @Test
    void theServerSideActionsOfTheOldKeysAreStillThere() throws IOException {
        assertTrue(code(SRC.resolve("sylph/SylphCasting.java")).contains("public static void toggle(ServerPlayer p)"));
        assertTrue(code(SRC.resolve("firecast/FireCasting.java")).contains("public static void toggleSpirit(ServerPlayer p)"));
        assertTrue(code(SRC.resolve("undine/UndineCasting.java")).contains("public static void toggle(ServerPlayer p)"));
        assertTrue(code(SRC.resolve("gnome/Gnome.java")).contains("public static void toggle(ServerPlayer p)"));
        assertTrue(code(SRC.resolve("spiritlight/SpiritLight.java")).contains("public static void onKey(ServerPlayer p, int action)"));
        assertTrue(code(SRC.resolve("spiritdark/SpiritDark.java")).contains("public static void onKey(ServerPlayer p, int action)"));
        assertTrue(code(SRC.resolve("spiritspace/SpiritSpace.java")).contains("public static void onKey(ServerPlayer p, int action, int ability, int slot)"));
    }

    // ------------------------------------------------------------------ the plane is server only and bounded
    @Test
    void theControlPlaneNamesNoClientClassAndNoGlobalCurrentPlayer() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : javaIn(CTL)) {
            String c = Files.readString(p, StandardCharsets.UTF_8);
            for (String b : new String[]{"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance", "net.neoforged.neoforge.client"}) if (c.contains(b)) bad.add(p.getFileName() + " uses " + b);
            String k = code(p);
            if (Pattern.compile("static\\s+(?!final)[\\w<>.]*(ServerPlayer|Player)\\s+\\w+\\s*[;=]").matcher(k).find()) bad.add(p.getFileName() + " keeps a static player");
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void thePackageDoesOneEntitySearchAndNeverTouchesChunksBlocksOrThreads() throws IOException {
        int searches = 0;
        for (Path p : javaIn(CTL)) {
            String c = code(p);
            Matcher m = Pattern.compile("getEntitiesOfClass\\(|getEntities\\(|getEntitiesOfClass ").matcher(c);
            while (m.find()) searches++;
            assertFalse(c.contains("getChunk") || c.contains("loadChunk") || c.contains("forceChunk") || c.contains("getBlockState") || c.contains("setBlock") || c.contains("destroyBlock"), p.getFileName() + " must not touch chunks or blocks");
            assertFalse(c.contains("new Thread") || c.contains("Executor") || c.contains("Timer"), p.getFileName() + " must not start threads");
        }
        assertEquals(1, searches, "exactly one entity search in the whole package: the combat decision's target search");
        String combat = code(CTL.resolve("SpiritCombat.java"));
        int find = combat.indexOf("private static LivingEntity findTarget(");
        int search = combat.indexOf("getEntitiesOfClass");
        int next = combat.indexOf("static boolean valid(");
        assertTrue(find > 0 && search > find && search < next, "the search is inside findTarget");
        assertTrue(combat.contains("now < f.nextScan") && combat.contains("SpiritControlConfig.targetCap()") && combat.contains("CombatRules.inFight"), "the search is behind its idle clock, capped, and only per fight");
        assertTrue(combat.contains("DECISIONS.take(now, SpiritControlConfig.decisionsPerTick())") && combat.contains("casts.tryAcquire(now)") && combat.contains("CombatRules.decisionTick("), "per tick budget, per second ceiling and the decision spread");
        assertTrue(combat.contains("CombatRules.clampDamage(") && combat.contains("SpiritControlConfig.maxDamage()"), "the damage is capped");
        assertTrue(combat.contains("Foes.isFoe(p, e)") && combat.contains("Foes.belongsToSide(p, e)"), "the existing foe helper decides who is a target");
        assertTrue(combat.contains("p.hasLineOfSight(target)"), "no cast without a line of sight, checked before anything is paid");
    }

    @Test
    void theTickHandlerCostsNothingWhileNobodyHasCombatModeOn() throws IOException {
        String ev = code(CTL.resolve("SpiritControlEvents.java"));
        assertTrue(ev.contains("SpiritCombat.ACTIVE.isEmpty() ||") && ev.contains("SpiritCombat.ACTIVE.containsKey(p.getUUID())"), "one empty check first, then one map lookup");
        for (String cleanup : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerRespawnEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"}) assertTrue(ev.contains(cleanup), cleanup);
        String combat = code(CTL.resolve("SpiritCombat.java"));
        assertTrue(combat.contains("Map<UUID, Fight> ACTIVE") && combat.contains("Map<UUID, Pools> POOLS"), "all state is per player UUID");
    }

    @Test
    void everyAskIsValidatedAndRateLimitedPerPlayer() throws IOException {
        String c = code(CTL.resolve("SpiritControl.java"));
        assertTrue(c.contains("SpiritControlConfig.enabled()") && c.contains("GAP.allow(p.getUUID()") && c.contains("h.eligible(p)"), "switch, rate limit per UUID, and the spirit's own rule");
        assertTrue(c.contains("abilityOf(h, abilityId)") && c.contains("arg >= MAX_SLOT"), "the ability must be one the spirit lists and the slot is range checked");
        String cmd = code(CTL.resolve("SpiritControlCommand.java"));
        assertTrue(cmd.contains("getPlayerOrException()") && !cmd.contains("hasPermission") && !cmd.contains("EntityArgument"), "the command only ever touches the caller's own spirit: no target argument, no permission needed");
        assertTrue(cmd.contains("IntegerArgumentType.integer(1, SpiritControl.MAX_SLOT)"), "the slot argument is bounded");
        assertTrue(code(SRC.resolve("multiverse/MultiverseCommands.java")).contains("SpiritControlCommand.addTo(spirit);"), "the verbs hang under /multiverse spirit");
    }

    // ------------------------------------------------------------------ the wiring
    @Test
    void everySpiritPackageRegistersItsHandleInItsBoot() throws IOException {
        String[][] want = {{"sylph/SylphBoot.java", "SylphSpirit"}, {"undine/UndineBoot.java", "UndineSpirit"}, {"gnome/GnomeBoot.java", "GnomeSpirit"}, {"firecast/FireCastBoot.java", "SalamanderSpirit"},
                {"spiritlight/SpiritLightBoot.java", "SpiritLightControl"}, {"spiritdark/SpiritDarkBoot.java", "SpiritDarkControl"}, {"spiritspace/SpiritSpaceBoot.java", "SpiritSpaceControl"}};
        for (String[] w : want)
            assertTrue(code(SRC.resolve(w[0])).contains("com.newuniverse.nusmp.spiritcontrol.SpiritControl.register(new " + w[1] + "());"), w[0]);
        assertTrue(code(CTL.resolve("SpiritControlBoot.java")).contains("SpiritControl.register(new LordSpirit());"));
        String main = code(SRC.resolve("NUSMP.java"));
        int dark = main.indexOf("SpiritDarkBoot.init(modEventBus, modContainer)"), ctl = main.indexOf("SpiritControlBoot.init(modEventBus, modContainer)");
        assertTrue(dark > 0 && ctl > dark, "the control plane boots after the last spirit package");
    }

    @Test
    void theConfigIsAServerConfigWithItsOwnFileAndTheDocumentedDefaults() throws IOException {
        String boot = code(CTL.resolve("SpiritControlBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER, SpiritControlConfig.SPEC, \"nusmp-spirit-control-server.toml\""));
        assertFalse(boot.contains("ModConfig.Type.CLIENT"));
        String cfg = code(CTL.resolve("SpiritControlConfig.java"));
        assertTrue(cfg.contains(".define(\"spiritControl\", true)"), "magic.spiritControl defaults to true");
        assertTrue(cfg.contains("defineInRange(\"userManaShare\", 0.5, 0.0, 1.0)"), "half and half");
        assertTrue(cfg.contains("defineInRange(\"decisionTicks\", 10, 5, 100)"), "one decision every ten ticks");
        assertTrue(cfg.contains("defineInRange(\"maxCastsPerSecondServer\", 24, 1, 200)") && cfg.contains("defineInRange(\"maxDecisionsPerTick\", 8, 1, 64)"), "the server ceilings have hard bounds");
        for (String spirit : new String[]{"salamander", "sylph", "undine", "gnome", "spiritLight", "spiritDark", "spiritSpace", "lord"}) assertTrue(cfg.contains("define(\"" + spirit + "\", true)"), "per spirit switch " + spirit);
        assertTrue(cfg.contains("b.push(\"combat\")"));
    }

    @Test
    void theGrimoireAppendsTheSixPagesLastAndAsksTheSpiritGateBeforeTheInstantRule() throws IOException {
        String book = Files.readString(SRC.resolve("book/GrimoireBook.java"), StandardCharsets.UTF_8);
        int sig = book.indexOf("SignatureSpell.pages()"), truth = book.indexOf("TrueMagic.page()"), spirit = book.indexOf("SpiritControlPages.pages()");
        assertTrue(sig > 0 && truth > sig && spirit > truth, "the spirit pages come after the Mana Zone's pages");
        int usable = book.indexOf("public boolean usable("), gate = book.indexOf("isZoneGated(mode)) return", usable), mine = book.indexOf("isSpiritPage(mode)) return", usable), instant = book.indexOf("if (instant(mode)) return true;", usable);
        assertTrue(usable > 0 && gate > usable && mine > gate && mine < instant, "the Mana Zone gate, then the spirit gate, then the instant-page rule");
        assertTrue(book.contains("isSpiritToggle(mode)") && book.contains("SpiritControlPages.press(p, this, instance, page(mode).id())"), "the toggles act on the press");
        assertTrue(book.contains("SpiritControlPages.cost(this, entity, p.id())") && book.contains("SpiritControlPages.label(instance, p)") && book.contains("SpiritControlPages.sync(this, i, p)"), "price, name and the once-a-second tag");
        assertTrue(book.contains("isSpiritPage(mode) ? com.newuniverse.nusmp.spiritcontrol.SpiritControlPages.label(instance, p) : SignatureSpell.pageName(instance, p)"), "every other page is named as before");
    }

    @Test
    void theSpiritLordsFreeCastGivesWayOnlyWhileCombatModeIsOn() throws IOException {
        for (String f : new String[]{"entity/SpiritLordEntity.java", "entity/ElementalSpiritLordEntity.java"}) {
            String c = code(SRC.resolve(f));
            int guard = c.indexOf("SpiritCombat.takesOver("), cast = c.indexOf("--castCooldown > 0");
            assertTrue(guard > 0 && cast > guard, f + ": the guard comes right before the cast");
        }
        String combat = code(CTL.resolve("SpiritCombat.java"));
        assertTrue(combat.contains("!ACTIVE.isEmpty() && owner instanceof ServerPlayer p && ACTIVE.containsKey(p.getUUID())"), "with combat mode off for the owner the Lord casts as it always did");
    }

    @Test
    void theSpiritLordSkillKeepsItsTwoModesAndGainsACombatModeAtTheEnd() throws IOException {
        String s = code(SRC.resolve("multiverse/SpiritLordSkill.java"));
        assertTrue(s.contains("CHANNEL = 0, CALL = 1, COMBAT = 2"));
        assertTrue(s.contains("getModes(ManasSkillInstance instance) { return 3; }"));
        assertTrue(s.contains("SpiritControl.toggleCombat(p)"));
    }

    // ------------------------------------------------------------------ text
    @Test
    void everyTranslatableKeyOfThePackageIsInTheLangFragment() throws IOException {
        JsonObject frag = JsonParser.parseString(Files.readString(Path.of("tools/lang/spirit_control.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> missing = new ArrayList<>();
        Pattern key = Pattern.compile("\"(nusmp\\.spirit_control\\.[a-z_]+)\"");
        for (Path p : javaIn(CTL)) {
            Matcher m = key.matcher(code(p));
            while (m.find()) if (!frag.has(m.group(1))) missing.add(m.group(1));
        }
        assertTrue(missing.isEmpty(), "keys missing from tools/lang/spirit_control.json: " + missing);
    }

    @Test
    void theDocAndEverySpiritDocPointToTheControlPlane() throws IOException {
        String doc = Files.readString(Path.of("docs/magic/spirit_control.md"), StandardCharsets.UTF_8);
        for (String h : new String[]{"NEW", "REPLACED", "KEPT", "worst case", "not verified"}) assertTrue(doc.toLowerCase().contains(h.toLowerCase()), "docs/magic/spirit_control.md lacks " + h);
        for (String d : new String[]{"fire_casting", "sylph", "undine", "gnome", "spirit_light", "spirit_dark", "spirit_space"})
            assertTrue(Files.readString(Path.of("docs/magic/" + d + ".md"), StandardCharsets.UTF_8).contains("Update 3.0.0"), "docs/magic/" + d + ".md lacks the 3.0.0 pointer");
        assertTrue(Files.readString(Path.of("docs/spirit_lords.md"), StandardCharsets.UTF_8).contains("Update 3.0.0"));
    }
}
