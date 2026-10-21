package com.newuniverse.nusmp.client.relics.worn;

import com.mojang.brigadier.context.CommandContext;
import com.newuniverse.nusmp.client.relics.RelicTextures;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.relics.worn.lib.ArmorItems;
import com.newuniverse.nusmp.client.relics.worn.lib.ArmorSlotClient;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary;
import com.newuniverse.nusmp.relics.RelicBearer;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /nusmp debug worn}, a client command (nothing is sent to the server, no permission is needed, it prints once when run): lists what the local player wears in the four armour
 * slots and in the Curios slots, and for each piece the way it is drawn right now ({@link ArmorSlotPlan} for an armour relic, {@link WornPlan} for a Curios relic) with the reason
 * when nothing of it reaches the body. Complements {@code /multiverse relic debug}, which looks at any wearer from the server's side. Client thread only.
 */
public final class WornDebugCommand {
    private WornDebugCommand() {}

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Game bus: {@code RegisterClientCommandsEvent}. */
    public static void register(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("nusmp").then(Commands.literal("debug").then(Commands.literal("worn").executes(WornDebugCommand::run))));
    }

    private static Component t(String key, String fallback, Object... args) { return Component.translatableWithFallback("message.nusmp.worn_debug." + key, fallback, args); }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return 0;
        List<Component> out = new ArrayList<>();
        out.add(t("title", "Worn pieces of %s (armour slots and Curios, this machine, this frame)", p.getName().getString()).copy().withStyle(ChatFormatting.GOLD));
        out.add(t("options", "relicfx.models=%s  relicfx.hideArmourRelics=%s  visual.upgrade=%s  armour layers painted by the %s painter", RelicFxOptions.models(), RelicFxOptions.hideArmourRelics(),
                RelicFxOptions.upgrade(), RelicTextures.paintedWithUpgrade() ? "1.110.0" : "older"));
        WornPlan.View view = WornExec.viewOf(p);
        for (EquipmentSlot slot : SLOTS) out.add(line(p, slot, view));
        List<Relics.Located> found = Relics.locatedList(p);
        int curios = 0;
        for (Relics.Located l : found) {
            if (l.where() == Relics.Where.ARMOUR || l.where() == Relics.Where.HAND) continue;
            curios++;
            RelicDef d = l.def();
            List<WornPlan.Entry> one = WornExec.entries(List.of(l), null, true);
            WornPlan.Plan plan = WornPlan.decide(new WornPlan.Ctx(false, view, false, false, false, RelicFxOptions.models(), RelicFxOptions.hideArmourRelics(), WornPlan.Fallback.BOXES), one);
            WornPlan.Item it = plan.all().get(0);
            out.add(t("curio", "%s (%s, found in %s): %s", d.id(), d.slot().name(), l.where().name().toLowerCase(), describe(it, WornLibrary.of(d))));
        }
        if (curios == 0) out.add(t("no_curios", "No relic is worn in a Curios slot (or Curios is not installed)."));
        for (Component c : out) mc.gui.getChat().addMessage(c);
        return 1;
    }

    private static Component line(Player p, EquipmentSlot slot, WornPlan.View view) {
        ItemStack s = p.getItemBySlot(slot);
        String slotName = slot.getName();
        if (s.isEmpty()) return t("empty", "%s: empty", slotName);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
        if (s.getItem() instanceof RelicBearer b) {
            RelicDef d = b.relic();
            ArmorSlotPlan.Verdict v = ArmorSlotClient.verdict(p, s, slot);
            if (v == null) return t("relic_wrong_slot", "%s: relic %s, but it is a %s relic: it is not drawn from this slot", slotName, d.id(), d.slot().name());
            WornLibrary.Ensemble e = WornLibrary.of(d);
            String layer = v.layer() == ArmorSlotPlan.Layer.PAINTED ? "the painted armour layer (" + (RelicTextures.hasArmorGlow(d.id()) ? "with" : "without") + " glow)" : "nothing (an empty armour model)";
            String model = e == null ? "no worn model in the library" : e.pieces.size() + " model pieces, " + e.baseVerts + " vertices, " + (WornLibrary.upgradeOf(d.id()) ? "upgraded" : "older") + " look";
            return t("relic", "%s: relic %s (%s): library %s; plan %s (%s); vanilla layer draws %s", slotName, d.id(), d.slot().name(), model, v.kind(), v.why().text, layer);
        }
        WornLibrary.Ensemble e = ArmorItems.of(s, slot);
        if (e != null) return t("item_library", "%s: %s: drawn by the model library (%s pieces); the older armour renderer draws nothing for a player", slotName, id, e.pieces.size());
        String path = id.getPath();
        boolean l1 = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/models/armor/" + path + "_layer_1.png")).isPresent();
        boolean l2 = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/models/armor/" + path + "_layer_2.png")).isPresent();
        return t("item_vanilla", "%s: %s: the vanilla armour layer of its material (own layer_1 %s, layer_2 %s; a material may name another texture)", slotName, id, l1 ? "found" : "not found", l2 ? "found" : "not found");
    }

    /** One line for the report: how a plan item is drawn and, when it is not, why. */
    static String describe(WornPlan.Item it, WornLibrary.Ensemble e) {
        String model = e == null ? "no worn model" : e.pieces.size() + " pieces";
        return it.kind() + ": " + it.why().text + " (" + model + ")";
    }
}
