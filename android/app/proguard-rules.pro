# The app parses JSON with kotlinx.serialization's JsonElement API (no reflection, no
# @Serializable classes), so no keep rules are needed for it.

# Keep line numbers so crash reports stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
