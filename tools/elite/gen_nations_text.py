#!/usr/bin/env python3
"""Why: the English texts of the Nations & War system live in ONE place (this file). It writes
  - tools/elite/nations_lang.json                      (the lang fragment the lead merges into en_us.json; the owner's rule: no edits to en_us.json from a builder)
  - src/main/java/com/newuniverse/nusmp/elite/nations/core/NationText.java   (the same strings as the English FALLBACK of every message, so the mod reads right before the merge)
Run from the repo root: python3 tools/elite/gen_nations_text.py
"""
import json
import os

T = {}


def g(k, v): T["nusmp.nation.gui." + k] = v
def l(k, v): T["nusmp.nation.l." + k] = v
def m(k, v): T["nusmp.nation.m." + k] = v


T["key.nusmp.nation_screen"] = "Open Nations screen"
T["block.nusmp.nation_banner"] = "Nation Banner"
T["block.nusmp.contract_board"] = "Contract Board"
g("abandon", "Abandon"); g("accept", "Accept"); g("ally_accept", "Accept alliance"); g("ally_break", "Break alliance"); g("ally_decline", "Decline"); g("ally_propose", "Propose alliance")
g("as_nation_mob", "Nation: mob"); g("as_nation_player", "Nation: player"); g("board", "Contract board"); g("bounties", "Bounties"); g("capital", "Capital: %s, %s, %s")
g("ceasefire_offered", "(ceasefire offered)"); g("decline", "Decline"); g("delete_role", "Delete role"); g("deliver", "Hand in items"); g("deposit", "Deposit")
g("dir_stats", "Lv %s  %s members"); g("disabled", "The Nations system is switched off on this server."); g("disband", "Disband nation"); g("disband_confirm", "Click again to disband")
g("found", "Found nation"); g("found_hint", "Type a name. Founding costs what the server sets (coins or items)."); g("get_banner", "Get banner item"); g("history", "War history")
g("invitations", "Invitations"); g("invite", "Invite"); g("kick", "Kick"); g("leader", "Leader: %s"); g("leave", "Leave nation"); g("ledger", "Treasury ledger"); g("level", "Level %s / %s")
g("max_level", "Maximum level"); g("members", "Members"); g("members_count", "Members: %s / %s"); g("my_wars", "Wars"); g("nations", "Nations"); g("new_role", "New role")
g("next_level", "%s points to next level"); g("no_capital", "No capital yet: place the banner."); g("no_nation", "You are not in a nation."); g("on_mob", "Bounty: mob"); g("on_player", "Bounty: player")
g("perks", "Perks"); g("post_bounty", "Post a bounty: target / coins"); g("rel_ally", "Ally"); g("rel_prop_in", "Asks alliance"); g("rel_prop_out", "Proposed"); g("reputation", "Reputation")
g("roles", "Roles"); g("select_member", "Select a member"); g("select_nation", "Select a nation"); g("source_ubs", "UBS cash"); g("source_wallet", "Wallet")
g("stats", "Contracts %s, wars won %s, lost %s, area %s"); g("tab_overview", "Overview"); g("tab_members", "Members"); g("tab_treasury", "Treasury"); g("tab_contracts", "Contracts"); g("tab_diplomacy", "Diplomacy")
g("take", "Take"); g("tax_line", "Tax %s%% (max %s%%)"); g("title", "Nations"); g("transfer", "Make leader"); g("treasury_line", "Treasury: %s / %s"); g("wallet", "Wallet: %s")
g("war_cancel", "Withdraw war"); g("war_ceasefire", "Ceasefire"); g("war_declare", "Declare war"); g("war_surrender", "Surrender"); g("withdraw", "Withdraw")

