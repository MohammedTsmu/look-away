package com.eyecare.lookaway.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.eyecare.lookaway.R
import com.eyecare.lookaway.receiver.ActionReceiver
import com.eyecare.lookaway.service.Phase
import com.eyecare.lookaway.service.ReminderEngine
import com.eyecare.lookaway.service.ReminderService
import com.eyecare.lookaway.service.RunState

/**
 * Home-screen widget: one-tap Start / Pause / Resume / Stop plus a short status
 * line. Button taps are broadcast to [ActionReceiver]; the running service keeps
 * the widget fresh via [refresh].
 */
class LookAwayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { updateOne(context, appWidgetManager, it) }
    }

    companion object {

        /** Re-render every placed widget instance. Safe to call often. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LookAwayWidgetProvider::class.java),
            )
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val enabled = RunState.isEnabled(context)
            val state = ReminderEngine.state.value
            // Localized context for the user-visible strings; RemoteViews still
            // needs the real package name (unchanged by the locale wrapper).
            val l = com.eyecare.lookaway.util.LocaleManager.wrap(context)
            val rv = RemoteViews(context.packageName, R.layout.widget_lookaway)

            val status = when {
                !enabled -> l.getString(R.string.widget_status_off)
                state.paused -> l.getString(R.string.widget_status_paused)
                state.phase == Phase.BREAK -> l.getString(R.string.widget_status_break)
                state.isRunning -> l.getString(
                    R.string.widget_status_running,
                    ReminderService.formatClock(state.secondsRemaining),
                )
                else -> l.getString(R.string.tile_running)
            }
            rv.setTextViewText(R.id.tvStatus, status)

            if (!enabled) {
                rv.setViewVisibility(R.id.btnStop, View.GONE)
                rv.setTextViewText(R.id.btnPrimary, l.getString(R.string.widget_start))
                rv.setOnClickPendingIntent(R.id.btnPrimary, broadcast(context, ReminderService.ACTION_START, 1))
            } else {
                rv.setViewVisibility(R.id.btnStop, View.VISIBLE)
                rv.setTextViewText(
                    R.id.btnPrimary,
                    l.getString(if (state.paused) R.string.widget_resume else R.string.widget_pause),
                )
                rv.setOnClickPendingIntent(R.id.btnPrimary, broadcast(context, ReminderService.ACTION_TOGGLE, 2))
                rv.setOnClickPendingIntent(R.id.btnStop, broadcast(context, ReminderService.ACTION_STOP, 3))
            }

            rv.setOnClickPendingIntent(R.id.widgetRoot, openApp(context))
            manager.updateAppWidget(id, rv)
        }

        private fun broadcast(context: Context, action: String, code: Int): PendingIntent {
            val intent = Intent(context, ActionReceiver::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context, 200 + code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun openApp(context: Context): PendingIntent {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent()
            return PendingIntent.getActivity(
                context, 210, launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
