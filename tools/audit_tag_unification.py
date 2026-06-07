"""Repeatable audit for tag-membership unification ("potato is a potato").

For every unification group in data/delightful_compat/unification/, this reads the addon
jars in libs-dev/ and reports, per group, which c:/minecraft ITEM tags each equivalent
belongs to. Where one equivalent is tagged into a category tag but another isn't, the group
is NOT fully interchangeable in recipes keyed on that tag (the camote-vs-veggies-sweet_potato
stuffed-pumpkin bug). The fix is to merge `#<group tag>` into that category tag.

CRUCIAL: a category tag must only be unified if it is NOT used as the input of a single-input
cooking recipe (minecraft:smelting/smoking/campfire_cooking/blasting or farmersdelight:cutting).
Expanding such a tag makes one input match several recipes -> ambiguous output (the same class
as the 1.3.0 cooking conflicts). This script flags those tags as [COOKING-INPUT: SKIP].

Usage:  put the addon jars in libs-dev/ (they are git-ignored), then:
    python tools/audit_tag_unification.py
It only reads jars; it writes nothing. Use its output to hand-write the tag-merge files under
src/main/resources/data/**/tags/item/ (see CLAUDE.md "Tag-membership unification").
"""
import json, os, glob, zipfile, io

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GROUPS_DIR = os.path.join(REPO, "src", "main", "resources", "data", "delightful_compat", "unification")
LIBS = os.path.join(REPO, "libs-dev")

COOKING_TYPES = {
    "minecraft:smelting",
    "minecraft:smoking",
    "minecraft:campfire_cooking",
    "minecraft:blasting",
    "farmersdelight:cutting",
}


def jar_entries():
    """Yield (entry_path, parsed_json) for every data/**/*.json in every libs-dev jar."""
    for jar in glob.glob(os.path.join(LIBS, "*.jar")):
        with zipfile.ZipFile(jar) as z:
            for name in z.namelist():
                if name.startswith("data/") and name.endswith(".json"):
                    try:
                        yield name, json.load(io.TextIOWrapper(z.open(name), "utf-8"))
                    except Exception:
                        continue


def tag_id(entry):  # data/<ns>/tags/item/<sub>.json -> ns:sub
    p = entry.split("/")
    return f"{p[1]}:{'/'.join(p[4:])[:-5]}"


def main():
    if not glob.glob(os.path.join(LIBS, "*.jar")):
        print("No jars in libs-dev/ — drop the addon jars there first.")
        return

    item_in_tag = {}          # item id -> set(tag id)   (c:/minecraft item tags only)
    cooking_input_tags = set()  # tags used as a single-input cooking/cutting input
    for entry, data in jar_entries():
        parts = entry.split("/")
        if len(parts) >= 5 and parts[2] == "tags" and parts[3] == "item" and parts[1] in ("c", "minecraft"):
            tid = tag_id(entry)
            for v in data.get("values", []):
                vid = v.get("id") if isinstance(v, dict) else v
                if isinstance(vid, str) and not vid.startswith("#"):
                    item_in_tag.setdefault(vid, set()).add(tid)
        elif "/recipe/" in entry or parts[2:3] == ["recipe"]:
            if data.get("type") in COOKING_TYPES:
                ing = data.get("ingredient") or data.get("ingredients")
                for one in (ing if isinstance(ing, list) else [ing]):
                    if isinstance(one, dict) and isinstance(one.get("tag"), str):
                        cooking_input_tags.add(one["tag"])

    groups = [json.load(open(p, encoding="utf-8")) for p in glob.glob(os.path.join(GROUPS_DIR, "*.json"))]
    for g in sorted(groups, key=lambda x: x["group"]):
        gtag = g["tag"]
        print(f"\n### group '{g['group']}'  (unifying tag {gtag})")
        union = {}
        for e in g["equivalents"]:
            tags = item_in_tag.get(e, set())
            print(f"   {e:42s} -> {sorted(tags) if tags else '(untagged / item absent from libs-dev)'}")
            for t in tags:
                union.setdefault(t, set()).add(e)
        targets = []
        for t in sorted(union):
            if t == gtag or t.startswith(gtag + "/") or gtag.startswith(t + "/"):
                continue  # the group's own tag or a parent/child of it
            flag = "  [COOKING-INPUT: SKIP]" if t in cooking_input_tags else ""
            targets.append(f"        {t}: members={sorted(union[t])}{flag}")
        if targets:
            print("   --> merge #%s into these category tags (skip flagged):" % gtag)
            print("\n".join(targets))
        else:
            print("   --> nothing to unify")


if __name__ == "__main__":
    main()
