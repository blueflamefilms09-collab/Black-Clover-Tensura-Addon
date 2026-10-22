package com.newuniverse.nusmp.spiritcontrol;

import java.util.Locale;

/**
 * Why: one special ability of a spirit (what the old extra keys did: the Space spirit's portal, the Light spirit's beam, the Darkness spirit's Shadow Step, a powered form ...), described as plain data so the
 * grimoire pages and the {@code /multiverse spirit ability} command read the same list. Pure (no Minecraft types).
 *
 * @param id               the lower case word the command takes ({@code beam}, {@code portal})
 * @param label            what the grimoire page is called (English fallback of the page name)
 * @param pageCostPercent  percent of the caster's maximum magicule the grimoire page asks (0 = the spirit's own code takes its price, e.g. the Space spirit)
 * @param pageCooldownTicks cooldown the page adds on a success, on top of the spirit's own cooldown (0 = only the spirit's own)
 * @param takesSlot        true when the ability needs a waypoint slot argument (portal, set)
 */
public record SpiritAbilityDef(String id, String label, double pageCostPercent, int pageCooldownTicks, boolean takesSlot) {
    public SpiritAbilityDef {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ability id");
        id = id.toLowerCase(Locale.ROOT);
        if (label == null || label.isBlank()) throw new IllegalArgumentException("ability label");
        if (pageCostPercent < 0 || pageCooldownTicks < 0) throw new IllegalArgumentException("ability price");
    }

    public SpiritAbilityDef(String id, String label, double pageCostPercent) { this(id, label, pageCostPercent, 0, false); }
}
