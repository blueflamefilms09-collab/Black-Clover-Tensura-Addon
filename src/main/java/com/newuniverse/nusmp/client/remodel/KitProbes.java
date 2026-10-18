package com.newuniverse.nusmp.client.remodel;

import com.newuniverse.nusmp.book.PaintStudio;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.dungeon.DungeonCreature;
import com.newuniverse.nusmp.dungeon.LivingArmorEntity;
import com.newuniverse.nusmp.entity.CottonSheepEntity;
import com.newuniverse.nusmp.entity.ElementalSpiritLordEntity;
import com.newuniverse.nusmp.entity.GrimoireDaemonEntity;
import com.newuniverse.nusmp.entity.PaintedConstructEntity;
import com.newuniverse.nusmp.entity.SpiritLordEntity;
import com.newuniverse.nusmp.entity.WindSpiritLordEntity;
import com.newuniverse.nusmp.entity.ZagredBossEntity;
import com.newuniverse.nusmp.entity.zogratis.BloodBeastEntity;
import com.newuniverse.nusmp.entity.zogratis.DarkDiscipleEntity;
import com.newuniverse.nusmp.entity.zogratis.VanicaMath;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisCloneEntity;
import com.newuniverse.nusmp.labyrinth.seal.FloorWardenEntity;
import com.newuniverse.nusmp.thread.RougeEntity;
import com.newuniverse.nusmp.thread.RougePerch;
import com.newuniverse.nusmp.wizard.TowerWizardEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Why: what each remodelled mob tells the generic renderer ({@link KitMobRenderer.Probe}), one small class each, read straight from what the entity already says (its synced state, its kind,
 * its tier, its role, its swing): which model and skin it wears, whether it is casting, asleep, rising, sitting, how it is tinted and scaled. Nothing here changes a mob's behaviour. The art kit's clips
 * have fixed names (idle walk run attack cast cast_big hurt transform death, optionally spawn, sleep, sit), so a state the old model had its own clip for is mapped to the nearest one: the Blood
 * Beast's crouch and leap are the first and the hit moment of the {@code attack} clip, a rising body is {@code spawn}, a sleeping predator {@code sleep}, a sitting Rouge {@code sit}.
 * See docs/arc/remodel.md for each mob.
 */
final class KitProbes {
    private KitProbes() {}

    /** The seconds into the kit's {@code attack} clip at which the blow lands (0.34 to 0.42 for every style; the clock starts it at 0.2). */
    static final float HIT_SECONDS = 0.36f, WIND_UP_SECONDS = 0.2f;

    private static float fraction(float value, float of) { return of <= 0f ? 0f : Mth.clamp(value / of, 0f, 1f); }

    // ---------------------------------------------------------------------------------------------- the Dark Disciple
    /** The devil of its master tints it (Lucifero violet, Megicula crimson, Beelzebub indigo, Astaroth gold, any other the neutral grey); its glow gathers over the telegraph of its ability. */
    static final KitMobRenderer.Probe<DarkDiscipleEntity> DISCIPLE = new KitMobRenderer.Probe<>() {
        private static final float LONGEST_WIND_UP = 26f;

        @Override public String model(DarkDiscipleEntity e) { return "dark_disciple"; }

        @Override
        public String skin(DarkDiscipleEntity e) {
            return switch (e.devil()) {
                case LUCIFERO -> "lucifero";
                case MEGICULA -> "megicula";
                case BEELZEBUB -> "beelzebub";
                case ASTAROTH -> "astaroth";
                default -> null;
            };
        }

        @Override
        public void fill(DarkDiscipleEntity e, ZogratisPose.Frame f, float partial) {
            int wind = e.castLeft();
            f.cast = wind > 0 ? 1 : 0;
            f.castProgress = wind > 0 ? 1f - Math.min(wind, LONGEST_WIND_UP) / LONGEST_WIND_UP : 0f;
        }
    };

