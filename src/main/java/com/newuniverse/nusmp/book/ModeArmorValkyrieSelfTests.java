package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Piece;
import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.valkyrielance.ValkyrieDrillLanceItem;
import com.newuniverse.nusmp.valkyrielance.ValkyrieLanceBoot;
import com.newuniverse.nusmp.valkyrielance.ValkyrieLanceRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Why: the Valkyrie armour v2 (the player-fitted layer of the Valkyrie Dress, its three looks and its drill lance, drawn by the client) and the Valkyrie Drill Lance item cannot be seen by their author in a game,
 * so the real game proves what can be proved without eyes (the CI server smoke test runs it; key prefix {@code magic.valkyrie_armor}): (1) every mesh file of every piece and level and the lance's three
 * levels is in the jar, parses and bakes into the quads of the piece table, with the vertex totals of the three looks under the pool; (2) the admission of a crowd of wearers, at every quality level's pool, never
 * passes the armour's own 1,200-quad pool and no far wearer carries a lance; (3) the atlas and the scale sheet are in the jar; (4) the options default to V2 / VALKYRIE / ATTACHED and parse; (5) the lance is registered
 * under its id as a spear-class sword with a trident's damage, is engraved once and its hit burst is capped and forgotten; (6) the lance spin never exceeds 720 degrees a second. Pure checks: nothing touches a client
 * class, so it runs on the dedicated server.
 */
