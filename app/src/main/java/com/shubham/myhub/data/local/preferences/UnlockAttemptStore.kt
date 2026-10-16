package com.shubham.myhub.data.local.preferences

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/**
 * Persists wrong-PIN attempts and the current lockout for locked notes, so leaving the app,
 * force-stopping it or rebooting the phone doesn't hand out fresh attempts.
 *
 * The lockout is timed with [SystemClock.elapsedRealtime] (immune to changing the phone's clock).
 * That timer restarts on reboot, so the wall-clock end time is also stored and used to carry the
 * remaining lockout over into the new boot.
 *
 * Writes use commit() rather than apply(): the attempt must be on disk before the app reacts,
 * otherwise killing the app right after a wrong PIN could discard it.
 */
class UnlockAttemptStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val contentResolver = context.contentResolver

    val failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    /** [SystemClock.elapsedRealtime] until which PIN entry is refused, or 0 if not locked out. */
    fun activeLockoutUntil(): Long {
        val untilElapsed = prefs.getLong(KEY_UNTIL_ELAPSED, 0L)
        if (untilElapsed == 0L) return 0L

        val nowElapsed = SystemClock.elapsedRealtime()
        val sameBoot = prefs.getInt(KEY_BOOT_COUNT, -1) == currentBootCount()
        val remaining = if (sameBoot) {
            untilElapsed - nowElapsed
        } else {
            prefs.getLong(KEY_UNTIL_WALL, 0L) - System.currentTimeMillis()
        }
        if (remaining <= 0L) return 0L

        // Never longer than the lockout that was originally imposed (guards against a clock set backwards).
        val capped = remaining.coerceAtMost(prefs.getLong(KEY_DURATION, remaining))
        val until = nowElapsed + capped
        if (!sameBoot) saveLockout(prefs.edit(), until, capped).commit() // re-anchor to this boot
        return until
    }

    /** Records one more wrong PIN, plus a lockout of [lockoutMillis] if it is > 0, in one write. */
    fun recordFailure(failedAttempts: Int, lockoutMillis: Long) {
        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failedAttempts)
        if (lockoutMillis > 0L) {
            saveLockout(editor, SystemClock.elapsedRealtime() + lockoutMillis, lockoutMillis)
        }
        editor.commit()
    }

    /** Called after a correct PIN. */
    fun reset() {
        prefs.edit().clear().commit()
    }

    private fun saveLockout(editor: android.content.SharedPreferences.Editor, untilElapsed: Long, duration: Long) =
        editor
            .putLong(KEY_UNTIL_ELAPSED, untilElapsed)
            .putLong(KEY_UNTIL_WALL, System.currentTimeMillis() + (untilElapsed - SystemClock.elapsedRealtime()))
            .putLong(KEY_DURATION, duration)
            .putInt(KEY_BOOT_COUNT, currentBootCount())

    private fun currentBootCount(): Int =
        Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)

    private companion object {
        const val PREFS_NAME = "note_unlock_attempts"
        const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        const val KEY_UNTIL_ELAPSED = "lockout_until_elapsed"
        const val KEY_UNTIL_WALL = "lockout_until_wall"
        const val KEY_DURATION = "lockout_duration"
        const val KEY_BOOT_COUNT = "lockout_boot_count"
    }
}