l("ally_in", "%s proposed an alliance (accept with /nation ally accept %s)"); l("ally_out", "Proposal sent to %s"); l("ally_row", "Ally: %s"); l("bounty_row", "#%s %s (%s): %s coins")
l("confirm_disband", "Type /nation disband confirm to disband your nation. The treasury is paid out to you."); l("contract_row", "#%s %s  %s/%s  tier %s  %s coins  %s  %s")
l("history_row", "%s vs %s: %s  %s : %s  spoils %s"); l("info_capital", "Capital in %s at %s, %s, %s"); l("info_head", "%s: level %s, %s members, leader %s")
l("info_nocapital", "No capital yet (place the nation banner)."); l("info_treasury", "Treasury %s / %s coins, tax %s%%"); l("info_xp", "XP %s, score %s, contracts %s, wars won %s / lost %s")
l("ledger_row", "%s  %s  %s  %s  (balance %s)  %s"); l("level", "Level %s of %s, score %s, %s points to the next level"); l("list_row", "%s [%s]  level %s, %s members")
l("member_row", "%s: %s"); l("no_wars", "No wars."); l("none", "Nothing here."); l("perk", "%s %s +%s"); l("perk_next", "Level %s: %s %s +%s"); l("rep_row", "%s: %s (%s)")
l("role_row", "%s (%s) rank %s, %s members: %s"); l("war_row", "%s: %s, %s s left, score %s : %s")

