package com.newuniverse.nusmp.avatar.party;

import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.Stance;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.avatar.goal.AvatarTargeting;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: an Avatar that names wild monsters keeps them as its NPC party, and somebody has to remember who is whose: which monsters belong to which Avatar (so a
 * party survives a restart and a monster that is not loaded is still counted), when each Avatar last named one (the cooldown), what the party costs it, and what
 * happens to the party when the Avatar falls. This is that record (a world-wide SavedData like {@code AvatarManager}) plus the once-a-second pass that looks after
 * the members that are loaded: it drops the dead, keeps the party together (a member that has fallen far behind is brought to the Avatar's side), shares the
 * Avatar's target with them and takes back any target the Avatar itself may not attack (its creator, the creator's friends and pets, the party), and puts the party
 * tax on the Avatar's maximum magicule. The truth about a member is a mark on the monster itself ({@link #MARK}, the Avatar's id, in its persistent data); this
 * record is the Avatar's index of them. A betrayed Avatar releases its party (a betrayal severs every bond); a dead one releases it too.
 */
public final class AvatarPartyManager extends SavedData {
    /** The persistent-data key a party member carries: the id of the Avatar it belongs to. */
    public static final String MARK = "nusmp_party_of";
    private static final String NAME = "nusmp_avatar_party";
    private static final ResourceLocation TAX_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_party_tax");
    /** A member that fell this far behind is brought back to the Avatar's side. */
    private static final double RECALL_DISTANCE = 28.0, REGROUP_DISTANCE = 7.0;

    /** One party member. {@code x}, {@code z} and {@code dim} are where it was last seen; {@code misses} counts the seconds it was missing from a loaded place. */
    public static final class Member {
        public final UUID id;
        public String name;
        public String dim = "";
        public int x, z, misses;

        Member(UUID id, String name) {
            this.id = id;
            this.name = name == null ? "" : name;
        }
    }

    private final Map<UUID, List<Member>> parties = new LinkedHashMap<>();
    private final Map<UUID, Long> lastNamed = new HashMap<>();

