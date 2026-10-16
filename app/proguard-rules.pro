# Project-specific R8 rules.

# Room creates generated *_Impl databases via reflection on their no-arg constructor.
# R8 full mode (AGP 9 default) strips it otherwise, crashing on launch.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# WorkManager (used by Glance to render the widget) creates input mergers and workers by reflection.
-keep class * extends androidx.work.InputMerger { <init>(); }
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Glance instantiates widget tap callbacks by class name.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
