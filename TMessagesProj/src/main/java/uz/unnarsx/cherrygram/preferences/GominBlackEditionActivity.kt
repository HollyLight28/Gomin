package uz.unnarsx.cherrygram.preferences

import android.content.Context
import android.view.View
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.R
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalFragment
import org.telegram.ui.ActionBar.Theme
import uz.unnarsx.cherrygram.preferences.helpers.SettingsHelper
import java.util.ArrayList

class GominBlackEditionActivity : UniversalFragment() {

    private val switchRow = 1

    override fun getTitle(): CharSequence {
        return "Гомін: Карбон"
    }

    override fun createView(context: Context): View {
        setMD3(true)
        return super.createView(context)
    }

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        items.add(UItem.asHeader("Ексклюзивний стиль"))
        
        val isBlackEditionActive = Theme.getActiveTheme().isMonet()
        
        items.add(
            SettingsHelper.asSwitchCG(
                switchRow, 
                "Активація режиму «Карбон»", 
                "Увімкнути фірмовий монохромний інтерфейс"
            ).setChecked(isBlackEditionActive)
        )

        // Custom Manifesto View
        val safeContext = getContext() ?: return
        val container = android.widget.LinearLayout(safeContext).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(AndroidUtilities.dp(24f), AndroidUtilities.dp(32f), AndroidUtilities.dp(24f), AndroidUtilities.dp(48f))
        }

        // Fix: Manifesto must ALWAYS be high-contrast to showcase the Black Edition experience
        // regardless of whether the switch is currently ON or OFF.
        val isDark = Theme.isCurrentThemeDark()
        val primaryColor = if (isDark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()

        fun addText(content: String, size: Float, isBold: Boolean = false, topMargin: Int = 16, leftMargin: Int = 0) {
            val tv = android.widget.TextView(safeContext).apply {
                text = content
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, size)
                setTextColor(primaryColor)
                setLineSpacing(0f, 1.35f)
                if (isBold) typeface = AndroidUtilities.bold()
            }
            container.addView(tv, org.telegram.ui.Components.LayoutHelper.createLinear(-1, -2, 0, leftMargin, topMargin, 0, 0))
        }

        addText("ГОМІН: КАРБОН", 28f, true, 0)
        addText("Істинне обличчя месенджера", 20f, true, 6)
        
        addText("Звичний кольоровий інтерфейс — це надійний фундамент, успадкований від класичного Telegram. Це чудова технічна база, але це лише оболонка. Те, що перед вами зараз — це істинне обличчя Гомону.", 17f, false, 24)
        addText("Карбон — це наше бачення того, яким має бути ідеальний цифровий простір. Ми повністю відкинули колір, щоб прибрати візуальний шум і залишити лише найважливіше: інформацію та людей, з якими ви спілкуєтеся.", 17f, false, 16)
        
        addText("Як колір хакає мозок", 18f, true, 32)
        addText("Сучасні додатки працюють як цифрові ігрові автомати. Яскраві іконки, градієнти та червоні бейджі діють як маніпулятивні тригери. Вони створені для того, щоб стимулювати викид швидкого дофаміну і тримати ваш мозок у постійній напрузі. Саме через цей візуальний шум ви заходите в месенджер на секунду, а залипаєте на пів години.", 17f, false, 10)
        
        addText("Когнітивна тиша", 18f, true, 24)
        addText("Режим Карбон жорстко нейтралізує цю маніпуляцію. Коли зникають яскраві акценти, вимикається фонова тривожність. Ваш мозок перестає сканувати екран у пошуках дешевої стимуляції. Створюється середовище «когнітивної тиші». Ви заходите в чат лише тоді, коли вам дійсно є що сказати. Контроль над власною увагою та часом повністю повертається до вас.", 17f, false, 10)
        
        addText("Фізика абсолютного чорного", 18f, true, 24)
        addText("Окрім психології, цей режим має пряме апаратне обґрунтування. На OLED-дисплеях глибокий чорний колір — це буквально відключені пікселі. Замість того, щоб безперервно випромінювати світло, фон перетворюється на фізичну порожнечу. Це дає два абсолютно практичних результати: кардинально знижується зорове навантаження під час нічного читання, а акумулятор вашого пристрою працює відчутно довше.", 17f, false, 10)
        
        addText("Інструмент, а не іграшка", 18f, true, 24)
        addText("Кольоровий режим існує для тих, хто звик до стандартів. Але Карбон — це простір для тих, хто цінує свій фокус. Це суворий, логічний та вивірений інтерфейс, де немає місця для візуального сміття.", 17f, false, 10)

        addText("Увімкніть Карбон.\nВідчуйте справжній Гомін.", 22f, true, 40)

        items.add(UItem.asCustom(container))

    }


    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        if (item.id == switchRow) {
            val wasActive = Theme.getActiveTheme().isMonet()
            val newActive = !wasActive
            
            val themeConfig = ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Context.MODE_PRIVATE)
            val editor = themeConfig.edit()
            
            if (newActive) {
                // Save current themes before switching to Monet
                val currentDay = themeConfig.getString("theme", "Blue")
                val currentNight = themeConfig.getString("nighttheme", "Night")
                if (currentDay != "Monet Light") editor.putString("pre_monet_day", currentDay)
                if (currentNight != "Monet AMOLED") editor.putString("pre_monet_night", currentNight)
                
                editor.putString("lastDayTheme", "Monet Light")
                editor.putString("lastDarkTheme", "Monet AMOLED")
                
                Theme.getTheme("Monet Light")?.let { Theme.setCurrentDayTheme(it) }
                Theme.getTheme("Monet AMOLED")?.let { Theme.setCurrentNightTheme(it) }
            } else {
                val oldDay = themeConfig.getString("pre_monet_day", "Day")
                val oldNight = themeConfig.getString("pre_monet_night", "Night")
                
                editor.putString("lastDayTheme", oldDay)
                editor.putString("lastDarkTheme", oldNight)
                
                Theme.getTheme(oldDay)?.let { Theme.setCurrentDayTheme(it) }
                Theme.getTheme(oldNight)?.let { Theme.setCurrentNightTheme(it) }
            }
            editor.apply()

            val targetThemeName = if (newActive) {
                if (Theme.isCurrentThemeDark()) "Monet AMOLED" else "Monet Light"
            } else {
                if (Theme.isCurrentThemeDark()) "Night" else "Day"
            }
            
            val targetTheme = Theme.getTheme(targetThemeName)
            if (targetTheme != null) {
                Theme.applyTheme(targetTheme, Theme.isCurrentThemeNight())
            }
            
            SettingsHelper.updateCheckState(view, newActive)
            
            // Apply changes and rebuild UI
            AndroidUtilities.runOnUIThread({
                parentLayout?.rebuildAllFragmentViews(true, true)
            }, 100)
        }
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean {
        return false
    }
}
