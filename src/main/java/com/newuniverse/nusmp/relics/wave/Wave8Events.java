package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Wave 8 gameplay: what the blades and axes do on their own (on-hit bonuses, kills, the staggered player tick). Everything keys off one flag
 * per weapon so the core's attunement rules decide whether it is on. Per-tick work is a main-hand check; the rest is staggered.
 */
final class Wave8Events {
    private Wave8Events() {}

    static final String F_SPOT = "w8_spotlight", F_DUAL = "w8_duality_blade", F_FROST = "w8_frostfang", F_SSB = "w8_soul_shatter_blade",
            F_BUSTER = "w8_buster_sword", F_FANG = "w8_dragon_tooth_blade_red", F_CORPSE = "w8_corpse_eater", F_CHARITY = "w8_random_axe_of_charity",
            F_LEV = "w8_leviathan_axe", F_BURN = "w8_shadow_burn";

    private static final String K_CT = "nusmp_w8_combo_t", K_CTICK = "nusmp_w8_combo_tick", K_CN = "nusmp_w8_combo_n",
            K_FRENZY = "nusmp_w8_frenzy_until", K_FRENZY_LV = "nusmp_w8_frenzy_lv", K_WHISPER = "nusmp_w8_whisper";
    private static final ResourceLocation LICH_HP = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w8_lich_hp"),
            LICH_ATK = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w8_lich_atk");
    private static final String[] WHISPERS = {
            "Come to me, my knight.", "Another soul for the hoard. Good.", "You were never alone in that blade.",
            "Rest is for the living. Rise.", "Every kill, I grow. Every kill, you are mine."};