    // ---------------------------------------------------------------------------------------------- the Blood Beast
    /**
     * It rises out of a puddle (spawn), crouches over the telegraph of its pounce (the wind-up of the attack clip), hangs open-jawed in the leap (the attack's hit pose), bites (attack) and melts when
     * it dies (death, faded). The more of Megicula's power its master had drawn, the bigger and brighter it is.
     */
    static final KitMobRenderer.Probe<BloodBeastEntity> BLOOD_BEAST = new KitMobRenderer.Probe<>() {
        private static final float CROUCH_TICKS = 10f;
        private static final int STAMPEDE_RISE = 12;

        @Override public String model(BloodBeastEntity e) { return "blood_beast"; }

        @Override
        public float size(BloodBeastEntity e, float partial) {
            int fill = Math.max(0, Math.min(100, e.fillPercent()));
            return (0.92f + 0.16f * fill / 100f) * (e.stampede() ? 0.9f : 1f);
        }

        @Override
        public boolean pick(BloodBeastEntity e, float partial, ZogratisPose clock, ZogratisPose.Frame f, ZogratisPose.Clips clips, KitMobRenderer.Pick out) {
            int rise = e.riseLeft(), crouch = e.pounceLeft();
            if (rise > 0 && clips.length(ZogratisPose.SPAWN) > 0f) {
                float total = Math.max(rise, e.stampede() ? STAMPEDE_RISE : VanicaMath.SPAWN_TICKS);
                float left = Math.max(0f, rise - partial);                     // the counters tick down once per tick: the partial tick keeps the clip smooth
                out.clip = ZogratisPose.SPAWN;
                out.seconds = (1f - left / total) * clips.length(ZogratisPose.SPAWN);
                return true;
            }
            if (crouch > 0) {
                float left = Math.max(0f, Math.min(crouch, CROUCH_TICKS) - partial);
                out.clip = ZogratisPose.ATTACK;
                out.seconds = (1f - left / CROUCH_TICKS) * WIND_UP_SECONDS;
                return true;
            }
            if (e.leaping()) {
                out.clip = ZogratisPose.ATTACK;                                // in the air of the leap: the body stretched at the blow
                out.seconds = HIT_SECONDS;
                return true;
            }
            return false;
        }

        @Override
        public float glowLevel(BloodBeastEntity e, ZogratisPose.Frame f) {
            float base = Math.max(0.2f, e.fillPercent() / 100f);
            return Math.min(1f, 0.55f + 0.45f * base + (e.pounceLeft() > 0 ? 0.3f : 0f) + 0.08f * Mth.sin(f.age * 0.2f));
        }
    };

    // ---------------------------------------------------------------------------------------------- the clones of Lucius
    /** A clone of Lucius: his body of the form he had when it was made (the divine one floats a little), gold when the Swap Strike has marked it, fading out over its last two seconds. */
    static final KitMobRenderer.Probe<ZogratisCloneEntity> CLONE = new KitMobRenderer.Probe<>() {
        private static final float TELL_TICKS = 14f;
        private static final int GOLD_FLASH_RGB = 0xFFD24A;

        private boolean divine(ZogratisCloneEntity e) { return e.form() == ZogratisBoss.FORM_DIVINE; }

        @Override public String model(ZogratisCloneEntity e) { return divine(e) ? "lucius_zogratis_form2" : "lucius_zogratis"; }

        @Override public float lift(ZogratisCloneEntity e, float partial) { return divine(e) ? 0.12f + 0.05f * Mth.sin((e.tickCount + partial) * 0.12f) : 0f; }

        @Override
        public float alpha(ZogratisCloneEntity e, float partial) { return e.fade(); }

        @Override
        public int tint(ZogratisCloneEntity e) {
            if (e.markLeft() <= 0) return 0xFFFFFF;
            float k = 0.5f + 0.5f * Mth.sin((e.tickCount) * 0.7f);
            return KitMath.mix(0xFFFFFF, GOLD_FLASH_RGB, k);
        }

        @Override
        public void fill(ZogratisCloneEntity e, ZogratisPose.Frame f, float partial) {
            int tell = e.castLeft();
            f.cast = tell > 0 ? 1 : 0;
            f.castProgress = tell > 0 ? 1f - Math.min(tell, TELL_TICKS) / TELL_TICKS : 0f;
        }

        @Override
        public float glowLevel(ZogratisCloneEntity e, ZogratisPose.Frame f) { return e.markLeft() > 0 ? 0.9f : (f.cast > 0 ? 0.95f : 0.6f + 0.1f * Mth.sin(f.age * 0.1f)); }
    };

    // ---------------------------------------------------------------------------------------------- the dungeon's creatures
    /** A creature of the Living Dungeon that sleeps when it has eaten (the Direwolf, the Basilisk): the kit model plays its {@code sleep} clip while the entity says it is asleep. */
    static final class Sleeper<T extends DungeonCreature> implements KitMobRenderer.Probe<T> {
        private final String model;

        Sleeper(String model) { this.model = model; }

        @Override public String model(T e) { return model; }

