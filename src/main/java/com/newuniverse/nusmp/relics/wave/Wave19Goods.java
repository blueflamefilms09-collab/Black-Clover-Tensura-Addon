package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 19, the drinks and sweets: Fey Honey, Blood Chocolates, the Ale-lemental, the Mug of The Boss and Holy Water.
 */
final class Wave19Goods {
    private Wave19Goods() {}

    static final String K_HONEY = "honey_until", K_BLOOD = "blood_until", K_MUG = "mug_until", K_MUG_TITLE = "mug_title";
    private static final String K_TITLE = "w19_mug_title", K_LAST = "w19_mug_last";

    // ================================================================ Fey Honey
    /**
     * Fey Honey: for twenty minutes (the tabletop hour) your force of personality is 21: Luck III, Hero of the Village, and Tensura's Thought
     * Communication lent to you. When it wears off the Constitution save (DC 10) is rolled: fail it and a wild magic surge erupts from you
     * (see the Scroll of Unreadable Spells). A pool of magicules at 80% or more counts as being of the Fey and shrugs it off. Drinking it while it
     * already works does nothing (the bottle is kept).
     */
    static void honey(ActCtx c) {
        ServerPlayer p = c.player();
        if (Wave19Util.running(p, K_HONEY)) {
            Wave19Scrolls.keep(c);
            Wave19Util.tell(p, "Fey Honey: your charm is already as high as it goes. The honey is kept.", ChatFormatting.GRAY);
            return;
        }
        int t = Wave19Rules.HONEY_TICKS;
        Wave19Util.data(p).putLong(K_HONEY, p.level().getGameTime() + t);
        Wave19Util.give(p, "minecraft:luck", t, 2);
        Wave19Util.give(p, "minecraft:hero_of_the_village", t, 0);
        boolean skill = Fx.lendSkill(p, "tensura:thought_communication", t);
        ServerLevel sl = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, Wave19Util.align(0xDDE6F2), p.getX(), p.getY() + 1.4, p.getZ(), 30, 0.5, 0.6, 0.5, 0.02);
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, FxTone.LIGHT, p.getX(), p.getY() + 1.4, p.getZ(), 14, 0.5, 0.6, 0.5, 0.05);
        sl.playSound(null, p.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 1.0f, 1.3f);
        Wave19Util.say(p, "Fey Honey: sweet, silver and intoxicating. For twenty minutes everyone finds you irresistible" + (skill ? " (Thought Communication lent)" : "") + ". Mind the come-down.");
    }

    // ================================================================ Blood Chocolates
    /**
     * Blood Chocolates: heals ten health at once and fills a little hunger, but for about seven minutes (the tabletop twenty) you take double damage
     * from radiant, holy and sacred sources.
     */
    static void chocolate(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.heal(p, Wave19Rules.CHOCOLATE_HEAL);
        p.getFoodData().eat(2, 0.3f);
        Wave19Util.data(p).putLong(K_BLOOD, p.level().getGameTime() + Wave19Rules.VULNERABLE_TICKS);
        ServerLevel sl = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, Wave19Util.align(0x8B0A1E), p.getX(), p.getY() + 1.3, p.getZ(), 24, 0.4, 0.5, 0.4, 0.02);
        sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0f, 0.7f);
        Wave19Util.say(p, "Blood Chocolates: a metallic sweetness; your wounds close. For seven minutes holy light will hurt you twice as much.");
    }

    // ================================================================ Ale-lemental
    /**
     * Ale-lemental: pour the tankard out and a water elemental rises from the puddle to fight for you for half a minute (three rounds, stretched
     * to be worth a summon). Tensura's Undine stands in; plain Minecraft gets a snow golem. It puts you out if you are on fire.
     */
    static void ale(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity e = Fx.summon(p, "tensura:undine", 600);
        if (e == null) e = Fx.summon(p, "minecraft:snow_golem", 600);
        if (e == null) {
            Wave19Scrolls.keep(c);
            Wave19Util.tell(p, "Ale-lemental: the ale only soaks into the ground. The tankard is kept.", ChatFormatting.GRAY);
            return;
        }
        p.clearFire();
        ServerLevel sl = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, FxTone.LIGHT, e.getX(), e.getY() + 1.0, e.getZ(), 50, 0.8, 1.0, 0.8, 0.2);
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, FxTone.LIGHT, e.getX(), e.getY() + 1.0, e.getZ(), 20, 0.6, 0.8, 0.6, 0.1);
        sl.playSound(null, p.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 0.8f);
        Wave19Util.say(p, "Ale-lemental: a frothing elemental of ale rises and takes your side.");
    }

    // ================================================================ Mug of The Boss
    /**
     * Mug of The Boss. Sneak while you drink to turn the mug to the next title (Boss, General, Lich, Chef, Wizard); the mug is kept. Otherwise, once
     * every 24 hours (a Minecraft day), you chug a full cup and the title takes hold for twenty minutes and everyone who sees the mug is moved:
     * <ul>
     *   <li><b>Boss</b>: Hero of the Village, and hostile creatures within twelve blocks of under sixty health stop hunting anyone for a minute.</li>
     *   <li><b>General</b>: Strength I and Speed I, and your tamed pets within sixteen blocks share the Strength.</li>
     *   <li><b>Lich</b>: undead within sixteen blocks stop hunting anyone for a minute; Night Vision; Poison Resistance lent.</li>
     *   <li><b>Chef</b>: a full meal, and Regeneration.</li>
     *   <li><b>Wizard</b>: Sage and Magic Sense lent, a third of your magicules back.</li>
     * </ul>
     * The flaws of the card: you sing when you bathe (notes, and a sloshing voice), you volunteer for everything (a little hunger drain) and you cannot
     * steer (bad luck while riding a boat or a minecart): all of them only while the title lasts.
     */
    static void mug(ActCtx c) {
        Wave19Scrolls.keep(c);
        ServerPlayer p = c.player();
        ItemStack stack = c.stack();
        CompoundTag t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int title = t.getInt(K_TITLE);
        if (c.sneaking()) {
            int next = Wave19Rules.nextTitle(title);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag x) -> x.putInt(K_TITLE, next));
            Wave19Util.tell(p, "Mug of The Boss: it now reads WORLD'S BEST " + Wave19Rules.title(next).toUpperCase() + ". Drink without sneaking to chug.", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        long now = p.level().getGameTime();
        long wait = Wave19Rules.mugWait(now, t.getLong(K_LAST));
        if (wait > 0) {
            Wave19Util.tell(p, "Mug of The Boss: the mug is dry. It fills again in " + (wait / 1200 + 1) + " minute(s).", ChatFormatting.GRAY);
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag x) -> x.putLong(K_LAST, now));
        int ticks = Wave19Rules.MUG_TICKS;
        CompoundTag d = Wave19Util.data(p);
        d.putLong(K_MUG, now + ticks);
        d.putInt(K_MUG_TITLE, title);
        int awed = 0;
        switch (Math.floorMod(title, Wave19Rules.TITLES.length)) {
            case 0 -> {
                Wave19Util.give(p, "minecraft:hero_of_the_village", ticks, 0);
                awed = awe(p, Wave19Rules.MUG_RANGE, false);
            }
            case 1 -> {
                Wave19Util.give(p, "minecraft:strength", ticks, 0);
                Wave19Util.give(p, "minecraft:speed", ticks, 0);
                for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16.0), x -> x instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID()) && x.isAlive()))
                    Wave19Util.give(e, "minecraft:strength", ticks, 0);
            }
            case 2 -> {
                Wave19Util.give(p, "minecraft:night_vision", ticks, 0);
                Fx.lendSkill(p, "tensura:poison_resistance", ticks);
                awed = awe(p, 16.0, true);
            }
            case 3 -> {
                p.getFoodData().eat(10, 1.0f);
                Wave19Util.give(p, "minecraft:regeneration", 600, 0);
            }
            default -> {
                Fx.lendSkills(p, ticks, "tensura:sage", "tensura:magic_sense");
                Wave19Util.addMagicules(p, 0.33);
            }
        }
        ServerLevel sl = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, FxTone.LIGHT, p.getX(), p.getY() + 2.2, p.getZ(), 6, 0.5, 0.2, 0.5, 1.0);
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, FxTone.LIGHT, p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.6, 0.6, 0.6, 0.05);
        sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 0.9f);
        Wave19Util.say(p, "Mug of The Boss: WORLD'S BEST " + Wave19Rules.title(title).toUpperCase() + "! You chug the cup and hold the mug up high"
                + (awed > 0 ? "; " + awed + " creature" + (awed == 1 ? " is" : "s are") + " awestruck." : "."));
    }

    /** Creatures that see the mug and fail the wisdom save stop hunting anyone for a minute (twelve at most). */
    private static int awe(ServerPlayer p, double range, boolean undeadOnly) {
        int n = 0;
        List<Mob> near = p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(range),
                m -> m.isAlive() && Wave19Util.foe(p, m) && Wave19Rules.mugAwes(m.getMaxHealth()) && (!undeadOnly || m.getType().is(EntityTypeTags.UNDEAD)));
        for (Mob m : near) {
            if (n >= 12) break;
            Wave19Field.charm(m, p, Wave19Rules.MUG_AWE_TICKS, true);
            com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_19, FxTone.LIGHT, m.getX(), m.getEyeY() + 0.4, m.getZ(), 2, 0.2, 0.2, 0.2, 1.0);
            n++;
        }
        return n;
    }

    // ================================================================ Holy Water
    private static final Set<String> FIENDS = Set.of("lesser_daemon", "greater_daemon", "arch_daemon", "zombie", "skeleton", "bone_golem");

    /** Fiends and undead: the undead tag, the Nether's blazes and ghasts, the Wither, and Tensura's daemons and undead. */
    static boolean unholy(LivingEntity e) {
        if (e.getType().is(EntityTypeTags.UNDEAD)) return true;
        EntityType<?> t = e.getType();
        if (t == EntityType.BLAZE || t == EntityType.GHAST || t == EntityType.WITHER) return true;
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(t);
        return key != null && "tensura".equals(key.getNamespace()) && FIENDS.contains(key.getPath());
    }

    private static boolean harmable(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        if (e instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID())) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o) && Wave19Util.running(o, K_BLOOD);   // only a player made vulnerable by blood chocolate
        return unholy(e);
    }

    /**
     * Holy Water: hurled up to twelve blocks along your look line, it shatters on the first creature or block and splashes a two-block radius:
     * eight radiant damage to fiends and undead only (everything else is untouched). A player under the vulnerability of Blood Chocolates takes it
     * doubled.
     */
    static void holy(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(Wave19Rules.HOLY_RANGE));
        HitResult blocks = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 stop = blocks.getType() == HitResult.Type.MISS ? end : blocks.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(sl, p, eye, stop, new AABB(eye, stop).inflate(1.0), e -> e instanceof LivingEntity l && harmable(p, l) && e.isPickable());
        if (ent != null) stop = ent.getLocation();
        Wave19Scrolls.beam(sl, eye.add(0, -0.3, 0), stop, 0xDDEEFF);
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, FxTone.LIGHT, stop.x, stop.y, stop.z, 30, 0.8, 0.5, 0.8, 0.2);
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, FxTone.LIGHT, stop.x, stop.y + 0.3, stop.z, 16, 0.8, 0.5, 0.8, 0.06);
        sl.playSound(null, BlockPos.containing(stop), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.3f);
        String type = holyType(p);
        int hit = 0;
        for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(stop, stop).inflate(Wave19Rules.HOLY_SPLASH), e -> harmable(p, e))) {
            if (hit >= 8) break;
            float dmg = Wave19Rules.radiantDamage(Wave19Rules.HOLY_DAMAGE, t instanceof Player o && Wave19Util.running(o, K_BLOOD), true);
            Fx.damage(p, t, dmg, type);
            t.clearFire();
            com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, FxTone.LIGHT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
            hit++;
        }
        Wave19Util.say(p, "Holy Water: " + (hit > 0 ? "it sears " + hit + " unholy creature" + (hit == 1 ? "." : "s.") : "it splashes harmlessly; nothing unholy was there."));
    }

    /** Tensura's holy damage type where it exists, plain magic otherwise. */
    private static String holyType(ServerPlayer p) {
        try {
            ResourceKey<net.minecraft.world.damagesource.DamageType> k = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("tensura", "holy_damage"));
            if (p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(k).isPresent()) return "tensura:holy_damage";
        } catch (RuntimeException ignored) {
            // no such damage type
        }
        return "minecraft:magic";
    }
}