m("admin_money", "Moved %s coins for %s."); m("ally_break_ok", "Alliance with %s broken."); m("ally_broken", "%s broke the alliance with your nation."); m("ally_declined", "Alliance proposal declined.")
m("ally_formed", "%s and %s are now allies."); m("ally_ok", "Alliance with %s agreed."); m("ally_proposed", "%s proposes an alliance. Accept with /nation ally accept %s."); m("ally_sent", "Alliance proposed to %s.")
m("ally_refused.self", "A nation cannot ally with itself."); m("ally_refused.already_allied", "You are already allied with %s."); m("ally_refused.at_war", "You are at war with %s.")
m("ally_refused.ally_at_war", "%s is at war with one of your allies."); m("ally_refused.limit_self", "Your nation has reached its alliance limit."); m("ally_refused.limit_other", "%s has reached its alliance limit.")
m("ally_refused.cooldown", "You broke an alliance with %s recently; wait before allying again."); m("ally_refused.duplicate", "You already proposed to %s."); m("ally_refused.no_proposal", "There is no proposal from %s.")
m("ally_refused.expired", "The proposal from %s has expired."); m("ally_refused.not_allied", "You are not allied with %s.")
m("already_in_nation", "You are already in a nation."); m("bad_amount", "Enter an amount above zero."); m("bad_name", "Names are %s to %s letters, digits, spaces, dashes or apostrophes.")
m("bad_permission", "Unknown permission '%s'."); m("bad_role", "Role ids are lower case letters, digits and underscores; names up to 24 characters."); m("bad_target", "That is not a valid target.")
m("bad_tax", "The tax must be between 0 and %s percent."); m("banner_given", "You received a nation banner."); m("banner_protected", "Only the leader can break the capital banner.")
m("bounty_cancelled", "Bounty on %s cancelled and refunded."); m("bounty_claimed", "You collected %s coins in bounties."); m("bounty_lapsed", "Your bounty on %s lapsed; %s coins returned.")
m("bounty_min", "The smallest bounty is %s coins."); m("bounty_ok", "Bounty on %s: %s coins posted."); m("bounty_posted", "A bounty was posted on %s: %s coins.")
m("cannot_cancel", "You can only withdraw a war that has not started fighting."); m("cannot_change_leader", "The leader's role cannot be changed this way.")
m("cannot_grant", "You cannot grant a permission you do not hold (%s)."); m("cannot_target_self", "You cannot do that to yourself."); m("capital_captured", "%s captured the capital of %s!")
m("capital_lost", "Your nation's capital banner is gone."); m("capital_ok", "The capital can be placed here."); m("capital_protected", "This is the capital area of another nation.")
m("capital_set", "The capital of your nation is now at %s, %s, %s."); m("ceasefire_agreed", "Ceasefire agreed. The war is over."); m("ceasefire_declined", "Ceasefire declined.")
m("ceasefire_pending", "A ceasefire offer is already pending."); m("ceasefire_proposed", "%s proposes a ceasefire."); m("ceasefire_sent", "Ceasefire proposed to %s."); m("color_set", "Banner colour set to #%s.")
m("contract_abandoned", "Contract abandoned: %s"); m("contract_done", "Contract done: %s. +%s coins, +%s nation XP, +%s reputation."); m("contract_expired", "Your contract ran out of time: %s")
m("contract_gone", "That contract is not available."); m("contract_progress", "%s: %s / %s"); m("contract_taken", "Contract taken: %s"); m("delivered", "Handed in %s items for: %s")
m("deposited", "Deposited %s coins into %s."); m("disabled", "The Nations system is switched off."); m("disbanded", "Nation %s disbanded. %s coins were paid out.")
m("found_need_coins", "Founding a nation costs %s coins."); m("found_need_item", "Founding a nation needs %s x %s."); m("founded", "Nation %s founded! Place the banner to set its capital.")
m("founded_broadcast", "The nation %s was founded by %s."); m("has_capital", "Your nation already has a capital."); m("invite_declined", "Invitation declined."); m("invite_sent", "Invitation sent to %s.")
m("invited", "You were invited to %s by %s. Accept with /nation accept %s."); m("joined", "%s joined the nation."); m("kick_ok", "%s was removed from the nation."); m("kicked", "%s was kicked from the nation.")
m("leader_cannot_leave", "The leader cannot leave: hand over leadership or disband."); m("leader_only", "Only the leader can do that."); m("left", "%s left the nation."); m("level_up", "%s reached level %s!")
m("name_taken", "A nation called %s already exists."); m("nation_full", "The nation is full (%s members)."); m("new_leader", "%s is the new leader."); m("no_active_war", "There is no active war with that nation.")
m("no_ceasefire", "There is no ceasefire offer to decline."); m("no_coins", "You need %s coins."); m("no_invite", "You have no invitation."); m("no_permission", "Your role lacks the '%s' permission.")
m("no_such_bounty", "There is no such bounty."); m("no_such_nation", "No nation '%s'."); m("no_such_role", "No role '%s'."); m("no_ubs_cash", "You do not have %s UBS cash.")
m("not_a_member", "That player is not in your nation."); m("not_deliver", "That is not a delivery contract."); m("not_in_dungeon", "A capital cannot stand in the dungeon."); m("not_in_nation", "You are not in a nation.")
m("not_your_bounty", "That is not your bounty."); m("nothing_to_deliver", "You carry nothing the contract needs."); m("orphan_banner", "This banner belongs to no nation."); m("paid", "Paid.")
m("player_offline", "%s is not online."); m("player_unknown", "The server does not know a player called %s."); m("pvp_deny_allied", "You cannot hurt a member of an allied nation.")
m("pvp_deny_not_at_war", "Your nations are not at war."); m("pvp_deny_same_nation", "You cannot hurt a member of your own nation."); m("rank_too_low", "Your rank is too low for that.")
m("renamed", "Nation renamed to %s."); m("rep_tier", "Your standing with %s is now: %s"); m("rep_too_low", "%s does not trust you with its board (%s)."); m("role_created", "Role %s created.")
m("role_deleted", "Role %s deleted."); m("role_exists", "The role '%s' already exists."); m("role_perm", "Role %s: permission %s %s."); m("role_protected", "The default roles cannot be deleted.")
m("role_set", "%s is now %s."); m("surrendered", "You surrendered."); m("target_in_nation", "%s is already in a nation."); m("tax_set", "Tax set to %s%%.")
m("tier_locked", "That contract is tier %s; your standing allows tier %s."); m("too_close", "Too close to the capital of %s."); m("too_fast", "Slow down.")
m("too_many_bounties", "You may have %s bounties at a time."); m("too_many_contracts", "You may hold %s contracts at a time."); m("too_many_nations", "The server has reached its nation limit.")
m("too_many_roles", "A nation may have %s custom roles."); m("treasury_full", "The treasury is full."); m("treasury_short", "The treasury only holds %s coins.")
m("ubs_unavailable", "The UBS banking bridge is not available."); m("unknown_action", "Unknown action."); m("use_transfer", "Use the transfer command to change the leader.")
m("war_active", "The war between %s and %s is now ACTIVE: fighting is allowed."); m("war_bounty", "%s has a bounty of %s coins on their head!"); m("war_cancelled", "War withdrawn.")
m("war_declared", "%s has declared war on %s!"); m("war_disabled", "Wars are switched off."); m("war_ended_draw", "The war between %s and %s ended without a winner."); m("war_force_ended", "The war was ended.")
m("war_ok", "War declared on %s. Fighting starts after the notice and preparation."); m("war_point", "+%s war points (total %s)"); m("war_prep", "War: %s vs %s enters the PREPARATION phase.")
m("war_refused.self", "A nation cannot declare war on itself."); m("war_refused.unknown_nation", "No such nation."); m("war_refused.allied", "%s is your ally: break the alliance first.")
m("war_refused.already_at_war", "You are already at war with %s."); m("war_refused.truce", "A truce with %s is still in force."); m("war_refused.too_many_wars", "Your nation has too many open wars.")
m("war_refused.level_too_low", "Your nation needs level %2$s to declare war."); m("war_refused.declarer_cooldown", "Your nation declared a war recently; wait a while.")
m("war_won", "%s won the war against %s and took %s coins in spoils."); m("withdrawn", "Withdrew %s coins from %s."); m("xp_added", "Added %s XP to %s."); m("you_joined", "You joined %s.")
m("you_left", "You left %s."); m("you_were_kicked", "You were kicked from %s."); m("your_nation_disbanded", "Your nation %s was disbanded.")

