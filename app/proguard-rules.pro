# Keep Room generated impl
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coroutines
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Compose
-keep class androidx.compose.** { *; }

# Coil
-keep class coil3.** { *; }

# security-crypto 传递依赖 tink，缺 errorprone annotations
-dontwarn com.google.errorprone.annotations.**