    // ================================================================ damage dealt
    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide() || Wave8Acts.acting) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer atk) || src.getDirectEntity() != atk || src.is(DamageTypeTags.IS_PROJECTILE)) return;
        String id = Wave8Util.heldId(atk);
        if (id.isEmpty()) return;
        long now = atk.level().getGameTime();
        CompoundTag ad = atk.getPersistentData();
        boolean pvp = victim instanceof Player;
        switch (id) {
            case "spotlight" -> {
                if (!RelicFlags.has(atk, F_SPOT) || ad.getLong(Wave8Acts.K_SPOT) < now) return;
                ad.remove(Wave8Acts.K_SPOT);                                                              // "advantage": the next blow lands true
                bonus(e, Math.min(6f, e.getAmount() * 0.5f), pvp, 3f);
                ((ServerLevel) victim.level()).sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 14, 0.3, 0.5, 0.3, 0.2);
            }
            case "duality_blade" -> {
                if (!RelicFlags.has(atk, F_DUAL)) return;
                boolean light = RelicEngine.isOn(atk, "duality_blade", "streamers");
                boolean dim = Wave8Rules.dim(atk.level().getMaxLocalRawBrightness(atk.blockPosition()));
                float b = Wave8Rules.dualityBonus(light, dim);
                if (light) {                                                                              // radiant: more against the undead, and it marks the foe
                    if (victim.getType().is(EntityTypeTags.UNDEAD)) b *= 1.5f;
                    Fx.addEffect(victim, "minecraft:glowing", 100, 0);
                } else if (dim) {                                                                         // necrotic: the unseen blade withers
                    Fx.addEffect(victim, "minecraft:wither", 40, 0);
                }
                bonus(e, b, pvp, 2f);
            }
            case "frostfang" -> {
                if (!RelicFlags.has(atk, F_FROST)) return;
                bonus(e, 6f, pvp, 3f);                                                                    // the card's 3d6 cold, shrunk
                Wave8Util.chill(victim, 80);
            }
            case "soul_shatter_blade" -> {
                if (!RelicFlags.has(atk, F_SSB) || !Wave8Rules.darkAura(Wave8Util.souls(atk))) return;
                bonus(e, 3.5f, pvp, 2f);                                                                  // the extra die of necrotic damage
                Fx.addEffect(victim, "minecraft:wither", 30, 0);
            }
            case "buster_sword" -> {
                if (!RelicFlags.has(atk, F_BUSTER)) return;
                e.setAmount(Wave8Rules.twoHanded(e.getAmount(), !atk.getOffhandItem().isEmpty()));
            }
            case "dragon_tooth_blade_red" -> {
                if (!RelicFlags.has(atk, F_FANG)) return;
                int n = Wave8Rules.comboNext(ad.getInt(K_CT), ad.getLong(K_CTICK), ad.getInt(K_CN), victim.getId(), now);
                ad.putInt(K_CT, victim.getId());
                ad.putLong(K_CTICK, now);
                if (n >= 2) {                                                                             // two strikes in a row: the target bursts into flame (2d6)
                    n = 0;
                    bonus(e, 7f, pvp, 3f);
                    victim.setRemainingFireTicks(100);
                    ((ServerLevel) victim.level()).sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY() + 1.0, victim.getZ(), 24, 0.3, 0.6, 0.3, 0.05);
                }
                ad.putInt(K_CN, n);
                if (ad.getLong(Wave8Acts.K_FANG) >= now) victim.setRemainingFireTicks(60);                // Searing Fang
            }
            case "corpse_eater" -> {
                if (!RelicFlags.has(atk, F_CORPSE)) return;
                bonus(e, ad.getLong(K_FRENZY) >= now ? 5.5f : 3.5f, pvp, 2f);                            // 1d6 necrotic, more in the frenzy
            }
            case "leviathan_axe" -> {
                if (!RelicFlags.has(atk, F_LEV)) return;
                boolean awake = ad.getLong(Wave8Acts.K_AWAKE) >= now;
                bonus(e, awake ? 6f : 3f, pvp, 3f);
                Wave8Util.chill(victim, awake ? 120 : 60);
            }
            case "shadow_burn" -> {
                if (!RelicFlags.has(atk, F_BURN)) return;
                if (ad.getLong(Wave8Acts.K_STALK) >= now) {                                               // Shadow Stalk's "advantage"
                    ad.remove(Wave8Acts.K_STALK);
                    bonus(e, Math.min(8f, e.getAmount() * 0.4f), pvp, 4f);
                    ((ServerLevel) victim.level()).sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 14, 0.3, 0.5, 0.3, 0.2);
                }
                if (Wave8Rules.dim(victim.level().getMaxLocalRawBrightness(victim.blockPosition()))) {    // a foe standing in shadow burns
                    bonus(e, 2f, pvp, 2f);
                    victim.setRemainingFireTicks(40);
                }
            }
            default -> { }
        }
    }

    private static void bonus(LivingIncomingDamageEvent e, float amount, boolean victimIsPlayer, float playerCap) {
        e.setAmount(e.getAmount() + Wave8Rules.bonusVs(amount, victimIsPlayer, playerCap));
    }

    // ================================================================ kills
    static void onDamagePost(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide() || !(victim.level() instanceof ServerLevel level) || !victim.isDeadOrDying()) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer atk) || atk == victim || src.getDirectEntity() != atk || src.is(DamageTypeTags.IS_PROJECTILE)) return;
        String id = Wave8Util.heldId(atk);
        if (id.equals("random_axe_of_charity") && RelicFlags.has(atk, F_CHARITY) && !(victim instanceof Player)) {
            int n = Wave8Rules.copperIngots(1 + level.random.nextInt(4), 1 + level.random.nextInt(4));    // 2d4 copper pieces fall around the dead
            ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 0.4, victim.getZ(), new ItemStack(Items.COPPER_INGOT, n));
            drop.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.2, 0.25, (level.random.nextDouble() - 0.5) * 0.2);
            level.addFreshEntity(drop);
        } else if (id.equals("soul_shatter_blade") && RelicFlags.has(atk, F_SSB) && countsAsSoul(atk, victim)) {
            float[] r = Wave8Rules.lifesteal(e.getNewDamage(), atk.getHealth(), atk.getMaxHealth(), 8f);   // heal what you dealt; the rest is temporary hit points
            if (r[0] > 0f) Fx.heal(atk, r[0]);
            if (r[1] > 0f) Fx.shield(atk, r[1], 1200);
            int before = Wave8Util.souls(atk);
            int after = Wave8Rules.addSoul(before);
            Wave8Util.setSouls(atk, after);
            level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 1.0, victim.getZ(), 8, 0.3, 0.5, 0.3, 0.03);
            milestone(atk, before, after);
        }
    }

    private static boolean countsAsSoul(ServerPlayer atk, LivingEntity victim) {
        if (victim instanceof ArmorStand) return false;
        if (victim instanceof OwnableEntity own && atk.getUUID().equals(own.getOwnerUUID())) return false;
        return !victim.getPersistentData().hasUUID("nusmp_relic_owner");
    }

    private static void milestone(ServerPlayer p, int before, int after) {
        int[] marks = {Wave8Rules.M_CHARGES, Wave8Rules.M_DARK, Wave8Rules.M_THREE, Wave8Rules.M_RAISE, Wave8Rules.M_VOICE, Wave8Rules.M_CALL};
        String[] text = {"The blade wakes: Inflict Wounds, 2 charges a day.", "A dark aura wraps the blade: its blows carry an extra die of necrotic harm.",
                "The blade has a third charge each day.", "The blade can Animate Dead and Speak with Dead.",
                "A voice speaks through the blade. Only an enchanted golden apple will cleanse you now.", "The lich calls its warrior. The transformation begins."};
        for (int i = 0; i < marks.length; i++) {
            if (before < marks[i] && after >= marks[i]) Fx.say(p, text[i] + " (" + marks[i] + " soul fragments)");
        }
    }

    // ================================================================ Feeding Frenzy
    static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || dead instanceof ArmorStand || level.players().isEmpty()) return;
        for (ServerPlayer p : level.players()) {
            if (p == dead || !p.isAlive() || p.distanceToSqr(dead) > 36.0 * 36.0) continue;               // 120 feet
            if (!Wave8Util.heldId(p).equals("corpse_eater") || !RelicFlags.has(p, F_CORPSE)) continue;
            frenzy(p, level.getGameTime());
        }
    }

    private static void frenzy(ServerPlayer p, long now) {
        CompoundTag pd = p.getPersistentData();
        int lv = pd.getLong(K_FRENZY) >= now ? pd.getInt(K_FRENZY_LV) : -1;
        lv = Wave8Rules.frenzyLevel(lv);
        pd.putLong(K_FRENZY, now + 160);
        pd.putInt(K_FRENZY_LV, lv);
        Fx.addEffect(p, "minecraft:haste", 160, lv + 1);                                                  // the immediate action: it swings again, faster
        Fx.addEffect(p, "minecraft:speed", 160, lv);
        if (lv >= 2) Fx.addEffect(p, "minecraft:strength", 160, 0);
        Fx.addEffect(p, "minecraft:hunger", 160, 0);                                                      // the smell of death makes the sword and its user go wild
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1.0, p.getZ(), 10, 0.4, 0.6, 0.4, 0.03);
        Wave8Util.tell(p, "Feeding Frenzy " + (lv + 1));
    }

    // ================================================================ cleansing the curse
    static void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack s = e.getItem();
        boolean ench = s.is(Items.ENCHANTED_GOLDEN_APPLE);
        if (!ench && !s.is(Items.GOLDEN_APPLE)) return;
        int souls = Wave8Util.souls(p);
        if (souls <= 0) return;
        int left = Wave8Rules.cleanse(souls, ench);
        if (left == souls) {
            Wave8Util.tell(p, "The lich's hold is too deep for this.");
            return;
        }
        Wave8Util.setSouls(p, left);
        Fx.say(p, "The soul fragments leave you.");
    }

    // ================================================================ the player tick
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide()) return;
        CompoundTag pd = p.getPersistentData();
        long now = p.level().getGameTime();
        if (pd.contains(Wave8Acts.K_SLAM)) {                                                              // Braver: the landing
            long until = pd.getLong(Wave8Acts.K_SLAM);
            p.fallDistance = 0;
            if (now > until) {
                pd.remove(Wave8Acts.K_SLAM);
            } else if (p.onGround() && now - pd.getLong(Wave8Acts.K_SLAM_T0) > 4) {
                pd.remove(Wave8Acts.K_SLAM);
                Wave8Acts.slamLanding(p);
            }
        }
        if (pd.contains(Wave8Acts.K_RET_AT) && now >= pd.getLong(Wave8Acts.K_RET_AT)) {                   // the Leviathan Axe comes back
            Wave8Acts.leviathanReturn(p, pd);
            pd.remove(Wave8Acts.K_RET_AT);
        }
        String held = Wave8Util.heldId(p);
        if (p.tickCount % 4 == 0 && held.equals("frostfang") && RelicFlags.has(p, F_FROST) && RelicEngine.isOn(p, "frostfang", "footfalls")) footfalls(p);
        if (p.tickCount % 10 != 3) {
            return;
        }
        lichKnight(p, held);
        switch (held) {
            case "spotlight" -> {
                if (pd.getLong(Wave8Acts.K_SPOT) >= now && RelicFlags.has(p, F_SPOT)) {
                    ServerLevel l = p.serverLevel();
                    for (int i = 0; i < 8; i++) l.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 0.5 + i * 0.5, p.getZ(), 1, 0.2, 0.0, 0.2, 0.0);
                }
            }
            case "duality_blade" -> {
                if (RelicFlags.has(p, F_DUAL) && RelicEngine.isOn(p, "duality_blade", "streamers")) {      // bright light: you shine, and you see
                    Fx.addEffect(p, "minecraft:glowing", 60, 0);
                    Fx.addEffect(p, "minecraft:night_vision", 260, 0);
                    p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 4, 0.5, 0.6, 0.5, 0.01);
                }
            }
            case "leviathan_axe" -> {
                if (pd.getLong(Wave8Acts.K_AWAKE) >= now && RelicFlags.has(p, F_LEV)) {                    // the freezing fog of the awakened runes
                    for (LivingEntity t : Fx.foesAround(p, 4.0)) Wave8Util.chill(t, 60);
                    p.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.0, p.getZ(), 14, 1.8, 0.8, 1.8, 0.02);
                }
            }
            case "soul_shatter_blade" -> whisper(p, pd, now);
            default -> { }
        }
    }

    /** The lich's call (30 fragments): while the blade is in hand the wielder is stronger and frailer, as the lich's knight. */
    private static void lichKnight(ServerPlayer p, String held) {
        boolean want = held.equals("soul_shatter_blade") && RelicFlags.has(p, F_SSB) && Wave8Rules.lichCalls(Wave8Util.souls(p));
        AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH), atk = p.getAttribute(Attributes.ATTACK_DAMAGE);
        if (hp == null || atk == null) return;
        boolean has = hp.hasModifier(LICH_HP);
        if (want && !has) {
            hp.addTransientModifier(new AttributeModifier(LICH_HP, -4.0, AttributeModifier.Operation.ADD_VALUE));
            atk.addTransientModifier(new AttributeModifier(LICH_ATK, 2.0, AttributeModifier.Operation.ADD_VALUE));
        } else if (!want && has) {
            hp.removeModifier(LICH_HP);
            atk.removeModifier(LICH_ATK);
        }
    }

    /** From 25 fragments the lich speaks through the blade now and then. */
    private static void whisper(ServerPlayer p, CompoundTag pd, long now) {
        if (!RelicFlags.has(p, F_SSB) || !Wave8Rules.lichSpeaks(Wave8Util.souls(p)) || pd.getLong(K_WHISPER) > now) return;
        pd.putLong(K_WHISPER, now + 3600 + p.getRandom().nextInt(3600));
        Fx.say(p, WHISPERS[p.getRandom().nextInt(WHISPERS.length)]);
    }

    /** Footfalls of Ice: still water under and around the wielder freezes (frosted ice that melts again), and a chill trails the steps. */
    private static void footfalls(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        BlockPos base = p.blockPosition().below();
        int placed = 0;
        for (int dx = -2; dx <= 2 && placed < 10; dx++) {
            for (int dz = -2; dz <= 2 && placed < 10; dz++) {
                if (dx * dx + dz * dz > 5) continue;
                BlockPos pos = base.offset(dx, 0, dz);
                BlockState s = l.getBlockState(pos);
                if (!s.is(Blocks.WATER) || !s.getFluidState().isSource() || !l.getBlockState(pos.above()).isAir() || !l.mayInteract(p, pos)) continue;
                l.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                l.scheduleTick(pos, Blocks.FROSTED_ICE, Mth.nextInt(p.getRandom(), 60, 120));
                placed++;
            }
        }
        l.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 0.1, p.getZ(), 3, 0.3, 0.05, 0.3, 0.01);
        if (p.tickCount % 20 == 0) for (LivingEntity t : Fx.foesAround(p, 2.5)) Wave8Util.chill(t, 40);
    }
}
