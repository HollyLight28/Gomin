package uz.unnarsx.cherrygram.preferences.tabs;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

import uz.unnarsx.cherrygram.core.configs.CherrygramAppearanceConfig;
import uz.unnarsx.cherrygram.core.ui.mainTabs.MainTabsManager;

public class MainTabsPreviewCell extends FrameLayout {

    private LinearLayout linearLayout;

    public MainTabsPreviewCell(Context context) {
        super(context);
        linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    public void setEditMode(boolean mode) {}

    public void setTabs(ArrayList<MainTabsManager.Tab> tabs, Context context, Theme.ResourcesProvider resourcesProvider, int currentAccount, boolean isPreview, boolean showTitle) {
        linearLayout.removeAllViews();
        for (MainTabsManager.Tab tab : tabs) {
            if (!tab.enabled) continue;
            
            FrameLayout tabFrame = new FrameLayout(context);
            LinearLayout content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER);
            
            ImageView imageView = new ImageView(context);
            imageView.setImageResource(MainTabsManager.getTabIcon(tab.getType()));
            imageView.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
            content.addView(imageView, LayoutHelper.createLinear(24, 24));
            
            if (CherrygramAppearanceConfig.INSTANCE.getShowMainTabsTitle()) {
                TextView textView = new TextView(context);
                textView.setTextSize(10);
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
                textView.setText(MainTabsManager.INSTANCE.getTabTitle(tab.getType()));
                content.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
            }
            
            tabFrame.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
            linearLayout.addView(tabFrame, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }
    }
}
