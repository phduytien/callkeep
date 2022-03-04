package io.wazo.callkeep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

const val CALL_CHANNEL_ID = "calls_channel_id"
const val CALL_CHANNEL_NAME = "Calls"

const val EXTRA_CALL_ID = "extra_call_id"
const val EXTRA_CALL_TYPE = "extra_call_type"
const val EXTRA_CALL_INITIATOR_ID = "extra_call_initiator_id"
const val EXTRA_CALL_INITIATOR_NAME = "extra_call_initiator_name"
const val EXTRA_CALL_OPPONENTS = "extra_call_opponents"
const val EXTRA_CALL_USER_INFO = "extra_call_user_info"

const val ACTION_CALL_ACCEPT = "action_call_accept"
const val ACTION_CALL_REJECT = "action_call_reject"
const val ACTION_CALL_NOTIFICATION_CANCELED = "action_call_notification_canceled"
const val ACTION_CALL_ENDED = "action_call_ended"

const val CALL_TYPE_PLACEHOLDER = "Incoming %s call"

const val CALL_STATE_PENDING: String = "pending"
const val CALL_STATE_ACCEPTED: String = "accepted"
const val CALL_STATE_REJECTED: String = "rejected"
const val CALL_STATE_UNKNOWN: String = "unknown"

fun cancelCallNotification(context: Context, callId: String) {
    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.cancel(callId.hashCode())
}

fun showCallNotification(
    context: Context, callId: String, callType: Int, callInitiatorId: Int,
    callInitiatorName: String, callOpponents: ArrayList<Int>, userInfo: String
) {
    val notificationManager = NotificationManagerCompat.from(context)

    val intent = getLaunchIntent(context)

    val pendingIntent = PendingIntent.getActivity(
        context,
        callId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )

//    val ringtone: Uri = RingtoneManager.getActualDefaultRingtoneUri(
//        context.applicationContext,
//        RingtoneManager.TYPE_RINGTONE
//    )

    val callTypeTitle =
        String.format(CALL_TYPE_PLACEHOLDER, if (callType == 1) "Video" else "Audio")

    val builder: NotificationCompat.Builder =
        createCallNotification(context, callInitiatorName, callTypeTitle, pendingIntent)

    // Add actions
    addCallRejectAction(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        userInfo
    )
    addCallAcceptAction(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        userInfo
    )

    // Add full screen intent (to show on lock screen)
    addCallFullScreenIntent(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        userInfo
    )

    // Add action when delete call notification
    addCancelCallNotificationIntent(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        userInfo
    )

    // Set small icon for notification
    setNotificationSmallIcon(context, builder)

    // Set notification color accent
    setNotificationColor(context, builder)

    createCallNotificationChannel(notificationManager)

    notificationManager.notify(callId.hashCode(), builder.build())
}

fun getLaunchIntent(context: Context): Intent? {
    val packageName = context.packageName
    val packageManager: PackageManager = context.packageManager
    return packageManager.getLaunchIntentForPackage(packageName)
}

fun createCallNotification(
    context: Context,
    title: String,
    text: String?,
    pendingIntent: PendingIntent,
//    ringtone: Uri
): NotificationCompat.Builder {
    val notificationBuilder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
    notificationBuilder
        .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        .setContentTitle(title)
        .setContentText(text)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(true)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setContentIntent(pendingIntent)
//        .setSound(ringtone)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setTimeoutAfter(60000)
    return notificationBuilder
}

fun addCallRejectAction(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    opponents: ArrayList<Int>,
    userInfo: String
) {
    val bundle = Bundle()
    bundle.putString(EXTRA_CALL_ID, callId)
    bundle.putInt(EXTRA_CALL_TYPE, callType)
    bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, opponents)
    bundle.putString(EXTRA_CALL_USER_INFO, userInfo)

    val declinePendingIntent: PendingIntent = PendingIntent.getBroadcast(
        context,
        callId.hashCode(),
        Intent(context, EventReceiver::class.java)
            .setAction(ACTION_CALL_REJECT)
            .putExtras(bundle),
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    val declineAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
        context.resources.getIdentifier(
            "ic_menu_close_clear_cancel",
            "drawable",
            context.packageName
        ),
        getColorizedText("Reject", "#E02B00"),
        declinePendingIntent
    )
        .build()

    notificationBuilder.addAction(declineAction)
}

