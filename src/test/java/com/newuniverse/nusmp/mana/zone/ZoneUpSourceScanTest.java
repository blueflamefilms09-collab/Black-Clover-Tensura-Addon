package com.newuniverse.nusmp.mana.zone;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins of the Mana Zone upgrade that can only be pinned by reading the source (nothing runs in a unit test): the packet is validated on the server before it is used, the common
 * code never touches a client class, no key is registered, nothing scans the world, the per-player state is cleaned up, and the shared files only gained hooks.
 */
class ZoneUpSourceScanTest {
    private static String src(String rel) throws Exception { return Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/" + rel)); }

    /** The code of a source without its comments and string contents. */
    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static final List<String> COMMON = List.of("mana/zone/ZoneCast.java", "mana/zone/ZoneCastNet.java", "mana/zone/ZoneEvasion.java", "mana/zone/ZoneUpMath.java", "mana/zone/ZoneUpRules.java",
            "mana/zone/ZoneUpSelfTests.java");

    @Test
    void theCommonCodeOfTheUpgradeTouchesNoClientClass() throws Exception {
        for (String f : COMMON) {
            String c = code(src(f));
            assertFalse(c.contains("net.minecraft.client"), f);
            assertFalse(c.contains("com.mojang.blaze3d"), f);
            assertFalse(c.contains("Minecraft.getInstance"), f);
            assertFalse(c.contains("nusmp.client."), f + " (the allow-list of ServerSafetyTest must not grow for the upgrade)");
        }
    }

    @Test
    void theClientHalfOnlyLooksAndAsksAndRegistersNoKey() throws Exception {
        String c = code(src("client/manazone/ZoneCastClient.java"));
        assertFalse(c.contains("new KeyMapping("), "no keybind");
        assertFalse(c.contains("RegisterKeyMappingsEvent"), "no keybind");
        assertTrue(c.contains("PacketDistributor.sendToServer(new ZoneCastNet.AimPayload("));
        assertTrue(c.contains("ACTIVATE_SLOT_1") && c.contains("isDown()"), "it reads the existing skill slot keys");
        assertTrue(c.contains("ManaZoneClient.ownRadius("), "the ray is as long as the zone");
        assertTrue(c.contains("getMainCamera()") && c.contains("getLookVector()"), "the ray starts at the camera");
        assertTrue(c.indexOf("ownRadius(") < c.indexOf("keyDown()", c.indexOf("void onClientTick(")), "the idle path returns before any key or ray work");
        assertFalse(Pattern.compile("ResourceLocation\\.(fromNamespaceAndPath|parse)\\(").matcher(c).find(), "no location built in a tick handler");
    }

    @Test
    void thePacketIsValidatedOnTheServerBeforeAnythingIsKept() throws Exception {
        String net = code(src("mana/zone/ZoneCastNet.java"));
        assertTrue(net.contains("playToServer(AimPayload.TYPE"), "client to server");
        assertTrue(net.contains("ctx.enqueueWork(") && net.contains("ctx.player() instanceof ServerPlayer sp"), "handled on the server thread with the real sender");
        assertFalse(net.contains("p.player") || net.contains("payload.player"), "the packet does not name its sender");
        String cast = code(src("mana/zone/ZoneCast.java"));
        int on = cast.indexOf("static void onAim(ServerPlayer p, double x, double y, double z, int entityId, long now)");
        assertTrue(on > 0);
        String body = cast.substring(on, cast.indexOf("// ----", on) > 0 ? cast.indexOf("// ----", on) : cast.length());
        int check = body.indexOf("ZoneUpMath.checkAim("), keep = body.indexOf("PENDING.computeIfAbsent(");
        assertTrue(check > 0 && keep > check, "checkAim runs before the aim is kept");
        assertTrue(body.contains("ManaZoneManager.zoneOf(p)"), "the zone is the server's own record");
        assertTrue(body.contains("entityIdOk(entityId)"), "the entity id is range-checked");
        assertTrue(body.contains("verdict != ZoneUpMath.Aim.OK) return"), "a failed check keeps nothing");
        assertTrue(body.contains("last[0] = now"), "the rate stamp is per sender (UUID keyed map)");
        assertTrue(cast.contains("Map<UUID, Pending> PENDING") && cast.contains("Map<UUID, long[]> LAST"), "state is keyed by UUID, never a static current player");
    }

    @Test
    void theCastIsGatedAndAlwaysRestored() throws Exception {
        String cast = code(src("mana/zone/ZoneCast.java"));
        int begin = cast.indexOf("private static Ctx begin(");
        String b = cast.substring(begin, cast.indexOf("private static Vec3 findOrigin") > 0 ? cast.indexOf("private static Vec3 findOrigin") : cast.length());
        for (String must : new String[]{"PENDING.remove(", "pageEligible(pageId)", "ZoneUpMath.fresh(", "ZoneUpMath.rateOk(", "zone.dim != p.level().dimension()", "ManaZoneMath.inside("})
            assertTrue(b.contains(must), "begin lacks " + must);
        int around = cast.indexOf("static boolean aroundAt(");
        String a = cast.substring(around, cast.indexOf("private static Ctx begin(", around));
        assertTrue(a.contains("try {") && a.contains("finally {") && a.contains("end(c)"), "the pose is put back in a finally");
        assertTrue(a.contains("if (!PENDING.isEmpty())"), "the idle path is one map check");
        assertTrue(a.contains("catch (RuntimeException e)"), "a fault in the upgrade must not lock the player's magic (SafeCast would)");
        String restore = cast.substring(cast.indexOf("private static void restoreQuietly("), cast.indexOf("public static Vec3 aimOverride"));
        for (String must : new String[]{"setPos(c.savedPos)", "setYRot(c.savedYRot)", "setXRot(c.savedXRot)", "setYHeadRot(c.savedHead)", "setDeltaMovement(c.savedDelta)"})
            assertTrue(restore.contains(must), "restore lacks " + must);
    }

    @Test
    void nothingScansTheWorldAndTheCostsAreBounded() throws Exception {
        String ev = code(src("mana/zone/ZoneEvasion.java")), cast = code(src("mana/zone/ZoneCast.java"));
        assertFalse(ev.contains("getEntitiesOfClass") || ev.contains("getEntities(") || ev.contains("getEntitiesOfClass"), "the evasion works on the zone scan's own list");
        assertFalse(cast.contains("getEntitiesOfClass") || cast.contains("getEntities("), "the zone-cast does one id lookup, no query");
        assertTrue(ev.contains("DODGES.tryTake(") && ev.contains("new PerfBudget("), "a global per-tick budget for dodges");
        assertTrue(ev.contains("RING = 4"), "four decided projectiles per owner");
        assertTrue(ev.contains("if (ManaZoneManager.activeCount() == 0) return"), "the impact hook returns at once while no zone is open");
        assertTrue(cast.contains("sl.clip(") && cast.split("sl\\.clip\\(", -1).length == 2, "exactly one ray in the origin search loop");
        String mgr = code(src("mana/zone/ManaZoneManager.java"));
        assertTrue(mgr.contains("looks < lookCap") && mgr.contains("ZoneEvasion.look(p, z, pr, now)"), "the scan hands the evasion a capped number of projectiles");
        assertFalse(mgr.contains("getEntitiesOfClass(Projectile.class") && mgr.split("getEntitiesOfClass\\(Projectile\\.class", -1).length > 2, "still one projectile query per scan");
    }

    @Test
    void thePerOwnerStateIsDroppedWithTheZone() throws Exception {
        String mgr = code(src("mana/zone/ManaZoneManager.java"));
        assertTrue(mgr.contains("ZoneCast.clear(p.getUUID())") && mgr.contains("ZoneEvasion.clear(p.getUUID())"), "end() clears both (death, logout, dimension, expiry, collapse all end the zone)");
        assertTrue(mgr.contains("ZoneCast.clear(z.owner)") && mgr.contains("ZoneEvasion.clear(z.owner)"), "an owner who vanished is cleared too");
        assertTrue(mgr.contains("ZoneCast.clearAll()") && mgr.contains("ZoneEvasion.clearAll()"), "server stop");
        assertTrue(mgr.contains("buffsOff(p)"), "the buffs leave with the zone");
    }

    @Test
    void theSharedFilesOnlyGainedHooksAndTheSwitchesAreServerConfigs() throws Exception {
        String book = src("book/GrimoireBook.java");
        assertTrue(book.contains("ZoneCast.around(player, mode, p.id()"), "castPage wraps the handler");
        assertTrue(book.contains("ZoneCast.targetOverride(p)") && book.contains("ZoneCast.aimOverride(p)"));
        assertEquals(2, book.split("ZoneCast.fromOverride\\(", -1).length - 1, "along and vfx");
        assertTrue(book.contains("p.pick(range, 1f, false).getLocation()"), "the ordinary aim is still there");
        String boot = src("mana/zone/ManaZoneBoot.java");
        assertTrue(boot.contains("ZoneCastNet::registerPayloads") && boot.contains("ZoneEvasion::onImpact") && boot.contains("ZoneUpSelfTests.register()"));
        assertTrue(boot.contains("ZoneCastClient.init()") && boot.contains("Dist.CLIENT"));
        String cfg = src("mana/zone/ManaZoneConfig.java");
        assertTrue(cfg.contains("b.push(\"upgrade\")") && boot.contains("ModConfig.Type.SERVER, ManaZoneConfig.SPEC"), "the settings are in the SERVER config");
        for (String key : new String[]{"\"evasion\"", "\"buffs\"", "\"zoneCast\"", "\"zoneCastRelocate\"", "\"zoneCastOrigin\""}) assertTrue(cfg.contains(key), key);
        assertEquals(1, boot.split("container.registerConfig\\(", -1).length - 1, "no second config file");
    }
}
