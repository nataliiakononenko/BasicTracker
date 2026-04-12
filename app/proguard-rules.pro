# Keep data classes
-keep class com.basicorganizer.tracker.data.** { *; }

# Keep database classes
-keep class com.basicorganizer.tracker.data.TrackerDatabase { *; }
-keep class com.basicorganizer.tracker.data.TrackingItem { *; }
-keep class com.basicorganizer.tracker.data.TrackingEntry { *; }
-keep class com.basicorganizer.tracker.data.Note { *; }

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
