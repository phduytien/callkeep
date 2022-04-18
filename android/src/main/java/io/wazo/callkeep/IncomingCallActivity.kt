package io.wazo.callkeep

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.*

class IncomingCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)
        val extras = intent.extras
        val callId = extras?.getString(Constants.EXTRA_CALL_UUID)
        val number = extras?.getString(Constants.EXTRA_CALL_NUMBER)
        val name = extras?.getString(Constants.EXTRA_CALLER_NAME)

        findViewById<TextView>(R.id.tv_doctor_name).text = name
        findViewById<TextView>(R.id.tv_phone_number).text = number

        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_CALL_UUID, callId)
        bundle.putString(Constants.EXTRA_CALL_NUMBER, number)
        bundle.putString(Constants.EXTRA_CALLER_NAME, name)

        findViewById<View>(R.id.btn_answer).setOnClickListener {
            sendCallRequestToActivity(
                Constants.ACTION_ANSWER_CALL,
                attributeMap = bundleToMap(bundle)
            )
            sendCallRequestToActivity(
                Constants.ACTION_AUDIO_SESSION,
                attributeMap = bundleToMap(bundle)
            )
            val context: Context = this.applicationContext
            val packageName = context.packageName
            val focusIntent =
                context.packageManager.getLaunchIntentForPackage(packageName)?.cloneFilter()

            val intent = focusIntent?.apply {
                putExtras(bundle)
            }
            startActivity(intent)
            finish()
        }
        findViewById<View>(R.id.btn_decline).setOnClickListener {
            sendCallRequestToActivity(
                Constants.ACTION_END_CALL,
                attributeMap = bundleToMap(bundle)
            )
            finish()
        }
    }

    override fun onDestroy() {
        NotificationManagerCompat.from(this)
            .cancel(intent.extras?.getString(Constants.EXTRA_CALL_UUID).hashCode())
        super.onDestroy()
    }
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

private fun Activity.sendCallRequestToActivity(
    action: String,
    attributeMap: HashMap<String, String>,
) {
    val intent = Intent(action)
    val extras = Bundle()
    extras.putSerializable("attributeMap", attributeMap)
    intent.putExtras(extras)
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}