for k, v in {"attribute": "Attribute", "shop_discount": "Shop discount %%", "treasury_cap": "Treasury cap", "member_cap": "Members", "guard_count": "Guards",
             "contract_bonus": "Contract bonus %%", "board_slots": "Board slots"}.items():
    T["nusmp.nation.perk." + k] = v
for k, v in {"invite": "Invite", "kick": "Kick", "treasury": "Treasury", "war": "War", "contracts": "Contracts", "build": "Build", "diplomacy": "Diplomacy", "roles": "Roles"}.items():
    T["nusmp.nation.perm." + k] = v
for k, v in {"hated": "Hated", "hostile": "Hostile", "unfriendly": "Unfriendly", "neutral": "Neutral", "friendly": "Friendly", "honored": "Honored", "revered": "Revered"}.items():
    T["nusmp.nation.tier." + k] = v
for k, v in {"declared": "Declared", "preparation": "Preparation", "active": "Active", "ceasefire": "Ceasefire", "ended": "Ended"}.items():
    T["nusmp.nation.war.phase." + k] = v


def jstr(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def main():
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
    with open(os.path.join(root, "tools", "elite", "nations_lang.json"), "w", encoding="utf-8") as f:
        json.dump(dict(sorted(T.items())), f, indent=2, ensure_ascii=False)
        f.write("\n")
    java = ["package com.newuniverse.nusmp.elite.nations.core;", "",
            "import java.util.Collections;", "import java.util.Map;", "import java.util.TreeMap;", "",
            "/**", " * Why: the English text of every message of the Nations & War system, as the FALLBACK of {@code Component.translatableWithFallback}: the mod reads correctly before the lang fragment",
            " * {@code tools/elite/nations_lang.json} is merged into en_us.json, and a resource pack can still translate every key. GENERATED by {@code tools/elite/gen_nations_text.py} (edit that file, not this one).",
            " * Pure: no Minecraft types, so a test can check that the fragment and this map agree.", " */",
            "public final class NationText {", "    private NationText() {}", "",
            "    private static final Map<String, String> EN = new TreeMap<>();", "", "    static {"]
    for k, v in sorted(T.items()):
        java.append("        EN.put(%s, %s);" % (jstr(k), jstr(v)))
    java += ["    }", "", "    /** The English text of a key, or the key itself when unknown. */",
             "    public static String en(String key) { return EN.getOrDefault(key, key); }", "",
             "    public static boolean has(String key) { return EN.containsKey(key); }", "",
             "    public static Map<String, String> all() { return Collections.unmodifiableMap(EN); }", "}", ""]
    path = os.path.join(root, "src", "main", "java", "com", "newuniverse", "nusmp", "elite", "nations", "core", "NationText.java")
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(java))
    print("wrote", len(T), "texts")


if __name__ == "__main__":
    main()
