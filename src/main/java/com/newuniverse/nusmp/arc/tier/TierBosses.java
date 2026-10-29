package com.newuniverse.nusmp.arc.tier;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.List;

/**
 * Why: the one list of the owner's tier-list bosses that exist, and the one place that registers them: the roster entry (floors, stats, lines, buff row), the entity
 * type (one per character, all made by the same {@link TierBoss} factory) and how it is drawn. A tier is one more line in {@link #all()}; nothing else changes. Called
 * from {@code ArcCharacters.bootstrap()}, before the registry events (entity types must be known by then).
 */
public final class TierBosses {
    private TierBosses() {}

    /** The one body every light boss wears (a plain person); each character is that body in its own skin and height. */
    public static final String BODY = "npc_default";
    /** The true height of that body model in blocks (the renderer scales it to each boss's own height). */
    public static final float BODY_HEIGHT = 1.97f;

    private static boolean done;

    /** Every light boss of the list that is built (see {@link TierRoster}). */
    public static List<TierRow> all() { return TierRoster.all(); }

    /** The ids of every light boss that is built (the player-model kit draws exactly these). */
    public static List<String> ids() { return TierRoster.ids(); }

    /** The skin texture name of a character ({@code textures/entity/arc_<id>.png}, written by tools/tier_bosses/gen_tier_skins.py). */
    public static String skin(String id) { return "arc_" + id; }

    /** How a light boss is drawn: the shared body, scaled to its own height, in its skin. */
    public static ArcLook look(TierRow row) {
        return new ArcLook(BODY, BODY_HEIGHT, 1.6f, 2.6f, 0.5f, 1.2f, 0.6f, true, skin(row.id()));
    }

    public static AttributeSupplier.Builder attributes(ArcCharacter c) {
        return ZogratisBoss.bossAttributes(c.health(), c.attack(), c.speed(), c.armor(), c.armor() * 0.5);
    }

    /** Is a character of the tier list allowed to appear (the {@code arc.tierBosses} switch)? Everyone else is always allowed. */
    public static boolean allowed(ArcCharacter c) { return c.saga() != ArcSaga.TIER_LIST || ArcConfig.tierBosses(); }

    /** Registers every light boss. Safe to call twice. */
    public static synchronized void registerAll() {
        if (done) return;
        done = true;
        for (TierRow row : all()) {
            ArcRoster.register(row.character());
            boolean light = TierRosterGag.LIGHT.equals(row.id());          // 3.36.0: the gag boss is its own entity class (Death Note scene, no knockback resistance)
            ArcEntities.<TierBoss>boss(row.id(), light ? (type, level) -> new LightYagamiBoss(type, level, row) : (type, level) -> new TierBoss(type, level, row),
                    row.width(), row.height(), look(row), () -> light ? LightYagamiBoss.attributes(row.character()) : attributes(row.character()));
        }
    }
}
