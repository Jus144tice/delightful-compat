DEV-ONLY mod folder (not part of the published jar; do not commit mod jars here).

Drop Farmer's Delight + any Delight addon .jar files into this folder, then run:
    ./gradlew runClient   (or runServer)
and build.gradle will load them as mods so the compatibility patches actually apply
and can be tested in-game. An empty folder is a harmless no-op.

Download the NeoForge 1.21.1 jars from Modrinth/CurseForge, e.g.:
  farmersdelight, oaksdelight (Oaks Delight), ramadandelight (Ramadan Delight),
  peruviansdelight (Peruvian's Delight), slavic_delight (Slavic Delight),
  moredelight (More Delight — also needs delightlib).
