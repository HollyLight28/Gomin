package uz.unnarsx.cherrygram.alerts

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import org.telegram.ui.Components.SizeNotifierFrameLayout
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalFragment
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    private var currentExecutor: java.util.concurrent.ExecutorService? = null


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
        swipeRefresh?.isRefreshing = true
        listView?.adapter?.update(false)
        fetchAllRegions()
    }

    private fun fetchAllRegions() {
        val executor = Executors.newFixedThreadPool(6)
        currentExecutor = executor

        executor.execute {
            val latch = CountDownLatch(26)
            val results = ConcurrentHashMap<Int, Boolean>()

            for (id in 1..26) {
                if (isDestroyed) break
                executor.execute {
                    try {
                        val url = URL("http://204.168.201.148:5000/status?region_id=$id")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000

                        if (connection.responseCode == 200) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(response)
                            results[id] = json.optBoolean("alert", false)
                        }
                    } catch (e: java.lang.Exception) {
                        FileLog.e(e)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            try {
                latch.await(20, TimeUnit.SECONDS)
            } catch (e: java.lang.Exception) {
                FileLog.e(e)
            } finally {
                executor.shutdown()
                AndroidUtilities.runOnUIThread {
                    if (isDestroyed || parentActivity == null) return@runOnUIThread
                    cachedResults = results.toMap()
                    lastFetchTime = System.currentTimeMillis()
                    isLoading = false
                    dataLoaded = true
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

        ImageView(context).also { icon ->
            icon.setImageResource(
                if (hasAlert) R.drawable.msg_notifications_solar
                else R.drawable.msg_bell_mute_solar
            )
            val color = if (hasAlert) 0xFFE53935.toInt()
            else Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
            icon.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            layout.addView(icon, LayoutHelper.createLinear(24, 24, 0f, 0f, 0f, 16f))
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

        ProgressBar(ctx).also { spinner ->
            layout.addView(spinner, LayoutHelper.createLinear(48, 48))
        }

        TextView(ctx).also { label ->
            label.text = "Завантаження..."
            label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText))
            label.gravity = Gravity.CENTER
            label.setPadding(0, AndroidUtilities.dp(16f), 0, 0)
            layout.addView(label, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
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
        try {
            currentExecutor?.shutdownNow()
        } catch (e: java.lang.Exception) {
            FileLog.e(e)
        }
    }
}
