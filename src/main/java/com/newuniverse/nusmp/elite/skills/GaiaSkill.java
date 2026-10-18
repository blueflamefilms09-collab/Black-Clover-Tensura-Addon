package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Gaia (Ultimate, designed for this mod; the original addon's source was not available): the earth mother. Mode 1, Raise Domain, raises a living domain around you (14 blocks, 20 once Gaia is
 * mastered) for two minutes (four): allies and you are blessed with regeneration and resistance, enemies of your nation are burdened, strangers cannot break the ground or blow it up
 * ({@link GaiaBlockGuard}), and the domain costs magicules every second ({@link GaiaCost}, dearer on a war footing, {@link GaiaWarCheck}). Press it again to dismiss the domain (nothing is spent).
 * Mode 2, Pillars, raises a line of earth pillars in front of you that strike and fling the foes on them (no blocks are placed, {@link GaiaPillars}). Mode 3, Bloom, spends the life the domain has
 * gathered ({@link GaiaBloomLedger}: a point a second while you stand in it, five per kill) to grow a vein of ore in natural stone inside it ({@link VeinBloom}), at most eight veins a game day.
 * The parts that need the world are in {@link GaiaWorld}. Gaia is the evolution of Iron Resolve in data/nusmp/elite/skills/evolutions.json.
 */
public class GaiaSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("gaia", "Gaia",
            "Ultimate. Raise a domain of the earth mother: allies are blessed, strangers cannot break the ground, pillars strike the foes before you, and the domain's gathered life blooms veins of ore.");

    public GaiaSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("gaia.domain", "Raise Domain", GaiaCost.raise(GaiaCost.radius(false)), GaiaCost.RAISE_COOLDOWN),
                EliteMode.of("gaia.pillars", "Pillars", GaiaCost.pillars(GaiaPillars.count(false)), GaiaCost.PILLARS_COOLDOWN),
                EliteMode.of("gaia.bloom", "Bloom", GaiaCost.bloom(), GaiaCost.BLOOM_COOLDOWN));
    }

    @Override protected int color() { return 0xFF6AAA3A; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        boolean mastered = masteredBy(instance, player);
        switch (mode) {
            case 0 -> {
                GaiaZone live = GaiaZone.of(player.getUUID());
                if (live != null && live.active(player.level().getGameTime())) {
                    GaiaWorld.dismiss(player, false);
                    return false;                                            // refunded: dismissing is free
                }
                GaiaWorld.raise(player, mastered);
                return true;
            }
            case 1 -> {
                if (!EliteSkillsConfig.get(EliteSkillsConfig.GAIA_PILLARS)) {
                    EliteBridge.fail(player, SkillMsg.t("gaia.pillars_off", "Pillars are switched off on this server."));
                    return false;
                }
                return GaiaWorld.pillars(player, mastered, masteryFrac(instance));
            }
            default -> {
                return GaiaWorld.bloom(player, instance, mastered);
            }
        }
    }

    @Override
    public void upkeep(ManasSkillInstance instance, ServerPlayer player) {
        if (EliteSkillsConfig.enabled()) GaiaWorld.upkeep(instance, player);
    }

    /** A kill by the owner inside the domain feeds the bloom ledger. */
    static void onKill(ManasSkillInstance instance, ServerPlayer killer, LivingEntity victim) { GaiaWorld.onKillInDomain(instance, killer, victim); }
}
