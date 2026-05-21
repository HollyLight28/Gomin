package uz.unnarsx.cherrygram.preferences;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;

import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

import org.telegram.messenger.R;
import uz.unnarsx.cherrygram.core.configs.CherrygramAppearanceConfig;
import uz.unnarsx.cherrygram.core.configs.CherrygramCoreConfig;
import uz.unnarsx.cherrygram.core.crashlytics.FirebaseAnalyticsHelper;
import uz.unnarsx.cherrygram.core.ui.CGBulletinCreator;
import uz.unnarsx.cherrygram.preferences.helpers.SettingsHelper;

public class GeneralPreferencesEntry extends UniversalFragment {

    private final int silenceNonContactsRow = 1;
    private final int hideStoriesRow = 2;
    private final int showIDRow = 3;

    @Override
    protected CharSequence getTitle() {
        FirebaseAnalyticsHelper.INSTANCE.trackEventWithEmptyBundle("general_preferences_screen");
        return getString(R.string.AP_Header_General);
    }

    @Override
    public View createView(Context context) {
        setMD3(true);
        return super.createView(context);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString(R.string.SettingsNotifications)));
        items.add(SettingsHelper.asSwitchCG(silenceNonContactsRow, getString(R.string.CP_SilenceNonContacts), getString(R.string.CP_SilenceNonContacts_Desc))
                .setChecked(CherrygramCoreConfig.INSTANCE.getSilenceNonContacts())
        );
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.FilterStories)));
        items.add(SettingsHelper.asSwitchCG(hideStoriesRow, getString(R.string.CP_HideStories), getString(R.string.CP_HideStories_Desc))
                .setChecked(CherrygramCoreConfig.INSTANCE.getHideStories())
        );
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.CP_ProfileReplyBackground)));
        items.add(SettingsHelper.asSwitchCG(showIDRow, getString(R.string.AP_ShowID))
                .setChecked(CherrygramAppearanceConfig.INSTANCE.getShowIDDC())
        );
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == silenceNonContactsRow) {
            CherrygramCoreConfig.INSTANCE.setSilenceNonContacts(!CherrygramCoreConfig.INSTANCE.getSilenceNonContacts());
            SettingsHelper.updateCheckState(view, CherrygramCoreConfig.INSTANCE.getSilenceNonContacts());
        } else if (item.id == hideStoriesRow) {
            CherrygramCoreConfig.INSTANCE.setHideStories(!CherrygramCoreConfig.INSTANCE.getHideStories());
            SettingsHelper.updateCheckState(view, CherrygramCoreConfig.INSTANCE.getHideStories());

            CGBulletinCreator.INSTANCE.createRestartBulletin(this);
        } else if (item.id == showIDRow) {
            CherrygramAppearanceConfig.INSTANCE.setShowIDDC(!CherrygramAppearanceConfig.INSTANCE.getShowIDDC());
            SettingsHelper.updateCheckState(view, CherrygramAppearanceConfig.INSTANCE.getShowIDDC());

            parentLayout.rebuildAllFragmentViews(false, false);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
