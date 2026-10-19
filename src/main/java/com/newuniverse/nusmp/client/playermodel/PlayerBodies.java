package com.newuniverse.nusmp.client.playermodel;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.client.arc.ArcNpcRenderer;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.elite.hub.HubNpcBase;
import com.newuniverse.nusmp.elite.hub.MerchantNPC;
import com.newuniverse.nusmp.entity.PaintedConstructEntity;
import com.newuniverse.nusmp.entity.ZagredBossEntity;
import com.newuniverse.nusmp.entity.avatar.DevilAvatarBoss;
import com.newuniverse.nusmp.entity.zogratis.DarkDiscipleEntity;
import com.newuniverse.nusmp.entity.zogratis.LuciusBoss;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisCloneEntity;
import com.newuniverse.nusmp.relics.order.GodOfOrder;
import com.newuniverse.nusmp.relics.order.OrderPlan;
import com.newuniverse.nusmp.wizard.TowerWizardEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: what each converted character tells the kit ({@link PlayerBody}), one small class each, read straight from what the entity already says (its synced state, its form, its devil, its
 * kind, its role, its swing). It replaces the art-kit renderers of those entities (client/remodel, client/arc, client/zogratis) with the same behaviour on the player model: the same form
 * rules (a second look from a form, or from the start of the transformation, as each boss's old renderer chose), the same glow curve ({@link ZogratisGlow}), the same fade of a dying body,
 * the same talk, cast and slump moments, now as arm and head poses ({@link PosePatterns}). Nothing here changes a game rule.
 */
public final class PlayerBodies {
    private PlayerBodies() {}

    /** The scale that draws a look at a hitbox's height (the old renderers' "fit"). */
    static float fit(LivingEntity e, PlayerLook look, float boost) {
        return e.getBbHeight() / Math.max(0.2f, look.scale() * PlayerLook.BASE_HEIGHT) * boost;
    }

    private static float speedOf(LivingEntity e) { return (float) Math.hypot(e.getX() - e.xo, e.getZ() - e.zo); }

    /** The id of the second form of a look, when there is one. */
    static String form2(String id) { return id + "_form2"; }

    // ================================================================================================ the arcs' friendly characters
    /** All friendly characters of the arcs are one entity type; its id picks the look. They stand and gesture when a player comes close (the rule is {@link ArcNpcRenderer#talkingRule}). */
    public static final class ArcNpc implements PlayerBody<ArcNpcEntity> {
        private static final double NAME_RANGE_SQR = 20.0 * 20.0;

        @Override
        public PlayerLook look(ArcNpcEntity e) {
            String id = e.characterId();
            return PlayerAssets.look(PlayerAssets.hasLook(id) ? id : SkinPath.DEFAULT_ID);
        }

        @Override
        public void pose(ArcNpcEntity e, float partial, PoseState p) {
            if (speedOf(e) >= ZogratisPose.WALK_ON || e.isDeadOrDying()) return;
            boolean talking = ArcNpcRenderer.talkingRule.test(e);
            if (talking) PosePatterns.talk(p, 1f, e.tickCount + partial);
        }

        @Override
        public boolean showName(ArcNpcEntity e, boolean vanilla, double distSqr) {
            if (vanilla) return true;
            return !e.isInvisible() && e.character() != null && distSqr < NAME_RANGE_SQR;
        }

        @Override
        public Component name(ArcNpcEntity e, Component vanilla) {
            ArcCharacter c = e.character();
            return e.hasCustomName() || c == null ? vanilla : Component.literal(c.name());
        }
    }

    // ================================================================================================ the Zogratis family's bosses and the arcs' bosses
    /**
     * The shared body of every {@link ZogratisBoss}: which look (the first, or the second from a form or from the start of the transformation), the glow curve of the power drawn, the cast,
     * power-up and slump poses, the fade of a dying body, and the flash of the boss's colour over the swap.
     */
    public abstract static class Boss<T extends ZogratisBoss> implements PlayerBody<T> {
        /** What the old renderer chose: the second look only from the new form ({@code split}), or already from the first frame of the transformation. */
        protected final boolean fromTransform;
        protected final boolean followsHitbox;
        protected final int flashRgb;

        protected Boss(boolean fromTransform, boolean followsHitbox, int flashRgb) {
            this.fromTransform = fromTransform;
            this.followsHitbox = followsHitbox;
            this.flashRgb = flashRgb;
        }

        protected abstract String first(T e);

        protected String second(T e) { return form2(first(e)); }

        /** Does the second look show now? */
        protected boolean useSecond(T e, boolean newForm) {
            return newForm || (fromTransform && e.transforming());
        }

        @Override
        public PlayerLook look(T e) {
            boolean newForm = e.form() != ZogratisBoss.FORM_BASE;
            String s = second(e);
            if (PlayerAssets.hasLook(s) && useSecond(e, newForm)) return PlayerAssets.look(s);
            return PlayerAssets.look(first(e));
        }

        @Override
        public void dynamic(T e, float partial, Dynamic d) {
            boolean transforming = e.transforming(), casting = e.state() == ZogratisBoss.STATE_CASTING, dead = e.isDeadOrDying();
            float phase = e.phaseProgress(), age = e.tickCount + partial;
            Move move = casting ? e.castingMoveOrNull() : null;
            boolean ultimate = move != null && move.ultimate();
            float fade = dead ? ZogratisGlow.deathFade(e.deathTime + partial, 20f) : 1f;
            int mode = dead ? ZogratisGlow.NORMAL : transforming ? ZogratisGlow.TRANSFORMING : casting ? (ultimate ? ZogratisGlow.ULTIMATE : ZogratisGlow.CASTING)
                    : e.exposed() ? ZogratisGlow.EXPOSED : ZogratisGlow.NORMAL;
            d.alpha = fade;
            d.glowLevel = ZogratisGlow.level(e.power(), age, mode, phase) * fade;
            d.cast = casting ? (ultimate ? 2 : 1) : 0;
            d.progress = phase;
            d.transforming = transforming;
            if (followsHitbox) d.size = e.getBbHeight() / e.getType().getHeight();
            float flash = transforming ? ZogratisGlow.flash(phase) : 0f;
            if (flash > 0.02f) {
                d.flash = flash * 0.8f;
                d.flashRgb = ZogratisGlow.mix(flashRgb >= 0 ? flashRgb : e.styleColor(), 0xFFFFFF, 0.5f);
            }
            hover(e, d, transforming, phase);
        }

        protected void hover(T e, Dynamic d, boolean transforming, float phase) { }

        @Override
        public void pose(T e, float partial, PoseState p) {
            if (e.isDeadOrDying()) return;
            float age = e.tickCount + partial;
            if (e.transforming()) PosePatterns.powerUp(p, e.phaseProgress(), age);
            else if (e.state() == ZogratisBoss.STATE_CASTING) {
                Move move = e.castingMoveOrNull();
                PosePatterns.cast(p, e.phaseProgress(), move != null && move.ultimate(), age);
            } else if (e.exposed()) PosePatterns.slump(p, 1f, age);
        }
    }

    /** One of the Triad, whose hybrid body bursts out of the first frame of the transformation. */
    public static final class Triad<T extends ZogratisBoss> extends Boss<T> {
        private final String id;

        public Triad(String id) {
            super(true, true, -1);
            this.id = id;
        }

        @Override protected String first(T e) { return id; }
    }

    /** Lucius: floats in his divine form, glows white-blue while Julius holds the body. */
    public static final class Lucius extends Boss<LuciusBoss> {
        public static final int JULIUS_RGB = 0xCFE6FF;

        public Lucius() { super(false, false, -1); }

        @Override protected String first(LuciusBoss e) { return "lucius_zogratis"; }

        @Override
        protected void hover(LuciusBoss e, Dynamic d, boolean transforming, float phase) {
            d.lift = e.hover() * ZogratisGlow.hoverRamp(transforming, phase, e.form() == ZogratisBoss.FORM_DIVINE);
        }

        @Override
        public void dynamic(LuciusBoss e, float partial, Dynamic d) {
            super.dynamic(e, partial, d);
            if (e.julius()) d.glowRgb = JULIUS_RGB;
        }
    }

    /** A boss of the arcs: the character's id is the look's id. Its hitbox decides its size (a boss that grows with a phase grows on screen). */
    public static final class Arc implements PlayerBody<ArcBoss> {
        private final Boss<ArcBoss> base = new Boss<>(false, false, -1) {
            @Override protected String first(ArcBoss e) { return idOf(e); }
        };

        static String idOf(ArcBoss e) {
            try {
                return e.character().id();
            } catch (RuntimeException ex) {
                return SkinPath.DEFAULT_ID;
            }
        }

        @Override public PlayerLook look(ArcBoss e) { return base.look(e); }

        @Override
        public void dynamic(ArcBoss e, float partial, Dynamic d) {
            base.dynamic(e, partial, d);
            d.size = fit(e, base.look(e), 1f);
        }

        @Override public void pose(ArcBoss e, float partial, PoseState p) { base.pose(e, partial, p); }
    }

    /** The Devil Avatar: the base body until the Gates open, then the second look; the Avatar of Megicula wears the crimson skin. */
    public static final class DevilAvatar extends Boss<DevilAvatarBoss> {
        public DevilAvatar() { super(false, true, -1); }

        @Override protected String first(DevilAvatarBoss e) { return e.megicula() ? "devil_avatar_megicula" : "devil_avatar"; }
    }

    // ================================================================================================ the Dark Disciple and the clones
    /** The hooded acolyte of a devil-bearer: the devil of its master tints it; its glow gathers over the telegraph of its ability. */
    public static final class Disciple implements PlayerBody<DarkDiscipleEntity> {
        private static final float LONGEST_WIND_UP = 26f;

        @Override
        public PlayerLook look(DarkDiscipleEntity e) {
            String devil = switch (e.devil()) {
                case LUCIFERO -> "lucifero";
                case MEGICULA -> "megicula";
                case BEELZEBUB -> "beelzebub";
                case ASTAROTH -> "astaroth";
                default -> null;
            };
            return PlayerAssets.look(devil == null ? "dark_disciple" : "dark_disciple_" + devil);
        }

        @Override
        public void dynamic(DarkDiscipleEntity e, float partial, Dynamic d) {
            int wind = e.castLeft();
            d.cast = wind > 0 ? 1 : 0;
            d.progress = wind > 0 ? 1f - Math.min(wind, LONGEST_WIND_UP) / LONGEST_WIND_UP : 0f;
            d.size = fit(e, look(e), 1f);
            float age = e.tickCount + partial;
            float fade = e.isDeadOrDying() ? ZogratisGlow.deathFade(e.deathTime + partial, 20f) : 1f;
            d.alpha = fade;
            d.glowLevel = (wind > 0 ? 1f : 0.72f + 0.12f * Mth.sin(age * 0.07f)) * fade;
        }

        @Override
        public void pose(DarkDiscipleEntity e, float partial, PoseState p) {
            int wind = e.castLeft();
            if (wind > 0) PosePatterns.cast(p, 1f - Math.min(wind, LONGEST_WIND_UP) / LONGEST_WIND_UP, false, e.tickCount + partial);
        }
    }

    /** A clone of Lucius: his body of the form he had when it was made (the divine one floats), gold when the Swap Strike has marked it, fading out over its last two seconds. */
    public static final class Clone implements PlayerBody<ZogratisCloneEntity> {
        private static final float TELL_TICKS = 14f;
        private static final int GOLD_FLASH_RGB = 0xFFD24A;

        private boolean divine(ZogratisCloneEntity e) { return e.form() == ZogratisBoss.FORM_DIVINE; }

        @Override public PlayerLook look(ZogratisCloneEntity e) { return PlayerAssets.look(divine(e) ? "lucius_zogratis_form2" : "lucius_zogratis"); }

        @Override
        public void dynamic(ZogratisCloneEntity e, float partial, Dynamic d) {
            d.alpha = e.fade();
            d.size = fit(e, look(e), 1f);
            d.lift = divine(e) ? 0.12f + 0.05f * Mth.sin((e.tickCount + partial) * 0.12f) : 0f;
            if (e.markLeft() > 0) {
                float k = 0.5f + 0.5f * Mth.sin(e.tickCount * 0.7f);
                d.tint = ZogratisGlow.mix(0xFFFFFF, GOLD_FLASH_RGB, k);
            }
            int tell = e.castLeft();
            d.cast = tell > 0 ? 1 : 0;
            d.progress = tell > 0 ? 1f - Math.min(tell, TELL_TICKS) / TELL_TICKS : 0f;
            float age = e.tickCount + partial;
            d.glowLevel = e.markLeft() > 0 ? 0.9f : (tell > 0 ? 0.95f : 0.6f + 0.1f * Mth.sin(age * 0.1f));
        }

        @Override
        public void pose(ZogratisCloneEntity e, float partial, PoseState p) {
            int tell = e.castLeft();
            if (tell > 0) PosePatterns.cast(p, 1f - Math.min(tell, TELL_TICKS) / TELL_TICKS, false, e.tickCount + partial);
        }
    }

    // ================================================================================================ the legacy Zagred and the God of Order
    /** The first Zagred boss: his look from the fourth phase on has the trident; he casts while he speaks a word (the old synced state). */
    public static final class LegacyZagred implements PlayerBody<ZagredBossEntity> {
        @Override public PlayerLook look(ZagredBossEntity e) { return PlayerAssets.look(e.phase() >= 4 ? "zagred_legacy_form2" : "zagred_legacy"); }

        @Override
        public void dynamic(ZagredBossEntity e, float partial, Dynamic d) {
            boolean casting = e.clientState() == ZagredBossEntity.STATE_CASTING;
            d.cast = casting ? 1 : 0;
            d.progress = casting ? 1f : 0f;
            d.size = fit(e, look(e), 1f);
            float age = e.tickCount + partial;
            float fade = e.isDeadOrDying() ? ZogratisGlow.deathFade(e.deathTime + partial, 20f) : 1f;
            d.alpha = fade;
            d.glowLevel = (0.65f + 0.35f * Mth.sin(age * (0.08f + 0.04f * e.phase()))) * fade;
        }

        @Override
        public void pose(ZagredBossEntity e, float partial, PoseState p) {
            if (e.clientState() == ZagredBossEntity.STATE_CASTING) PosePatterns.cast(p, 1f, false, e.tickCount + partial);
        }

        /** His fresnel aura (a layer) and his target reticle (a decoration), as the old renderer had them. */
        @Override
        public void layers(PlayerModelRenderer.Body<ZagredBossEntity> body, net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx) { body.addLayer(new ZagredAuraLayer(body)); }

        @Override
        public void decorate(ZagredBossEntity e, float partial, Decoration deco) { ZagredAuraLayer.reticle(e, partial, deco); }
    }

    /**
     * The God of Order's visit: it steps out of its column (grows to full size in 14 ticks), reads (arms open), levels its staff at the holder (beam) and bows when it leaves; the clips the old
     * model had are poses of the player model now.
     */
    public static final class Order extends Boss<GodOfOrder> {
        private static final float APPEAR_TICKS = 14f, BOW_SECONDS = 2f;
        private final Map<Entity, float[]> phaseStart = new WeakHashMap<>();

        public Order() { super(false, false, 0xE8B923); }

        @Override protected String first(GodOfOrder e) { return "god_of_order"; }

        @Override
        public void dynamic(GodOfOrder e, float partial, Dynamic d) {
            super.dynamic(e, partial, d);
            float t = Mth.clamp((e.tickCount + partial) / APPEAR_TICKS, 0.02f, 1f);
            d.size = fit(e, look(e), 1f) * (t * t * (3f - 2f * t));
        }

        @Override
        public void pose(GodOfOrder e, float partial, PoseState p) {
            if (e.isDeadOrDying() || e.transforming() || e.state() == ZogratisBoss.STATE_CASTING) {
                super.pose(e, partial, p);
                return;
            }
            OrderPlan.Phase visit = e.orderPhase();
            float age = e.tickCount + partial;
            float[] since = phaseStart.computeIfAbsent(e, k -> new float[]{-1f, age});
            if (since[0] != visit.ordinal()) {
                since[0] = visit.ordinal();
                since[1] = age;
            }
            float sec = (age - since[1]) / 20f;
            if (visit == OrderPlan.Phase.READING) PosePatterns.read(p, 1f, age);
            else if (visit == OrderPlan.Phase.BEAM) PosePatterns.point(p, 1f, age);
            else if (visit == OrderPlan.Phase.BOW && e.targetId() < 0) PosePatterns.bow(p, Mth.sin(Mth.clamp(sec / BOW_SECONDS, 0f, 1f) * Mth.PI));
            else super.pose(e, partial, p);
        }
    }

    // ================================================================================================ people of the world
    /** The Hub's Quest Giver and Merchant: people who stand at their post and look at you. */
    public static final class HubNpc implements PlayerBody<HubNpcBase> {
        @Override public PlayerLook look(HubNpcBase e) { return PlayerAssets.look(e instanceof MerchantNPC ? "merchant_npc" : "quest_giver_npc"); }

        @Override
        public void pose(HubNpcBase e, float partial, PoseState p) {
            LocalPlayer pl = Minecraft.getInstance().player;
            if (pl != null && !e.isDeadOrDying() && speedOf(e) < ZogratisPose.WALK_ON && e.distanceToSqr(pl) <= 3.5 * 3.5) PosePatterns.talk(p, 1f, e.tickCount + partial);
        }
    }

    /** The keeper of a Grimoire Tower stands still and gestures when a player stands within 3.2 blocks. */
    public static final class Wizard implements PlayerBody<TowerWizardEntity> {
        private static final double TALK_RANGE = 3.2;

        @Override public PlayerLook look(TowerWizardEntity e) { return PlayerAssets.look("tower_wizard"); }

        @Override
        public void dynamic(TowerWizardEntity e, float partial, Dynamic d) { d.size = fit(e, look(e), 1f); }

        @Override
        public void pose(TowerWizardEntity e, float partial, PoseState p) {
            LocalPlayer pl = Minecraft.getInstance().player;
            boolean near = pl != null && !e.isDeadOrDying() && e.distanceToSqr(pl) <= TALK_RANGE * TALK_RANGE && speedOf(e) < ZogratisPose.WALK_ON;
            if (near) PosePatterns.talk(p, 1f, e.tickCount + partial);
        }
    }

    /**
     * The painted knight of Painting Magic: painted once in pale paint and tinted by the paint's colour, rising off the canvas over 16 ticks (flat and wide, springing up).
     */
    public static final class PaintedKnight implements PlayerBody<PaintedConstructEntity> {
        @Override public PlayerLook look(PaintedConstructEntity e) { return PlayerAssets.look("painted_knight"); }

        @Override
        public void dynamic(PaintedConstructEntity e, float partial, Dynamic d) {
            int rgb = e.paint().color & 0xFFFFFF;
            d.tint = rgb;
            int r = Math.min(255, ((rgb >> 16) & 255) + 70), g = Math.min(255, ((rgb >> 8) & 255) + 70), b = Math.min(255, (rgb & 255) + 70);
            d.glowRgb = (r << 16) | (g << 8) | b;
            d.size = fit(e, look(e), 1.15f);
            d.squash = paintedRise((e.tickCount + partial) / 16f);
            d.glowLevel = 0.55f + 0.25f * (0.5f + 0.5f * Mth.sin((e.tickCount + partial) * 0.2f));
        }

        /** The ease-out-back curve of a body springing up (the old renderer's own). */
        static float[] paintedRise(float t) {
            if (t >= 1f) return null;
            float c = Math.max(0f, t), k = 1.70158f, u = c - 1f;
            float eased = 1f + (k + 1f) * u * u * u + k * u * u;
            float wide = 1f + 0.3f * (1f - c);
            return new float[]{wide, Math.max(0.04f, eased), wide};
        }
    }
}