public final class ModeArmorValkyrieSelfTests {
    private ModeArmorValkyrieSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.valkyrie_armor", ModeArmorValkyrieSelfTests::check);
    }

    private static Result check(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        // ---- the meshes
        int files = 0;
        for (Piece p : Piece.values()) {
            for (int l = 0; l < 3; l++) {
                if (!ModeArmorValkyrieMesh.exists(p.file(l))) { bad.add("missing mesh " + p.file(l)); continue; }
                files++;
                for (boolean right : new boolean[]{false, true}) {
                    ModeArmorValkyrieMesh m = ModeArmorValkyrieMesh.armour(p, l, right);
                    if (m.quads != p.quads(l)) bad.add(p.file(l) + " bakes " + m.quads + " quads, the table says " + p.quads(l));
                    for (float f : m.pos) if (!Float.isFinite(f)) { bad.add(p.file(l) + " has a non-finite vertex"); break; }
                }
            }
        }
        for (int l = 0; l < 3; l++) {
            if (!ModeArmorValkyrieMesh.exists(ModeArmorValkyrieRules.lanceFile(l))) { bad.add("missing mesh " + ModeArmorValkyrieRules.lanceFile(l)); continue; }
            files++;
            if (ModeArmorValkyrieMesh.lance(l).quads != ModeArmorValkyrieRules.LANCE_QUADS[l]) bad.add("the lance bakes " + ModeArmorValkyrieMesh.lance(l).quads + " quads at level " + l);
        }
        for (Form f : Form.values()) {
            for (int l = 0; l < 3; l++) {
                int q = ModeArmorValkyrieRules.armourQuads(f, l) + ModeArmorValkyrieRules.lanceQuads(l);
                if (q > ModeArmorValkyrieBudget.POOL_QUADS) bad.add(f + " level " + l + " with the lance is " + q + " quads, over the pool");
            }
        }
        // ---- a crowd of wearers under every pool
        int worst = 0;
        int n = 16;
        double[] d = new double[n];
        int[] form = new int[n], level = new int[n];
        boolean[] want = new boolean[n], lance = new boolean[n], done = new boolean[n];
        for (int pool : new int[]{ModeArmorValkyrieBudget.poolFor(40000), ModeArmorValkyrieBudget.poolFor(14000), ModeArmorValkyrieBudget.poolFor(6000)}) {
            for (int i = 0; i < n; i++) { d[i] = (2.0 + i * 4.0) * (2.0 + i * 4.0); form[i] = i % 3; want[i] = true; }
            int used = ModeArmorValkyrieBudget.admit(d, form, want, n, level, lance, done, pool);
            worst = Math.max(worst, used);
            if (used > pool) bad.add("a crowd of " + n + " wearers is admitted over the pool " + pool + ": " + used);
            for (int i = 0; i < n; i++) if (lance[i] && !ModeArmorValkyrieRules.lanceAt(level[i])) bad.add("a far wearer carries a lance");
        }
        // ---- assets
        for (String t : new String[]{"textures/entity/mode/valk2_atlas.png", "textures/entity/mode/valk2_scales.png", "valkyrie_meshes/pieces.json", "weapon_meshes/valkyrie_drill_lance.txt",
                "textures/entity/weapon/valkyrie_drill_lance.png", "textures/entity/weapon/valkyrie_drill_lance_glow.png", "textures/item/valkyrie_drill_lance.png",
                "models/item/valkyrie_drill_lance.json", "models/item/valkyrie_drill_lance_icon.json"})
            if (ModeArmorValkyrieSelfTests.class.getClassLoader().getResource("assets/nusmp/" + t) == null) bad.add("missing asset " + t);
        // ---- options
        if (ModeArmorValkyrieRules.DEFAULT_LOOK != ModeArmorValkyrieRules.Look.V2 || ModeArmorValkyrieRules.parseLook("garbage") != ModeArmorValkyrieRules.Look.V2 || ModeArmorValkyrieRules.parseLook("classic") != ModeArmorValkyrieRules.Look.CLASSIC
                || ModeArmorValkyrieRules.DEFAULT_FORM != Form.VALKYRIE || ModeArmorValkyrieRules.parseForm("mermaid") != Form.MERMAID || ModeArmorValkyrieRules.parseForm("saint") != Form.SAINT
                || ModeArmorValkyrieRules.DEFAULT_LANCE != ModeArmorValkyrieRules.LanceMode.ATTACHED || ModeArmorValkyrieRules.parseLance("floating") != ModeArmorValkyrieRules.LanceMode.FLOATING || ModeArmorValkyrieRules.parseLance("off") != ModeArmorValkyrieRules.LanceMode.OFF)
            bad.add("the valkyrieLook / valkyrieForm / valkyrieLance options do not default to V2 / VALKYRIE / ATTACHED or do not parse");
        // ---- the spin
        float s = ModeArmorValkyrieSpin.idleSpeedPerTick();
        for (int i = 0; i < 40; i++) s = ModeArmorValkyrieSpin.step(s, true, 1f);
        if (s * 20f > ModeArmorValkyrieSpin.MAX_DEG_PER_SEC + 0.01f) bad.add("the lance spins faster than " + ModeArmorValkyrieSpin.MAX_DEG_PER_SEC + " degrees a second: " + s * 20f);
        // ---- the lance item
        ServerLevel level0 = server.overworld();
        if (!ValkyrieLanceRules.ID.equals(BuiltInRegistries.ITEM.getKey(ValkyrieLanceBoot.LANCE.get()).getPath())) bad.add("the lance is not registered as " + ValkyrieLanceRules.ID);
        if (!(ValkyrieLanceBoot.LANCE.get() instanceof SwordItem)) bad.add("the lance is not a sword-class item");
        ItemStack stack = new ItemStack(ValkyrieLanceBoot.LANCE.get());
        double dmg = stack.getAttributeModifiers().compute(1.0, EquipmentSlot.MAINHAND), trident = new ItemStack(Items.TRIDENT).getAttributeModifiers().compute(1.0, EquipmentSlot.MAINHAND);
        if (Math.abs(dmg - trident) > 1.0e-6) bad.add("the lance's damage " + dmg + " is not the trident's " + trident);
        WeaponEngravings.engrave(stack, level0.registryAccess());
        int eng = stack.getEnchantments().size();
        if (eng < 4) bad.add("the lance came out with " + eng + " engravings, expected barrier piercing, elemental boost, crushing and magic weapon");
        WeaponEngravings.engrave(stack, level0.registryAccess());
        if (stack.getEnchantments().size() != eng) bad.add("the lance was engraved twice");
        HashMap<UUID, Long> map = new HashMap<>();
        UUID u = UUID.randomUUID();
        if (!ValkyrieLanceRules.hitBurstAllowed(map, u, 100L) || ValkyrieLanceRules.hitBurstAllowed(map, u, 101L) || !ValkyrieLanceRules.hitBurstAllowed(map, u, 100L + ValkyrieLanceRules.HIT_GAP_TICKS)) bad.add("the hit burst is not throttled to one per gap");
        for (int i = 0; i < ValkyrieLanceRules.MAX_REMEMBERED * 3; i++) ValkyrieLanceRules.hitBurstAllowed(map, UUID.randomUUID(), 1000L);
        if (map.size() > ValkyrieLanceRules.MAX_REMEMBERED) bad.add("the hit burst map grows past " + ValkyrieLanceRules.MAX_REMEMBERED);
        ValkyrieDrillLanceItem.clearAll();
        if (ValkyrieDrillLanceItem.remembered() != 0) bad.add("the lance remembers attackers after clearAll");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(files + " mesh files; looks " + ModeArmorValkyrieRules.armourQuads(Form.VALKYRIE, 0) + " / " + ModeArmorValkyrieRules.armourQuads(Form.SAINT, 0) + " / " + ModeArmorValkyrieRules.armourQuads(Form.MERMAID, 0)
                + " quads near (+ lance " + ModeArmorValkyrieRules.lanceQuads(0) + "); a crowd of " + n + " wearers held to " + worst + " of " + ModeArmorValkyrieBudget.POOL_QUADS + " quads; the lance: trident damage, " + eng
                + " engravings once, hit burst capped; default V2 / VALKYRIE / ATTACHED");
    }
}
