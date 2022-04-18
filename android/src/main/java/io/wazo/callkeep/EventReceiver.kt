package io.wazo.callkeep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.*


class EventReceiver : BroadcastReceiver() {
    private val TAG = "EventReceiver"

    private fun sendCallRequestToActivity(
        action: String,
        attributeMap: HashMap<String, String>,
        context: Context
    ) {
        val intent = Intent(action)
        val extras = Bundle()
        extras.putSerializable("attributeMap", attributeMap)
        intent.putExtras(extras)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun bundleToMap(extras: Bundle): HashMap<String, String> {
        val extrasMap = HashMap<String, String>()
        val keySet = extras.keySet()
        val iterator: Iterator<String> = keySet.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (extras[key] != null) {
                extrasMap[key] = extras[key].toString()
            }
        }
        return extrasMap
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || TextUtils.isEmpty(intent.action)) return
        when (intent.action) {
            Constants.ACTION_CALL_REJECT -> {

                val extras = intent.extras
                val callId = extras?.getString(Constants.EXTRA_CALL_UUID)
                val bundle = Bundle()
                bundle.putString(Constants.EXTRA_CALL_UUID, callId)
                sendCallRequestToActivity(
                    Constants.ACTION_END_CALL,
                    context = context,
                    attributeMap = bundleToMap(bundle)
                )

                NotificationManagerCompat.from(context).cancel(callId.hashCode())
            }

            Constants.ACTION_CALL_ACCEPT -> {

                val extras = intent.extras
                val callId = extras?.getString(Constants.EXTRA_CALL_UUID)
                val number = extras?.getString(Constants.EXTRA_CALL_NUMBER)
                val callInitiatorName = extras?.getString(Constants.EXTRA_CALLER_NAME)
                Log.i(TAG, "NotificationReceiver onReceive Call ACCEPT, callId: $callId")

                val bundle = Bundle()

                bundle.putString(Constants.EXTRA_CALL_UUID, callId)
                bundle.putString(Constants.EXTRA_CALLER_NAME, callInitiatorName)
                bundle.putString(Constants.EXTRA_CALL_NUMBER, number)
                sendCallRequestToActivity(
                    Constants.ACTION_ANSWER_CALL,
                    context = context,
                    attributeMap = bundleToMap(bundle)
                )
                sendCallRequestToActivity(
                    Constants.ACTION_AUDIO_SESSION,
                    context = context,
                    attributeMap = bundleToMap(bundle)
                )
                NotificationManagerCompat.from(context).cancel(callId.hashCode())
                val packageName = context.applicationContext.packageName
                context.packageManager.getLaunchIntentForPackage(packageName)?.cloneFilter()?.run {
                    intent.putExtras(bundle)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(this)
                }
            }

            Constants.ACTION_CALL_NOTIFICATION_CANCELED -> {
                val extras = intent.extras
                val callId = extras?.getString(Constants.EXTRA_CALL_UUID)
                Log.i(
                    TAG,
                    "NotificationReceiver onReceive Delete Call Notification, callId: $callId"
                )
                LocalBroadcastManager.getInstance(context.applicationContext)
                    .sendBroadcast(
                        Intent(Constants.ACTION_CALL_NOTIFICATION_CANCELED).putExtra(
                            Constants.EXTRA_CALL_UUID,
                            callId
                        )
                    )
            }
        }
    }
}