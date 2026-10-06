package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.book.SpiritBond;
import com.newuniverse.nusmp.book.SpiritSlots;
import com.newuniverse.nusmp.entity.SpiritLordEntity;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Spirit Lord (Unique): the path for mages without a grimoire. Granted by the Spirit Lord Skill item (or /multiverse spirit bond);
 * bonds you to one elemental lord (Salamander, Undine, Sylph, Gnome; one mage per spirit on the server) or to the Anti-Magic lord.
 * <ul>
 *   <li>Passives: physical damage reduced (30% by default), immune to falls.</li>
 *   <li>Mode 0, Spirit Channeling: hold to fill a 0-100% gauge, an aura growing round you and pulling enemies in. Release at
 *       25% for Overdrive (a spirit dash), 50% for Nova (a burst), 100% for Cataclysm (six seconds as a living conduit).</li>
 *   <li>Mode 1, Call Spirit: once the spirit trusts you fully and has a True Name (incarnation), call it to your side (a small
 *       floating orb) or send it back.</li>
 * </ul>
 * Trust, traits and incarnation are the existing spirit bond system (book.SpiritBond).
 */
public class SpiritLordSkill extends Skill {
    public static final String[] TYPES = {"Salamander", "Undine", "Sylph", "Gnome", "Anti"};
    private static final int CHANNEL = 0, CALL = 1;

    public SpiritLordSkill() { super(SkillType.UNIQUE); }

    // ---------------------------------------------------------------- access
    public static Optional<ManasSkillInstance> instance(Player p) {
        return SkillAPI.getSkillsFrom(p).getSkill(NUSkills.SPIRIT_LORD.getId());
    }

    public static String bondedType(Player p) {
        Optional<ManasSkillInstance> i = instance(p);
        if (i.isPresent()) return i.get().getOrCreateTag().getString("Spirit");
        String kind = com.newuniverse.nusmp.book.ForbiddenMagic.data(p).getString("nusmp_spirit_kind");
        return kind.isEmpty() ? "-" : kind;
    }

    /** Current channel gauge (0-100); shown as the spirit's energy. */
    public static int energy(Player p) { return instance(p).map(i -> i.getOrCreateTag().getInt("Gauge")).orElse(0); }

    public static String normalize(String type) {
        for (String t : TYPES) if (t.equalsIgnoreCase(type) || (t.equals("Anti") && type.toLowerCase().startsWith("anti"))) return t;
        return null;
    }