        @Override
        public boolean pick(T e, float partial, ZogratisPose clock, ZogratisPose.Frame f, ZogratisPose.Clips clips, KitMobRenderer.Pick out) {
            if (!e.asleep() || clips.length("sleep") <= 0f) return false;
            out.clip = "sleep";
            out.seconds = f.age / 20f;
            return true;
        }

        @Override
        public float glowLevel(T e, ZogratisPose.Frame f) { return (e.isAggressive() ? 0.9f : e.asleep() ? 0.3f : 0.72f) + 0.15f * Mth.sin(f.age * 0.17f); }
    }

    /** The slime child draws translucent, like the slime's outer layer did. */
    static final KitMobRenderer.Probe<com.newuniverse.nusmp.dungeon.DungeonSlimeEntity> SLIME = new KitMobRenderer.Probe<>() {
        @Override public String model(com.newuniverse.nusmp.dungeon.DungeonSlimeEntity e) { return "dungeon_slime"; }

        @Override public float alpha(com.newuniverse.nusmp.dungeon.DungeonSlimeEntity e, float partial) { return 0.84f; }
    };

    /** The Living Armor, the Floor Warden (its Living Armor body grown huge) and the Labyrinth Guardian (the Warden grown colossal): the Warden's model for both bosses, scaled by the hitbox. */
    static final KitMobRenderer.Probe<LivingArmorEntity> ARMOUR = new KitMobRenderer.Probe<>() {
        @Override public String model(LivingArmorEntity e) { return e instanceof FloorWardenEntity ? "floor_warden" : "living_armor"; }
    };

    // ---------------------------------------------------------------------------------------------- the spirits
    private static String spiritModel(String kind) {
        return switch (kind) {
            case "UNDINE" -> "spirit_undine";
            case "SYLPH" -> "spirit_sylph";
            case "GNOME" -> "spirit_gnome";
            case "BLACK_DEVIL" -> "spirit_black_devil";
            default -> "spirit_salamander";
        };
    }

    /** How long a spirit's cast pose lasts after the cast event, in ticks. */
    static final int CAST_TICKS = 16;

    /** The companion Spirit Lord: its kind picks the body; it casts when the server says so (an entity event the client keeps the tick of). */
    static final KitMobRenderer.Probe<SpiritLordEntity> SPIRIT_LORD = new KitMobRenderer.Probe<>() {
        @Override public String model(SpiritLordEntity e) { return spiritModel(e.kind().name()); }

        @Override public float alpha(SpiritLordEntity e, float partial) { return e.kind() == SpiritLordEntity.Kind.UNDINE ? 0.88f : 1f; }

        @Override
        public void fill(SpiritLordEntity e, ZogratisPose.Frame f, float partial) {
            int age = e.tickCount - e.clientCastTick;
            f.cast = age >= 0 && age < CAST_TICKS ? 1 : 0;
            f.castProgress = f.cast > 0 ? age / (float) CAST_TICKS : 0f;
        }
    };

    /** The four elemental Spirit Lords of Black Clover in their true bodies. */
    static final KitMobRenderer.Probe<ElementalSpiritLordEntity> ELEMENTAL = new KitMobRenderer.Probe<>() {
        @Override public String model(ElementalSpiritLordEntity e) { return spiritModel(e.kind().name()); }

        @Override public float alpha(ElementalSpiritLordEntity e, float partial) { return e.kind() == ElementalSpiritLordEntity.Kind.UNDINE ? 0.88f : 1f; }

        @Override
        public void fill(ElementalSpiritLordEntity e, ZogratisPose.Frame f, float partial) {
            int age = e.tickCount - e.clientCastTick;
            f.cast = age >= 0 && age < CAST_TICKS ? 1 : 0;
            f.castProgress = f.cast > 0 ? age / (float) CAST_TICKS : 0f;
        }
    };

    /** The Wind Spirit Lord boss wears Sylph's body, drawn larger by its hitbox. */
    static final KitMobRenderer.Probe<WindSpiritLordEntity> WIND = new KitMobRenderer.Probe<>() {
        @Override public String model(WindSpiritLordEntity e) { return "spirit_sylph"; }
    };

