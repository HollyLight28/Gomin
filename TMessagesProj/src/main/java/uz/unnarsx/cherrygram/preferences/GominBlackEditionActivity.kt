package uz.unnarsx.cherrygram.preferences

import android.content.Context
import android.view.View
import org.telegram.messenger.AndroidUtilities
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
        return "Гомін Black Edition"
    }

    override fun createView(context: Context): View {
        setMD3(true)
        return super.createView(context)
    }

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        items.add(UItem.asHeader("Ексклюзивний преміум"))
        
        // Check if Monet theme is currently active
        val isBlackEditionActive = Theme.getActiveTheme().isMonet()
        
        items.add(
            SettingsHelper.asSwitchCG(
                switchRow, 
                "Активація Black Edition", 
                "Увімкнути ультра-преміальний монохромний режим"
            ).setChecked(isBlackEditionActive)
        )
        
        val manifestText = "ГОМІН BLACK EDITION: ФІЛОСОФІЯ ЦИФРОВОЇ СУВЕРЕННОСТІ\n\n" +
                "Сучасний інтерфейс — це не просто дизайн, це складна система маніпуляції вашою увагою. Використання насичених кольорів базується на принципах нейромаркетингу, що стимулюють лімбічну систему мозку. Кожна яскрава іконка діє як візуальний подразник, викликаючи мікро-викиди дофаміну. Це створює «дофамінову петлю», яка змушує вас повертатися до екрана підсвідомо.\n\n" +
                "Black Edition — це радикальна відповідь на «економіку уваги». Ми перетворюємо ваш месенджер з джерела дофамінового шуму на професійний інструмент.\n\n" +
                "НАУКОВЕ ОБҐРУНТУВАННЯ:\n\n" +
                "1. Нейробіологічний детокс (Дофаміновий контроль):\n" +
                "За даними Center for Humane Technology (США), вимкнення колірних стимулів знижує активність вентрального стріатуму — зони мозку, відповідальної за винагороду. Монохромний режим робить додаток «нецікавим» для вашої підсвідомості, повертаючи вам контроль над власним часом.\n\n" +
                "2. Оптимізація когнітивного ресурсу:\n" +
                "Обробка кольору — це енергозатратний процес для зорової кори (зона V4). Згідно з дослідженнями когнітивного навантаження (Дж. Свеллер), візуальний шум створює стороннє навантаження на робочу пам'ять. Вимикаючи колір, ви вивільняєте ресурси мозку для глибокої концентрації. Ви починаєте сприймати текст швидше, бо фокусуєтесь виключно на сенсах.\n\n" +
                "3. Фізика AMOLED та гігієна зору:\n" +
                "На AMOLED-матрицях чорний колір — це повністю вимкнені пікселі. Це мінімізує світловий стрес для сітківки, допомагає підтримувати природний рівень мелатоніну ввечері та заощаджує до 20% заряду акумулятора.\n\n" +
                "Black Edition — вибір тих, хто керує своєю увагою самостійно. Відчуйте тишу. Концентруйтеся на важливому."
                
        items.add(UItem.asShadow(manifestText))
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        if (item.id == switchRow) {
            val wasActive = Theme.getActiveTheme().isMonet()
            val newActive = !wasActive
            
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
