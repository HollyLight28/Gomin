/**
 * This is the source code of Cherrygram for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Please, be respectful and credit the original author if you use this code.
 *
 * Copyright github.com/arsLan4k1390, 2022-2026.
 */

package uz.unnarsx.cherrygram.preferences.helpers;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class SettingsHelper {

    public static UItem asCustomWithBackground(int id, View view) {
        UItem i = new UItem(UniversalAdapter.VIEW_TYPE_CUSTOM_WITH_BACKGROUND, false);
        i.id = id;
        i.view = view;
        i.intValue = LayoutHelper.MATCH_PARENT;
        return i;
    }

    public static UItem asCustomWithBackground(View view) {
        UItem i = new UItem(UniversalAdapter.VIEW_TYPE_CUSTOM_WITH_BACKGROUND, false);
        i.view = view;
        i.intValue = LayoutHelper.MATCH_PARENT;
        return i;
    }

    public static UItem asCustomWithBackground(int id, View view, int heightDp) {
        UItem i = new UItem(UniversalAdapter.VIEW_TYPE_CUSTOM_WITH_BACKGROUND, false);
        i.id = id;
        i.view = view;
        i.intValue = heightDp;
        return i;
    }

    public static UItem asCustomWithBackground(View view, int heightDp) {
        UItem i = new UItem(UniversalAdapter.VIEW_TYPE_CUSTOM_WITH_BACKGROUND, false);
        i.view = view;
        i.intValue = heightDp;
        return i;
    }

    public static UItem asTextDetail(int id, int iconResId, CharSequence text, CharSequence value) {
        UItem i = new UItem(UniversalAdapter.VIEW_TYPE_TEXT_DETAIL_SETTINGS, false);
        i.id = id;
        i.iconResId = iconResId;
        i.text = text;
        i.textValue = value;
        return i;
    }

    public static UItem asSpaceCG(int height) {
        UItem item = new UItem(UniversalAdapter.VIEW_TYPE_SPACE_CG, false);
        item.intValue = height;
        return item;
    }

    public static UItem asSwitchCG(int id, CharSequence text) {
        UItem i = new UItem(UniversalAdapter.VIEW_TYPE_CHECK, false);
        i.id = id;
        i.text = text;
        return i;
    }

    public static UItem asSwitchCG(int id, CharSequence text, CharSequence subtext) {
        UItem i = new UItem(UniversalAdapter.VIEW_TYPE_TEXT_CHECK, false);
        i.id = id;
        i.text = text;
        i.subtext = subtext;
        return i;
    }

    /**
     * Створює заголовок секції з мінімалістичною іконкою.
     * Стиль — як у стандартного HeaderCell (синій жирний текст 15sp), але з іконкою 18dp ліворуч.
     */
    public static UItem asHeaderWithIcon(Context context, int iconResId, CharSequence text) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        int headerColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader);

        ImageView iconView = new ImageView(context);
        iconView.setImageResource(iconResId);
        iconView.setColorFilter(new PorterDuffColorFilter(headerColor, PorterDuff.Mode.SRC_IN));
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        layout.addView(iconView, LayoutHelper.createLinear(18, 18, 0, 0, 0, 0));

        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setTextColor(headerColor);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setMinHeight(AndroidUtilities.dp(34));
        layout.addView(textView, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT,
                10, 0, 0, 0
        ));

        int paddingH = AndroidUtilities.dp(20);
        layout.setPadding(paddingH, AndroidUtilities.dp(6), paddingH, AndroidUtilities.dp(6));

        return UItem.asCustom(layout);
    }

    public static void updateCheckState(View view, boolean isChecked) {
        if (view instanceof NotificationsCheckCell notificationsCheckCell) {
            notificationsCheckCell.setChecked(isChecked);
        } else if (view instanceof TextCheckCell textCheckCell) {
            textCheckCell.setChecked(isChecked);
        } else {
            if (view != null) {
                FileLog.e("Unknown view type for setChecked: " + view.getClass().getName());
            } else {
                FileLog.e("Attempted to update check state on a NULL view");
            }
        }
    }

    public static void updateButtonValue(View view, String value) {
        if (view instanceof TextCell textCell) {
            textCell.setValue(value, true);
        } else {
            if (view != null) {
                FileLog.e("Unknown view type for setChecked: " + view.getClass().getName());
            } else {
                FileLog.e("Attempted to update check state on a NULL view");
            }
        }
    }

}
