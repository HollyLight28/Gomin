package uz.unnarsx.cherrygram.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AirAlertStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "STOP_SIREN") {
            AirAlertController.stopSiren()
            // При бажанні можна також прибрати саме сповіщення, але зазвичай 
            // краще залишити його, щоб користувач бачив, що тривога ще триває
            // AirAlertNotificationHelper.cancelAll(context)
        }
    }
}
