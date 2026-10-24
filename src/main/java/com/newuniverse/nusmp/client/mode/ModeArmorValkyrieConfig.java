package com.newuniverse.nusmp.client.mode;

import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.LanceMode;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Look;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The three Valkyrie Dress options of the CLIENT config {@code nusmp-modearmor-client.toml} (the file {@link ModeArmorWindV2Config} registers; the options sit in its {@code modeArmor} group, defined from its spec
 * block, and are mirrored into {@link ModeArmorValkyrieV2} when the file loads or reloads, so the layer reads a field, not a config, every frame). They only change what THIS client draws; the dedicated server never
 * registers the file and nothing is sent for them:
 * <ul>
 *   <li>{@code modeArmor.valkyrieLook}: V2 (default: the player-fitted armour of {@link ModeArmorValkyrieV2}) or CLASSIC (exactly the old overlay of {@link ModeArmorLayer}).</li>
 *   <li>{@code modeArmor.valkyrieForm}: which look V2 draws: VALKYRIE (default), SAINT (feathered wings, halo, winged helm, gold sash) or MERMAID (a scaled tail, the lance as its tail lance; the vanilla legs are hidden while it is on).</li>
 *   <li>{@code modeArmor.valkyrieLance}: the drill lance: OFF, ATTACHED (default; at the hips, or beside the tail in the Mermaid look) or FLOATING beside the wearer.</li>
 * </ul>
 */
final class ModeArmorValkyrieConfig {
    private static ModConfigSpec.EnumValue<Look> LOOK;
    private static ModConfigSpec.EnumValue<Form> FORM;
    private static ModConfigSpec.EnumValue<LanceMode> LANCE;

    private ModeArmorValkyrieConfig() {}

    /** Adds the options to the {@code modeArmor} group of the spec being built. */
    static void define(ModConfigSpec.Builder b) {
        LOOK = b.comment("The Valkyrie Dress overlay (Noelle's water armour): V2 (default: a player-fitted 3D armour with marbled veil wings, scarf blade, fin boots, tiara, petal bodice and skirt, a drill lance, in its own 1200-quad per-frame pool) or CLASSIC (the old overlay of flat cards, exactly). Only changes what this client draws.")
                .defineEnum("valkyrieLook", Look.V2);
        FORM = b.comment("Which look the V2 Valkyrie Dress draws: VALKYRIE (default), SAINT (feathered wings, halo, winged helm, gold sash) or MERMAID (a scaled tail ending in the lance; your own legs are hidden while it is on). Only changes what this client draws; valkyrieLook = CLASSIC ignores it.")
                .defineEnum("valkyrieForm", Form.VALKYRIE);
        LANCE = b.comment("The spinning drill lance of the V2 Valkyrie Dress: OFF, ATTACHED (default: at the hips, beside the tail in the Mermaid look) or FLOATING beside the wearer. It is drawn at near and mid distance only, and not while you hold the Valkyrie Drill Lance item. Only changes what this client draws.")
                .defineEnum("valkyrieLance", LanceMode.ATTACHED);
    }

    /** Mirrors the three values into the layer; the defaults when the file is not loaded yet. */
    static void apply() {
        try {
            ModeArmorValkyrieV2.setLook(LOOK.get());
            ModeArmorValkyrieV2.setForm(FORM.get());
            ModeArmorValkyrieV2.setLance(LANCE.get());
        } catch (IllegalStateException | NullPointerException notLoaded) {
            ModeArmorValkyrieV2.setLook(Look.V2);
            ModeArmorValkyrieV2.setForm(Form.VALKYRIE);
            ModeArmorValkyrieV2.setLance(LanceMode.ATTACHED);
        }
    }
}
