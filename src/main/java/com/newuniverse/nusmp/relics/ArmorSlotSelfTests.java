package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.client.relics.ArmorPainter;
import com.newuniverse.nusmp.client.relics.ArmorPainterV2;
import com.newuniverse.nusmp.client.relics.worn.ArmorSlotPlan;
import com.newuniverse.nusmp.client.relics.worn.WornAssets;
import com.newuniverse.nusmp.client.relics.worn.WornPlan;
import com.newuniverse.nusmp.client.relics.worn.lib.ArmorLooks;
import com.newuniverse.nusmp.client.relics.worn.lib.ArmorSlotCoverage;
import com.newuniverse.nusmp.client.relics.worn.lib.UpgradeCatalog;
import com.newuniverse.nusmp.client.relics.worn.lib.WornAnchor;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The self-test {@code visual.armor_slot_relics} (run by the CI server smoke test, see {@link SelfTests}; it reads only registries, files in the jar and pure classes, so it runs on a
 * dedicated server): every item of the mod that can be worn in a vanilla armour slot has a way onto the body.
 * <ul>
 *   <li>every HEAD, CHEST and FEET relic is a registered {@link RelicArmorItem} whose armour material names the layer {@code nusmp:relic_<id>}, resolves through the real catalogs to a
 *       worn model that hangs on the right body part ({@link ArmorSlotCoverage}), and has a painted layer (old painter and 1.110.0 painter cover the same, non-empty pixels, and the
 *       glow channel lies on covered pixels);</li>
 *   <li>every other armour item of the mod (robes, dungeon armour) resolves to library models whose files exist;</li>
 *   <li>for every circumstance a wearer can be seen in, {@link ArmorSlotPlan} leaves something of an armour relic on the body (the model or the painted layer), except where the player switched
 *       armour visuals off or the camera is in the head.</li>
 * </ul>
 */
public final class ArmorSlotSelfTests {
    private ArmorSlotSelfTests() {}

    public static void register() { SelfTests.register("visual.armor_slot_relics", server -> run()); }

    private static List<String> lines(String path) throws IOException {
        List<String> out = new ArrayList<>();
        try (InputStream in = ArmorSlotSelfTests.class.getResourceAsStream(path)) {
            if (in == null) return out;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String l;
                while ((l = r.readLine()) != null) out.add(l);
            }
        }
        return out;
    }

    static SelfTests.Result run() throws IOException {
        UpgradeCatalog up = UpgradeCatalog.parse(lines("/assets/nusmp/visual_upgrade/catalog.txt"));
        int relics = 0, upgraded = 0, items = 0;
        for (RelicDef d : Relics.all()) {
            if (!d.slot().isArmor() || d.isAdopted()) continue;
            relics++;
            ResourceLocation key = ResourceLocation.fromNamespaceAndPath("nusmp", d.id());
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(key);
            if (item.isEmpty() || !(item.get() instanceof RelicArmorItem a)) return SelfTests.Result.fail(d.id() + " is a " + d.slot() + " relic but is not a registered RelicArmorItem");
            String layer = a.getMaterial().value().layers().isEmpty() ? "" : a.getMaterial().value().layers().get(0).texture(false).toString();
            if (!layer.equals("nusmp:textures/models/armor/" + RelicArmorMaterials.name(d.id()) + "_layer_1.png")) return SelfTests.Result.fail(d.id() + " names the armour layer '" + layer + "', expected nusmp:" + RelicArmorMaterials.name(d.id()));
            ArmorSlotCoverage.Resolution res = ArmorSlotCoverage.resolve(d.id(), d.slot().name(), d.look().name(), WornAssets.catalog(), up, n -> {
                Optional<WornMesh> m = WornAssets.mesh(n);
                return m.isPresent() ? m.get().anchor : (WornAnchor) null;
            });
            if (!res.ok()) return SelfTests.Result.fail(d.id() + " (" + d.slot() + "/" + d.look() + ") has no worn model on its body part: " + res.detail());
            if (res.route() == ArmorSlotCoverage.Route.UPGRADED_MODEL) upgraded++;
            long seed = d.id().hashCode();
            int[] old = ArmorPainter.paint(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), seed);
            int[] now = ArmorPainterV2.paint(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), seed);
            int[] glow = ArmorPainterV2.paintGlow(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), seed);
            int covered = 0;
            for (int i = 0; i < old.length; i++) {
                boolean a0 = old[i] >>> 24 != 0, a1 = now[i] >>> 24 != 0;
                if (a0 != a1) return SelfTests.Result.fail(d.id() + ": the 1.110.0 layer does not cover the same pixels as the older one (pixel " + i + ")");
                if (a1) covered++;
                if (glow[i] >>> 24 != 0 && !a1) return SelfTests.Result.fail(d.id() + ": the glow channel lies outside the painted layer (pixel " + i + ")");
            }
            if (covered == 0) return SelfTests.Result.fail(d.id() + ": its painted layer is fully transparent");
            for (boolean models : new boolean[]{true, false}) for (boolean far : new boolean[]{false, true}) for (WornPlan.View view : WornPlan.View.values()) {
                for (boolean hasModel : new boolean[]{true, false}) {
                    ArmorSlotPlan.Verdict v = ArmorSlotPlan.decide(d.slot().name(), hasModel, false, models, false, view, far);
                    boolean cameraInHead = view == WornPlan.View.SELF_FIRST && d.slot() == RelicSlot.HEAD;
                    if (!v.visible() && !cameraInHead) return SelfTests.Result.fail(d.id() + " is invisible: models=" + models + " far=" + far + " view=" + view + " hasModel=" + hasModel + " -> " + v);
                }
            }
        }
        if (relics < 100) return SelfTests.Result.fail("only " + relics + " armour-slot relics found, the registry should hold over a hundred");
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ArmorItem a) || item instanceof RelicArmorItem) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!"nusmp".equals(id.getNamespace())) continue;
            ArmorLooks.Slot slot = switch (a.getEquipmentSlot()) {
                case HEAD -> ArmorLooks.Slot.HEAD;
                case CHEST -> ArmorLooks.Slot.CHEST;
                case LEGS -> ArmorLooks.Slot.LEGS;
                case FEET -> ArmorLooks.Slot.FEET;
                default -> null;
            };
            if (slot == null) continue;
            items++;
            ArmorLooks.Generic g = ArmorLooks.resolve(slot, id.getPath());
            if (g.pieces().isEmpty()) return SelfTests.Result.fail(id + " (" + a.getEquipmentSlot() + ") resolves to no worn model");
            for (String piece : g.pieces()) {
                String name = com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog.piece(piece).model();
                if (!WornAssets.exists(WornAssets.BASE + name + ".txt")) return SelfTests.Result.fail(id + ": model file " + name + " is not in the jar");
            }
        }
        return SelfTests.Result.ok(relics + " armour-slot relics (" + upgraded + " with an upgraded model) and " + items + " other armour items all have a way onto the body");
    }
}
