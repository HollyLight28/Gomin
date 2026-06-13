package uz.unnarsx.cherrygram.alerts

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONObject
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.messenger.Utilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RLottieImageView
import org.telegram.ui.Components.SizeNotifierFrameLayout
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalFragment
import java.net.HttpURLConnection
import java.net.URL

class AirAlertStatsActivity : UniversalFragment() {

    private val regions = listOf(
        1 to "Вінницька", 2 to "Волинська", 3 to "Дніпропетровська", 4 to "Донецька",
        5 to "Житомирська", 6 to "Закарпатська", 7 to "Запорізька", 8 to "Івано-Франківська",
        9 to "Київська", 10 to "Кіровоградська", 11 to "Луганська", 12 to "Львівська",
        13 to "Миколаївська", 14 to "Одеська", 15 to "Полтавська", 16 to "Рівненська",
        17 to "Сумська", 18 to "Тернопільська", 19 to "Харківська", 20 to "Херсонська",
        21 to "Хмельницька", 22 to "Черкаська", 23 to "Чернівецька", 24 to "Чернігівська",
        25 to "м. Київ", 26 to "АР Крим"
    )

    private var cachedResults: Map<Int, Boolean> = emptyMap()
    private var lastFetchTime: Long = 0L
    private var isLoading = false
    private var swipeRefresh: SwipeRefreshLayout? = null
    private var dataLoaded = false
    private var isDestroyed = false


    override fun getTitle(): CharSequence {
        return LocaleController.getString("CG_AirAlertStats", R.string.CG_AirAlertStats)
    }

    override fun createView(context: Context): View {
        setMD3(true)
        val baseView = super.createView(context) as SizeNotifierFrameLayout

        swipeRefresh = SwipeRefreshLayout(context).also {
            it.addView(baseView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
            it.setColorSchemeColors(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader))
            it.setProgressBackgroundColorSchemeColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            it.setOnRefreshListener {
                cachedResults = emptyMap()
                lastFetchTime = 0L
                loadData()
            }
        }
        fragmentView = swipeRefresh

        loadData()
        return swipeRefresh!!
    }

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        if (isLoading && cachedResults.isEmpty()) {
            items.add(buildLoadingItem())
            return
        }
        if (cachedResults.isEmpty() && dataLoaded) {
            items.add(buildErrorItem())
            return
        }
        if (cachedResults.isEmpty()) return

        items.add(UItem.asShadow(LocaleController.getString("CG_AirAlertStats", R.string.CG_AirAlertStats)))
        for ((id, name) in regions) {
            val hasAlert = cachedResults[id] ?: false
            items.add(buildRegionItem(id, name, hasAlert))
        }
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {}

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean = false

    private fun loadData() {
        if (System.currentTimeMillis() - lastFetchTime < 30_000 && cachedResults.isNotEmpty()) {
            listView?.adapter?.update(false)
            return
        }
        isLoading = true
        dataLoaded = false
        listView?.adapter?.update(false)
        fetchAllRegions()
    }

    private fun fetchAllRegions() {
        Utilities.globalQueue.postRunnable {
            val results = mutableMapOf<Int, Boolean>()
            var success = false
            try {
                val url = URL("http://204.168.201.148:5000/status/all")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val id = key.toIntOrNull()
                        if (id != null) {
                            results[id] = json.optBoolean(key, false)
                        }
                    }
                    success = true
                }
            } catch (e: Exception) {
                FileLog.e(e)
            } finally {
                AndroidUtilities.runOnUIThread {
                    if (isDestroyed || parentActivity == null) return@runOnUIThread
                    if (success) {
                        cachedResults = results.toMap()
                        dataLoaded = true
                    } else {
                        dataLoaded = true
                    }
                    lastFetchTime = System.currentTimeMillis()
                    isLoading = false
                    swipeRefresh?.isRefreshing = false
                    listView?.adapter?.update(false)
                }
            }
        }
    }

    private fun buildRegionItem(id: Int, name: String, hasAlert: Boolean): UItem {
        val ctx = getContext() ?: ApplicationLoader.applicationContext
        return UItem.asCustom(id, createRegionView(ctx, name, hasAlert))
    }

    private fun createRegionView(context: Context, name: String, hasAlert: Boolean): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                AndroidUtilities.dp(20f), AndroidUtilities.dp(12f),
                AndroidUtilities.dp(20f), AndroidUtilities.dp(12f)
            )
        }

        TextView(context).also { text ->
            text.text = name
            text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            text.setTextColor(
                if (hasAlert) 0xFFE53935.toInt()
                else Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
            )
            if (hasAlert) text.typeface = AndroidUtilities.bold()
            text.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            layout.addView(text, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f))
        }

        TextView(context).also { status ->
            status.text = if (hasAlert) "ТРИВОГА" else "Спокій"
            status.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            status.setTextColor(
                if (hasAlert) 0xFFE53935.toInt()
                else 0xFF4CAF50.toInt()
            )
            if (hasAlert) status.typeface = AndroidUtilities.bold()
            status.gravity = Gravity.CENTER_VERTICAL or Gravity.END
            layout.addView(status, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        }

        return layout
    }

    private fun buildLoadingItem(): UItem {
        val ctx = getContext() ?: ApplicationLoader.applicationContext

        val layout = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setPadding(0, AndroidUtilities.dp(48f), 0, AndroidUtilities.dp(48f))
        }

        RLottieImageView(ctx).also { anim ->
            anim.setAutoRepeat(true)
            anim.setAnimation(R.raw.statistic_preload, 64, 64)
            anim.playAnimation()
            layout.addView(anim, LayoutHelper.createLinear(64, 64))
        }

        return UItem.asCustom(layout)
    }

    private fun buildErrorItem(): UItem {
        val ctx = getContext() ?: ApplicationLoader.applicationContext

        val layout = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setPadding(0, AndroidUtilities.dp(48f), 0, AndroidUtilities.dp(48f))
        }

        TextView(ctx).also { label ->
            label.text = "Не вдалося завантажити дані"
            label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText))
            label.gravity = Gravity.CENTER
            layout.addView(label, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        }

        TextView(ctx).also { retry ->
            retry.text = "Спробувати знову"
            retry.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            retry.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader))
            retry.gravity = Gravity.CENTER
            retry.setPadding(0, AndroidUtilities.dp(16f), 0, 0)
            retry.setOnClickListener { loadData() }
            layout.addView(retry, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        }

        return UItem.asCustom(layout)
    }

    override fun onFragmentDestroy() {
        super.onFragmentDestroy()
        isDestroyed = true
    }
}
