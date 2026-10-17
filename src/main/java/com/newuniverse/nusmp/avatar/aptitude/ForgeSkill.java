package com.newuniverse.nusmp.avatar.aptitude;

/**
 * Why: page 4 of the Soul Forge lets the creator bind one Unique and one Ultimate skill to the Avatar. A skill is an id that already exists in the game's
 * skill registry plus the numbers the forge needs: what it costs (a permanent tax on the creator, here called the EP tax because that is how the owner
 * names it), how much existence the creator must already have to carry it, and how it is drawn. It is pure data so the screen, the server validator and the
 * tests read the same numbers; the skill itself (name, icon, behaviour) stays Tensura's or this mod's. Entries are loaded from
 * {@code data/<namespace>/avatar_skills/*.json}.
 *
 * @param id          the registry id of the skill ("tensura:predator"); must exist at runtime or the entry is left out of the forge
 * @param kind        the slot it fits (the server re-reads the real category from the registry and trusts that over this hint)
 * @param name        display name used when the registry has none
 * @param epTaxPercent permanent percent of the creator's maximum magicule taken for the skill (0..90), before the config scale
 * @param minEp       the creator's maximum EP must be at least this to carry the skill (0 = no gate)
 * @param tint        0xRRGGBB, the aura tint the preview takes when the skill is selected
 * @param icon        optional texture override ("" = the skill's own icon from the registry)
 * @param description one short line for the tooltip (may be empty)
 */
public record ForgeSkill(String id, Kind kind, String name, double epTaxPercent, double minEp, int tint, String icon, String description) {
    /** The two slots of the screen. Only ever APPEND values. */
    public enum Kind {
        UNIQUE, ULTIMATE;

        public static Kind byName(String s) {
            if (s == null) return null;
            for (Kind k : values()) if (k.name().equalsIgnoreCase(s.strip())) return k;
            return null;
        }
    }

    public ForgeSkill {
        if (!ForgeJson.looksLikeResourceId(id)) throw new IllegalArgumentException("a skill id is namespace:path: " + id);
        if (kind == null) throw new IllegalArgumentException("a skill needs a kind: " + id);
        name = name == null || name.isBlank() ? id.substring(id.indexOf(':') + 1) : name;
        epTaxPercent = Math.max(0.0, Math.min(90.0, epTaxPercent));
        minEp = Math.max(0.0, minEp);
        icon = icon == null ? "" : icon;
        description = description == null ? "" : description;
    }

    /** The same skill in another slot (used when the registry says the hint was wrong). */
    public ForgeSkill withKind(Kind k) { return new ForgeSkill(id, k, name, epTaxPercent, minEp, tint, icon, description); }
}