    public static AvatarPartyManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(AvatarPartyManager::new, AvatarPartyManager::load, null), NAME);
    }

    private static AvatarPartyManager load(CompoundTag tag, HolderLookup.Provider provider) {
        AvatarPartyManager m = new AvatarPartyManager();
        ListTag list = tag.getList("Parties", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                CompoundTag t = list.getCompound(i);
                UUID avatar = t.getUUID("Avatar");
                if (t.contains("Named")) m.lastNamed.put(avatar, t.getLong("Named"));
                List<Member> members = new ArrayList<>();
                ListTag ms = t.getList("Members", Tag.TAG_COMPOUND);
                for (int k = 0; k < ms.size(); k++) {
                    CompoundTag c = ms.getCompound(k);
                    Member mem = new Member(c.getUUID("Id"), c.getString("Name"));
                    mem.dim = c.getString("Dim");
                    mem.x = c.getInt("X");
                    mem.z = c.getInt("Z");
                    members.add(mem);
                }
                if (!members.isEmpty()) m.parties.put(avatar, members);
            } catch (RuntimeException skip) { /* a damaged row is dropped, the rest still load */ }
        }
        return m;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        Set<UUID> avatars = new LinkedHashSet<>(parties.keySet());
        avatars.addAll(lastNamed.keySet());
        for (UUID avatar : avatars) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Avatar", avatar);
            if (lastNamed.containsKey(avatar)) t.putLong("Named", lastNamed.get(avatar));
            ListTag ms = new ListTag();
            for (Member mem : parties.getOrDefault(avatar, List.of())) {
                CompoundTag c = new CompoundTag();
                c.putUUID("Id", mem.id);
                c.putString("Name", mem.name);
                c.putString("Dim", mem.dim);
                c.putInt("X", mem.x);
                c.putInt("Z", mem.z);
                ms.add(c);
            }
            t.put("Members", ms);
            list.add(t);
        }
        tag.put("Parties", list);
        return tag;
    }

    // ---------------------------------------------------------------- the record
    public List<Member> members(UUID avatar) { return parties.getOrDefault(avatar, List.of()); }

    public int count(UUID avatar) { return members(avatar).size(); }

    /** Game time of the Avatar's last naming, 0 when it never named one. */
    public long lastNamedAt(UUID avatar) { return lastNamed.getOrDefault(avatar, 0L); }

    public Set<String> namesOf(UUID avatar) {
        Set<String> out = new LinkedHashSet<>();
        for (Member m : members(avatar)) out.add(m.name);
        return out;
    }

    /** Adds a monster to the Avatar's party and starts the naming cooldown. */
    public void add(UUID avatar, UUID member, String name, long now) {
        parties.computeIfAbsent(avatar, k -> new ArrayList<>()).add(new Member(member, name));
        lastNamed.put(avatar, now);
        setDirty();
    }

    public boolean remove(UUID avatar, UUID member) {
        List<Member> list = parties.get(avatar);
        if (list == null || !list.removeIf(m -> m.id.equals(member))) return false;
        if (list.isEmpty()) parties.remove(avatar);
        setDirty();
        return true;
    }

    /** Forgets the Avatar's party altogether (the cooldown goes with it). */
    public void forgetAvatar(UUID avatar) {
        boolean a = parties.remove(avatar) != null;
        boolean b = lastNamed.remove(avatar) != null;
        if (a || b) setDirty();
    }

    // ---------------------------------------------------------------- marks
    /** The id of the Avatar this monster belongs to, or null when it is nobody's party member. */
    public static UUID markOf(Entity e) {
        if (e == null) return null;
        CompoundTag d = e.getPersistentData();
        return d.hasUUID(MARK) ? d.getUUID(MARK) : null;
    }

    public static boolean isMember(Entity e) { return markOf(e) != null; }

    /** The Avatar a party member belongs to (found in the member's own level), or null. */
    public static AvatarEntity avatarOf(Entity member) {
        UUID id = markOf(member);
        if (id == null || !(member.level() instanceof ServerLevel sl)) return null;
        return sl.getEntity(id) instanceof AvatarEntity a ? a : null;
    }

    // ---------------------------------------------------------------- the once-a-second pass (called from the Avatar's own slow tick)
    /** Looks after one Avatar's party. Cheap when the party is empty. Server side. */
    public static void tick(AvatarEntity a) {
        if (!(a.level() instanceof ServerLevel sl)) return;
        AvatarPartyManager mgr = get(sl.getServer());
        List<Member> list = mgr.members(a.getUUID());
        if (list.isEmpty()) {
            applyTax(a, 0);
            return;
        }
        if (a.isBetrayed()) {
            release(sl.getServer(), a.getUUID());
            applyTax(a, 0);
            return;
        }
        LivingEntity target = a.getTarget();
        String dim = sl.dimension().location().toString();
        boolean changed = false;
        for (Member m : new ArrayList<>(list)) {
            Entity e = sl.getEntity(m.id);
            if (e == null) {
                // not in this level: unloaded, or somewhere else. Only a member whose last known place is loaded and EMPTY now has really gone.
                if (dim.equals(m.dim) && sl.hasChunkAt(BlockPos.containing(m.x, 64, m.z))) {
                    if (++m.misses >= 3) { mgr.remove(a.getUUID(), m.id); changed = true; }
                }
                continue;
            }
            m.misses = 0;
            if (!(e instanceof Mob mob) || !e.isAlive() || !a.getUUID().equals(markOf(e))) {
                mgr.remove(a.getUUID(), m.id);
                changed = true;
                continue;
            }
            m.dim = dim;
            m.x = e.getBlockX();
            m.z = e.getBlockZ();
            assist(a, mob, target);
        }
        if (changed) mgr.setDirty();
        applyTax(a, mgr.count(a.getUUID()));
    }

    /** One member, one second: no forbidden target, the Avatar's target, back to the Avatar's side if it fell behind. */
    private static void assist(AvatarEntity a, Mob mob, LivingEntity avatarTarget) {
        mob.setPersistenceRequired();
        LivingEntity own = mob.getTarget();
        if (own != null && (!AvatarTargeting.canHarm(a, own) || isMember(own))) {
            mob.setTarget(null);
            own = null;
        }
        if (avatarTarget != null && avatarTarget != mob && avatarTarget.isAlive() && own != avatarTarget && AvatarTargeting.canHarm(a, avatarTarget) && !isMember(avatarTarget)
                && mob.distanceToSqr(a) < 32.0 * 32.0) {
            mob.setTarget(avatarTarget);
            own = avatarTarget;
        }
        double d2 = mob.distanceToSqr(a);
        if (d2 > RECALL_DISTANCE * RECALL_DISTANCE) recall(a, mob);
        else if (own == null && d2 > REGROUP_DISTANCE * REGROUP_DISTANCE && a.stance() != Stance.STAY) mob.getNavigation().moveTo(a, 1.1);
    }

    /** Brings a member that fell far behind to a free spot beside the Avatar. */
    private static void recall(AvatarEntity a, Mob mob) {
        if (mob.level() != a.level()) return;
        for (int i = 0; i < 8; i++) {
            double x = a.getX() + (a.getRandom().nextDouble() - 0.5) * 6.0;
            double z = a.getZ() + (a.getRandom().nextDouble() - 0.5) * 6.0;
            AABB box = mob.getBoundingBox().move(x - mob.getX(), a.getY() - mob.getY(), z - mob.getZ());
            if (a.level().noCollision(mob, box)) {
                mob.teleportTo(x, a.getY(), z);
                mob.getNavigation().stop();
                return;
            }
        }
    }

    /** The party tax: a share of the Avatar's maximum magicule per living member, for as long as the member lives (one transient modifier, updated, never stacked). */
    static void applyTax(AvatarEntity a, int members) {
        try {
            AttributeInstance inst = a.getAttribute(TensuraAttributes.MAX_MAGICULE);
            if (inst == null) return;
            double tax = PartyRules.tax(members, AvatarAiConfig.partyTaxPercent());
            AttributeModifier old = inst.getModifier(TAX_ID);
            if (tax <= 0.0) {
                if (old != null) inst.removeModifier(TAX_ID);
                return;
            }
            if (old != null && Math.abs(old.amount() + tax) < 1.0e-9) return;
            inst.removeModifier(TAX_ID);
            inst.addTransientModifier(new AttributeModifier(TAX_ID, -tax, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } catch (Throwable soft) {
            // no Tensura attribute on this build: the party simply costs nothing
        }
    }

    // ---------------------------------------------------------------- the party ends
    /** The Avatar fell (or left the world for good): its party is released. */
    public static void onAvatarGone(MinecraftServer server, AvatarEntity a) {
        release(server, a.getUUID());
    }

    /** Frees every member of the Avatar's party that can be found: the mark and Tensura's owner record go, the monsters stay named. */
    public static void release(MinecraftServer server, UUID avatar) {
        AvatarPartyManager mgr = get(server);
        for (Member m : new ArrayList<>(mgr.members(avatar))) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity e = level.getEntity(m.id);
                if (e == null) continue;
                free(e);
                break;
            }
        }
        mgr.forgetAvatar(avatar);
    }

    private static void free(Entity e) {
        e.getPersistentData().remove(MARK);
        if (!(e instanceof Mob mob)) return;
        TensuraBridge.setPermanentOwner(mob, null);
        try {
            if (mob instanceof TamableAnimal t) {
                t.setOwnerUUID(null);
                t.setTame(false, false);
            }
        } catch (RuntimeException soft) { /* a monster that refuses to forget its master is still released from the party */ }
        mob.setTarget(null);
    }

    /** A party member died: its Avatar's record forgets it (and the tax with it). */
    public static void onDeath(LivingDeathEvent e) {
        Entity dead = e.getEntity();
        if (dead.level().isClientSide || !(dead.level() instanceof ServerLevel sl)) return;
        try {
            UUID owner = markOf(dead);
            if (owner != null) get(sl.getServer()).remove(owner, dead.getUUID());
        } catch (RuntimeException soft) { /* never break a death */ }
    }
}
