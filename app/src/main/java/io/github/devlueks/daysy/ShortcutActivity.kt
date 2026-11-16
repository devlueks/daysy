package io.github.devlueks.daysy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.github.devlueks.daysy.data.Kind

/**
 * Invisible trampoline for the app shortcuts ("New countdown" / "New time since").
 *
 * The launcher starts shortcuts as the base intent of a fresh task. If MainActivity were that
 * target, Android would re-deliver the shortcut every time the task is rebuilt (e.g. after an app
 * update) and the editor would pop up again on a normal app start. So the request is handed over
 * once, in memory, and MainActivity is opened with a plain launcher intent.
 */
class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PendingShortcut.kind = when (intent?.action) {
            ACTION_NEW_COUNTDOWN -> Kind.COUNTDOWN
            ACTION_NEW_COUNTUP -> Kind.COUNTUP
            else -> null
        }
        startActivity(
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        )
        finish()
    }

    companion object {
        const val ACTION_NEW_COUNTDOWN = "io.github.devlueks.daysy.NEW_COUNTDOWN"
        const val ACTION_NEW_COUNTUP = "io.github.devlueks.daysy.NEW_COUNTUP"
    }
}

/** A shortcut request waiting for MainActivity; read (and cleared) exactly once. */
object PendingShortcut {
    @Volatile
    var kind: Kind? = null

    fun take(): Kind? = kind.also { kind = null }
}
