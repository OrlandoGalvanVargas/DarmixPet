package com.darmix.darmixpet.notifications

import android.app.Notification
import android.service.notification.StatusBarNotification

object NotificationFilter {

    private const val PERSISTENT_FLAGS =
        Notification.FLAG_ONGOING_EVENT or
                Notification.FLAG_NO_CLEAR or
                Notification.FLAG_FOREGROUND_SERVICE

    fun isPersistent(sbn: StatusBarNotification): Boolean =
        !sbn.isClearable || (sbn.notification.flags and PERSISTENT_FLAGS) != 0

    fun isGroupSummary(sbn: StatusBarNotification): Boolean =
        (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

    fun filter(all: Array<StatusBarNotification>, ownPackage: String): List<StatusBarNotification> {
        val candidates = all.filter { it.packageName != ownPackage && !isPersistent(it) }

        val groupsWithChildren = candidates
            .filter { it.isGroup && !isGroupSummary(it) }
            .mapTo(HashSet()) { it.groupKey }

        return candidates.filterNot { isGroupSummary(it) && it.groupKey in groupsWithChildren }
    }
}