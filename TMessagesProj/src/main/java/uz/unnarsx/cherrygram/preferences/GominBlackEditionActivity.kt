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
        return "Гомін: Пітьма"
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
                "Активація режиму «Пітьма»", 
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

        addText("🌑 ГОМІН: ПІТЬМА", 28f, true, 0)
        addText("Твій простір цифрової свободи.", 20f, true, 6)
        
        addText("Ми створили Black Edition не для того, щоб просто змінити кольори. Ми створили його, щоб повернути тобі контроль над власною увагою.", 18f, false, 32)
        
        addText("Сучасний світ — це безперервна війна за твій фокус. Яскраві іконки та агресивні кольори — це пастки, що тримають твій мозок у напрузі, змушуючи відкривати телефон знову і знову.", 18f, false, 18)
        
        addText("Пітьма вимикає цей шум.", 20f, true, 32)
        addText("Вона залишає тебе наодинці з головним: твоїми думками та людьми, які мають значення.", 18f, false, 10)

        addText("🧘 Когнітивна тиша. Коли колірний хаос зникає, мозок миттєво розслабляється. Ти більше не витрачаєш ресурс на обробку візуального сміття.", 17f, false, 24, 8)
        addText("🛡️ Свідомий вибір. Жодних колірних пасток. Ти заходиш у месенджер лише тоді, коли це потрібно тобі, а не коли тебе туди «покликав» яскравий акцент.", 17f, false, 16, 8)
        addText("🔋 Фізика спокою. На OLED-екранах чистий чорний — це тиша пікселів. Це здоров’я твоїх очей та енергія твого пристрою.", 17f, false, 16, 8)

        addText("Відчуй справжню тишу.\nКеруй своєю увагою.", 22f, true, 40)

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
