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
            ACTION_CALL_REJECT -> {

                val extras = intent.extras
                val callId = extras?.getString(EXTRA_CALL_ID)
                val callType = extras?.getInt(EXTRA_CALL_TYPE)
                val callInitiatorId = extras?.getInt(EXTRA_CALL_INITIATOR_ID)
                val callInitiatorName = extras?.getString(EXTRA_CALL_INITIATOR_NAME)
                val callOpponents = extras?.getIntegerArrayList(EXTRA_CALL_OPPONENTS)
                val userInfo = extras?.getString(EXTRA_CALL_USER_INFO)
                Log.i(TAG, "NotificationReceiver onReceive Call REJECT, callId: $callId")

                val broadcastIntent = Intent(ACTION_CALL_REJECT)
                val bundle = Bundle()
                bundle.putString(EXTRA_CALL_ID, callId)
                bundle.putString(Constants.EXTRA_CALL_UUID, callId)
                bundle.putInt(EXTRA_CALL_TYPE, callType!!)
                bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId!!)
                bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
                bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
                bundle.putString(EXTRA_CALL_USER_INFO, userInfo)
//                broadcastIntent.putExtras(bundle)
//
//                LocalBroadcastManager.getInstance(context.applicationContext)
//                    .sendBroadcast(broadcastIntent)


                bundle.putString(Constants.EXTRA_CALL_UUID, callId)
                bundle.putString(Constants.EXTRA_CALLER_NAME, callInitiatorName)
                bundle.putString(Constants.EXTRA_CALL_NUMBER, userInfo)
                sendCallRequestToActivity(
                    Constants.ACTION_END_CALL,
                    context = context,
                    attributeMap = bundleToMap(bundle)
                )

                NotificationManagerCompat.from(context).cancel(callId.hashCode())
            }

            ACTION_CALL_ACCEPT -> {

                val extras = intent.extras
                val callId = extras?.getString(EXTRA_CALL_ID)
                val callType = extras?.getInt(EXTRA_CALL_TYPE)
                val callInitiatorId = extras?.getInt(EXTRA_CALL_INITIATOR_ID)
                val callInitiatorName = extras?.getString(EXTRA_CALL_INITIATOR_NAME)
                val callOpponents = extras?.getIntegerArrayList(EXTRA_CALL_OPPONENTS)
                val userInfo = extras?.getString(EXTRA_CALL_USER_INFO)
                Log.i(TAG, "NotificationReceiver onReceive Call ACCEPT, callId: $callId")

                val broadcastIntent = Intent(ACTION_CALL_ACCEPT)
                val bundle = Bundle()
                bundle.putString(EXTRA_CALL_ID, callId)
                bundle.putString(Constants.EXTRA_CALL_UUID, callId)
                bundle.putInt(EXTRA_CALL_TYPE, callType!!)
                bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId!!)
                bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
                bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
                bundle.putString(EXTRA_CALL_USER_INFO, userInfo)
//                broadcastIntent.putExtras(bundle)
//
//                LocalBroadcastManager.getInstance(context.applicationContext)
//                    .sendBroadcast(broadcastIntent)
                bundle.putString(Constants.EXTRA_CALL_UUID, callId)
                bundle.putString(Constants.EXTRA_CALLER_NAME, callInitiatorName)
                bundle.putString(Constants.EXTRA_CALL_NUMBER, userInfo)

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

                val launchIntent =
                    context.packageManager.getLaunchIntentForPackage("net.vinbrain.smartcare")
                
                context.startActivity(launchIntent)


                NotificationManagerCompat.from(context).cancel(callId.hashCode())
            }

            ACTION_CALL_NOTIFICATION_CANCELED -> {
                val extras = intent.extras
                val callId = extras?.getString(EXTRA_CALL_ID)
                val callType = extras?.getInt(EXTRA_CALL_TYPE)
                val callInitiatorId = extras?.getInt(EXTRA_CALL_INITIATOR_ID)
                val callInitiatorName = extras?.getString(EXTRA_CALL_INITIATOR_NAME)
                val userInfo = extras?.getString(EXTRA_CALL_USER_INFO)
                Log.i(
                    TAG,
                    "NotificationReceiver onReceive Delete Call Notification, callId: $callId"
                )
                LocalBroadcastManager.getInstance(context.applicationContext)
                    .sendBroadcast(
                        Intent(ACTION_CALL_NOTIFICATION_CANCELED).putExtra(
                            EXTRA_CALL_ID,
                            callId
                        )
                    )
            }
        }
    }
}