    /**
     * Bonds the player to a spirit lord and teaches this skill. type null = the first elemental lord nobody holds yet.
     * Returns a reason on failure, or null on success.
     */
    public static String bond(ServerPlayer p, String type) {
        if (instance(p).isPresent()) return "You are already bound to a spirit lord.";
        SpiritSlots slots = SpiritSlots.get(p.getServer());
        if (type == null) {
            for (int k = 0; k < 4 && type == null; k++) if (slots.owner(TYPES[k]) == null) type = TYPES[k];
            if (type == null) return "Every elemental spirit lord already has a mage.";
        }
        type = normalize(type);
        if (type == null) return "Unknown spirit (Salamander, Undine, Sylph, Gnome or Anti).";
        if (!type.equals("Anti") && !slots.claim(type, p.getUUID())) return type + " has already chosen another mage.";
        ManasSkillInstance inst = NUSkills.SPIRIT_LORD.get().createDefaultInstance();
        inst.getOrCreateTag().putString("Spirit", type);
        if (!SkillAPI.getSkillsFrom(p).learnSkill(inst, Component.literal("A Spirit Lord answers you: " + type + ".").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))) {
            if (!type.equals("Anti")) slots.release(type);
            return "The spirit could not bind to you (another mod refused the skill).";
        }
        if (!type.equals("Anti")) SpiritBond.bond(p, type);
        MultiverseSync.markDirty(p);
        return null;
    }

    private static String type(ManasSkillInstance i) { String t = i.getOrCreateTag().getString("Spirit"); return t.isEmpty() ? "Salamander" : t; }

    private static boolean anti(ManasSkillInstance i) { return "Anti".equals(type(i)); }

    /** 0 fire, 1 water, 2 wind, 3 earth: the colour variant the spirit effects use. */
    private static int variant(ManasSkillInstance i) {
        return switch (type(i)) { case "Undine" -> 1; case "Sylph" -> 2; case "Gnome" -> 3; default -> 0; };
    }

    private static SpiritLordEntity.Kind kind(ManasSkillInstance i) { return anti(i) ? SpiritLordEntity.Kind.BLACK_DEVIL : SpiritLordEntity.Kind.forSpirit(type(i)); }

    // ---------------------------------------------------------------- skill plumbing
    @Override public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/storm.png");
    }
    @Override public int getModes(ManasSkillInstance instance) { return 2; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return mode == CHANNEL ? CALL : CHANNEL; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == CHANNEL ? "spirit_channeling" : "call_spirit"; }
    @Override public Component getModeName(ManasSkillInstance i, int mode) {
        return Component.literal(mode == CHANNEL ? "Spirit Channeling" : "Call " + (anti(i) ? "the Anti-Magic Lord" : type(i)));
    }
    @Override public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) { return 0; }
    @Override public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) { return 400; }
    @Override public boolean canTick(ManasSkillInstance i, LivingEntity e) { return true; }

    @Override
    public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
        if (mode != CALL || !(e instanceof ServerPlayer p)) return;
        if (!anti(i) && !SpiritBond.incarnate(p)) {
            p.displayClientMessage(Component.literal(type(i) + " has no body yet. Earn its full trust and give it a True Name (Spirit Charm).")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        SpiritLordEntity.toggle(p, kind(i));
    }

    @Override
    public boolean onHeld(ManasSkillInstance i, LivingEntity e, int heldTicks, int mode) {
        if (mode != CHANNEL || !(e instanceof ServerPlayer p)) return true;
        if (!SpiritBond.willAnswer(p)) {
            if (heldTicks == 1) p.displayClientMessage(Component.literal("Your spirit will not answer you.").withStyle(ChatFormatting.RED), true);
            return true;
        }
        int gauge = Math.min(100, heldTicks);
        i.getOrCreateTag().putInt("Gauge", gauge);
        if (heldTicks % 5 == 0) p.displayClientMessage(Component.literal("Spirit Channeling " + gauge + "%"
                + (gauge >= 100 ? "  CATACLYSM" : gauge >= 50 ? "  Nova" : gauge >= 25 ? "  Overdrive" : "")).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        if (heldTicks % 10 == 0) {
            double r = 2 + 6 * gauge / 100.0;        // the aura grows and drags enemies in
            for (LivingEntity t : enemies(p, p.position(), r)) {
                t.setDeltaMovement(p.position().subtract(t.position()).normalize().scale(0.25));
                t.hurtMarked = true;
                hit(p, t, 1.5f);
            }
            fx(p, i, anti(i) ? VfxShape.SPIRIT_AURA : VfxShape.FX_SPIRIT_AURA, p.position(), p.position(), (float) (0.6 + gauge / 100.0), true);
        }
        MasteryPages.onChanneling(p, i, heldTicks);
        return true;
    }

    @Override
    public void onRelease(ManasSkillInstance i, LivingEntity e, int heldTicks, int keyNumber, int mode) {
        if (mode != CHANNEL || !(e instanceof ServerPlayer p)) return;
        int gauge = Math.min(100, heldTicks);
        i.getOrCreateTag().putInt("Gauge", 0);
        MultiverseSync.markDirty(p);
        if (!SpiritBond.willAnswer(p) || gauge < 25) { if (gauge < 25) p.displayClientMessage(Component.literal("The channel fades.").withStyle(ChatFormatting.GRAY), true); return; }
        if (i.onCoolDown(CHANNEL)) { p.displayClientMessage(Component.literal("Your spirit is still gathering itself (" + i.getCoolDown(CHANNEL) + "s).").withStyle(ChatFormatting.RED), true); return; }
        Vec3 look = p.getViewVector(1f);
        if (gauge >= 100) {                                        // Cataclysm: six seconds as a living conduit
            i.getOrCreateTag().putLong("CataclysmUntil", p.level().getGameTime() + 120);
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1));
            fx(p, i, anti(i) ? VfxShape.SPIRIT_AURA : VfxShape.FX_SPIRIT_CATACLYSM, p.position(), p.position(), 1.5f, true);
            i.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(600), CHANNEL);
            SpiritBond.heavyUse(p);
        } else if (gauge >= 50) {                                  // Nova: a burst round you
            for (LivingEntity t : enemies(p, p.position(), 6)) {
                hit(p, t, 12f);
                Vec3 away = t.position().subtract(p.position()).normalize();
                t.knockback(1.6, -away.x, -away.z);
            }
            fx(p, i, anti(i) ? VfxShape.ANTI_MAGIC_SLASH : VfxShape.FX_SPIRIT_NOVA, p.position(), p.position().add(0, 1, 0), 1.2f, false);
            i.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(240), CHANNEL);
            SpiritBond.heavyUse(p);
        } else {                                                   // Overdrive: a spirit dash
            Vec3 from = p.position(), to = from.add(look.multiply(1, 0.3, 1).normalize().scale(8));
            for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.5), x -> x != p && x.isAlive() && !x.isAlliedTo(p))) hit(p, t, 8f);
            p.setDeltaMovement(look.scale(2.2));
            p.hurtMarked = true;
            p.fallDistance = 0;
            fx(p, i, anti(i) ? VfxShape.ANTI_MAGIC_SLASH : VfxShape.FX_SPIRIT_OVERDRIVE, from.add(0, 1, 0), to.add(0, 1, 0), 1f, false);
            i.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(80), CHANNEL);
        }
        addMasteryPoint(i, e);
        i.markDirty();
    }

    @Override
    public void onTick(ManasSkillInstance i, LivingEntity e) {
        if (!(e instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        if (p.level().getGameTime() < i.getOrCreateTag().getLong("CataclysmUntil")) {
            for (LivingEntity t : enemies(p, p.position(), 6)) hit(p, t, 3f);
        }
    }

    /** Spirit body: physical blows lose 30%, falls do nothing; a living conduit also shrugs off the elements. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (source.is(DamageTypeTags.IS_FALL) && MultiverseConfig.get(MultiverseConfig.SPIRIT_FALL_IMMUNE)) { amount.set(0f); return true; }
        if (!AntiMagic.isMagic(source) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            amount.set(amount.get() * (1f - MultiverseConfig.get(MultiverseConfig.SPIRIT_PHYSICAL_REDUCTION).floatValue()));
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong("CataclysmUntil")
                && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FREEZING))) amount.set(0f);
        return true;
    }

    // ---------------------------------------------------------------- helpers
    private static List<LivingEntity> enemies(ServerPlayer p, Vec3 c, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                x -> x != p && x.isAlive() && !x.isAlliedTo(p) && x.distanceToSqr(c) <= r * r);
    }

    private static void hit(ServerPlayer p, LivingEntity t, float amount) { t.hurt(p.damageSources().indirectMagic(p, p), amount); }

    private static void fx(ServerPlayer p, ManasSkillInstance i, VfxShape shape, Vec3 from, Vec3 to, float power, boolean follow) {
        int colour = anti(i) ? 0xFF1A1018 : variant(i);            // spirit (FX_) effects read the colour field as the element 0-3
        VfxSpawn.send(p.serverLevel(), new VfxPayload(shape.ordinal(), from, to, colour, 0, power, follow ? p.getId() : -1, p.getRandom().nextLong()));
    }
}
