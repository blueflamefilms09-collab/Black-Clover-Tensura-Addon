package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.ability.magic.spiritual.SpiritualMagic;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A grimoire = a Tensura UNIQUE skill (gold name, Unique list). The book item is only the key.
 * Hold the Tensura skill key to chant (20 ticks, 8 when mastered), release to cast. Cost goes
 * through getMagiculeCost + EnergyHelper.isOutOfEnergy, damage through createSource with Tensura
 * damage types, cooldowns through instance.setCoolDown(cooldown, mode) only after a successful cast.
 *
 * Modes: the family's pages (mode 0 unlocked, the rest open by mastery: multiverse.MasteryPages), then Spirit Dive
 * (element books, Spirit Lord gate) and Devil Union (five-leaf / triple-spade covers only).
 */
public abstract class GrimoireBook extends Skill {
    public static final int CAST_TICKS = 20, CAST_TICKS_MASTERED = 8;
    public final MagicType magic;
    public final int color;
    private final ResourceLocation icon;
    private List<BookPage> all;

    protected GrimoireBook(MagicType magic, int color) {
        this(magic, color, ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/" + magic.name().toLowerCase() + ".png"));
    }

    protected GrimoireBook(MagicType magic, int color, ResourceLocation icon) {
        super(Skill.SkillType.UNIQUE);
        this.magic = magic;
        this.color = color;
        this.icon = icon;
    }

    /** 0.47: a book of another Tensura skill class (Kotodama is God-class: ULTIMATE). */
    protected GrimoireBook(MagicType magic, int color, Skill.SkillType type) {
        super(type);
        this.magic = magic;
        this.color = color;
        this.icon = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/" + magic.name().toLowerCase() + ".png");
    }

    /** The family's own pages, mode order (mode 0 = starter). */
    protected abstract List<BookPage> familyPages();

    /** Tensura damage type for this book. */
    public abstract ResourceKey<DamageType> damageType();

    /** Element for Spirit Dive, or null if this book has no spirit. */
    public Element spiritElement() { return null; }
    public String spiritName() { return null; }

    // ---------------------------------------------------------------- pages / modes
    public final List<BookPage> pages() {
        if (all == null) {
            List<BookPage> l = new ArrayList<>(familyPages());
            if (spiritElement() != null) {
                l.add(BookPage.signature("spirit_dive", "Spirit Dive", GrimoireBook::spiritDive).withCooldown(0));
                l.add(BookPage.signature("spirit_channeling", "Spirit Channeling", GrimoireBook::channel));
            }
            if (!(this instanceof ForbiddenBook)) l.add(BookPage.signature("devil_union", "Devil Union", GrimoireBook::devilUnion));
            // base ability, last so every existing page keeps its mode number: free, instant, no chant
            if (!(this instanceof ForbiddenBook)) l.add(new BookPage(SUMMON_ID, "Summon Grimoire", "Summon Grimoire", 0, 0, 0, (b, i, p, m) -> true));
            if (!(this instanceof ForbiddenBook)) {                                 // 0.54: as in the show, a weapon goes back into the book and comes out again
                l.add(new BookPage(STORE_ID, "Store Weapon", "Store Weapon", 0, 0, 0, (b, i, p, m) -> true));
                l.add(new BookPage(DRAW_ID, "Draw Weapon", "Draw Weapon", 0, 0, 0, (b, i, p, m) -> true));
            }
            all = l;
        }
        return all;
    }

    public int familyCount() { return familyPages().size(); }

    /**
     * Anything besides mastery, rank and race that a page waits for before it can open (0.34): character spells, Charmy's
     * Cotton / Food half. Null = nothing; otherwise who or what it waits for, shown to the player once.
     */
    public String pageBlock(ServerPlayer p, ManasSkillInstance inst, int mode) {
        return CharacterSpells.block(this, page(mode), p);
    }
    public BookPage page(int mode) { return mode >= 0 && mode < pages().size() ? pages().get(mode) : null; }
    private boolean isDive(int mode) { return page(mode) != null && (page(mode).id().equals("spirit_dive") || page(mode).id().equals("spirit_channeling")); }
    private boolean isChannel(int mode) { return page(mode) != null && page(mode).id().equals("spirit_channeling"); }
    private boolean isUnion(int mode) { return page(mode) != null && page(mode).id().equals("devil_union"); }
    public static final String SUMMON_ID = "summon_grimoire";
    private boolean isSummon(int mode) { return page(mode) != null && page(mode).id().equals(SUMMON_ID); }
    /** 0.54: the last two pages of every grimoire: put the weapon in your hand back into the book, and draw a stored one (anim.WeaponStore). */
    public static final String STORE_ID = "store_weapon", DRAW_ID = "draw_weapon";
    private boolean isStore(int mode) { return page(mode) != null && page(mode).id().equals(STORE_ID); }
    private boolean isDrawWeapon(int mode) { return page(mode) != null && page(mode).id().equals(DRAW_ID); }
    /** Pages that act on the key press itself (no chant, no cost, no cooldown). */
    private boolean instant(int mode) { return isSummon(mode) || isStore(mode) || isDrawWeapon(mode); }

    public static boolean isUnlocked(ManasSkillInstance i, int mode) { return mode == 0 || (i.getOrCreateTag().getInt("Unlocked") & (1 << mode)) != 0; }

    public static void unlock(ManasSkillInstance i, int mode) {
        i.getOrCreateTag().putInt("Unlocked", i.getOrCreateTag().getInt("Unlocked") | (1 << mode));
        i.markDirty();
    }

    /** Can this mode be selected/cast right now? */
    public boolean usable(ManasSkillInstance i, LivingEntity e, int mode) {
        if (mode < familyCount()) return isUnlocked(i, mode);
        if (isDive(mode)) return e instanceof ServerPlayer p ? spiritGateOpen(p) : true;
        if (isUnion(mode)) return cover(i).isForbidden();
        if (instant(mode)) return true;
        return false;
    }

    @Override public ResourceLocation getSkillIcon() { return icon; }
    @Override public int getModes(ManasSkillInstance instance) { return pages().size(); }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        int n = pages().size();
        int next = 0;
        for (int step = 1; step <= n; step++) {
            int m = Math.floorMod(mode + (reverse ? -step : step), n);
            if (usable(instance, entity, m)) { next = m; break; }
        }
        // switching spells while the grimoire is summoned flips its pages (server tells everyone watching)
        if (next != mode && entity instanceof ServerPlayer sp) GrimoireSummon.onSpellSwitch(sp, this instanceof ForbiddenBook ? null : magic, reverse);
        return next;
    }

