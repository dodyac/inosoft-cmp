# Room 3 uses generated implementations discovered from the database definition.
-keep class * extends androidx.room3.RoomDatabase { <init>(); }

# Keep serialized NewsAPI DTO members. Kotlin serialization also contributes rules.
-keepattributes *Annotation*

