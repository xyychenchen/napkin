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

# Coil 3 是 Kotlin Multiplatform 项目，部分 expect/actual 类 R8 找不到 actual 实现
-keep class coil3.** { *; }
-dontwarn coil3.**
-dontwarn coil3.network.**

# security-crypto 传递依赖 tink，缺 errorprone annotations
-dontwarn com.google.errorprone.annotations.**

# 其他常见 missing class
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