fun addCallAcceptAction(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    opponents: ArrayList<Int>,
    userInfo: String
) {
    val bundle = Bundle()
    bundle.putString("callUUID", callId)
    bundle.putString(EXTRA_CALL_ID, callId)
    bundle.putInt(EXTRA_CALL_TYPE, callType)
    bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, opponents)
    bundle.putString(EXTRA_CALL_USER_INFO, userInfo)

    val acceptPendingIntent: PendingIntent = PendingIntent.getBroadcast(
        context,
        callId.hashCode(),
        Intent(context, EventReceiver::class.java)
            .setAction(ACTION_CALL_ACCEPT)
            .putExtras(bundle),
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    val acceptAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
        context.resources.getIdentifier("ic_menu_call", "drawable", context.packageName),
        getColorizedText("Accept", "#4CB050"),
        acceptPendingIntent
    )
        .build()
    notificationBuilder.addAction(acceptAction)
}

fun createStartIncomingScreenIntent(
    context: Context, callId: String, callType: Int, callInitiatorId: Int,
    callInitiatorName: String, opponents: ArrayList<Int>, userInfo: String
): Intent {
    val activityClass = Class.forName("net.vinbrain.smartcare.MainActivity")
    val intent = Intent(context, activityClass)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.putExtra(EXTRA_CALL_ID, callId)
    intent.putExtra(EXTRA_CALL_TYPE, callType)
    intent.putExtra(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    intent.putExtra(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    intent.putIntegerArrayListExtra(EXTRA_CALL_OPPONENTS, opponents)
    intent.putExtra(EXTRA_CALL_USER_INFO, userInfo)
    return intent
}

fun addCallFullScreenIntent(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    callOpponents: ArrayList<Int>,
    userInfo: String
) {
    val callFullScreenIntent: Intent = createStartIncomingScreenIntent(
        context,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        userInfo
    )
    val fullScreenPendingIntent = PendingIntent.getActivity(
        context,
        callId.hashCode(),
        callFullScreenIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
}

fun addCancelCallNotificationIntent(
    appContext: Context?,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    userInfo: String
) {
    val bundle = Bundle()
    bundle.putString(EXTRA_CALL_ID, callId)
    bundle.putInt(EXTRA_CALL_TYPE, callType)
    bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    bundle.putString(EXTRA_CALL_USER_INFO, userInfo)

    val deleteCallNotificationPendingIntent = PendingIntent.getBroadcast(
        appContext,
        callId.hashCode(),
        Intent(appContext, EventReceiver::class.java)
            .setAction(ACTION_CALL_NOTIFICATION_CANCELED)
            .putExtras(bundle),
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    notificationBuilder.setDeleteIntent(deleteCallNotificationPendingIntent)
}

fun createCallNotificationChannel(notificationManager: NotificationManagerCompat) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CALL_CHANNEL_ID,
            CALL_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
//        channel.setSound(
//            sound, AudioAttributes.Builder()
//                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
//                .build()
//        )
        notificationManager.createNotificationChannel(channel)
    }
}

fun setNotificationSmallIcon(context: Context, notificationBuilder: NotificationCompat.Builder) {
    val resID =
        context.resources.getIdentifier("ic_launcher_foreground", "drawable", context.packageName)
    if (resID != 0) {
        notificationBuilder.setSmallIcon(resID)
    } else {
        notificationBuilder.setSmallIcon(context.applicationInfo.icon)
    }
}

fun setNotificationColor(context: Context, notificationBuilder: NotificationCompat.Builder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val accentID = context.resources.getIdentifier(
            "call_notification_color_accent",
            "color",
            context.packageName
        )
        if (accentID != 0) {
            notificationBuilder.color = context.resources.getColor(accentID, null)
        } else {
            notificationBuilder.color = Color.parseColor("#4CAF50")
        }
    }
}

fun getColorizedText(string: String, colorHex: String): Spannable {
    val spannable: Spannable = SpannableString(string)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor(colorHex)),
            0,
            spannable.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
    }
    return spannable
}
