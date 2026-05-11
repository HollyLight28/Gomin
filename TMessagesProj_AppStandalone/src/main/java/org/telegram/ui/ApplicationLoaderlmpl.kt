/**
 * This is the source code of Cherrygram for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Please, be respectful and credit the original author if you use this code.
 *
 * Copyright github.com/arsLan4k1390, 2022-2026.
 */

package org.telegram.ui

import android.os.Handler
import android.os.Looper



import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.ApplicationLoaderImpl
import uz.unnarsx.cherrygram.Extra
import uz.unnarsx.cherrygram.core.helpers.AppRestartHelper
import uz.unnarsx.cherrygram.donates.DonatesManager

class ApplicationLoaderlmpl : ApplicationLoaderImpl() {

    /** SDK Integration start */
    companion object {
        private val expectedPackageName = getPkgName()

        private val expectedSigningCertificateHashBase64 = arrayOf(
            getPkgHash(), getPkgHashGP()
        )

        private const val watcherMail = "arslan4k1390@gmail.com"

        private val supportedAlternativeStores = arrayOf(
            "com.android.vending",               // Google Play Store
            "com.sec.android.app.samsungapps",   // Samsung Galaxy Store
            "com.huawei.appmarket",              // Huawei AppGallery
            "com.xiaomi.market",                 // Xiaomi GetApps
            "com.oppo.market",                   // OPPO / Realme / OnePlus
            "com.bbk.appstore",                  // Vivo
            "com.lenovo.leos.appstore",          // Lenovo/Moto
            "com.amazon.venezia"                 // Amazon AppStore
        )

        private const val isProd = true
        private const val killOnBypass = true

        private fun getPkgName() : String {
            val check = Extra.pkg_arrOne + Extra.pkg_arrTwo + Extra.pkg_arrThree
            return check.joinToString().replace(",", "").replace(" ", "")
        }

        private fun getPkgHash() : String {
            val check = Extra.pkg_hashOne + Extra.pkg_hashTwo + Extra.pkg_hashThree
            return check.joinToString().replace(",", "").replace(" ", "")
        }

        private fun getPkgHashGP() : String {
            val check = Extra.pkg_hashGPOne + Extra.pkg_hashGPTwo + Extra.pkg_hashGPThree
            return check.joinToString().replace(",", "").replace(" ", "")
        }
    }

    override fun onCreate() {
        super.onCreate()


        /*registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                ScreenProtector.INSTANCE.registerScreenCallbacks(activity)
            }
            override fun onActivityPaused(activity: Activity) {
                ScreenProtector.INSTANCE.unregisterScreenCallbacks(activity)
            }
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })*/
    }

    private fun uh() {
        Handler(Looper.getMainLooper()).postDelayed({
            AppRestartHelper.restartApp(ApplicationLoader.applicationContext)
        }, 15_000)
    }

}
