package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.core.magic.profile.MagicKeys;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Pure validation of a merge request: the parents must be 2..4 DISTINCT real magics that are not restricted, the world must have room,
 * and a custom name must be clean and not taken. Returns the refusal (with the lang key the command shows) or a go-ahead with the
 * canonical parents, the id and the cleaned name.
 */
public final class MergeCheck {
    private MergeCheck() {}

    public enum Refusal {
        NONE("nusmp.merge.ok"),
        TOO_FEW("nusmp.merge.too_few"),
        TOO_MANY("nusmp.merge.too_many"),
        DUPLICATE_PARENT("nusmp.merge.duplicate_parent"),
        UNKNOWN_PARENT("nusmp.merge.unknown_parent"),
        RESTRICTED_PARENT("nusmp.merge.restricted_parent"),
        LIMIT_REACHED("nusmp.merge.limit_reached"),
        BAD_NAME("nusmp.merge.bad_name"),
        NAME_TAKEN("nusmp.merge.name_taken");

        public final String langKey;
        Refusal(String langKey) { this.langKey = langKey; }
    }

    /** @param detail the offending parent or name (for the message) */
    public record Result(Refusal refusal, String detail, List<String> parents, String id, String name) {
        public boolean ok() { return refusal == Refusal.NONE; }
    }

    private static Result no(Refusal r, String detail) { return new Result(r, detail == null ? "" : detail, List.of(), "", ""); }

    /**
     * @param rawParents   the parent names as typed (any case); duplicates are a refusal, not silently merged
     * @param customName   the owner's name, null or blank for the automatic one
     * @param existingCount how many merged magics the world already has (the limit applies to NEW ones only: see {@link #idAlreadyExists})
     * @param existsAlready is a merged magic with this id already there (then the count limit does not apply: the request is idempotent)
     * @param isRealMagic  is this canonical name a MagicType constant
     * @param takenNames   lower-case names already used by merged magics and reserved by the game's own magics
     */
    public static Result check(List<String> rawParents, String customName, int existingCount, Predicate<String> existsAlready,
                               Predicate<String> isRealMagic, Set<String> takenNames, java.util.function.Function<String, String> partName) {
        if (rawParents == null || rawParents.size() < MergeLimits.MIN_PARENTS) return no(Refusal.TOO_FEW, String.valueOf(rawParents == null ? 0 : rawParents.size()));
        if (rawParents.size() > MergeLimits.MAX_PARENTS) return no(Refusal.TOO_MANY, String.valueOf(rawParents.size()));
        List<String> canon = MergedKeys.canonicalParents(rawParents);
        if (canon.size() != rawParents.size()) {
            for (String r : rawParents) {
                int n = 0;
                for (String s : rawParents) if (MagicKeys.enumKey(s).equals(MagicKeys.enumKey(r))) n++;
                if (n > 1 || MagicKeys.enumKey(r).isEmpty()) return no(Refusal.DUPLICATE_PARENT, MagicKeys.enumKey(r));
            }
            return no(Refusal.DUPLICATE_PARENT, "");
        }
        for (String p : canon) {
            if (!isRealMagic.test(p)) return no(Refusal.UNKNOWN_PARENT, p);
            if (MagicKeys.isRestricted(p)) return no(Refusal.RESTRICTED_PARENT, p);
        }
        String id = MergedKeys.idOf(canon);
        boolean exists = existsAlready != null && existsAlready.test(id);
        if (!exists && existingCount >= MergeLimits.MAX_MERGED) return no(Refusal.LIMIT_REACHED, String.valueOf(MergeLimits.MAX_MERGED));
        String name;
        if (customName == null || customName.isBlank()) {
            name = MergeNaming.autoName(canon, partName);
        } else {
            name = MergeNaming.cleanCustomName(customName);
            if (name == null) return no(Refusal.BAD_NAME, customName);
            if (!exists && takenNames != null && takenNames.contains(name.toLowerCase(java.util.Locale.ROOT))) return no(Refusal.NAME_TAKEN, name);
        }
        return new Result(Refusal.NONE, "", canon, id, name);
    }
}