    // ---------------------------------------------------------------------------------------------- Zagred's daemons
    /** One entity, four tiers: the tier picks the body (lesser, greater, arch, the rule stone); a grimoire that is being used makes the daemon cast. */
    static final KitMobRenderer.Probe<GrimoireDaemonEntity> DAEMON = new KitMobRenderer.Probe<>() {
        @Override
        public String model(GrimoireDaemonEntity e) {
            return switch (e.tier()) {
                case GrimoireDaemonEntity.GREATER -> "grimoire_daemon_greater";
                case GrimoireDaemonEntity.ARCH -> "grimoire_daemon_arch";
                case GrimoireDaemonEntity.STONE -> "grimoire_daemon_stone";
                default -> "grimoire_daemon";
            };
        }

        @Override
        public void fill(GrimoireDaemonEntity e, ZogratisPose.Frame f, float partial) {
            f.cast = e.castLeft() > 0 ? 1 : 0;
            f.castProgress = f.cast > 0 ? 1f : 0f;
        }
    };

    // ---------------------------------------------------------------------------------------------- Painting Magic
    /** A living illustration: the kind picks the body, the paint tints it (the body is painted once in pale paint and the tint multiplies it), and it rises from flat to full height over 16 ticks. */
    static final KitMobRenderer.Probe<PaintedConstructEntity> PAINTED = new KitMobRenderer.Probe<>() {
        @Override
        public String model(PaintedConstructEntity e) {
            return switch (e.kind()) {
                case KNIGHT -> "painted_knight";
                case GIANT -> "painted_giant";
                default -> "painted_beast";
            };
        }

        @Override public int tint(PaintedConstructEntity e) { return e.paint().color & 0xFFFFFF; }

        @Override
        public int glowRgb(PaintedConstructEntity e) {
            int rgb = e.paint().color & 0xFFFFFF;
            int r = Math.min(255, ((rgb >> 16) & 255) + 70), g = Math.min(255, ((rgb >> 8) & 255) + 70), b = Math.min(255, (rgb & 255) + 70);
            return (r << 16) | (g << 8) | b;
        }

        /** Rising off the canvas: flat and wide at first, springing up (ease-out-back) over 16 ticks. */
        @Override
        public float[] squash(PaintedConstructEntity e, float partial) {
            return KitMath.paintedRise((e.tickCount + partial) / 16f);
        }

        @Override public float glowLevel(PaintedConstructEntity e, ZogratisPose.Frame f) { return 0.55f + 0.25f * (0.5f + 0.5f * Mth.sin(f.age * 0.2f)); }
    };

    // ---------------------------------------------------------------------------------------------- Rouge
    /**
     * Rouge, the cat of red thread: sitting (the {@code sit} clip), scurrying and hopping (walk and run by the ground covered), leaping and tapping (the attack's wind-up and its blow), unravelling
     * (shrinking and fading) and re-knitting (the {@code spawn} clip, growing back with a little overshoot). Settled on her summoner's head she is drawn by the head layer, not here.
     */
    static final KitMobRenderer.Probe<RougeEntity> ROUGE = new KitMobRenderer.Probe<>() {
        @Override public String model(RougeEntity e) { return "rouge"; }

        @Override public boolean visible(RougeEntity e) { return RougePerch.bodyDrawn(e.state() == RougeEntity.HEAD, e.stateAge()); }

        @Override
        public float size(RougeEntity e, float partial) {
            float t = e.stateAge() + partial;
            switch (e.state()) {
                case RougeEntity.UNRAVEL -> { return KitMath.unravelScale(t / RougeEntity.UNRAVEL_TICKS); }
                case RougeEntity.REKNIT -> { return KitMath.reknitScale(t / RougeEntity.REKNIT_TICKS); }
                case RougeEntity.HOP -> { return 0.85f; }
                default -> { return 1f; }
            }
        }

        @Override
        public float alpha(RougeEntity e, float partial) {
            float t = e.stateAge() + partial;
            if (e.state() == RougeEntity.UNRAVEL) return 1f - Mth.clamp(t / RougeEntity.UNRAVEL_TICKS, 0f, 1f);
            if (e.state() == RougeEntity.REKNIT) return Math.min(1f, 0.3f + Mth.clamp(t / RougeEntity.REKNIT_TICKS, 0f, 1f));
            return 1f;
        }

        @Override
        public boolean pick(RougeEntity e, float partial, ZogratisPose clock, ZogratisPose.Frame f, ZogratisPose.Clips clips, KitMobRenderer.Pick out) {
            float t = e.stateAge() + partial;
            switch (e.state()) {
                case RougeEntity.SIT, RougeEntity.HEAD -> {
                    if (clips.length("sit") <= 0f) return false;
                    out.clip = "sit";
                    out.seconds = f.age / 20f;
                    return true;
                }
                case RougeEntity.LEAP -> { out.clip = ZogratisPose.ATTACK; out.seconds = HIT_SECONDS; return true; }
                case RougeEntity.TAP -> {
                    out.clip = ZogratisPose.ATTACK;
                    out.seconds = WIND_UP_SECONDS + fraction(t, RougeEntity.TAP_TICKS) * (HIT_SECONDS + 0.1f - WIND_UP_SECONDS);
                    return true;
                }
                case RougeEntity.REKNIT -> {
                    if (clips.length(ZogratisPose.SPAWN) <= 0f) return false;
                    out.clip = ZogratisPose.SPAWN;
                    out.seconds = fraction(t, RougeEntity.REKNIT_TICKS) * clips.length(ZogratisPose.SPAWN);
                    return true;
                }
                default -> { return false; }
            }
        }

        @Override public float glowLevel(RougeEntity e, ZogratisPose.Frame f) { return 0.45f + 0.15f * Mth.sin(f.age * 0.13f); }
    };

