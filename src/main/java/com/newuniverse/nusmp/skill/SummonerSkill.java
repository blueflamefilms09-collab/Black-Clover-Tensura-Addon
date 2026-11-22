package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.AuraRules;
import com.newuniverse.nusmp.skill.rules.SummonRules;
import com.newuniverse.nusmp.skill.rules.SummonRules.Card;
import com.newuniverse.nusmp.skill.rules.SummonerRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * Summoner (Allen, Hell Mode): the ONE skill that holds everything Allen does (docs/summoner_rework.md). Modes (append-only indexes):
 * Beast Card (a tamed fighting wolf), Stone Card (a guardian golem), Recall (his Deletion: dismiss all of your summons), and with the rework on
 * Bird Card (the old Hawk Eye: a toggle, night vision and a glow on hostile creatures), Spirit Card (the old Spirit Card: a toggle, a healing
 * aura), Strengthening (a stat boost on all your summons) and Synthesis (two summons of the same card and rank become one of the next rank);
 * the stat reflection passive (the owner takes a share of what the summons they keep are good at) opens at 75 % mastery. Each folded-in mode
 * opens at a mastery share ({@link SummonerRules#unlockAt}); a holder carried over from the old Hawk Eye / Spirit Card has theirs at once
 * ({@link SummonerMigration}). Summons are capped (config: 3, mastered 6), follow and defend their owner, and vanish when the owner dies or leaves.
 * Summoner of Beginnings (class {@link LordOfSummonsSkill}, the Ultimate, registry id lord_of_summons) has every mode open, two more cards and
 * doubled numbers. The numbers are in {@link SummonRules} and {@link SummonerRules}; the creatures and their upkeep are in {@link Summons}.
 * With the SERVER setting summonerRework off the skill is exactly as before (three modes, five for the Ultimate, one mastery point a cast).
 * Canon pass (SERVER setting summonerCanon, docs/summoner_rework.md, Hell Mode wiki): modes open with the Summoning level (mastery / 7), the summon types without a
 * card of their own are casts (Insect, Fish, Spirit, Dragon, the Ultimate's Angel), plus Awakening, Deputize and Storage ({@link SummonerCanon}); mode 6 is the Grass
 * type's healing, the Bird card's sight is the Hawk's Hawk Eye; with it off the skill is exactly the 4.5.0 one.
 */
public class SummonerSkill extends NUSkill {
    public static final String SUMMON_TAG = "nusmp_summon";
    public static final String OWNER_KEY = "nusmp_owner";
    /** Tag keys on the skill instance (carried over when Summoner evolves): toggles that are on, and modes opened for good by the old skills. */
    static final String TAG_ON = "NuSummonerOn", TAG_FORCE = "NuSummonerForce";

    static final SkillText TEXT = SkillText.of("summoner", "Summoner",
            "Unique. Allen's class (Hell Mode): call summon cards (Beast, Insect), dismiss them with Recall (Deletion), and as your Summoning level grows open Synthesis and the Bird card (2), Strengthening and the Grass card (3), the Stone card and Storage (4), the Fish card (5), Awakening and the Spirit card (6), the Dragon card and Deputize (7).");

    /** Mode indexes (append-only: the Ultimate adds LEGION and WRAITH after Recall, the folded-in skills come after those). */
    static final int BEAST = SummonerRules.BEAST, STONE = SummonerRules.STONE, RECALL = SummonerRules.RECALL, LEGION = SummonerRules.LEGION,
            WRAITH = SummonerRules.WRAITH, BIRD = SummonerRules.BIRD, SPIRIT = SummonerRules.SPIRIT, STRENGTHEN = SummonerRules.STRENGTHEN,
            SYNTHESIS = SummonerRules.SYNTHESIS;

    public SummonerSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected SummonerSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    @Override public int getModes(ManasSkillInstance instance) { return NUConfig.summonerCanon() ? SummonerRules.MODES_CANON : NUConfig.summonerRework() ? SummonerRules.MODES : ultimate ? 5 : 3; }

    private boolean listed(int mode) {
        if (NUConfig.summonerCanon()) return SummonerRules.listedCanon(mode, ultimate);
        return NUConfig.summonerRework() ? SummonerRules.listed(mode, ultimate) : mode < (ultimate ? 5 : 3);
    }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        int count = getModes(instance), m = mode;
        for (int i = 0; i < count; i++) {
            m = reverse ? (m + count - 1) % count : (m + 1) % count;
            if (listed(m)) return m;
        }
        return mode;
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return "summoner." + switch (mode) {
            case BEAST -> "beast";
            case STONE -> "stone";
            case BIRD -> "bird";
            case SPIRIT -> "spirit";
            case STRENGTHEN -> "strengthen";
            case SYNTHESIS -> "synthesis";
            case SummonerRules.INSECT -> "insect";
            case SummonerRules.FISH -> "fish";
            case SummonerRules.SPIRIT_CAST -> "spirit_card";
            case SummonerRules.DRAGON -> "dragon";
            case SummonerRules.AWAKENING -> "awakening";
            case SummonerRules.DEPUTIZE -> "deputize";
            case SummonerRules.STORAGE -> "storage";
            case SummonerRules.ANGEL -> "angel";
            default -> "recall";
        };
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        return switch (mode) {
            case BEAST -> SkillMsg.mode("summoner.beast", "Beast Card");
            case STONE -> SkillMsg.mode("summoner.stone", "Stone Card");
            case BIRD -> SkillMsg.mode("summoner.bird", "Bird Card");
            case SPIRIT -> NUConfig.summonerCanon() ? SkillMsg.mode("summoner.grass", "Grass Card") : SkillMsg.mode("summoner.spirit", "Spirit Card");   // canon: the healing pulse is the Grass type's role
            case STRENGTHEN -> SkillMsg.mode("summoner.strengthen", "Strengthening");
            case SYNTHESIS -> SkillMsg.mode("summoner.synthesis", "Synthesis");
            case SummonerRules.INSECT -> SkillMsg.mode("summoner.insect", "Insect Card");
            case SummonerRules.FISH -> SkillMsg.mode("summoner.fish", "Fish Card");
            case SummonerRules.SPIRIT_CAST -> SkillMsg.mode("summoner.spirit_card", "Spirit Card");
            case SummonerRules.DRAGON -> SkillMsg.mode("summoner.dragon", "Dragon Card");
            case SummonerRules.AWAKENING -> SkillMsg.mode("summoner.awakening", "Awakening");
            case SummonerRules.DEPUTIZE -> SkillMsg.mode("summoner.deputize", "Deputize");
            case SummonerRules.STORAGE -> SkillMsg.mode("summoner.storage", "Storage");
            case SummonerRules.ANGEL -> SkillMsg.mode("summoner.angel", "Angel Card");
            default -> SkillMsg.mode("summoner.recall", "Recall");
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        boolean canon = NUConfig.summonerCanon();
        if (mode >= BIRD || (canon && mode == STONE)) {
            if (!NUConfig.summonerRework() || (mode >= SummonerRules.INSECT && !canon) || (mode == SummonerRules.ANGEL && !ultimate)) {
                SkillUtil.fail(player, SkillMsg.t("summoner.reworked_off", "This Summoner mode is switched off on this server."));
                return;
            }
            if (!modeUnlocked(instance, player, mode)) {
                if (canon) SkillUtil.fail(player, SkillMsg.t("summoner.locked_canon", "%s opens at Summoning level %s (%s%% mastery of the Summoner).", getModeName(instance, mode), SummonerRules.canonLevel(mode), SummonerRules.canonUnlockPercent(mode)));
                else SkillUtil.fail(player, SkillMsg.t("summoner.locked", "%s opens at %s%% mastery of the Summoner.", getModeName(instance, mode), SummonerRules.unlockPercent(mode)));
                return;
            }
        }
        switch (mode) {
            case RECALL -> recall(player);
            case BEAST -> castCard(instance, player, Card.BEAST, mode);
            case STONE -> castCard(instance, player, Card.STONE, mode);
            case BIRD, SPIRIT -> toggleMode(instance, player, mode);
            case STRENGTHEN -> strengthen(instance, player, mode);
            case SYNTHESIS -> synthesize(instance, player, mode);
            case SummonerRules.INSECT -> canonCast(instance, player, mode, SummonerRules.Family.INSECT, 1, (t, mf) -> SummonerCanon.insect(player, t));
            case SummonerRules.FISH -> canonCast(instance, player, mode, SummonerRules.Family.FISH, 1, (t, mf) -> SummonerCanon.fish(player, t));
            case SummonerRules.SPIRIT_CAST -> canonCast(instance, player, mode, SummonerRules.Family.SPIRIT, 2, (t, mf) -> SummonerCanon.spirit(player, t, instance, mf));
            case SummonerRules.DRAGON -> canonCast(instance, player, mode, SummonerRules.Family.DRAGON, 2, (t, mf) -> SummonerCanon.dragon(player, t, instance, mf));
            case SummonerRules.ANGEL -> canonCast(instance, player, mode, null, 3, (t, mf) -> SummonerCanon.angel(player, t, instance, mf));
            case SummonerRules.AWAKENING -> awaken(instance, player, mode);
            case SummonerRules.DEPUTIZE -> deputize(instance, player);
            case SummonerRules.STORAGE -> SummonerCanon.openStorage(player);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ unlocks and the two toggles
    /** True when this holder may use the folded-in mode: by mastery, as the Ultimate, or because the old skill's holder keeps it. */
    final boolean modeUnlocked(ManasSkillInstance instance, ServerPlayer player, int mode) {
        CompoundTag tag = instance.getTag();
        int forced = tag == null ? 0 : tag.getInt(TAG_FORCE);
        if (NUConfig.summonerCanon()) return SummonerRules.unlockedCanon(mode, masteryFrac(instance), tier(instance, player).ultimate(), SummonerRules.forced(forced, mode));
        return SummonerRules.unlocked(mode, masteryFrac(instance), tier(instance, player).ultimate(), SummonerRules.forced(forced, mode));
    }

    private void toggleMode(ManasSkillInstance instance, ServerPlayer player, int mode) {
        int bit = SummonerRules.forceFlag(mode);
        CompoundTag tag = instance.getOrCreateTag();
        int on = tag.getInt(TAG_ON);
        boolean now = (on & bit) == 0;
        tag.putInt(TAG_ON, now ? on | bit : on & ~bit);
        instance.markDirty();
        Component name = getModeName(instance, mode);
        SkillUtil.actionbar(player, now ? SkillMsg.t("toggle.on", "%s: ON", name).withStyle(ChatFormatting.GREEN)
                : SkillMsg.t("toggle.off", "%s: OFF", name).withStyle(ChatFormatting.GRAY));
        if (!now && mode == BIRD) HawkEyeSkill.clearSight(player);
        if (now) Summons.perform(player, mode == BIRD ? com.newuniverse.nusmp.skill.rules.SummonSpecies.TSUBAME : com.newuniverse.nusmp.skill.rules.SummonSpecies.SOLARIN, tier(instance, player));   // 4.106.0: Tsubame (Bird) / Solarin (Grass) appear when the pulse turns on
    }

    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        CompoundTag tag = instance.getTag();
        return tag != null && tag.getInt(TAG_ON) != 0 && NUConfig.summonerRework();
    }

    /** The Bird Card and the Spirit Card pulses (the old Hawk Eye and Spirit Card toggles): paid per pulse, off by itself when the magicules run out. */
    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer player) || !player.isAlive() || !NUConfig.summonerRework()) return;
        CompoundTag tag = instance.getTag();
        int on = tag == null ? 0 : tag.getInt(TAG_ON);
        if (on == 0) return;
        // a player who holds both forms only runs the Ultimate's toggles
        if (!ultimate && SkillAPI.getSkillsFrom(player).getSkill(NUSkills.LORD_OF_SUMMONS.get().getRegistryName()).isPresent()) return;
        Tier tier = tier(instance, player);
        int now = player.tickCount;
        boolean changed = false;
        if ((on & SummonerRules.FORCE_BIRD) != 0 && now % SummonerRules.sightInterval() == 0) {
            if (!modeUnlocked(instance, player, BIRD) || !pay(instance, player, BIRD, DMUtil.cost(AuraRules.HAWK_DRAIN))) {
                switchOff(instance, player, BIRD);
                changed = true;
            } else HawkEyeSkill.sight(player, tier);
        }
        if ((on & SummonerRules.FORCE_SPIRIT) != 0 && now % Math.max(1, SummonerRules.spiritInterval()) == 0) {
            if (!modeUnlocked(instance, player, SPIRIT) || !pay(instance, player, SPIRIT, DMUtil.cost(AuraRules.SPIRIT_DRAIN))) {
                switchOff(instance, player, SPIRIT);
                changed = true;
            } else SpiritCardSkill.heal(player, tier);
        }
        if (now % 200 == 0 && instance.getTag().getInt(TAG_ON) != 0) {
            instance.addMasteryPoint(player, SummonerRules.pulseGain(instance.getMaxMastery(), NUConfig.summonerMasteryPercent(), true));
            changed = true;
        }
        if (changed) instance.markDirty();
    }

    private void switchOff(ManasSkillInstance instance, ServerPlayer player, int mode) {
        CompoundTag tag = instance.getOrCreateTag();
        tag.putInt(TAG_ON, tag.getInt(TAG_ON) & ~SummonerRules.forceFlag(mode));
        SkillUtil.actionbar(player, SkillMsg.t("toggle.off", "%s: OFF", getModeName(instance, mode)).withStyle(ChatFormatting.GRAY));
        if (mode == BIRD) HawkEyeSkill.clearSight(player);
    }

    // ------------------------------------------------------------------ Strengthening and Synthesis
    /** Strengthening: a stat boost on every summon within range, for a while. */
    private void strengthen(ManasSkillInstance instance, ServerPlayer player, int mode) {
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("summoner.cooldown", "Summoner is on cooldown.")); return; }
        if (Summons.owned(player).isEmpty()) { SkillUtil.fail(player, SkillMsg.t("summoner.no_summons", "You have no summons to strengthen.")); return; }
        Tier tier = tier(instance, player);
        boolean canon = NUConfig.summonerCanon();
        double cardCost = SummonRules.cost(Card.BEAST, NUConfig.SUMMON_MAGICULE_COST.get());
        double price = price(instance, player, canon ? SummonerRules.canonStrengthenCost(cardCost) : SummonerRules.strengthenCost(cardCost));
        if (!charge(instance, player, mode, price)) return;
        int n = canon ? Summons.strengthenCanon(player, SummonerRules.strengthenLevel(masteryFrac(instance), tier.ultimate())) : Summons.strengthen(player, tier);
        SkillUtil.actionbar(player, SkillMsg.t("summoner.strengthened", "Strengthened %s summon(s).", n).withStyle(ChatFormatting.AQUA));
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.EVOKER_CAST_SPELL, 0.9F, 0.9F);
        cooldown(instance, mode, SummonerRules.strengthenCooldown());
        grow(instance, player, SummonerRules.castGain(instance.getMaxMastery(), 1, NUConfig.summonerMasteryPercent(), true));
    }

    /** Synthesis: two summons of the same card and rank become one of the next rank (one fewer creature, never more). */
    private void synthesize(ManasSkillInstance instance, ServerPlayer player, int mode) {
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("summoner.cooldown", "Summoner is on cooldown.")); return; }
        Tier tier = tier(instance, player);
        List<Mob> two = Summons.pair(player, tier);
        if (two.isEmpty()) { SkillUtil.fail(player, SkillMsg.t("summoner.no_pair", "Synthesis needs two summons of the same card and rank (and below the top rank).")); return; }
        double price = price(instance, player, SummonerRules.synthesisCost(SummonRules.cost(Card.BEAST, NUConfig.SUMMON_MAGICULE_COST.get())));
        if (!charge(instance, player, mode, price)) return;
        Mob made = Summons.merge(player, two, tier);
        if (made == null) {
            SkillUtil.refund(player, price);
            SkillUtil.fail(player, SkillMsg.t("summoner.failed", "The card did not answer."));
            return;
        }
        SkillUtil.actionbar(player, SkillMsg.t("summoner.synthesized", "Two cards became one: %s.", made.getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 1.5F);
        cooldown(instance, mode, SummonerRules.synthesisCooldown());
        grow(instance, player, SummonerRules.castGain(instance.getMaxMastery(), 2, NUConfig.summonerMasteryPercent(), true));
    }

    // ------------------------------------------------------------------ canon: the summon types, Awakening, Deputize
    /** Shared flow of a type cast: cooldown, price (the type's creation mana ratio, or ten card prices for the Angel), the effect, cooldown, mastery. A cast that touched nothing is refunded. */
    private void canonCast(ManasSkillInstance instance, ServerPlayer player, int mode, SummonerRules.Family family, int weight, java.util.function.BiFunction<Tier, Double, Integer> effect) {
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("summoner.cooldown", "Summoner is on cooldown.")); return; }
        Tier tier = tier(instance, player);
        double cardCost = SummonRules.cost(Card.BEAST, NUConfig.SUMMON_MAGICULE_COST.get());
        double price = price(instance, player, family == null ? SummonerRules.awakeningCost(cardCost) : SummonerRules.typeCost(cardCost, family));
        if (!charge(instance, player, mode, price)) return;
        int n = effect.apply(tier, masteryFrac(instance));
        if (n <= 0) {
            SkillUtil.refund(player, price);
            SkillUtil.fail(player, SkillMsg.t("summoner.canon_nothing", "Nothing answered the card."));
            return;
        }
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.0F);
        cooldown(instance, mode, SummonerRules.castCooldown(NUConfig.SUMMON_COOLDOWN.get(), mode));
        grow(instance, player, SummonerRules.castGain(instance.getMaxMastery(), weight, NUConfig.summonerMasteryPercent(), true));
    }

    /** Awakening (100 MP, once a day; 6 h / 3 h / 1 h with a Soldier / General / King among your summons): every summon uses its awakened skill. */
    private void awaken(ManasSkillInstance instance, ServerPlayer player, int mode) {
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("summoner.cooldown", "Summoner is on cooldown.")); return; }
        Tier tier = tier(instance, player);
        double price = price(instance, player, SummonerRules.awakeningCost(SummonRules.cost(Card.BEAST, NUConfig.SUMMON_MAGICULE_COST.get())));
        if (!charge(instance, player, mode, price)) return;
        int n = SummonerCanon.awaken(player, tier, modeUnlocked(instance, player, BIRD), modeUnlocked(instance, player, SPIRIT));
        if (n <= 0) {
            SkillUtil.refund(player, price);
            SkillUtil.fail(player, SkillMsg.t("summoner.canon_nothing", "Nothing answered the card."));
            return;
        }
        SkillUtil.actionbar(player, SkillMsg.t("summoner.awakened", "Awakened: %s summon(s) used their awakened skill.", Summons.owned(player).size()).withStyle(ChatFormatting.GOLD));
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 0.6F);
        cooldown(instance, mode, SummonerRules.awakeningCooldown(Summons.topCommand(player)));
        grow(instance, player, SummonerRules.castGain(instance.getMaxMastery(), 3, NUConfig.summonerMasteryPercent(), true));
    }

    /** Deputize (free): the summon you look at becomes a General (three per kind), a General a King with Kingnification (the Ultimate), a King a plain summon again. */
    private void deputize(ManasSkillInstance instance, ServerPlayer player) {
        LivingEntity look = SkillTargets.lookAlly(player, 24.0);
        if (!(look instanceof Mob mob) || !Summons.ownedBy(mob, player.getUUID())) {
            SkillUtil.fail(player, SkillMsg.t("summoner.deputize_look", "Look at one of your summons to deputize it."));
            return;
        }
        int now = Summons.deputize(player, mob, tier(instance, player).ultimate());
        if (now < 0) {
            SkillUtil.fail(player, SkillMsg.t("summoner.deputize_limit", "That kind of summon already has three Generals."));
            return;
        }
        Component what = switch (now) {
            case SummonerRules.CMD_GENERAL -> SkillMsg.t("summoner.general", "General");
            case SummonerRules.CMD_KING -> SkillMsg.t("summoner.king", "King");
            default -> SkillMsg.t("summoner.plain", "plain summon");
        };
        SkillUtil.actionbar(player, SkillMsg.t("summoner.deputized", "%s is now: %s.", mob.getName(), what).withStyle(ChatFormatting.GOLD));
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 1.8F);
        grow(instance, player, SummonerRules.castGain(instance.getMaxMastery(), 1, NUConfig.summonerMasteryPercent(), true));
    }

    /** Recall (Allen's Deletion): every summon of the caster goes away (never anybody else's). */
    protected final void recall(ServerPlayer player) {
        int removed = Summons.recall(player);
        SkillUtil.actionbar(player, Summons.countMessage(removed));
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.EVOKER_CAST_SPELL, 0.8F, 1.4F);
    }

    /** The shared flow of a card: cooldown, cap, cost, creatures, effects, cooldown, mastery. */
    protected final void castCard(ManasSkillInstance instance, ServerPlayer player, Card card, int mode) {
        Tier tier = tier(instance, player);
        if (!SummonRules.available(card, tier)) {
            SkillUtil.fail(player, SkillMsg.t("summoner.lord_only", "Only the Summoner of Beginnings can call this card."));
            return;
        }
        if (instance.onCoolDown(mode)) {
            SkillUtil.fail(player, SkillMsg.t("summoner.cooldown", "Summoner is on cooldown."));
            return;
        }

        int max = SummonRules.maxSummons(NUConfig.SUMMON_MAX.get(), NUConfig.SUMMON_MAX_MASTERED.get(), tier);
        int want = SummonRules.mobCount(card, tier);
        int count = SummonRules.allowed(Summons.owned(player).size(), max, want);
        if (count <= 0) {
            SkillUtil.fail(player, SkillMsg.t("summoner.cap", "You can only have %s summons at once.", max));
            return;
        }
        double price = price(instance, player, SummonRules.cost(card, NUConfig.SUMMON_MAGICULE_COST.get()) * count / want);   // a partial cast costs a partial price
        if (!charge(instance, player, mode, price)) return;

        int made = Summons.spawn(player, card, tier, count);
        if (made <= 0) {
            SkillUtil.refund(player, price);
            SkillUtil.fail(player, SkillMsg.t("summoner.failed", "The card did not answer."));
            return;
        }
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, card == Card.STONE ? 0.8F : 1.2F);
        SkillFx.ring(player.serverLevel(), player.position(), 2.5, card == Card.WRAITH ? 0xB070FF : 0xFFC860);
        SkillAnimeFx2.summon(player, card == Card.WRAITH, card == Card.LEGION);         // VFX wave 4: the card flips open on a rune circle (also the Ultimate's cards)
        cooldown(instance, mode, SummonRules.cooldown(card, NUConfig.SUMMON_COOLDOWN.get()));
        SkillUtil.castVfx(player, 0xFFB8A0FF);
        grow(instance, player, SummonerRules.castGain(instance.getMaxMastery(), SummonRules.masteryGain(card), NUConfig.summonerMasteryPercent(), NUConfig.summonerRework()));
    }

    // ------------------------------------------------------------------ stat reflection (passive) and the cap
    /** The Summoner or the Ultimate the player holds (the Ultimate first), or null. */
    static ManasSkillInstance holder(ServerPlayer player) {
        var skills = SkillAPI.getSkillsFrom(player);
        var lord = skills.getSkill(NUSkills.LORD_OF_SUMMONS.get().getRegistryName());
        if (lord.isPresent()) return lord.get();
        return skills.getSkill(NUSkills.SUMMONER.get().getRegistryName()).orElse(null);
    }

    /**
     * Stat reflection (the anime's season two: Allen takes a share of the attributes of the summons he keeps): once a second, for a holder who has
     * opened it, a short effect per kind of card alive (speed for wolves, resistance for golems, slow fall for flying spirits); a level higher
     * with four or more summons of an Ultimate. At most three short effects a second, nothing stored.
     */
    static void reflect(ServerPlayer player, List<Mob> mine) {
        if (!NUConfig.summonerRework() || mine.isEmpty()) return;
        ManasSkillInstance inst = holder(player);
        boolean canon = NUConfig.summonerCanon();   // canon: the summons you keep always buff you (the wiki's status pages), so no mastery gate
        if (inst == null || !(canon || SummonerRules.reflects(masteryFrac(inst), inst.getSkill() instanceof SummonerSkill s && s.ultimate))) return;
        int wolves = 0, golems = 0, spirits = 0;
        for (Mob m : mine) {
            int c = m.getPersistentData().getInt(Summons.CARD);
            if (c == Card.STONE.ordinal()) golems++;
            else if (c == Card.WRAITH.ordinal()) spirits++;
            else wolves++;
        }
        boolean strong = inst.getSkill() instanceof SummonerSkill s2 && s2.ultimate;
        give(player, canon ? MobEffects.DAMAGE_BOOST : MobEffects.MOVEMENT_SPEED, SummonerRules.reflectLevel(wolves, strong));   // canon: Beast summons give attack
        give(player, MobEffects.DAMAGE_RESISTANCE, SummonerRules.reflectLevel(golems, strong));
        give(player, MobEffects.SLOW_FALLING, SummonerRules.reflectLevel(spirits, false));
    }

    private static void give(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int level) {
        if (level >= 0) player.addEffect(new MobEffectInstance(effect, SummonerRules.REFLECT_TICKS, level, true, false));
    }

    /** The cap that applies to this player's upkeep: their strongest Summoner skill's, or no trimming if they have neither. */
    static int capFor(ServerPlayer player) {
        var skills = SkillAPI.getSkillsFrom(player);
        var lord = skills.getSkill(NUSkills.LORD_OF_SUMMONS.get().getRegistryName());
        if (lord.isPresent()) return SummonRules.maxSummons(NUConfig.SUMMON_MAX.get(), NUConfig.SUMMON_MAX_MASTERED.get(), Tier.ULTIMATE);
        var base = skills.getSkill(NUSkills.SUMMONER.get().getRegistryName());
        if (base.isPresent()) {
            return SummonRules.maxSummons(NUConfig.SUMMON_MAX.get(), NUConfig.SUMMON_MAX_MASTERED.get(),
                    Tier.of(base.get().isMastered(player), false));
        }
        return SummonRules.HARD_CAP;
    }
}
