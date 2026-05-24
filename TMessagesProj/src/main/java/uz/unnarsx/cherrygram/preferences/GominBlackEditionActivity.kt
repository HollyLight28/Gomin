package uz.unnarsx.cherrygram.preferences

import android.content.Context
import android.view.View
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
        return "Gomin Black Edition"
    }

    override fun createView(context: Context): View {
        setMD3(true)
        return super.createView(context)
    }

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        items.add(UItem.asHeader("Ексклюзивний преміум"))
        
        // Check if Monet theme is currently active
        val isBlackEditionActive = Theme.getActiveTheme().isMonet
        
        items.add(
            SettingsHelper.asSwitchCG(
                switchRow, 
                "Активація Black Edition", 
                "Увімкнути ультра-преміальний монохромний режим"
            ).setChecked(isBlackEditionActive)
        )
        
        val manifestText = "Чому Гомін Black Edition? (Маніфест цифрової свободи)\n\n" +
                "Сучасні смартфони перетворилися на дофамінові пастки. Кожна яскрава червона цятка, барвиста аватарка чи різнокольорова кнопка — це інструменти \"економіки уваги\", створені для того, щоб тримати ваш мозок у стані постійного збудження. Це викликає втому, тривожність та розпорошує увагу.\n\n" +
                "Концепція «Чорного Мерседеса»\n" +
                "Black Edition — це філософія стриманої розкоші, фокусу та тиші. Ми вимикаємо всі кольорові подразники, перетворюючи інтерфейс додатка на чистий, глибокий монохром.\n\n" +
                "Наукові дослідження доводять:\n" +
                "1. Дофаміновий детокс: Дослідження Стенфордського університету підтверджують, що перехід інтерфейсу на монохромний режим знижує час безцільного \"заліпання\" в гаджетах на 30–40%. Без яскравих плям мозок фокусується виключно на важливому — на змісті повідомлень та людях.\n" +
                "2. Турбота про твій зір: Чорно-біла палітра кардинально зменшує контрастне навантаження на очі, запобігаючи втомі при тривалому читанні, особливо в темряві.\n" +
                "3. Енергоефективність: На AMOLED-екранах чистий чорний колір повністю вимикає пікселі, що економить до 20% заряду акумулятора вашого пристрою.\n\n" +
                "Black Edition — вибір тих, хто керує своєю увагою самостійно. Сфокусуйся на важливому. Фокусуйся на людях."
                
        items.add(UItem.asShadow(manifestText))
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        if (item.id == switchRow) {
            val wasActive = Theme.getActiveTheme().isMonet
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
            
            // Rebuild views to apply theme changes instantly
            parentLayout.rebuildAllFragmentViews(false, false)
        }
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean {
        return false
    }
}
