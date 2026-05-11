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
        private const val isProd = true
    }

    override fun onCreate() {
        super.onCreate()
    }

    private fun uh() {
        Handler(Looper.getMainLooper()).postDelayed({
            // AppRestartHelper.restartApp(ApplicationLoader.applicationContext)
        }, 15_000)
    }
}
