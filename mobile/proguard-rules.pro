# R8 in full mode strips Kotlin internal interfaces Compose still uses at
# runtime (e.g. kotlin.sequences.d), causing IllegalAccessError at startup.
-keep class kotlin.** { *; }
