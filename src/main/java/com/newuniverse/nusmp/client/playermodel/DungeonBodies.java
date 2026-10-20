package com.newuniverse.nusmp.client.playermodel;

import com.newuniverse.nusmp.client.floor.goliath.GoliathPose;
import com.newuniverse.nusmp.client.labyrinth.foundry.FoundryIndex;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.corpse.CorpseDecay;
import com.newuniverse.nusmp.corpse.CorpseEffects;
import com.newuniverse.nusmp.corpse.NamedCorpseEntity;
import com.newuniverse.nusmp.dungeon.LivingArmorEntity;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathBoss;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import com.newuniverse.nusmp.labyrinth.mega.guard.AbyssalLooks;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.seal.FloorWardenEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: what each humanoid of the dungeon tells the kit ({@link PlayerBody}) in phase 2 of the player-model conversion (docs/playermodel_phase2.md), one small class each, read straight from what
 * the entity already says (its synced look, its phase, its identity, its decay stage, its cast countdown): the same behaviour as the renderers they stand beside, on the player model, so a
 * boss keeps its cast cue, its glow that brightens with the fight, its dust death, its rise out of the floor, and a mob keeps its tier glow and its wind-up. The look ids come from the pure
 * {@link DungeonLooks}; each body also says whether the kit draws a given entity ({@code kitDraws}): the registration ({@link DungeonRenderers}) routes the rest, and every entity when the
 * fallback flag is off, to the renderer it had before. Everything per frame is a map get: the foundry's look of an id is cached per body until the next resource reload
 * ({@link PlayerAssets#generation}), nothing builds a string.
 */
public final class DungeonBodies {
    private DungeonBodies() {}

    /** Ticks a cast cue of the Guardian shows the cast pose (the abyssal renderer's number). */
    public static final int CAST_TICKS = 24;

    private static float fitOver(LivingEntity e, PlayerLook look, float boost) {
        return PlayerBodies.fit(e, look, boost) / Math.max(0.0625f, e.getScale());     // the renderer has already applied the SCALE attribute: the body adds only what it still lacks
    }

    // ================================================================================================ the armoured bodies
    /**
     * The Living Armor, the Floor Warden (its body grown huge) and the Labyrinth Guardian. A Guardian with a look of a humanoid boss wears that boss's player-model look in the phase it has
     * reached ({@link DungeonLooks#guardian}: the loader of looks_abyssal.json), tinted and brightened as the old renderer did ({@link AbyssalLooks.Look#glow}, the glow level of the phase), with
     * the cast cue from the boss's synced cast time; a Guardian with no look is the Warden's body. The kit does not draw a Guardian whose look is a creature (a beholder, a dragon): those keep
     * their own model.
     */
    public static final class Armour implements PlayerBody<LivingArmorEntity> {
        private final Map<LivingEntity, Integer> lastDust = new WeakHashMap<>();

        /** The id of the look the kit draws this entity with, or "" when it is not drawn by the kit (a creature look). */
        static String lookId(LivingArmorEntity e) {
            if (e instanceof LabyrinthGuardianEntity g) return DungeonLooks.guardian(g.lookId(), g.lookPhase());
            return e instanceof FloorWardenEntity ? DungeonLooks.FLOOR_WARDEN : DungeonLooks.LIVING_ARMOR;
        }

        /** Does the kit draw this entity (it has a player-model look)? */
        public static boolean kitDraws(LivingArmorEntity e) {
            String id = lookId(e);
            return !id.isEmpty() && PlayerAssets.hasLook(id);
        }

        @Override
        public PlayerLook look(LivingArmorEntity e) {
            String id = lookId(e);
            return PlayerAssets.look(id.isEmpty() ? DungeonLooks.FLOOR_WARDEN : id);
        }

        private static AbyssalLooks.Look abyssal(LivingArmorEntity e) {
            if (!(e instanceof LabyrinthGuardianEntity g)) return null;
            AbyssalLooks.Look l = AbyssalLooks.get(g.lookId());
            return l != null && l.humanoid() ? l : null;
        }

        /** Progress 0..1 of the cast cue the Guardian synced (-1 when it is not casting). */
        private static float cast(LivingArmorEntity e, float partial) {
            if (!(e instanceof LabyrinthGuardianEntity g) || e.isDeadOrDying()) return -1f;
            long since = e.level().getGameTime() - g.castAt();
            return since >= 0 && since < CAST_TICKS ? Mth.clamp((since + partial) / CAST_TICKS, 0f, 1f) : -1f;
        }

        @Override
        public void dynamic(LivingArmorEntity e, float partial, Dynamic d) {
            PlayerLook look = look(e);
            AbyssalLooks.Look al = abyssal(e);
            boolean dead = e.isDeadOrDying();
            float age = e.tickCount + partial;
            float fade = dead ? ZogratisGlow.deathFade(e.deathTime + partial, 20f) : 1f;
            float cast = cast(e, partial);
            d.size = fitOver(e, look, al != null ? al.boost() : 1f);
            d.alpha = fade;
            if (cast >= 0f) {
                d.cast = ((LabyrinthGuardianEntity) e).castKind() == 2 ? 2 : 1;
                d.progress = cast;
            }
            if (al != null) {
                float level = Mth.clamp(al.glowLevel(((LabyrinthGuardianEntity) e).lookPhase()) * (cast >= 0f ? 1f : 0.86f + 0.14f * Mth.sin(age * 0.09f)), 0f, 1f);
                d.glowLevel = level * fade;
                d.glowRgb = al.glow();
                d.shadow = al.shadow() * (e.getBbHeight() / Math.max(0.2f, al.height()) * al.boost()) / Math.max(0.0625f, e.getScale());
            } else {
                d.glowLevel = (0.78f + 0.18f * Mth.sin(age * 0.08f)) * fade;
            }
        }

        @Override
        public void pose(LivingArmorEntity e, float partial, PoseState p) {
            float cast = cast(e, partial);
            if (cast >= 0f) PosePatterns.cast(p, cast, ((LabyrinthGuardianEntity) e).castKind() == 2, e.tickCount + partial);
        }

        /** The dust death of an undead look: ash and soul fire rise off the body while it dies, once per tick. */
        @Override
        public void decorate(LivingArmorEntity e, float partial, Decoration deco) {
            AbyssalLooks.Look al = abyssal(e);
            if (al == null || !al.dust() || !e.isDeadOrDying()) return;
            int t = e.deathTime;
            Integer last = lastDust.get(e);
            if (last != null && last == t) return;
            lastDust.put(e, t);
            var level = e.level();
            double h = e.getBbHeight(), w = Math.max(0.5, e.getBbWidth());
            for (int i = 0; i < 6; i++) {
                double x = e.getX() + (level.random.nextDouble() - 0.5) * w * 1.2, y = e.getY() + level.random.nextDouble() * h, z = e.getZ() + (level.random.nextDouble() - 0.5) * w * 1.2;
                level.addParticle(i % 3 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ASH, x, y, z, 0.0, 0.03, 0.0);
            }
        }
    }

    // ================================================================================================ the Mob Foundry's people
    /**
     * Every {@link FoundryMobEntity}: one entity type, the mob is a synced id, so the look is {@code foundry_<id>}, found once per id. A mob whose id has no look is a creature and the kit does
     * not draw it. The glow brightens with the tier and with the wind-up of an ability, a dying body fades as the old renderer's did, the wind-up is the cast pose, and the SCALE attribute that
     * made the hitbox the mob's height is divided out because the look's own scale already carries the height times the size.
     */
    public static final class Foundry implements PlayerBody<FoundryMobEntity> {
        private final Map<String, PlayerLook> cache = new HashMap<>();
        private int generation = -1;

        /** Does the kit draw this mob (its id has a look)? */
        public static boolean kitDraws(FoundryMobEntity e) {
            String id = e.foundryId();
            return !id.isEmpty() && PlayerAssets.hasLook(DungeonLooks.foundry(id));
        }

        @Override
        public PlayerLook look(FoundryMobEntity e) {
            int g = PlayerAssets.generation();
            if (g != generation) {
                cache.clear();
                generation = g;
            }
            String id = e.foundryId();
            PlayerLook l = cache.get(id);
            if (l == null) {
                l = PlayerAssets.look(id.isEmpty() ? SkinPath.DEFAULT_ID : DungeonLooks.foundry(id));
                cache.put(id, l);
            }
            return l;
        }

        @Override public boolean visible(FoundryMobEntity e) { return !e.foundryId().isEmpty(); }

        @Override
        public void dynamic(FoundryMobEntity e, float partial, Dynamic d) {
            d.size = 1f / Math.max(0.0625f, e.getScale());
            boolean casting = e.casting();
            float progress = casting ? 1f - e.castLeft() / (float) Math.max(1, e.castMax()) : 0f;
            FoundryIndex.Entry en = FoundryIndex.get(e.foundryId());
            int tier = en == null ? 1 : en.tier();
            float age = e.tickCount + partial;
            float fade = e.isDeadOrDying() ? ZogratisGlow.deathFade(e.deathTime + partial, 20f) : 1f;
            d.alpha = fade;
            d.glowLevel = Mth.clamp(0.50f + 0.06f * tier + 0.10f * Mth.sin(age * 0.07f) + (casting ? 0.25f * progress : 0f), 0f, 1f) * fade;
            d.cast = casting ? (e.castMax() >= 20 ? 2 : 1) : 0;
            d.progress = progress;
        }

        @Override
        public void pose(FoundryMobEntity e, float partial, PoseState p) {
            if (!e.casting() || e.isDeadOrDying()) return;
            PosePatterns.cast(p, 1f - e.castLeft() / (float) Math.max(1, e.castMax()), e.castMax() >= 20, e.tickCount + partial);
        }
    }

    // ================================================================================================ the soul-corpse puppets
    /**
     * Jimmy, Alfredo and Carl: the decay stage the Soul Corpse mark carries picks the skin ({@code corpse_<name>_s<stage>}); the glow pulses; Carl's throw is an arm pose (the right arm up and
     * back, then forward) paced by the countdown the entity syncs.
     */
    public static final class Corpse implements PlayerBody<NamedCorpseEntity> {
        private static int stageOf(NamedCorpseEntity e) {
            MobEffectInstance mark = e.getEffect(CorpseEffects.holder());
            return mark == null ? 0 : CorpseDecay.stageOf(mark.getAmplifier());
        }

        @Override public PlayerLook look(NamedCorpseEntity e) { return PlayerAssets.look(DungeonLooks.corpse(e.kind().id, stageOf(e))); }

        public static boolean kitDraws(NamedCorpseEntity e) { return PlayerAssets.hasLook(DungeonLooks.corpse(e.kind().id, 0)); }

        private static float throwProgress(NamedCorpseEntity e) {
            int c = e.castLeft();
            return c > 0 ? 1f - Math.min(c, NamedCorpseEntity.THROW_TICKS) / (float) NamedCorpseEntity.THROW_TICKS : -1f;
        }

        @Override
        public void dynamic(NamedCorpseEntity e, float partial, Dynamic d) {
            float age = e.tickCount + partial;
            float fade = e.isDeadOrDying() ? ZogratisGlow.deathFade(e.deathTime + partial, 20f) : 1f;
            d.alpha = fade;
            d.glowLevel = (0.65f + 0.25f * Mth.sin(age * 0.1f)) * fade;
            float t = throwProgress(e);
            d.cast = t >= 0f ? 1 : 0;
            d.progress = Math.max(0f, t);
        }

        @Override
        public void pose(NamedCorpseEntity e, float partial, PoseState p) {
            float t = throwProgress(e);
            if (t >= 0f && !e.isDeadOrDying()) DungeonPoses.throwArm(p, t, e.tickCount + partial);
        }
    }

    // ================================================================================================ the Goliath
    /**
     * The Goliath on the player model, drawn at the hitbox, the Gates' second look from the new form. It rises out of the floor (sunk by the eased rise value, no shadow until it stands, the
     * arms overhead while the fists break through) and each move has its pose ({@link DungeonPoses#goliath}); the glow, the fade, the flash of amber over the swap and the cast of a move come from
     * the shared boss body.
     */
    public static final class Goliath extends PlayerBodies.Boss<GoliathBoss> {
        public Goliath() { super(false, true, 0xFFAA33); }

        @Override protected String first(GoliathBoss e) { return DungeonLooks.GOLIATH; }

        @Override protected String second(GoliathBoss e) { return DungeonLooks.GOLIATH_FORM2; }

        public static boolean kitDraws(GoliathBoss e) { return PlayerAssets.hasLook(DungeonLooks.GOLIATH); }

        @Override
        public void dynamic(GoliathBoss e, float partial, Dynamic d) {
            super.dynamic(e, partial, d);
            float rise = e.rise(partial);
            if (rise < 1f) {
                d.lift = -(1f - rise) * (e.getBbHeight() + 0.4f);
                d.noShadow = true;
            }
        }

        @Override
        public void pose(GoliathBoss e, float partial, PoseState p) {
            if (e.isDeadOrDying()) return;
            float rise = e.rise(partial);
            if (rise < 1f) {
                DungeonPoses.rise(p, rise);
                return;
            }
            if (e.state() == ZogratisBoss.STATE_CASTING && !e.transforming()) {
                Move move = e.castingMoveOrNull();
                String clip = GoliathPose.clipOf(move == null ? null : move.id());
                if (clip != null && DungeonPoses.goliath(clip, p, e.phaseProgress(), e.tickCount + partial)) return;
            }
            super.pose(e, partial, p);
        }
    }
}
