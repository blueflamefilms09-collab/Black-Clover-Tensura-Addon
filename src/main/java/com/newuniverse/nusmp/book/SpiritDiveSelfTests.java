package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the looks of the remade Spirit Dive can not be seen by their author, so the real game proves what can be proved without eyes (the CI server smoke test runs it).
 * <ul>
 *   <li>{@code magic.spirit_dive_looks}: every file the remade look names exists (the new pieces of every tier, the sprites, the vein layers, the fire skin, the particle themes); with a stand-in
 *       player the tier of the dive follows the bound grimoire: a mage without a canon grimoire dives the standard tier, Yuno's grimoire on Wind the Yuno tier, Fuegoleon's on Fire the Fuegoleon tier,
 *       Noelle's on Water the Noelle tier, and a canon grimoire of another character never earns another's look; the style number round-trips through the lease.</li>
 *   <li>{@code magic.fuegoleon_arm}: Fuegoleon's flaming arm is the server's decision: no arm without the contract, the arm with the Salamander contract (lease sent), never for another canon grimoire or
 *       without a grimoire, gone again when the contract is gone, on for the Fire dive; one renewal per player every {@link FuegoleonArmRules#RENEW_TICKS} ticks.</li>
 * </ul>
 * Registered from {@link SpiritDiveBoot}.
 */
public final class SpiritDiveSelfTests {
    private SpiritDiveSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_dive_looks", SpiritDiveSelfTests::looks);
        SelfTests.register("magic.fuegoleon_arm", SpiritDiveSelfTests::arm);
        SelfTests.register("magic.spirit_dive_djinn", SpiritDiveSelfTests::djinn);
    }

    private static boolean has(String path) {
        return SpiritDiveSelfTests.class.getClassLoader().getResource(path) != null;
    }

    private static boolean asset(String path) { return has("assets/nusmp/" + path); }

    // ------------------------------------------------------------------ magic.spirit_dive_looks
    private static Result looks(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        int[] n = {0};
        SpiritDivePieces.forEachId(id -> {
            n[0]++;
            String stem = SpiritDivePieces.key(id) + "_arm_" + SpiritDivePieces.name(id);
            for (String rel : new String[]{"geo/entity/" + stem + ".geo.json", "textures/entity/" + stem + ".png", "textures/entity/" + stem + "_glow.png"})
                if (!asset(rel)) bad.add("missing " + rel);
        });
        for (String s : new String[]{"sdv_column", "sdv_pillar", "sdv_shock", "sdv_runering", "sdv_wisp", "sdv_flame_sheet", "sdv_sigil_wind", "sdv_sigil_fire", "sdv_sigil_water", "sdv_sigil_earth",
                "spirit2_wind_veins", "spirit2_water_veins", "spirit2_fire_veins", "spirit2_earth_veins", "spirit2_fire_shell", "spirit2_fire_shell_glow"})
            if (!asset("textures/aura/" + s + ".png")) bad.add("missing aura/" + s);
        if (!asset("animations/entity/spiritdive_arm_flame_arm_r.animation.json")) bad.add("the flame arm has no animation");
        String themes = "data/nusmp/particle_themes.json";
        if (!has(themes)) bad.add("no particle themes file");
        else {
            try (var in = SpiritDiveSelfTests.class.getClassLoader().getResourceAsStream(themes)) {
                String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                for (String t : new String[]{"wind", "fire", "water", "earth", "yuno", "fuegoleon", "noelle"})
                    if (!text.contains("\"spirit_dive." + t + "\"")) bad.add("no particle theme spirit_dive." + t);
            } catch (java.io.IOException | RuntimeException ex) {
                bad.add("the particle themes could not be read: " + ex);
            }
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        ServerLevel level = server.overworld();
        ServerPlayer fp = SelfTests.player(level, "spirit_dive_looks", 0.5, 100, 0.5);
        GrimoireBook wind = NUSkills.bookFor(MagicType.WIND), fire = NUSkills.bookFor(MagicType.FLAME), water = NUSkills.bookFor(MagicType.WATER);
        if (wind == null || fire == null || water == null) return Result.ok(n[0] + " pieces and the sprites are in place; the tier checks were skipped (no Wind, Fire or Water book)");
        ItemStack kept = GrimoireSlot.get(fp);
        try {
            check(fp, wind, SpiritDivePieces.Element.WIND, null, SpiritDiveTier.Grade.STANDARD, bad, "a mage without a canon grimoire");
            check(fp, wind, SpiritDivePieces.Element.WIND, CanonBook.YUNO, SpiritDiveTier.Grade.YUNO, bad, "Yuno's grimoire on Wind");
            check(fp, wind, SpiritDivePieces.Element.WIND, CanonBook.FUEGOLEON, SpiritDiveTier.Grade.STANDARD, bad, "Fuegoleon's grimoire on Wind");
            check(fp, fire, SpiritDivePieces.Element.FIRE, CanonBook.FUEGOLEON, SpiritDiveTier.Grade.FUEGOLEON, bad, "Fuegoleon's grimoire on Fire");
            check(fp, fire, SpiritDivePieces.Element.FIRE, CanonBook.YUNO, SpiritDiveTier.Grade.STANDARD, bad, "Yuno's grimoire on Fire");
            check(fp, water, SpiritDivePieces.Element.WATER, CanonBook.NOELLE, SpiritDiveTier.Grade.NOELLE, bad, "Noelle's grimoire on Water");
        } catch (RuntimeException ex) {
            bad.add("the dive threw " + ex);
        } finally {
            GrimoireSlot.set(fp, kept);
            ArmourState.end(fp, ArmourRules.Drop.DEATH);
            try { PlayerAuras.clear(fp, Aura.SPIRIT_DIVE); } catch (NullPointerException noConnection) { /* a fake player */ }
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(n[0] + " pieces and the sprites are in place; the tier follows the bound grimoire (standard, Yuno, Fuegoleon, Noelle, and no borrowed looks)");
    }

    /** One dive of a stand-in player holding the given canon grimoire (null = none): the style the lease carries must be the expected tier. */
    private static void check(ServerPlayer fp, GrimoireBook book, SpiritDivePieces.Element el, CanonBook canon, SpiritDiveTier.Grade want, List<String> bad, String who) {
        GrimoireSlot.set(fp, canon == null ? ItemStack.EMPTY : GrimoireItem.createCanon(canon));
        ArmourState.takeOff(fp);
        ManasSkillInstance inst = book.createDefaultInstance();
        SpiritDiveForm.begin(fp, book, inst);
        if (!PlayerAuras.active(fp, Aura.SPIRIT_DIVE)) { bad.add(who + ": the dive started no look"); return; }
        int style = PlayerAuras.style(fp, Aura.SPIRIT_DIVE);
        if (SpiritDivePieces.element(style) != el) bad.add(who + ": the element is " + SpiritDivePieces.element(style));
        SpiritDiveTier.Grade got = SpiritDivePieces.grade(style);
        if (got != want) bad.add(who + ": the tier is " + got + ", expected " + want);
        if (!SpiritDivePieces.valid(style) || style >= 64) bad.add(who + ": the style " + style + " is not valid");
        if (SpiritDivePieces.pieces(el, SpiritDivePieces.stage(style), got).isEmpty()) bad.add(who + ": no pieces");
        SpiritDiveForm.end(fp, book);
        ArmourState.takeOff(fp);
    }

    // ------------------------------------------------------------------ magic.spirit_dive_djinn
    /**
     * The Djinn-style dive: every piece of every set the lists can name has its model, texture and glow map (and its clip when it moves); the lists of every element, stage and tier are non-empty, in the
     * Djinn's forge order, left-only for the first Wind dive, without a right-arm plate for Fuegoleon; the pieces' draw budget is under the Djinn Full Equip's; the forge cues are known sounds; and with a
     * stand-in player the dive's lease still carries the tier of the bound grimoire (the Djinn look changes what is drawn, not what is sent).
     */
    private static Result djinn(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        int pieces = 0;
        for (SpiritDivePieces.Element e : SpiritDivePieces.Element.values())
            for (boolean ch : new boolean[]{false, true}) {
                if (ch && SpiritDiveTier.gradeOfElement(e) == SpiritDiveTier.Grade.STANDARD) continue;                    // Earth has no character set
                java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>(SpiritDiveDjinnPieces.fullNames(e, ch));
                names.addAll(SpiritDiveDjinnPieces.halfNames(e, ch));
                for (String n : names) {
                    pieces++;
                    SpiritDiveDjinnPieces.Def def = SpiritDiveDjinnPieces.def(n, ch);
                    if (def == null) { bad.add("no definition for " + n); continue; }
                    String stem = SpiritDiveDjinnPieces.modelId(e, ch, n);
                    for (String rel : new String[]{"geo/entity/" + stem + ".geo.json", "textures/entity/" + stem + ".png", "textures/entity/" + stem + "_glow.png"})
                        if (!asset(rel)) bad.add("missing " + rel);
                    if (!def.clip().isEmpty() && !asset("animations/entity/" + stem + ".animation.json")) bad.add(stem + " has no animation for its clip");
                    if (def.delay() < 0f || def.delay() > 12f) bad.add(n + " delay " + def.delay());
                }
                for (SpiritDivePieces.Stage st : SpiritDivePieces.Stage.values())
                    for (SpiritDiveTier.Grade g : SpiritDiveTier.Grade.values())
                        if (SpiritDiveDjinnPieces.pieces(e, st, g, ch).isEmpty()) bad.add("an empty list for " + e + "/" + st + "/" + g);
            }
        for (SpiritDivePieces.Element e : SpiritDivePieces.Element.values())
            for (boolean ch : new boolean[]{false, true}) {
                if (ch && SpiritDiveTier.gradeOfElement(e) == SpiritDiveTier.Grade.STANDARD) continue;
                int v = 0;
                for (SpiritDiveDjinnPieces.Def d : SpiritDiveDjinnPieces.pieces(e, SpiritDivePieces.Stage.FULL, SpiritDiveTier.Grade.STANDARD, ch)) v += faces(SpiritDiveDjinnPieces.modelId(e, ch, d.name())) * 4 * d.passes();
                if (v > SpiritDiveDjinnPieces.pieceVertexCap(ch)) bad.add(e + (ch ? " character" : " standard") + " set: " + v + " vertices > " + SpiritDiveDjinnPieces.pieceVertexCap(ch));
            }
        if (SpiritDiveDjinnPieces.names(SpiritDivePieces.Element.FIRE, SpiritDivePieces.Stage.FULL, SpiritDiveTier.Grade.FUEGOLEON, true).contains("gauntlet_r")) bad.add("Fuegoleon's dive has a right-arm plate");
        if (SpiritDiveDjinnPieces.names(SpiritDivePieces.Element.WIND, SpiritDivePieces.Stage.HALF, SpiritDiveTier.Grade.YUNO, true).stream().anyMatch(n -> n.endsWith("_r") || n.equals("crest_c")))
            bad.add("the first Wind dive is not left-only");
        for (SpiritDiveForge.Kind k : SpiritDiveForge.Kind.values())
            if (SpiritDiveForge.cue(k, SpiritDivePieces.Stage.FULL, true).length > SpiritDiveForge.MAX_SOUNDS) bad.add("a forge cue with too many sounds");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        ServerLevel level = server.overworld();
        ServerPlayer fp = SelfTests.player(level, "spirit_dive_djinn", 0.5, 100, 0.5);
        GrimoireBook wind = NUSkills.bookFor(MagicType.WIND);
        if (wind == null) return Result.ok(pieces + " pieces are in place and the lists hold; the lease check was skipped (no Wind book)");
        ItemStack kept = GrimoireSlot.get(fp);
        try {
            check(fp, wind, SpiritDivePieces.Element.WIND, CanonBook.YUNO, SpiritDiveTier.Grade.YUNO, bad, "the Djinn look: Yuno's grimoire on Wind");
            int style = PlayerAuras.active(fp, Aura.SPIRIT_DIVE) ? PlayerAuras.style(fp, Aura.SPIRIT_DIVE) : 0;
            if (style != 0 && !SpiritDivePieces.valid(style)) bad.add("the style " + style + " is not valid");
        } catch (RuntimeException ex) {
            bad.add("the dive threw " + ex);
        } finally {
            GrimoireSlot.set(fp, kept);
            ArmourState.end(fp, ArmourRules.Drop.DEATH);
            try { PlayerAuras.clear(fp, Aura.SPIRIT_DIVE); } catch (NullPointerException noConnection) { /* a fake player */ }
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(pieces + " pieces (models, textures, glow maps, clips) are in place; the lists, the forge cues and the draw budget hold; the lease still carries the tier");
    }

    /** The faces (quads) a model file draws in one pass: the entries of every cube's uv object (0 when the file cannot be read: the missing-file check reports that). */
    private static int faces(String stem) {
        try (var in = SpiritDiveSelfTests.class.getClassLoader().getResourceAsStream("assets/nusmp/geo/entity/" + stem + ".geo.json")) {
            if (in == null) return 0;
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            int n = 0;
            for (var geo : root.getAsJsonArray("minecraft:geometry"))
                for (var bone : geo.getAsJsonObject().getAsJsonArray("bones"))
                    if (bone.getAsJsonObject().has("cubes"))
                        for (var cube : bone.getAsJsonObject().getAsJsonArray("cubes")) n += cube.getAsJsonObject().getAsJsonObject("uv").size();
            return n;
        } catch (java.io.IOException | RuntimeException ex) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ magic.fuegoleon_arm
    private static Result arm(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        ServerLevel level = server.overworld();
        ServerPlayer fp = SelfTests.player(level, "fuegoleon_arm", 0.5, 100, 0.5);
        ItemStack kept = GrimoireSlot.get(fp);
        CompoundTag data = ForbiddenMagic.data(fp);
        String keptKind = data.getString("nusmp_spirit_kind");
        boolean hadKind = data.contains("nusmp_spirit_kind");
        try {
            GrimoireSlot.set(fp, GrimoireItem.createCanon(CanonBook.FUEGOLEON));
            setKind(fp, "");
            if (FuegoleonArm.reasonFor(fp) != FuegoleonArmRules.Reason.NONE) bad.add("the arm is on without the Salamander contract");
            if (FuegoleonArm.update(fp) || PlayerAuras.active(fp, Aura.FUEGOLEON_ARM)) bad.add("a lease was sent without the contract");
            setKind(fp, "Salamander");
            if (FuegoleonArm.reasonFor(fp) != FuegoleonArmRules.Reason.CONTRACT) bad.add("Fuegoleon with the contract has no arm (" + FuegoleonArm.reasonFor(fp) + ")");
            if (!FuegoleonArm.update(fp) || !PlayerAuras.active(fp, Aura.FUEGOLEON_ARM)) bad.add("no lease for the arm");
            else if (PlayerAuras.style(fp, Aura.FUEGOLEON_ARM) != FuegoleonArmRules.STYLE) bad.add("the arm at rest has the wrong style " + PlayerAuras.style(fp, Aura.FUEGOLEON_ARM));
            setKind(fp, "Undine");
            if (FuegoleonArm.update(fp) || PlayerAuras.active(fp, Aura.FUEGOLEON_ARM)) bad.add("the arm stayed after the contract changed to another spirit");
            setKind(fp, "");
            GrimoireSlot.set(fp, GrimoireItem.createCanon(CanonBook.YUNO));
            setKind(fp, "Salamander");
            if (FuegoleonArm.reasonFor(fp) != FuegoleonArmRules.Reason.NONE || FuegoleonArm.update(fp)) bad.add("a grimoire of another character earned Fuegoleon's arm");
            GrimoireSlot.set(fp, ItemStack.EMPTY);
            if (FuegoleonArm.reasonFor(fp) != FuegoleonArmRules.Reason.NONE) bad.add("a mage without a grimoire earned Fuegoleon's arm");
            GrimoireSlot.set(fp, GrimoireItem.createCanon(CanonBook.FUEGOLEON));
            setKind(fp, "");
            try { PlayerAuras.set(fp, Aura.SPIRIT_DIVE, 100, SpiritDivePieces.style(SpiritDivePieces.Element.FIRE, SpiritDivePieces.Stage.FULL)); } catch (NullPointerException noConnection) { /* a fake player */ }
            if (FuegoleonArm.reasonFor(fp) != FuegoleonArmRules.Reason.DIVE) bad.add("the Fire dive does not switch the arm on (" + FuegoleonArm.reasonFor(fp) + ")");
            else if (FuegoleonArm.update(fp) && FuegoleonArmRules.styleOf(FuegoleonArm.reasonFor(fp)) != PlayerAuras.style(fp, Aura.FUEGOLEON_ARM)) bad.add("the dive's arm has the wrong style");
            try { PlayerAuras.clear(fp, Aura.SPIRIT_DIVE); } catch (NullPointerException noConnection) { /* a fake player */ }
            FuegoleonArm.update(fp);
            if (PlayerAuras.active(fp, Aura.FUEGOLEON_ARM)) bad.add("the arm stayed after the dive and without the contract");
            if (FuegoleonArmRules.packetsPerSecond() > 1.0) bad.add("the arm costs more than one packet a second");
        } catch (RuntimeException ex) {
            bad.add("the check threw " + ex);
        } finally {
            GrimoireSlot.set(fp, kept);
            setKind(fp, hadKind ? keptKind : "");
            try { PlayerAuras.clear(fp, Aura.FUEGOLEON_ARM); PlayerAuras.clear(fp, Aura.SPIRIT_DIVE); } catch (NullPointerException noConnection) { /* a fake player */ }
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok("the arm follows the Salamander contract and Fuegoleon's grimoire (on with the contract and the dive, off without, never for another grimoire), one renewal per "
                + FuegoleonArmRules.RENEW_TICKS + " ticks");
    }

    /** Writes the spirit bond kind the way {@link SpiritBond#bond} does (empty removes it). */
    private static void setKind(ServerPlayer p, String kind) {
        CompoundTag d = ForbiddenMagic.data(p);
        if (kind.isEmpty()) d.remove("nusmp_spirit_kind"); else d.putString("nusmp_spirit_kind", kind);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, d);
    }
}
