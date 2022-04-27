package io.wazo.callkeep

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.wazo.callkeep.Constants.ACTION_ANSWER_CALL
import io.wazo.callkeep.Constants.ACTION_END_CALL
import java.util.*

class IncomingCallActivity : AppCompatActivity() {
    private var voiceBroadcastReceiver: VoiceBroadcastReceiver? = null
    private var vibrator: Vibrator? = null

    inner class VoiceBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_END_CALL -> {
                    finish()
                }
                ACTION_ANSWER_CALL -> {
                    finish()
                }
            }
        }
    }

    private fun registerReceiver() {
        unregisterReceiver()
        val voiceBroadcastReceiver = VoiceBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_END_CALL)
        intentFilter.addAction(ACTION_ANSWER_CALL)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(voiceBroadcastReceiver, intentFilter)
        this.voiceBroadcastReceiver = voiceBroadcastReceiver
    }

    private fun unregisterReceiver() {
        voiceBroadcastReceiver?.let {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
        }
        voiceBroadcastReceiver = null
    }

    private fun startVibrator() {
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 1000, 300, 200, 100, 500, 200, 100),
                    0
                )
            )
        } else {
            vibrator?.vibrate(
                longArrayOf(0, 100, 1000, 300, 200, 100, 500, 200, 100),
                0
            )
        }
    }

    private fun stopVibrator() {
        vibrator?.cancel()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver()
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
            CallContainer.saveUUid(callId)
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
        startVibrator()
    }

    override fun onDestroy() {
        cancelCallNotification(this)
        unregisterReceiver()
        stopVibrator()
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