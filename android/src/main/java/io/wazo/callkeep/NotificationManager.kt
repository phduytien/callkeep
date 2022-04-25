package io.wazo.callkeep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

const val CALL_CHANNEL_ID = "incoming_call"
const val CALL_CHANNEL_NAME = "Incoming Call"
const val incomingCallNotificationId = 766261102

fun cancelCallNotification(context: Context) {
    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.cancel(766261102)
}

fun showCallNotification(
    context: Context,
    uuid: String,
    name: String,
    phoneNumber: String
) {
    cancelCallNotification(context)
    try {
        context.startActivity(createStartIncomingScreenIntent(context, uuid, name, phoneNumber))
    } catch (ignore: Exception) {
    }
    val notificationManager = NotificationManagerCompat.from(context)

    val intent = createStartIncomingScreenIntent(context, uuid, name, phoneNumber)

    val pendingIntent = PendingIntent.getActivity(
        context,
        uuid.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    val callTypeTitle = context.resources.getString(R.string.incoming_call)

    val builder: NotificationCompat.Builder =
        createCallNotification(context, name, callTypeTitle, pendingIntent)
    val sound = Uri.parse(
        ("android.resource://"
                + context.packageName) + "/" + R.raw.ringtone_unique
    )
    builder.setSound(sound)
    val notificationLayout = buildRemoteView(context, uuid, name, phoneNumber)
    builder.setCustomBigContentView(notificationLayout)
    builder.setCustomHeadsUpContentView(notificationLayout)

    // Add full screen intent (to show on lock screen)
    addCallFullScreenIntent(
        context,
        builder,
        uuid,
        name,
        phoneNumber
    )

    // Set small icon for notification
    setNotificationSmallIcon(context, builder)

    // Set notification color accent
    setNotificationColor(context, builder)

    createCallNotificationChannel(notificationManager, sound)

    notificationManager.notify(incomingCallNotificationId, builder.build())
}

fun buildRemoteView(
    context: Context,
    uuid: String,
    phoneNumber: String,
    doctorName: String
): RemoteViews {
    val result = RemoteViews(context.packageName, R.layout.notification_incoming_call)


    val bundle = Bundle()
    bundle.putString(Constants.EXTRA_CALL_UUID, uuid)

    val rejectPendingIntent: PendingIntent = PendingIntent.getBroadcast(
        context,
        uuid.hashCode(),
        Intent(context, EventReceiver::class.java)
            .setAction(Constants.ACTION_CALL_REJECT)
            .putExtras(bundle),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    val acceptPendingIntent: PendingIntent = PendingIntent.getBroadcast(
        context,
        uuid.hashCode(),
        Intent(context, EventReceiver::class.java)
            .setAction(Constants.ACTION_CALL_ACCEPT)
            .putExtras(bundle),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    result.setTextViewText(R.id.tv_doctor_name, doctorName)
    result.setTextViewText(R.id.tv_phone_number, phoneNumber)
    result.setOnClickPendingIntent(R.id.btn_answer, acceptPendingIntent)
    result.setOnClickPendingIntent(R.id.btn_decline, rejectPendingIntent)

    return result
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
        .setDefaults(NotificationCompat.DEFAULT_SOUND)
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

fun createStartIncomingScreenIntent(
    context: Context,
    uuid: String,
    name: String,
    phoneNumber: String
): Intent {
    val intent = Intent(context, IncomingCallActivity::class.java)
    intent.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    intent.putExtra(Constants.EXTRA_CALL_UUID, uuid)
    intent.putExtra(Constants.EXTRA_CALLER_NAME, name)
    intent.putExtra(Constants.EXTRA_CALL_NUMBER, phoneNumber)
    return intent
}

fun addCallFullScreenIntent(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    uuid: String,
    name: String,
    phoneNumber: String
) {
    val callFullScreenIntent: Intent = createStartIncomingScreenIntent(
        context,
        uuid,
        name,
        phoneNumber
    )
    val fullScreenPendingIntent = PendingIntent.getActivity(
        context,
        uuid.hashCode(),
        callFullScreenIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
}

fun createCallNotificationChannel(notificationManager: NotificationManagerCompat, sound: Uri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CALL_CHANNEL_ID,
            CALL_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setSound(
            sound, AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
        )
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
