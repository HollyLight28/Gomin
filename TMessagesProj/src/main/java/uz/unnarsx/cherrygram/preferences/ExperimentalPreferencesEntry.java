/**
 * This is the source code of Cherrygram for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Please, be respectful and credit the original author if you use this code.
 *
 * Copyright github.com/arsLan4k1390, 2022-2026.
 */

package uz.unnarsx.cherrygram.preferences;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

import uz.unnarsx.cherrygram.core.configs.CherrygramCoreConfig;
import uz.unnarsx.cherrygram.core.crashlytics.FirebaseAnalyticsHelper;
import uz.unnarsx.cherrygram.core.ui.CGBulletinCreator;
import uz.unnarsx.cherrygram.helpers.ui.PopupHelper;
import uz.unnarsx.cherrygram.preferences.helpers.SettingsHelper;

public class ExperimentalPreferencesEntry extends UniversalFragment {

    private final int downloadSpeedBoostRow = 1;
    private final int uploadSpeedBoostRow = 2;
    private final int edgeToEdgeRow = 3;
    private final int tabletModeRow = 4;

    @Override
    protected CharSequence getTitle() {
        FirebaseAnalyticsHelper.INSTANCE.trackEventWithEmptyBundle("experimental_preferences_screen");
        return getString(R.string.EP_Category_Experimental);
    }

    @Override
    public View createView(Context context) {
        setMD3(true);
        return super.createView(context);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString(R.string.EP_Header_Network)));
        items.add(UItem.asButton(downloadSpeedBoostRow, getString(R.string.EP_DownloadSpeedBoost), getDownloadSpeedBoostValue()));
        items.add(SettingsHelper.asSwitchCG(uploadSpeedBoostRow, getString(R.string.EP_UploadSpeedBoost))
                .setChecked(CherrygramCoreConfig.INSTANCE.getUploadSpeedBoost())
        );
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.EP_Header_Interface)));
        items.add(UItem.asButton(edgeToEdgeRow, getString(R.string.CP_EdgeToEdge), getEdgeToEdgeValue()));
        items.add(UItem.asButton(tabletModeRow, getString(R.string.AP_Tablet_Mode), getTabletModeValue()));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == downloadSpeedBoostRow) {
            ArrayList<String> configStringKeys = new ArrayList<>();
            ArrayList<Integer> configValues = new ArrayList<>();

            configStringKeys.add(getString(R.string.Disable));
            configValues.add(CherrygramCoreConfig.BOOST_NONE);

            configStringKeys.add(getString(R.string.EP_SpeedBoost_Average));
            configValues.add(CherrygramCoreConfig.BOOST_AVERAGE);

            configStringKeys.add(getString(R.string.EP_SpeedBoost_Extreme));
            configValues.add(CherrygramCoreConfig.BOOST_EXTREME);

            PopupHelper.show(configStringKeys, getString(R.string.EP_DownloadSpeedBoost), configValues.indexOf(CherrygramCoreConfig.INSTANCE.getDownloadSpeedBoost()), getContext(), i -> {
                CherrygramCoreConfig.INSTANCE.setDownloadSpeedBoost(configValues.get(i));
                SettingsHelper.updateButtonValue(view, getDownloadSpeedBoostValue());
            });
        } else if (item.id == uploadSpeedBoostRow) {
            CherrygramCoreConfig.INSTANCE.setUploadSpeedBoost(!CherrygramCoreConfig.INSTANCE.getUploadSpeedBoost());
            SettingsHelper.updateCheckState(view, CherrygramCoreConfig.INSTANCE.getUploadSpeedBoost());
        } else if (item.id == edgeToEdgeRow) {
            ArrayList<String> configStringKeys = new ArrayList<>();
            ArrayList<Integer> configValues = new ArrayList<>();

            configStringKeys.add(getString(R.string.Enable));
            configValues.add(CherrygramCoreConfig.EDGE_MODE_ENABLE);

            configStringKeys.add(getString(R.string.Disable));
            configValues.add(CherrygramCoreConfig.EDGE_MODE_DISABLE);

            configStringKeys.add(getString(R.string.Default));
            configValues.add(CherrygramCoreConfig.EDGE_MODE_AUTO);

            PopupHelper.show(configStringKeys, getString(R.string.CP_EdgeToEdge), configValues.indexOf(CherrygramCoreConfig.INSTANCE.getEdgeToEdgeMode()), getContext(), i -> {
                CherrygramCoreConfig.INSTANCE.setEdgeToEdgeMode(configValues.get(i));
                SettingsHelper.updateButtonValue(view, getEdgeToEdgeValue());

                CGBulletinCreator.INSTANCE.createRestartBulletin(this);
            });
        } else if (item.id == tabletModeRow) {
            ArrayList<String> configStringKeys = new ArrayList<>();
            ArrayList<Integer> configValues = new ArrayList<>();

            configStringKeys.add(getString(R.string.Enable));
            configValues.add(CherrygramCoreConfig.TABLET_MODE_ENABLE);

            configStringKeys.add(getString(R.string.Disable));
            configValues.add(CherrygramCoreConfig.TABLET_MODE_DISABLE);

            configStringKeys.add(getString(R.string.Default));
            configValues.add(CherrygramCoreConfig.TABLET_MODE_AUTO);

            PopupHelper.show(configStringKeys, getString(R.string.AP_Tablet_Mode), configValues.indexOf(CherrygramCoreConfig.INSTANCE.getTabletMode()), getContext(), i -> {
                CherrygramCoreConfig.INSTANCE.setTabletMode(configValues.get(i));
                SettingsHelper.updateButtonValue(view, getTabletModeValue());

                CGBulletinCreator.INSTANCE.createRestartBulletin(this);
            });
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private String getDownloadSpeedBoostValue() {
        return switch (CherrygramCoreConfig.INSTANCE.getDownloadSpeedBoost()) {
            case CherrygramCoreConfig.BOOST_AVERAGE -> getString(R.string.EP_SpeedBoost_Average);
            case CherrygramCoreConfig.BOOST_EXTREME -> getString(R.string.EP_SpeedBoost_Extreme);
            default -> getString(R.string.Disable);
        };
    }

    private String getEdgeToEdgeValue() {
        return switch (CherrygramCoreConfig.INSTANCE.getEdgeToEdgeMode()) {
            case CherrygramCoreConfig.EDGE_MODE_ENABLE -> getString(R.string.Enable);
            case CherrygramCoreConfig.EDGE_MODE_DISABLE -> getString(R.string.Disable);
            default -> getString(R.string.Default);
        };
    }

    private String getTabletModeValue() {
        return switch (CherrygramCoreConfig.INSTANCE.getTabletMode()) {
            case CherrygramCoreConfig.TABLET_MODE_ENABLE -> getString(R.string.Enable);
            case CherrygramCoreConfig.TABLET_MODE_DISABLE -> getString(R.string.Disable);
            default -> getString(R.string.Default);
        };
    }

}
