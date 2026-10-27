package com.oneorigin.studio

import android.app.Application

class OriginStudioApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // The emulator reads its colour table from a static, so it has to be in place before the
        // first session is created or the terminal renders in the library's own palette.
        com.oneorigin.studio.shell.ShellTheme.init(this)

        // Notifications for work that finishes while the user is elsewhere.
        com.oneorigin.studio.notify.AppPresence.register(this)
        com.oneorigin.studio.notify.SessionWatcher.start(this)
        com.oneorigin.studio.shell.TerminalHost.onBellRung = { key, title ->
            com.oneorigin.studio.notify.SessionWatcher.onBell(this, key, title)
        }
    }
}
