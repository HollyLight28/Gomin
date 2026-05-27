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
    private val historyText: String
) : BottomSheet(chatActivity.parentActivity, false, chatActivity.resourceProvider) {

    private val container: LinearLayout
    private val scrollView: ScrollView
    private val textView: TextView
    private val progressView: RadialProgressView
    private val loadingText: TextView
    private val actionButton: ButtonWithCounterView
    private val closeButton: ButtonWithCounterView

    private var analysisResult: String? = null

    init {
        // Запобігаємо закриттю жестом вниз, щоб користувач міг скролити текст аналізу
        setCanDismissWithSwipe(false)
        backgroundPaddingLeft = 0

        val context = chatActivity.parentActivity

        // Головний контейнер вертикальної верстки
        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16f), 0, dp(16f))
            setBackgroundColor(getThemedColor(Theme.key_dialogBackground))
        }

        // Заголовок Bottom Sheet
        val headerView = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22f), 0, dp(22f), dp(12f))
        }

        val headerTitle = TextView(context).apply {
            text = LocaleController.getString(R.string.CG_GominShield)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
            setTextColor(getThemedColor(Theme.key_dialogTextBlack))
            typeface = AndroidUtilities.bold()
        }
        headerView.addView(headerTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

        val headerSubtitle = TextView(context).apply {
            text = " • Аналіз маніпуляцій з $partnerName"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTextColor(getThemedColor(Theme.key_dialogTextGray2))
            setPadding(dp(4f), dp(2f), 0, 0)
        }
        headerView.addView(headerSubtitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

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

        progressView = RadialProgressView(context, chatActivity.resourceProvider).apply {
            setSize(dp(40f))
            setProgressColor(getThemedColor(Theme.key_chats_actionBackground))
        }
        loadingLayout.addView(progressView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

        loadingText = TextView(context).apply {
            text = "Ментальний щит аналізує переписку на предмет маніпуляцій, газлайтингу та токсичності. Зачекай секунду, бро... 🤖☕"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTextColor(getThemedColor(Theme.key_dialogTextGray2))
            gravity = Gravity.CENTER
            setPadding(dp(32f), dp(16f), dp(32f), 0)
            lineHeight = dp(20f)
        }
        loadingLayout.addView(loadingText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

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

    private fun startAnalysis(loadingLayout: LinearLayout) {
        GominAiChatHelper.analyzeManipulation(partnerName, historyText) { success, resultText ->
            loadingLayout.visibility = View.GONE
            scrollView.visibility = View.VISIBLE
            
            if (success) {
                analysisResult = resultText
                textView.text = resultText
                actionButton.visibility = View.VISIBLE
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
                        val senderUser = MessagesController.getInstance(currentAccount).getUser(messageObject.messageOwner.from_id.user_id)
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

            val bottomSheet = GominShieldBottomSheet(chatActivity, partnerName, historyText)
            bottomSheet.show()
        }
    }
}
