package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.entity.ElementalSpiritLordEntity;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing about the looks of the Great Spirits can be seen by its author, so the real game proves what can be proved without eyes (the CI server smoke test runs it; key prefix
 * {@code magic}). {@code magic.spirit_looks}: (1) every model, texture, glow map and clip file the new spirit looks name exists in the mod (the tiny Sylph, the Undine mermaid, Salamander's blaze,
 * and every piece of every Spirit Dive at every stage); (2) the spirits have their canon size: Sylph a tiny fairy, Undine a giant, Salamander massive, the earth giant unchanged; (3) with a
 * stand-in player: a first Wind dive runs the HALF look (Aura.SPIRIT_DIVE with the half style), the same mage with three dives behind them the FULL look, ending the dive keeps the look for the
 * shatter, the Earth dive starts its own layer and not the Gnome Plate. Registered from {@code NUSMP}.
 */
public final class SpiritLooksSelfTests {
    private SpiritLooksSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_looks", SpiritLooksSelfTests::looks);
    }

    private static boolean has(String path) {
        return SpiritLooksSelfTests.class.getClassLoader().getResource("assets/nusmp/" + path) != null;
    }

    private static Result looks(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        // ---- 1. the files
        for (String model : new String[]{"spirit_sylph_fairy", "spirit_sylph_fairy_wings", "spirit_undine_mermaid", "spirit_undine_mermaid_water", "spirit_salamander_blaze"}) {
            for (String rel : new String[]{"geo/entity/" + model + ".geo.json", "animations/entity/" + model + ".animation.json", "textures/entity/" + model + ".png", "textures/entity/" + model + "_glow.png"})
                if (!has(rel)) bad.add("missing " + rel);
        }
        int[] pieces = {0};
        SpiritDivePieces.forEachId(id -> {
            pieces[0]++;
            String stem = SpiritDivePieces.key(id) + "_arm_" + SpiritDivePieces.name(id);
            for (String rel : new String[]{"geo/entity/" + stem + ".geo.json", "textures/entity/" + stem + ".png", "textures/entity/" + stem + "_glow.png"})
                if (!has(rel)) bad.add("missing " + rel);
        });
        for (String el : new String[]{"wind", "water", "fire", "earth"})
            for (String part : new String[]{"shell", "haze"})
                if (!has("textures/aura/spirit_" + el + "_" + part + ".png")) bad.add("missing the " + part + " of the " + el + " dive");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        // ---- 2. the sizes
        ServerLevel level = server.overworld();
        float sylph = height(level, ElementalSpiritLordEntity.Kind.SYLPH), undine = height(level, ElementalSpiritLordEntity.Kind.UNDINE),
                salamander = height(level, ElementalSpiritLordEntity.Kind.SALAMANDER), gnome = height(level, ElementalSpiritLordEntity.Kind.GNOME);
        if (sylph > 0.6f) bad.add("Sylph is not a tiny fairy (" + sylph + " blocks)");
        if (undine < 3.0f) bad.add("Undine is not a giant (" + undine + " blocks)");
        if (salamander < 2.2f) bad.add("Salamander is not massive (" + salamander + " blocks)");
        if (gnome < 2.5f) bad.add("the earth giant shrank (" + gnome + " blocks)");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad));

        // ---- 3. the dive with a stand-in player
        ServerPlayer fp = SelfTests.player(level, "spirit_looks", 0.5, 100, 0.5);
        GrimoireBook wind = NUSkills.bookFor(MagicType.WIND), earth = NUSkills.bookFor(MagicType.EARTH);
        if (wind == null || earth == null) return Result.ok(pieces[0] + " pieces and the spirit models are in place, the sizes are canon; the dive checks were skipped (no Wind or Earth book)");
        ArmourState.takeOff(fp);
        try {
            ManasSkillInstance wi = wind.createDefaultInstance();
            SpiritDiveForm.begin(fp, wind, wi);
            if (!ArmourState.inForm(fp, MagicType.WIND)) bad.add("the Wind dive did not start the form");
            if (!PlayerAuras.active(fp, Aura.SPIRIT_DIVE)) bad.add("the Wind dive started no look");
            int style = PlayerAuras.style(fp, Aura.SPIRIT_DIVE);
            if (style != SpiritDivePieces.style(SpiritDivePieces.Element.WIND, SpiritDivePieces.Stage.HALF)) bad.add("a first Wind dive is not the half look (style " + style + ")");
            if (SpiritDiveForm.stageNow(fp, wi) != SpiritDivePieces.Stage.HALF) bad.add("a first Wind dive is not a half dive");
            SpiritDiveForm.end(fp, wind);
            if (ArmourState.held(fp) != null) bad.add("the Wind dive did not end");
            if (!PlayerAuras.active(fp, Aura.SPIRIT_DIVE)) bad.add("the look vanished at once instead of shattering out");
            if (PlayerAuras.style(fp, Aura.SPIRIT_DIVE) != style) bad.add("the shatter changed the style");
            wi.getOrCreateTag().putInt(SpiritDiveForm.DIVES, ArmourRules.HALF_DIVES);                   // three dives behind: the next one is full
            SpiritDiveForm.begin(fp, wind, wi);
            int full = PlayerAuras.style(fp, Aura.SPIRIT_DIVE);
            if (full != SpiritDivePieces.style(SpiritDivePieces.Element.WIND, SpiritDivePieces.Stage.FULL)) bad.add("a Wind dive after three dives is not the full look (style " + full + ")");
            ArmourState.takeOff(fp);

            ManasSkillInstance ei = earth.createDefaultInstance();
            SpiritDiveForm.begin(fp, earth, ei);
            ArmourRules.Held h = ArmourState.held(fp);
            if (h == null || !h.form()) bad.add("the Earth dive did not start the form");
            else if (!h.aura().isEmpty()) bad.add("the Earth dive still puts the Gnome Plate layer on (" + h.aura() + ")");
            if (PlayerAuras.active(fp, Aura.WIKI)) bad.add("the Earth dive started the generic armour layer");
            if (SpiritDivePieces.element(PlayerAuras.style(fp, Aura.SPIRIT_DIVE)) != SpiritDivePieces.Element.EARTH) bad.add("the Earth dive shows another element");
        } catch (RuntimeException ex) {
            bad.add("the dive threw " + ex);
        } finally {
            ArmourState.end(fp, ArmourRules.Drop.DEATH);
            try { PlayerAuras.clear(fp, Aura.SPIRIT_DIVE); } catch (NullPointerException noConnection) { /* a fake player */ }
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(pieces[0] + " pieces and the spirit models are in place; Sylph " + sylph + ", Undine " + undine + ", Salamander " + salamander + ", the earth giant " + gnome
                + " blocks; the stand-in's dives showed the half look, the full look, the shatter and the earth look");
    }

    private static float height(ServerLevel level, ElementalSpiritLordEntity.Kind kind) {
        ElementalSpiritLordEntity e = NUEntities.ELEMENTAL_SPIRIT_LORD.get().create(level);
        if (e == null) return 0f;
        e.setKind(kind);
        float h = e.getBbHeight();
        e.discard();
        return h;
    }
}
