package com.eyecare.lookaway.media

import android.service.notification.NotificationListenerService

/**
 * An (otherwise empty) notification listener. Its only purpose is to be an
 * *enabled* listener component, which is the gate Android requires before
 * [android.media.session.MediaSessionManager.getActiveSessions] will hand us
 * the active media sessions to pause/resume. We never read notifications here.
 */
class MediaNotificationListener : NotificationListenerService()
