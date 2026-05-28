/**
 * This is the source code of Gomin Suite for Android.
 * It is licensed under GNU GPL v. 2 or later.
 */

package uz.unnarsx.cherrygram.chats.gemini

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.text.TextUtils
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.UserObject
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BottomSheet
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.LaunchActivity
import org.telegram.ui.Stories.recorder.ButtonWithCounterView
import uz.unnarsx.cherrygram.misc.Constants

class GominShieldBottomSheet(
    private val chatActivity: ChatActivity,
    private val partnerName: String,
    private val historyText: String,
    private val cachedResult: String? = null
) : BottomSheet(chatActivity.parentActivity, false, chatActivity.resourceProvider) {

    private val container: LinearLayout
    private val scrollView: ScrollView
    private val textView: TextView
    private val loadingText: TextView
    private val actionButton: ButtonWithCounterView
    private val closeButton: ButtonWithCounterView

    private var analysisResult: String? = null
    private var updateProgressRunnable: Runnable? = null

    init {
        // Дозволяємо нативний скрол та свайп вниз для закриття
        setCanDismissWithSwipe(true)
        backgroundPaddingLeft = 0
        backgroundPaddingTop = 0
        setApplyTopPadding(false)

        val context = chatActivity.parentActivity

        // Головний контейнер вертикальної верстки
        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16f), 0, dp(16f))
            setBackgroundColor(getThemedColor(Theme.key_dialogBackground))
        }

        // Заголовок Bottom Sheet (вертикальний преміум-вигляд)
        val headerView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22f), 0, dp(22f), dp(12f))
        }

        val headerTitle = TextView(context).apply {
            text = LocaleController.getString(R.string.CG_GominShield)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
            setTextColor(getThemedColor(Theme.key_dialogTextBlack))
            typeface = AndroidUtilities.bold()
        }
        headerView.addView(headerTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val headerSubtitle = TextView(context).apply {
            text = "Аналіз діалогу з $partnerName"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            setTextColor(getThemedColor(Theme.key_dialogTextGray2))
            setPadding(0, dp(4f), 0, 0)
        }
        headerView.addView(headerSubtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        container.addView(headerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        // Розділювальна тонка лінія
        val divider = View(context).apply {
            setBackgroundColor(getThemedColor(Theme.key_dialogShadowLine))
        }
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0f, 0f, 0f, 8f))

        // Скрол для результату аналізу
        scrollView = ScrollView(context).apply {
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            isVerticalScrollBarEnabled = true
        }
        nestedScrollChild = scrollView

        // Текстове поле для аналізу
        textView = TextView(context).apply {
            setPadding(dp(22f), dp(8f), dp(22f), dp(16f))
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            setTextColor(getThemedColor(Theme.key_dialogTextBlack))
            setTextIsSelectable(true)
            lineHeight = dp(22f)
            text = ""
        }
        scrollView.addView(textView)

        // Контейнер для завантаження
        val loadingLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(40f), 0, dp(40f))
        }

        // progressView removed for premium minimal look

        loadingText = TextView(context).apply {
            text = "Зчитуємо контекст вашого діалогу... 🔍"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTextColor(getThemedColor(Theme.key_dialogTextGray2))
            gravity = Gravity.CENTER
            setPadding(dp(32f), dp(16f), dp(32f), 0)
            lineHeight = dp(20f)
        }
        loadingLayout.addView(loadingText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        // Постійний красивий дисклеймер знизу
        val disclaimerText = TextView(context).apply {
            text = "⚠️ Дисклеймер: Аналіз виконано штучним інтелектом на основі відкритих психологічних патернів. Гомін AI може помилятися та не дає медичних чи юридичних діагнозів. Головний орієнтир — це твоє самопочуття, бро."
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f)
            setTextColor(getThemedColor(Theme.key_dialogTextGray2))
            gravity = Gravity.CENTER
            setPadding(dp(32f), dp(16f), dp(32f), 0)
            lineHeight = dp(16f)
        }
        loadingLayout.addView(disclaimerText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        container.addView(loadingLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        container.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, dp(340f))) // Фіксуємо висоту скролу для нативності
        scrollView.visibility = View.GONE

        // Контейнер кнопок знизу
        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16f), dp(12f), dp(16f), 0)
        }

        actionButton = ButtonWithCounterView(context, chatActivity.resourceProvider).apply {
            setRound()
            setFilled(true)
            setText("💬 Почати чат про це", false)
            visibility = View.GONE
            setOnClickListener {
                dismiss()
                analysisResult?.let { result ->
                    GominAiChatHelper.activeShieldContext = result
                    GominAiChatHelper.activeShieldHistory = historyText
                    GominAiChatHelper.activeShieldPartnerName = partnerName
                    
                    // Переходимо у віртуальний чат Gomin AI
                    val args = Bundle().apply {
                        putLong("user_id", Constants.GOMIN_AI_DIALOG_ID)
                    }
                    val gominChatActivity = ChatActivity(args)
                    LaunchActivity.instance?.let { la ->
                        la.actionBarLayout?.presentFragment(gominChatActivity)
                    }
                }
            }
        }
        buttonLayout.addView(actionButton, LayoutHelper.createLinear(0, 48, 1f, 0, 0, 8, 0))

        closeButton = ButtonWithCounterView(context, chatActivity.resourceProvider).apply {
            setRound()
            setFilled(false)
            setText("Закрити", false)
            setOnClickListener {
                dismiss()
            }
        }
        buttonLayout.addView(closeButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48))

        container.addView(buttonLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        setCustomView(container)

        // Запуск асинхронного аналізу
        startAnalysis(loadingLayout)
    }

    override fun dismiss() {
        super.dismiss()
        updateProgressRunnable?.let {
            AndroidUtilities.cancelRunOnUIThread(it)
            updateProgressRunnable = null
        }
    }

    private fun startAnalysis(loadingLayout: LinearLayout) {
        if (cachedResult != null) {
            loadingLayout.visibility = View.GONE
            scrollView.visibility = View.VISIBLE
            analysisResult = cachedResult
            textView.text = cachedResult
            actionButton.visibility = View.VISIBLE
            return
        }

        val steps = arrayOf(
            "Зчитуємо контекст вашого діалогу... 🔍",
            "Аналізуємо структуру реплік на предмет пасивної агресії та тиску... 🧩",
            "Перевіряємо наявність газлайтингу, знецінення та патернів DARVO... 🚩",
            "Оцінюємо психологічний вплив на твої особисті кордони... 🛡️",
            "Визначаємо приховані наміри та вигоди співрозмовника... 🧐",
            "Формулюємо готові фрази-відповіді для впевненого захисту... 🗣️",
            "Завершуємо формування твого ментального щита... 🚀"
        )
        var stepIndex = 0

        val run = object : Runnable {
            override fun run() {
                if (stepIndex < steps.size) {
                    val nextText = steps[stepIndex]
                    stepIndex++
                    
                    // Плавне згасання поточного тексту, зміна тексту та плавна поява нового
                    loadingText.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            loadingText.text = nextText
                            loadingText.animate()
                                .alpha(1f)
                                .setDuration(200)
                                .start()
                        }
                        .start()
                        
                    AndroidUtilities.runOnUIThread(this, 2200)
                }
            }
        }
        updateProgressRunnable = run
        AndroidUtilities.runOnUIThread(run)

        GominAiChatHelper.analyzeManipulation(partnerName, historyText) { success, resultText ->
            updateProgressRunnable?.let {
                AndroidUtilities.cancelRunOnUIThread(it)
                updateProgressRunnable = null
            }
            loadingLayout.visibility = View.GONE
            scrollView.visibility = View.VISIBLE
            
            if (success) {
                analysisResult = resultText
                textView.text = resultText
                actionButton.visibility = View.VISIBLE
                GominAiChatHelper.saveToCache(chatActivity.dialogId, resultText, historyText)
            } else {
                textView.text = resultText
                textView.setTextColor(getThemedColor(Theme.key_text_RedRegular))
            }
        }
    }

    companion object {
        @JvmStatic
        fun show(chatActivity: ChatActivity) {
            val messages = chatActivity.messages
            val currentAccount = chatActivity.currentAccount

            // Збір імені співрозмовника
            val partnerUser = MessagesController.getInstance(currentAccount).getUser(chatActivity.dialogId)
            val partnerName = if (partnerUser != null) UserObject.getUserName(partnerUser) else "Співрозмовник"

            // Екстракція останніх 1000 повідомлень чату в хронологічному порядку (Оптімізовано O(N) з таймстампами)
            val historyList = ArrayList<String>()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            var count = 0
            
            for (i in 0 until messages.size) {
                val messageObject = messages[i]
                if (messageObject != null && messageObject.messageOwner != null && messageObject.messageOwner.message != null) {
                    val text = messageObject.messageOwner.message.trim()
                    if (!TextUtils.isEmpty(text)) {
                        val fromId = messageObject.messageOwner.from_id?.user_id ?: messageObject.messageOwner.peer_id?.user_id ?: 0L
                        val senderUser = if (fromId != 0L) MessagesController.getInstance(currentAccount).getUser(fromId) else null
                        val sender = if (senderUser != null) UserObject.getUserName(senderUser) else "Невідомий"
                        val formattedTime = sdf.format(java.util.Date(messageObject.messageOwner.date * 1000L))
                        historyList.add("[$formattedTime] $sender: $text")
                        count++
                        if (count >= 1000) break
                    }
                }
            }

            historyList.reverse()
            val historyText = historyList.joinToString("\n").trim()

            if (TextUtils.isEmpty(historyText)) {
                AndroidUtilities.runOnUIThread {
                    val builder = AlertDialog.Builder(chatActivity.parentActivity, chatActivity.resourceProvider)
                    builder.setTitle(LocaleController.getString(R.string.CG_GominShield))
                    builder.setMessage("Бро, у цьому чаті немає текстових повідомлень для аналізу!")
                    builder.setPositiveButton("Зрозуміло", null)
                    builder.show()
                }
                return
            }

            val cachedHistory = GominAiChatHelper.getCachedHistory(chatActivity.dialogId)
            val cachedResult = GominAiChatHelper.getCachedResult(chatActivity.dialogId)
            val isCacheValid = cachedHistory != null && cachedResult != null && cachedHistory == historyText

            val bottomSheet = GominShieldBottomSheet(
                chatActivity,
                partnerName,
                historyText,
                if (isCacheValid) cachedResult else null
            )
            bottomSheet.show()
        }
    }
}
