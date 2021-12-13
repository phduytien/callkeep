package io.wazo.callkeep

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.*

class IncomingCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)
        val extras = intent.extras
        val callId = extras?.getString(EXTRA_CALL_ID)
        val callType = extras?.getInt(EXTRA_CALL_TYPE)
        val callInitiatorId = extras?.getInt(EXTRA_CALL_INITIATOR_ID)
        val callInitiatorName = extras?.getString(EXTRA_CALL_INITIATOR_NAME)
        val callOpponents = extras?.getIntegerArrayList(EXTRA_CALL_OPPONENTS)
        val userInfo = extras?.getString(EXTRA_CALL_USER_INFO)

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



        findViewById<View>(R.id.btn_answer).setOnClickListener {
            sendCallRequestToActivity(
                Constants.ACTION_ANSWER_CALL,
                attributeMap = bundleToMap(bundle)
            )
            sendCallRequestToActivity(
                Constants.ACTION_AUDIO_SESSION,
                attributeMap = bundleToMap(bundle)
            )
            finish()
            val launchIntent = packageManager.getLaunchIntentForPackage("net.vinbrain.smartcare")
            startActivity(launchIntent)
        }
        findViewById<View>(R.id.btn_reject).setOnClickListener {
            sendCallRequestToActivity(
                Constants.ACTION_END_CALL,
                attributeMap = bundleToMap(bundle)
            )
            finish()
        }
    }

    override fun onDestroy() {
        NotificationManagerCompat.from(this)
            .cancel(intent.extras?.getString(EXTRA_CALL_ID).hashCode())
        super.onDestroy()
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

    private fun sendCallRequestToActivity(
        action: String,
        attributeMap: HashMap<String, String>,
    ) {
        val intent = Intent(action)
        val extras = Bundle()
        extras.putSerializable("attributeMap", attributeMap)
        intent.putExtras(extras)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

    }
}