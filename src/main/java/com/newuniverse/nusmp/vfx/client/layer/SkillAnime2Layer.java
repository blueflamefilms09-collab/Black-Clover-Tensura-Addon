package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.Set;

/**
 * VFX wave 4: the skills wave 3 left out, the Beast Will moves, the hero weapon techniques and the weakest grimoire attributes, drawn on top of their old effects
 * (docs/vfx_remake/skill_anime_wave4.md). One layer, four shapes, each a family of styles (the styles of one shape share a power encoding, {@link SkillAnime2Vfx}); the drawing code of each lives
 * in a helper next to this file and uses the kit of {@link SkillAnimeLayer} (life ramps, eased opening, hoops, flashes, zigzag bolts, seeded randomness so every client sees the same picture):
 * <ul>
 *   <li>ANIME2_SKILL: {@link SkillAnime2More}: Liaris Freese, Hati, Hawk Eye, Spirit Card, the summon cards, Shishou, the Gate's Armour / Vimana / King / Elixir, the Devil's Kick, The Almighty, The Visionary.</li>
 *   <li>ANIME2_BEAST: {@link SkillAnime2Beast}: six move shapes (line, rush, burst, volley, seal, aura) x twelve elements, one themed effect family per element.</li>
 *   <li>ANIME2_HERO: {@link SkillAnime2Hero}: seven weapon motifs (slash, thrust, shock, ward, cast, thread, shard) with sub-forms, in the weapon's own colour.</li>
 *   <li>ANIME2_GRIMOIRE: {@link SkillAnime2Grim}: Cotton, Mirror, Plant, Sealing and Thread, the cast circle and the impact of each.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> The engine caps every activation at 100 quads (400 vertices) and this layer keeps the default; every loop is a constant or a {@code ctx.seg} (distance and particle setting),
 * ring / hoop / beam loops check the budget, nothing is kept between frames, a followed effect whose creature is gone draws nothing. With {@code skills.classicVfx2} on the layer draws nothing.
 */
public class SkillAnime2Layer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(SkillAnime2Vfx.fileName(n)); }

    public static final ResourceLocation LEAF = t("leaf");
    public static final ResourceLocation DROPLET = t("droplet");
    public static final ResourceLocation ROCK = t("rock");
    public static final ResourceLocation BUBBLE = t("bubble");
    public static final ResourceLocation FEATHER = t("feather");
    public static final ResourceLocation RUNE = t("rune");
    public static final ResourceLocation CLAW = t("claw");
    public static final ResourceLocation CARD = t("card");
    public static final ResourceLocation EYE = t("eye");
    public static final ResourceLocation CRESCENT = t("crescent");
    public static final ResourceLocation HEX = t("hex");
    public static final ResourceLocation STITCH = t("stitch");
    public static final ResourceLocation NOTE = t("note");
    public static final ResourceLocation PUFF = t("puff");
    public static final ResourceLocation FACET = t("facet");

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ANIME2_SKILL, VfxShape.ANIME2_BEAST, VfxShape.ANIME2_HERO, VfxShape.ANIME2_GRIMOIRE); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.ANIME2_GRIMOIRE ? 22 : 28; }

    @Override
    public int defaultColor(VfxShape s) { return 0xFFFFE7A0; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (SkillAnime2Vfx.classic()) return;
        if (inst.payload.followEntity() >= 0 && Float.isNaN(inst.followYaw(ctx))) return;        // the followed creature is gone (died, left): the effect ends with it
        switch (inst.shape) {
            case ANIME2_SKILL -> SkillAnime2More.draw(inst, ctx, buf);
            case ANIME2_BEAST -> SkillAnime2Beast.draw(inst, ctx, buf);
            case ANIME2_HERO -> SkillAnime2Hero.draw(inst, ctx, buf);
            case ANIME2_GRIMOIRE -> SkillAnime2Grim.draw(inst, ctx, buf);
            default -> { }
        }
    }
}
