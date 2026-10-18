package com.newuniverse.nusmp.elite.nations.contract;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Why: one live contract on a nation's board: copied from a template when posted, then TAKEN by one member (their progress is tracked), then DONE (paid) or EXPIRED (back to the board or removed). Pure data;
 * the rules that advance it live in {@link ContractRules}. {@code seen} holds the distinct keys of an explore contract.
 */
public final class Contract {
    public enum State { OPEN, TAKEN, DONE, EXPIRED }

    public long uid;
    public String templateId = "";
    public ContractType type = ContractType.KILL_MOB;
    public String target = "";
    public int goal = 1, progress, tier = 1;
    public Reward reward = Reward.NONE;
    public String title = "";
    public State state = State.OPEN;
    public UUID taker;
    public long createdAt, deadline;
    public final Set<String> seen = new LinkedHashSet<>();

    public static Contract from(long uid, ContractTemplate t, long now) {
        Contract c = new Contract();
        c.uid = uid;
        c.templateId = t.id();
        c.type = t.type();
        c.target = t.target();
        c.goal = t.count();
        c.tier = t.tier();
        c.reward = t.reward();
        c.title = t.title();
        c.createdAt = now;
        c.deadline = t.timeLimitMinutes() > 0 ? now + t.timeLimitMinutes() * 60L * 20L : 0L;
        return c;
    }

    public boolean complete() { return progress >= goal; }

    public boolean takenBy(UUID p) { return state == State.TAKEN && taker != null && taker.equals(p); }

    public boolean expired(long now) { return deadline > 0 && now >= deadline; }
}