    // ---------------------------------------------------------------------------------------------- the Tower Wizard
    /** The keeper of a Grimoire Tower stands still and gestures (the kit's NPC {@code cast} clip is its talking) when a player stands within 3.2 blocks. */
    static final KitMobRenderer.Probe<TowerWizardEntity> WIZARD = new KitMobRenderer.Probe<>() {
        private static final double TALK_RANGE = 3.2;

        @Override public String model(TowerWizardEntity e) { return "tower_wizard"; }

        @Override
        public void fill(TowerWizardEntity e, ZogratisPose.Frame f, float partial) {
            LocalPlayer p = Minecraft.getInstance().player;
            boolean near = p != null && !e.isDeadOrDying() && e.distanceToSqr(p) <= TALK_RANGE * TALK_RANGE && f.rawSpeed < ZogratisPose.WALK_ON;
            f.cast = near ? 1 : 0;
            f.castProgress = near ? 1f : 0f;
        }
    };

    // ---------------------------------------------------------------------------------------------- Charmy's cotton sheep
    /**
     * The sheep of Cotton Magic: the role picks the body (the cook wears the chef's hat), a leap hangs at the blow of the attack clip, a hug is the cast pose, and a cook flips its pan with the
     * attack clip again and again.
     */
    static final KitMobRenderer.Probe<CottonSheepEntity> SHEEP = new KitMobRenderer.Probe<>() {
        @Override public String model(CottonSheepEntity e) { return e.role() == CottonSheepEntity.COOK ? "cotton_sheep_cook" : "cotton_sheep"; }

        @Override
        public void fill(CottonSheepEntity e, ZogratisPose.Frame f, float partial) {
            f.cast = e.anim() == CottonSheepEntity.HUG ? 1 : 0;
            f.castProgress = f.cast > 0 ? 1f : 0f;
        }

        @Override
        public boolean pick(CottonSheepEntity e, float partial, ZogratisPose clock, ZogratisPose.Frame f, ZogratisPose.Clips clips, KitMobRenderer.Pick out) {
            float len = clips.length(ZogratisPose.ATTACK);
            if (len <= 0f) return false;
            if (e.anim() == CottonSheepEntity.LEAP) { out.clip = ZogratisPose.ATTACK; out.seconds = HIT_SECONDS; return true; }
            if (e.anim() == CottonSheepEntity.COOKING) {
                out.clip = ZogratisPose.ATTACK;
                out.seconds = WIND_UP_SECONDS + (f.age / 20f) % (len - WIND_UP_SECONDS);
                return true;
            }
            return false;
        }
    };

    // ---------------------------------------------------------------------------------------------- the legacy Zagred
    /** The first Zagred boss: its model from the fourth phase on is the one with the trident in its hand; it casts while it speaks a word (the old synced state). */
    static final KitMobRenderer.Probe<ZagredBossEntity> ZAGRED = new KitMobRenderer.Probe<>() {
        @Override public String model(ZagredBossEntity e) { return e.phase() >= 4 ? "zagred_legacy_form2" : "zagred_legacy"; }

        @Override
        public void fill(ZagredBossEntity e, ZogratisPose.Frame f, float partial) {
            boolean casting = e.clientState() == ZagredBossEntity.STATE_CASTING;
            f.cast = casting ? 1 : 0;
            f.castProgress = casting ? 1f : 0f;
        }

        @Override public float glowLevel(ZagredBossEntity e, ZogratisPose.Frame f) { return 0.65f + 0.35f * Mth.sin(f.age * (0.08f + 0.04f * e.phase())); }
    };
}