    @Override public String getModeId(ManasSkillInstance instance, int mode) { BookPage p = page(mode); return p == null ? "none" : p.id(); }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        BookPage p = page(mode);
        if (p == null) return Component.literal("?");
        if (mode < familyCount() && !isUnlocked(instance, mode)) return Component.literal("Sealed Page").withStyle(ChatFormatting.DARK_GRAY);
        int cd = instance.getCoolDown(mode);   // ManasCore cooldowns are in seconds
        return Component.literal(cd > 0 ? p.name() + " (" + cd + "s)" : p.name());
    }

    // ---------------------------------------------------------------- cost & cast loop
    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        BookPage p = page(mode);
        if (p == null) return 0;
        double c = BalanceLaw.cost(entity, p.costPercent(), p.costFloor()) * cover(instance).cost;
        if (p.costPercent() <= 0 && p.costFloor() <= 0) return 0;                       // e.g. Anti-Magic costs nothing
        if (instance.getOrCreateTag().getBoolean("ManaZone")) c *= 1.5;                   // Mana Zone costs more
        if (cover(instance).kingdom == com.newuniverse.nusmp.blackclover.Kingdom.HEART && p.costPercent() >= 20) c *= 0.85; // Mana Method
        if (entity instanceof ServerPlayer sp) c *= com.newuniverse.nusmp.item.MagicGear.costMult(sp) * (1 + ForbiddenMagic.manaTax(sp));
        return c;
    }

    @Override public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) { return 200; }

    public int castTicks(ManasSkillInstance i, LivingEntity e) { return i.isMastered(e) ? CAST_TICKS_MASTERED : CAST_TICKS; }

    /** Summon Grimoire acts on the key press itself (no chant). Every other page is chanted in onHeld / cast in onRelease. */
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (!instant(mode)) { super.onPressed(instance, entity, keyNumber, mode); return; }
        if (!(entity instanceof ServerPlayer p)) return;
        if (isSummon(mode)) GrimoireSummon.toggle(p, this);
        else if (isStore(mode)) com.newuniverse.nusmp.anim.WeaponStore.store(p, this, instance);
        else com.newuniverse.nusmp.anim.WeaponStore.draw(p, this, instance);
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (!(entity instanceof ServerPlayer p) || instant(mode)) return true;
        int need = castTicks(instance, entity);
        BookPage page = page(mode);
        if (page == null) return true;
        if (isChannel(mode)) {   // Spirit Channeling: a 0-100% gauge and an aura that grows and pulls
            int gauge = Math.min(100, heldTicks);
            if (usable(instance, entity, mode)) com.newuniverse.nusmp.multiverse.MasteryPages.onChanneling(p, instance, heldTicks);   // channeling trains mastery
            if (heldTicks % 5 == 0) p.displayClientMessage(Component.literal("Spirit Channeling " + gauge + "%"
                    + (gauge >= 100 ? "  CATACLYSM" : gauge >= 50 ? "  Spirit Nova" : gauge >= 25 ? "  Overdrive" : "")).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
            if (heldTicks % 10 == 0 && usable(instance, entity, mode)) {
                double r = 2 + 6 * gauge / 100.0;
                for (LivingEntity t : around(p, p.position(), r)) {
                    t.setDeltaMovement(p.position().subtract(t.position()).normalize().scale(0.25));
                    t.hurtMarked = true;
                    hurt(instance, p, t, mode, 1.5f);
                }
                spiritFx(p, VfxShape.FX_SPIRIT_AURA, p.position(), p.position(), (float) (0.6 + gauge / 100.0), true);
            }
            return true;
        }
        if (heldTicks == 1) {
            com.newuniverse.nusmp.anim.CastAnim.play(p, com.newuniverse.nusmp.anim.CastAnim.CHANT);          // 0.54: the chant pose
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MANA_CHARGE, p, p.position().add(0, 1, 0), color, need * 2, 1f);
            // 0.27: the floating spell card in front of the face is no longer shown (owner's request); the shape stays registered
        }
        if (heldTicks == need * 2 && !instance.getOrCreateTag().getBoolean("ManaZone")) {
            instance.getOrCreateTag().putBoolean("ManaZone", true);
            p.displayClientMessage(Component.literal("Mana Zone! " + page.name() + " overcharged (+25% size, +50% cost)").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WIND_RING, p, p.position().add(0, 1, 0), color, 20, 1.2f);
            com.newuniverse.nusmp.anim.CastAnim.play(p, com.newuniverse.nusmp.anim.CastAnim.MANA_ZONE);       // 0.54: the overcharge stance
        } else if (heldTicks % 4 == 0 && heldTicks <= need) {
            int pct = Math.min(100, heldTicks * 100 / need);
            p.displayClientMessage(Component.literal("[" + stage(instance) + "] Chanting " + page.name() + "... " + pct + "%").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return true;
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player) || instant(mode)) return;
        BookPage p = page(mode);
        if (p == null) return;
        if (heldTicks < castTicks(instance, entity)) { fail(player, "The chant broke off."); instance.getOrCreateTag().putBoolean("ManaZone", false); return; }   // fail() plays the flinch
        instance.getOrCreateTag().putInt("HeldTicks", heldTicks);
        if (!usable(instance, entity, mode)) { fail(player, "That page is still sealed."); return; }
        if (!holdingBook(player)) { fail(player, "Your pages are sealed shut. Summon your " + magic.displayName + " grimoire first (Summon Grimoire)."); return; }
        if (p.weaponTag() != null && !com.newuniverse.nusmp.item.WeaponMagicHelper.holdsAny(player, p.weaponTag())) {
            fail(player, "You must hold the required weapon to cast " + p.name() + ".");
            return;
        }
        if (player.getPersistentData().getLong("nusmp_sealed_until") > player.level().getGameTime()) { fail(player, "Your grimoire has been sealed!"); return; }
        if (instance.onCoolDown(mode)) { fail(player, p.name() + " is recharging (" + instance.getCoolDown(mode) + "s)."); return; }
        if (EnergyHelper.isOutOfEnergy(entity, instance, mode)) return;   // Tensura checks and spends
        if (!p.cast().cast(this, instance, player, mode)) { com.newuniverse.nusmp.anim.CastAnim.play(player, com.newuniverse.nusmp.anim.CastAnim.FAIL); return; }
        com.newuniverse.nusmp.anim.CastAnim.play(player, com.newuniverse.nusmp.anim.CastAnim.releaseFor(p));   // 0.54: the release body animation
        shout(player, p.incantation());
        if (p.cooldown() > 0) {
            int ticks = (int) (p.cooldown() * com.newuniverse.nusmp.item.MagicGear.cooldownMult(player) * com.newuniverse.nusmp.NUGameRules.spellCooldown(player.level()));   // 0.48: 40% shorter by default
            instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(ticks), mode);
        }
        addMasteryPoint(instance, entity);
        instance.getOrCreateTag().putInt("LastMode", mode);
        instance.getOrCreateTag().putBoolean("ManaZone", false);
        com.newuniverse.nusmp.item.MagicGear.onCast(player);
        com.newuniverse.nusmp.multiverse.SecretQuests.onCast(player);              // 0.39: secret quest progress
        DreamWorld.onCast(player, magic);                                         // 0.41: Spatial / Time / Anti-Magic / Dream tear a dream open
        MirrorWorks.echo(player);                                                 // 0.42: Real Doubles cast with you
        exhaustion(player);
        instance.markDirty();
    }

    // ---------------------------------------------------------------- shared spell helpers
    public static GrimoireCover cover(ManasSkillInstance i) { return GrimoirePages.coverOf(i); }
    public double masteryFrac(ManasSkillInstance i) { return Math.min(1.0, i.getMastery() / (double) Math.max(1, getMaxMastery())); }

    /** Spirit Dive makes this book's spells 20% bigger. */
    public static float size(ManasSkillInstance i, LivingEntity e) {
        float s = e.level().getGameTime() < i.getOrCreateTag().getLong("DiveUntil") ? 1.2f : 1f;
        return i.getOrCreateTag().getBoolean("ManaZone") ? s * 1.25f : s;   // Mana Zone: overcharged chant
    }
    public static boolean inUnion(ManasSkillInstance i, LivingEntity e) { return inDevilState(e); }

    /** Devil Union (element books) or Devil Dive (Forbidden book) is active on this entity. */
    public static boolean inDevilState(LivingEntity e) { return e.level().getGameTime() < e.getPersistentData().getLong("nusmp_devil_until"); }

    public static void startDevilState(ServerPlayer p, int ticks) {
        p.getPersistentData().putLong("nusmp_devil_until", p.level().getGameTime() + ticks);
        p.getPersistentData().putLong("nusmp_devil_paid", p.level().getGameTime());
    }

    /** Balance-law damage through a Tensura damage source (+10% pierce during Devil Union). */
    public void hurt(ManasSkillInstance i, ServerPlayer caster, LivingEntity target, int mode, float raw) {
        hurtAs(i, caster, target, mode, raw, damageType());
    }

    /** 0.45: {@link #hurt} through a chosen Tensura damage type (Dice elements, Light), same multipliers. */
    public void hurtAs(ManasSkillInstance i, ServerPlayer caster, LivingEntity target, int mode, float raw, ResourceKey<DamageType> type) {
        if (target == caster || target.isAlliedTo(caster)) return;
        float r = raw * (float) cover(i).damage * (inUnion(i, caster) ? 1.1f : 1f)
                * (float) com.newuniverse.nusmp.item.MagicGear.damageMult(caster, magic);
        // Dark Magic is Arcane Stage: strong against devils and devil-tainted mages.
        if ((magic == MagicType.DARK || magic == MagicType.SHADOW) && (inDevilState(target)
                || (target instanceof ServerPlayer tp && GrimoirePages.grimoireOf(tp).map(g -> cover(g).isForbidden()).orElse(false)))) r *= 1.25f;
        com.newuniverse.nusmp.blackclover.CopyMemory.remember(target, this, mode);
        target.hurt(createSource(i, caster, type, mode), BalanceLaw.damage(caster, target, r, masteryFrac(i)));
    }

    public static LivingEntity target(ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition(), end = start.add(p.getViewVector(1f).scale(range));
        HitResult wall = p.level().clip(new net.minecraft.world.level.ClipContext(start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, start, end,
                p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1), e -> e instanceof LivingEntity && e.isAlive() && e != p);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** Point under the crosshair (block hit or max range). */
    public static Vec3 aim(ServerPlayer p, double range) { return p.pick(range, 1f, false).getLocation(); }

    public static List<LivingEntity> around(ServerPlayer p, Vec3 c, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(p) && e.distanceToSqr(c) <= r * r);
    }

    /** Entities within 'width' of the segment a->b. */
    public static List<LivingEntity> along(ServerPlayer p, Vec3 a, Vec3 b, double width) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 ab = b.subtract(a);
        double len2 = Math.max(1e-6, ab.lengthSqr());
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(width + 1),
                e -> e != p && e.isAlive() && !e.isAlliedTo(p))) {
            Vec3 c = e.getBoundingBox().getCenter();
            double t = Math.max(0, Math.min(1, c.subtract(a).dot(ab) / len2));
            if (a.add(ab.scale(t)).distanceTo(c) <= width + e.getBbWidth() / 2) out.add(e);
        }
        return out;
    }

    /** Hard control gate (shared 12 s lock). */
    public static boolean control(ServerPlayer p) {
        if (BalanceLaw.beginControl(p)) return true;
        fail(p, "Your control magic needs " + (BalanceLaw.controlCooldownLeft(p) + 19) / 20 + "s.");
        return false;
    }

    // ---------------------------------------------------------------- VFX helpers
    public void vfx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxSpawn.send(p.serverLevel(), shape, from, to, color, ticks, power);
    }

    /** Spinning magic circle under the caster in this book's color (every cast). */
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MAGIC_CIRCLE, p, p.position().add(0, 1, 0), color, 24, power);
    }

    /** Circle burst on impact. */
    public void impact(ServerPlayer p, Vec3 at, float power) {
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), color, 20, power);
    }

    /** Element index for the spirit effects: 0 fire, 1 water, 2 wind, 3 earth. */
    public int spiritVariant() {
        Element e = spiritElement();
        return e == Element.WATER ? 1 : e == Element.WIND ? 2 : e == Element.EARTH ? 3 : 0;
    }

    /** Sends a spec-driven spirit effect (the server reports where; the client builds the look). */
    public void spiritFx(ServerPlayer p, VfxShape shape, net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to, float power, boolean follow) {
        VfxSpawn.send(p.serverLevel(), new com.newuniverse.nusmp.vfx.VfxPayload(shape.ordinal(), from, to, spiritVariant(), 0, power,
                follow ? p.getId() : -1, p.getRandom().nextLong()));
    }

    // ---------------------------------------------------------------- Spirit Dive / Devil Union
    public boolean spiritGateOpen(ServerPlayer p) {
        Element el = spiritElement();
        if (el == null) return false;
        var spirit = TensuraStorages.getSpiritFrom(p);
        if (spirit == null || spirit.getSpiritLevel(el) != SpiritualMagic.SpiritLevel.LORD) return false;
        var owner = SpiritSlots.get(p.getServer()).owner(spiritName());
        return owner == null || owner.equals(p.getUUID());
    }

    /** 0.38: the transformation armour this book's Spirit Dive puts on (null = none): Sylph's wind, Salamander's fire. */
    public com.newuniverse.nusmp.blackclover.ModeArmor.Mode diveArmor() {
        if (this instanceof WindBook) return com.newuniverse.nusmp.blackclover.ModeArmor.Mode.WIND_SPIRIT_DIVE;
        if (this instanceof FireBook) return com.newuniverse.nusmp.blackclover.ModeArmor.Mode.FIRE_SPIRIT_DIVE;
        return null;
    }

    private static boolean spiritDive(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        long now = p.level().getGameTime();
        if (now < i.getOrCreateTag().getLong("DiveUntil")) {       // recast ends it early, lock still applies
            i.getOrCreateTag().putLong("DiveUntil", now);
            i.setCoolDown(60, mode);                                   // seconds
            if (b.diveArmor() != null) com.newuniverse.nusmp.blackclover.ModeArmor.stop(p, b.diveArmor());
            return false;
        }
        if (!SpiritBond.willAnswer(p)) { fail(p, b.spiritName() + " will not answer you. Earn back its trust."); return false; }
        SpiritSlots slots = SpiritSlots.get(p.getServer());
        if (slots.owner(b.spiritName()) == null && !com.newuniverse.nusmp.item.MagicGear.consumeSpiritCharm(p)) {
            fail(p, "You need a Spirit Charm to reach " + b.spiritName() + " the first time.");
            return false;
        }
        if (!slots.claim(b.spiritName(), p.getUUID())) {
            fail(p, b.spiritName() + " has already chosen a mage.");
            return false;
        }
        SpiritBond.bond(p, b.spiritName());
        SpiritBond.addTrust(p, 2, "spirit_dive");
        SpiritBond.heavyUse(p);
        i.getOrCreateTag().putLong("DiveUntil", now + 160);
        if (b.diveArmor() != null) com.newuniverse.nusmp.blackclover.ModeArmor.start(p, b.diveArmor(), 160);   // 0.38: the Spirit Dive armour overlay
        i.setCoolDown(8 + 60, mode);                                  // seconds: 8 s dive + 60 s spirit lock
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SPIRIT_AURA, p, p.position().add(0, 1, 0), b.color, 160, 1.0F);
        p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " dives with " + b.spiritName() + "!")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MAGIC_CIRCLE_EXPLOSION, p, p.position().add(0, 1, 0), b.color, 30, 2.0F);
        return true;
    }

    /** Spirit Channeling release: 25%+ Overdrive dash, 50%+ Spirit Nova, 100% Spirit Sovereign's Cataclysm. */
    private static boolean channel(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!SpiritBond.willAnswer(p)) { fail(p, b.spiritName() + " will not answer you."); return false; }
        int gauge = Math.min(100, i.getOrCreateTag().getInt("HeldTicks"));
        if (gauge < 25) { fail(p, "The spirit energy disperses (" + gauge + "%)."); return false; }
        SpiritBond.heavyUse(p);
        net.minecraft.world.phys.Vec3 eye = p.getEyePosition(), from = p.position();
        if (gauge < 50) {          // Elemental Overdrive: a dash cloaked in the element
            net.minecraft.world.phys.Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), dest = from;
            for (double d = 8; d >= 1; d -= 0.5) {
                net.minecraft.world.phys.Vec3 c = from.add(dir.scale(d));
                if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
            }
            for (LivingEntity t : along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.5)) b.hurt(i, p, t, mode, 10f);
            p.teleportTo(dest.x, dest.y, dest.z);
            b.spiritFx(p, VfxShape.FX_SPIRIT_OVERDRIVE, from, dest, 1f, false);
        } else if (gauge < 100) {  // Spirit Nova: an 8-block burst that destroys projectiles
            for (var pr : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, p.getBoundingBox().inflate(8)))
                if (pr.getOwner() != p) pr.discard();
            for (LivingEntity t : around(p, from, 8)) {
                b.hurt(i, p, t, mode, 14f);
                net.minecraft.world.phys.Vec3 away = t.position().subtract(from).normalize();
                t.knockback(1.8, -away.x, -away.z);
            }
            b.spiritFx(p, VfxShape.FX_SPIRIT_NOVA, from, from, 1.2f, false);
        } else {                   // Cataclysm: 6 s as a living conduit of the element
            i.getOrCreateTag().putLong("CataclysmUntil", p.level().getGameTime() + 120);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 120, 2));
            com.newuniverse.nusmp.blackclover.TimedModifiers.apply(p, net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,
                    ResourceLocation.fromNamespaceAndPath("nusmp", "cataclysm_kb"), 1.0, 120);
            b.spiritFx(p, VfxShape.FX_SPIRIT_CATACLYSM, from, from, 1.2f, true);
            SpellRuntime.zone(p.serverLevel(), 120, 10, age -> {
                for (LivingEntity t : around(p, p.position(), 4 * size(i, p))) b.hurt(i, p, t, mode, 6f);
            });
            p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " becomes a living conduit of " + b.spiritName() + "!")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        }
        return true;
    }

    private static boolean devilUnion(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        startDevilState(p, 160);
        p.getPersistentData().putString("nusmp_devil_src", "union");
        p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " has entered Devil Union.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.DEVIL_CIRCLE, p, p.position().add(0, 1, 0), 0xFFB01010, 40, 2.0F);
        VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, p.getEyePosition(), p.getEyePosition().add(p.getViewVector(1f).scale(4)), 0xFF2A0A18, 18, 2.0F);
        return true;
    }

    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onTick(ManasSkillInstance i, LivingEntity e) {
        if (e instanceof ServerPlayer p) second(i, p);
    }

    /**
     * 0.48: the book's once-a-second upkeep. ManasCore only ticks the skills it treats as active (a grimoire sitting in the skill list
     * may never tick, which left Cotton / Food grimoires unrolled), so our own player tick ({@link #onPlayerTick}) drives it too;
     * whichever comes first in a second runs it, the other skips.
     */
    public void second(ManasSkillInstance i, ServerPlayer p) {
        long now = p.level().getGameTime();
        var stamp = i.getOrCreateTag();
        long last = stamp.getLong("SecondAt");
        if (last <= now && now - last < 20) return;
        stamp.putLong("SecondAt", now);
        // Spirit Dive upkeep: magicule each second; ends if the Lord contract is lost.
        if (now < i.getOrCreateTag().getLong("DiveUntil")) {
            if (!spiritGateOpen(p) || !drain(p, EnergyHelper.getMaxMagicule(p) * 0.02)) {
                i.getOrCreateTag().putLong("DiveUntil", now);
                if (diveArmor() != null) com.newuniverse.nusmp.blackclover.ModeArmor.stop(p, diveArmor());
                fail(p, "The spirit slips away.");
            }
        }
        // Devil Union: HP drain; at the price floor it ends and you're stunned 2 s.
        if (inDevilState(p) && !(this instanceof ForbiddenBook) && "union".equals(p.getPersistentData().getString("nusmp_devil_src"))) {
            if (p.getHealth() <= 6f) {
                p.getPersistentData().putLong("nusmp_devil_until", now);
                TimeStop.freeze(p, 40);
                fail(p, "The devil lets go. You collapse.");
            } else {
                p.hurt(p.damageSources().magic(), (float) com.newuniverse.nusmp.item.MagicGear.selfDamageMult(p));
            }
        }
        manaSense(p);
        tickBook(i, p);
    }

    /** Per-second hook for subclasses. */
    protected void tickBook(ManasSkillInstance i, ServerPlayer p) {}

    /** Every second: each grimoire book the player has gets its upkeep (see {@link #second}). */
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 3) return;
        var skills = io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(p);
        for (var h : com.newuniverse.nusmp.skill.NUSkills.BOOKS)
            skills.getSkill(h.getId()).ifPresent(i -> { if (i.getSkill() instanceof GrimoireBook b) b.second(i, p); });
    }

    private static boolean drain(ServerPlayer p, double amount) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || ex.getMagicule() < amount) return false;
        ex.setMagicule(ex.getMagicule() - amount);
        ex.markDirty();
        return true;
    }

    // ---------------------------------------------------------------- mana (Black Clover rules on Tensura magicule)
    /** Heart Kingdom Magic Stages: Stage 9 (new) to Stage 0 (mastered). Anti-Magic and devils are Arcane Stage. */
    public String stage(ManasSkillInstance i) {
        if (magic == MagicType.ANTI_MAGIC || this instanceof ForbiddenBook) return "Arcane Stage";
        return "Stage " + (9 - (int) Math.round(masteryFrac(i) * 9));
    }

    /** Spending your mana to nearly nothing exhausts you (Black Clover: empty mana = collapse). */
    private static void exhaustion(ServerPlayer p) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        if (ex.getMagicule() < EnergyHelper.getMaxMagicule(p) * 0.05) {
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 1));
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 100, 1));
            p.displayClientMessage(Component.literal("Your mana is spent. Exhaustion takes you.").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** Mana Skin: while you hold at least half your magicule, incoming damage is reduced 10%. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, net.minecraft.world.damagesource.DamageSource source,
                                 io.github.manasmods.manascore.network.api.util.Changeable<Float> amount) {
        var ex = TensuraStorages.getExistenceFrom(owner);
        if (ex != null && ex.getMagicule() >= EnergyHelper.getMaxMagicule(owner) * 0.5) amount.set(amount.get() * 0.9f);
        if (owner.getPersistentData().getLong("nusmp_misfortune_until") > owner.level().getGameTime()) amount.set(amount.get() * 1.1f);
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong("CataclysmUntil")) {   // living conduit: a spiritual body
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)
                    || source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING) || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) amount.set(0f);
            else if (!com.newuniverse.nusmp.antimagic.AntiMagic.isMagic(source)) amount.set(amount.get() * 0.7f);
        }
        return true;
    }

    /** Five-sided diamond: a fortune ward negates one lethal blow per day. */
    @Override
    public boolean onDeath(ManasSkillInstance i, LivingEntity owner, net.minecraft.world.damagesource.DamageSource source) {
        if (!(owner instanceof ServerPlayer p) || cover(i) != GrimoireCover.FIVE_SIDED) return true;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) return true;
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong("nusmp_fortune_ready")) return true;
        p.getPersistentData().putLong("nusmp_fortune_ready", now + 24000);
        p.setHealth(8f);
        p.displayClientMessage(Component.literal("Good Fortune turns the blow aside.").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        impact(p, p.position().add(0, 1, 0), 1f);
        return false;
    }

    /** Wind-derived mages (wind, lightning, storm, mist) sense mana: hostiles hunting you glow. */
    protected void manaSense(ServerPlayer p) {
        if (p.tickCount % 100 != 0) return;
        if (magic != MagicType.WIND && magic != MagicType.LIGHTNING && magic != MagicType.STORM && magic != MagicType.MIST) return;
        for (net.minecraft.world.entity.Mob m : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(16))) {
            if (m.getTarget() == p) m.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 40, 0, true, false));
        }
    }

    /** Granting this skill (acceptance, admin or /tensura) always hands over the matching grimoire. */
    @Override
    public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        if (entity instanceof ServerPlayer p && !(this instanceof ForbiddenBook)) GrimoireKeeper.ensureGrimoire(p, this, instance);
    }

    // ---------------------------------------------------------------- misc
    /**
     * Casting needs your grimoire of this book's magic summoned (0.24; the Forbidden book accepts any of yours). Holding it, or having
     * it on the hotbar, is not enough. The Anti-Magic exception: the Anti-Magic Lord's Black Form toggle and the demon swords work
     * without a summoned book (they are the swords drawn from it), but the Anti-Magic book's pages still need it out.
     */
    private boolean holdingBook(ServerPlayer p) {
        return GrimoireSummon.isFloating(p, this instanceof ForbiddenBook ? null : magic);
    }

    public static void fail(ServerPlayer p, String msg) {
        p.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.RED), true);
        com.newuniverse.nusmp.anim.CastAnim.play(p, com.newuniverse.nusmp.anim.CastAnim.FAIL);                 // 0.54: the fizzle flinch
    }

    private void shout(ServerPlayer p, String incantation) {
        Component line = Component.literal(magic.displayName + ": " + incantation + "!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        for (ServerPlayer other : p.serverLevel().players()) if (other.distanceToSqr(p) < 32 * 32) other.displayClientMessage(line, true);
    }

    public static ServerLevel level(ServerPlayer p) { return p.serverLevel(); }
}
