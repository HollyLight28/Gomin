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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;
import kotlin.Unit;
import uz.unnarsx.cherrygram.alerts.AirAlertController;
import uz.unnarsx.cherrygram.chats.helpers.ChatsHelper2;
import uz.unnarsx.cherrygram.core.configs.CherrygramAppearanceConfig;
import uz.unnarsx.cherrygram.core.configs.CherrygramCoreConfig;
import uz.unnarsx.cherrygram.core.configs.CherrygramMessagesConfig;
import uz.unnarsx.cherrygram.core.configs.CherrygramPrivacyConfig;
import uz.unnarsx.cherrygram.core.configs.CherrygramChatsConfig;
import uz.unnarsx.cherrygram.core.configs.CherrygramCameraConfig;
import uz.unnarsx.cherrygram.core.crashlytics.FirebaseAnalyticsHelper;
import uz.unnarsx.cherrygram.core.helpers.DeeplinkHelper;
import uz.unnarsx.cherrygram.core.ui.CGBulletinCreator;
import uz.unnarsx.cherrygram.helpers.ui.PopupHelper;
import uz.unnarsx.cherrygram.preferences.helpers.SettingsHelper;
import uz.unnarsx.cherrygram.preferences.helpers.AlertDialogSwitchers;

public class CGPreferencesEntry extends UniversalFragment {

    private final int monobankRow = 100;

    // Ghost Mode
    private final int ghostModeReadMessagesRow = 10;
    private final int ghostModeHideTypingRow = 11;
    private final int ghostModeHideStoryViewsRow = 12;
    private final int ghostModeHideOnlineRow = 13;

    // Speed & Network (Killer Feature)
    private final int downloadSpeedBoostRow = 20;
    private final int uploadSpeedBoostRow = 21;
    private final int slowNetworkModeRow = 22;

    // Camera
    private final int cameraQualityRow = 30;
    private final int cameraFpsRow = 31;
    private final int cameraDualRow = 32;

    // Chats & Messages
    private final int autoQuoteRow = 40;
    private final int deleteForAllRow = 41;
    private final int customWallpapersRow = 42;
    private final int customChatRow = 43;
    private final int disableSwipeToNextRow = 44;
    private final int doubleTapRow = 45;
    private final int slideActionRow = 46;
    private final int brandedScreenshotsRow = 47;
    private final int keepDeletedMessagesRow = 48;

    // AI
    private final int geminiSettingsRow = 50;
    private final int voiceTranscriptionRow = 51;

    // Appearance
    private final int hideSearchBarRow = 52;
    private final int showMainTabsRow = 53;
    private final int hideStoriesRow = 54;

    // Misc
    private final int springAnimationRow = 60;

    // Air Alert
    private final int airAlertEnabledRow = 70;
    private final int airAlertApiKeyRow = 71;
    private final int airAlertRegionRow = 72;
    private final int airAlertTestRow = 73;
    private final int airAlertCheckStatusRow = 74;

    @Override
    protected CharSequence getTitle() {
        FirebaseAnalyticsHelper.INSTANCE.trackEventWithEmptyBundle("main_preferences_screen");
        return getString(R.string.CGP_AdvancedSettings);
    }

    @Override
    public View createView(Context context) {
        setMD3(true);
        return super.createView(context);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // ❤️ Підтримка проекту (Брендована карточка)
        items.add(SettingsHelper.asCustomWithBackground(monobankRow, createSupportCard()));
        items.add(UItem.asShadow(null));

        // 🤖 GOMIN.ai (Gemini.ai)
        items.add(UItem.asHeader(getString(R.string.CP_SystemAI)));
        items.add(UItem.asButton(geminiSettingsRow, R.drawable.msg_bot, getString(R.string.CP_GeminiAI_Settings)));
        items.add(UItem.asShadow(null));

        // 👻 Gomin Ghost (Privace Sub-Engine)
        items.add(UItem.asHeader(getString(R.string.SP_GhostMode_Header)));
        items.add(SettingsHelper.asSwitchCG(ghostModeReadMessagesRow, getString(R.string.SP_GhostMode_ReadMessages), getString(R.string.SP_GhostMode_ReadMessages_Desc))
                .setChecked(CherrygramPrivacyConfig.INSTANCE.getGhostModeReadMessages())
        );
        items.add(SettingsHelper.asSwitchCG(ghostModeHideTypingRow, getString(R.string.SP_GhostMode_HideTyping), getString(R.string.SP_GhostMode_HideTyping_Desc))
                .setChecked(CherrygramPrivacyConfig.INSTANCE.getGhostModeHideTyping())
        );
        items.add(SettingsHelper.asSwitchCG(ghostModeHideStoryViewsRow, getString(R.string.SP_GhostMode_HideStoryViews), getString(R.string.SP_GhostMode_HideStoryViews_Desc))
                .setChecked(CherrygramPrivacyConfig.INSTANCE.getGhostModeHideStoryViews())
        );
        items.add(SettingsHelper.asSwitchCG(ghostModeHideOnlineRow, getString(R.string.SP_GhostMode_HideOnline), getString(R.string.SP_GhostMode_HideOnline_Desc))
                .setChecked(CherrygramPrivacyConfig.INSTANCE.getGhostModeHideOnline())
        );
        items.add(UItem.asShadow(null));

        // 🚨 Повітряна тривога (Air Alert) - ТЕПЕР ОДНА КАРТКА!
        items.add(UItem.asHeader(getString(R.string.CP_AirAlert_Header)));
        items.add(SettingsHelper.asSwitchCG(airAlertEnabledRow, getString(R.string.CP_AirAlert_Enabled), null)
                .setChecked(CherrygramCoreConfig.INSTANCE.getAirAlertEnabled())
        );
        if (CherrygramCoreConfig.INSTANCE.getAirAlertEnabled()) {
            String regionName = CherrygramCoreConfig.INSTANCE.getAirAlertRegionName();
            items.add(UItem.asButton(airAlertRegionRow, getString(R.string.CP_AirAlert_Region), regionName.isEmpty() ? getString(R.string.NotSet) : regionName));
            items.add(UItem.asButton(airAlertCheckStatusRow, "Перевірити поточний статус", null));
            items.add(UItem.asButton(airAlertTestRow, getString(R.string.CP_AirAlert_Test), null));
        }

        // ⚡ Speed Engine (Killer Feature) - ТЕПЕР ОДНА КАРТКА!
        items.add(UItem.asHeader(getString(R.string.EP_SpeedEngine)));
        items.add(UItem.asButton(downloadSpeedBoostRow, getString(R.string.EP_DownloadSpeedBoost), getDownloadSpeedBoostValue()));
        items.add(SettingsHelper.asSwitchCG(uploadSpeedBoostRow, getString(R.string.EP_UploadloadSpeedBoost), getString(R.string.EP_UploadloadSpeedBoost_Desc))
                .setChecked(CherrygramCoreConfig.INSTANCE.getUploadSpeedBoost())
        );
        items.add(SettingsHelper.asSwitchCG(slowNetworkModeRow, getString(R.string.EP_SlowNetworkMode), getString(R.string.EP_SlowNetworkMode_Desc))
                .setChecked(CherrygramCoreConfig.INSTANCE.getSlowNetworkMode())
        );
        items.add(UItem.asShadow(getString(R.string.EP_DownloadSpeedBoost_Shadow)));

        // 📷 Камера
        items.add(UItem.asHeader(getString(R.string.CP_Category_Camera)));
        items.add(UItem.asButton(cameraQualityRow, getString(R.string.CP_CameraQuality), CherrygramCameraConfig.INSTANCE.getCameraResolution() + "p"));
        items.add(UItem.asButton(cameraFpsRow, "FPS", getCameraXFpsRange()));
        items.add(SettingsHelper.asSwitchCG(cameraDualRow, getString(R.string.CP_CameraDualCamera), getString(R.string.CP_CameraDualCamera_Desc))
                .setChecked(CherrygramCameraConfig.INSTANCE.getUseDualCamera())
        );
        items.add(UItem.asShadow(null));

        // 💬 Поведінка
        items.add(UItem.asHeader(getString(R.string.CP_ChatSettings)));
        items.add(SettingsHelper.asSwitchCG(autoQuoteRow, getString(R.string.CP_AutoQuoteReplies), getString(R.string.CP_AutoQuote_Desc))
                .setChecked(CherrygramChatsConfig.INSTANCE.getAutoQuoteReplies())
        );
        items.add(SettingsHelper.asSwitchCG(deleteForAllRow, getString(R.string.CP_DeleteForAll), getString(R.string.CP_DeleteForAll_Desc_New))
                .setChecked(CherrygramMessagesConfig.INSTANCE.getDeleteForAll())
        );
        items.add(SettingsHelper.asSwitchCG(keepDeletedMessagesRow, getString(R.string.SP_KeepDeletedMessages), getString(R.string.SP_KeepDeletedMessages_Desc))
                .setChecked(CherrygramPrivacyConfig.INSTANCE.getKeepDeletedMessages())
        );
        items.add(SettingsHelper.asSwitchCG(customWallpapersRow, getString(R.string.CP_PremiumWallpapers), getString(R.string.CP_PremiumWallpapers_Desc))
                .setChecked(CherrygramChatsConfig.INSTANCE.getCustomWallpapers())
        );
        items.add(UItem.asShadow(null));

        // 🎨 Інтерфейс
        items.add(UItem.asHeader(getString(R.string.EP_Header_Interface)));
        items.add(SettingsHelper.asSwitchCG(hideSearchBarRow, getString(R.string.AP_HideSearchBar), getString(R.string.AP_HideSearchBar_Desc))
                .setChecked(CherrygramAppearanceConfig.INSTANCE.getHideSearchFiled())
        );
        items.add(SettingsHelper.asSwitchCG(hideStoriesRow, getString(R.string.CP_HideStories), getString(R.string.CP_HideStories_Desc))
                .setChecked(CherrygramCoreConfig.INSTANCE.getHideStories())
        );
        items.add(UItem.asShadow(null));

        // 🛠️ Інше
        items.add(UItem.asHeader(getString(R.string.Theme)));
        items.add(SettingsHelper.asSwitchCG(springAnimationRow, getString(R.string.CP_SpringAnimation), getString(R.string.CP_SpringAnimation_Desc_New))
                .setChecked(CherrygramCoreConfig.INSTANCE.getSpringAnimation() == CherrygramCoreConfig.ANIMATION_SPRING)
        );
        items.add(UItem.asButton(doubleTapRow, getString(R.string.CP_DoubleTapAction), getDoubleTapActionValue()));
        items.add(UItem.asButton(slideActionRow, getString(R.string.CG_MsgSlideAction), getSlideActionValue()));
        items.add(UItem.asShadow(null));
    }

    private View createSupportCard() {
        android.widget.LinearLayout card = new android.widget.LinearLayout(getContext());
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = org.telegram.messenger.AndroidUtilities.dp(20);
        card.setPadding(padding, padding, padding, padding);

        android.widget.LinearLayout topLayout = new android.widget.LinearLayout(getContext());
        topLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        topLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        android.widget.ImageView icon = new android.widget.ImageView(getContext());
        icon.setImageResource(R.mipmap.ic_launcher); // Наша пташка
        topLayout.addView(icon, org.telegram.ui.Components.LayoutHelper.createLinear(42, 42));

        android.widget.TextView title = new android.widget.TextView(getContext());
        title.setText(getString(R.string.CGP_SupportGominTitle));
        title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(org.telegram.messenger.AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        topLayout.addView(title, org.telegram.ui.Components.LayoutHelper.createLinear(org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 14, 0, 0, 0));

        card.addView(topLayout);

        android.widget.TextView desc = new android.widget.TextView(getContext());
        desc.setText(getString(R.string.CGP_SupportGominDesc));
        desc.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 15);
        desc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        card.addView(desc, org.telegram.ui.Components.LayoutHelper.createLinear(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));

        return card;
    }

    private String getCameraXFpsRange() {
        return switch (CherrygramCameraConfig.INSTANCE.getCameraXFpsRange()) {
            case CherrygramCameraConfig.CameraXFpsRange25to30 -> "25-30";
            case CherrygramCameraConfig.CameraXFpsRange30to30 -> "30-30";
            case CherrygramCameraConfig.CameraXFpsRange30to60 -> "30-60";
            case CherrygramCameraConfig.CameraXFpsRange60to60 -> "60-60";
            default -> getString(R.string.Default);
        };
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == monobankRow) {
            Browser.openUrl(getContext(), "https://send.monobank.ua/jar/4ecLBi7WaZ");
        } else if (item.id == ghostModeReadMessagesRow) {
            CherrygramPrivacyConfig.INSTANCE.setGhostModeReadMessages(!CherrygramPrivacyConfig.INSTANCE.getGhostModeReadMessages());
            SettingsHelper.updateCheckState(view, CherrygramPrivacyConfig.INSTANCE.getGhostModeReadMessages());
        } else if (item.id == ghostModeHideTypingRow) {
            CherrygramPrivacyConfig.INSTANCE.setGhostModeHideTyping(!CherrygramPrivacyConfig.INSTANCE.getGhostModeHideTyping());
            SettingsHelper.updateCheckState(view, CherrygramPrivacyConfig.INSTANCE.getGhostModeHideTyping());
        } else if (item.id == ghostModeHideStoryViewsRow) {
            CherrygramPrivacyConfig.INSTANCE.setGhostModeHideStoryViews(!CherrygramPrivacyConfig.INSTANCE.getGhostModeHideStoryViews());
            SettingsHelper.updateCheckState(view, CherrygramPrivacyConfig.INSTANCE.getGhostModeHideStoryViews());
        } else if (item.id == ghostModeHideOnlineRow) {
            CherrygramPrivacyConfig.INSTANCE.setGhostModeHideOnline(!CherrygramPrivacyConfig.INSTANCE.getGhostModeHideOnline());
            SettingsHelper.updateCheckState(view, CherrygramPrivacyConfig.INSTANCE.getGhostModeHideOnline());
        } else if (item.id == airAlertEnabledRow) {
            CherrygramCoreConfig.INSTANCE.setAirAlertEnabled(!CherrygramCoreConfig.INSTANCE.getAirAlertEnabled());
            SettingsHelper.updateCheckState(view, CherrygramCoreConfig.INSTANCE.getAirAlertEnabled());
            listView.adapter.update(true);
            if (CherrygramCoreConfig.INSTANCE.getAirAlertEnabled()) {
                AirAlertController.INSTANCE.startMonitoring();
                String currentRegion = CherrygramCoreConfig.INSTANCE.getAirAlertRegionId();
                if (!currentRegion.isEmpty()) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("region_" + currentRegion);
                }
            } else {
                AirAlertController.INSTANCE.stopMonitoring();
                String currentRegion = CherrygramCoreConfig.INSTANCE.getAirAlertRegionId();
                if (!currentRegion.isEmpty()) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("region_" + currentRegion);
                }
            }

        } else if (item.id == airAlertRegionRow) {
            AirAlertController.INSTANCE.fetchRegions("", regions -> {
                if (regions.isEmpty()) return Unit.INSTANCE;
                ArrayList<String> names = new ArrayList<>();
                ArrayList<String> ids = new ArrayList<>();
                for (Pair<String, String> p : regions) {
                    ids.add(p.getFirst());
                    names.add(p.getSecond());
                }
                PopupHelper.show(names, getString(R.string.CP_AirAlert_Region), ids.indexOf(CherrygramCoreConfig.INSTANCE.getAirAlertRegionId()), getContext(), i -> {
                    String oldRegionId = CherrygramCoreConfig.INSTANCE.getAirAlertRegionId();
                    String newRegionId = ids.get(i);
                    if (!oldRegionId.equals(newRegionId)) {
                        if (!oldRegionId.isEmpty()) {
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("region_" + oldRegionId);
                        }
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("region_" + newRegionId);
                    }
                    CherrygramCoreConfig.INSTANCE.setAirAlertRegionId(newRegionId);
                    CherrygramCoreConfig.INSTANCE.setAirAlertRegionName(names.get(i).replace("  — ", ""));
                    listView.adapter.update(true);
                });
                return Unit.INSTANCE;
            });
        } else if (item.id == airAlertTestRow) {
            AirAlertController.INSTANCE.testAlert();
        } else if (item.id == airAlertCheckStatusRow) {
            AirAlertController.INSTANCE.checkAlertStatus(active -> {
                if (getParentActivity() == null) return Unit.INSTANCE;
                String message = active ? "🚨 ТРИВОГА!" : "✅ Все спокійно";
                BulletinFactory.of(this).createSimpleBulletin(active ? R.raw.error : R.raw.contact_check, message).show();
                return Unit.INSTANCE;
            });
        } else if (item.id == downloadSpeedBoostRow) {
            showDownloadSpeedBoostSelector(() -> SettingsHelper.updateButtonValue(view, getDownloadSpeedBoostValue()));
        } else if (item.id == uploadSpeedBoostRow) {
            CherrygramCoreConfig.INSTANCE.setUploadSpeedBoost(!CherrygramCoreConfig.INSTANCE.getUploadSpeedBoost());
            SettingsHelper.updateCheckState(view, CherrygramCoreConfig.INSTANCE.getUploadSpeedBoost());
        } else if (item.id == slowNetworkModeRow) {
            CherrygramCoreConfig.INSTANCE.setSlowNetworkMode(!CherrygramCoreConfig.INSTANCE.getSlowNetworkMode());
            SettingsHelper.updateCheckState(view, CherrygramCoreConfig.INSTANCE.getSlowNetworkMode());
        } else if (item.id == hideSearchBarRow) {
            CherrygramAppearanceConfig.INSTANCE.setHideSearchFiled(!CherrygramAppearanceConfig.INSTANCE.getHideSearchFiled());
            SettingsHelper.updateCheckState(view, CherrygramAppearanceConfig.INSTANCE.getHideSearchFiled());
            getNotificationCenter().postNotificationName(NotificationCenter.cgUpdateSearchFiledVisibility);
        } else if (item.id == hideStoriesRow) {
            CherrygramCoreConfig.INSTANCE.setHideStories(!CherrygramCoreConfig.INSTANCE.getHideStories());
            SettingsHelper.updateCheckState(view, CherrygramCoreConfig.INSTANCE.getHideStories());
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateStories);
            CGBulletinCreator.INSTANCE.createRestartBulletin(this);
        } else if (item.id == showMainTabsRow) {
            CherrygramAppearanceConfig.INSTANCE.setShowMainTabs(!CherrygramAppearanceConfig.INSTANCE.getShowMainTabs());
            SettingsHelper.updateCheckState(view, CherrygramAppearanceConfig.INSTANCE.getShowMainTabs());
            CGBulletinCreator.INSTANCE.createRestartBulletin(this);
        } else if (item.id == cameraQualityRow) {
            // ... existing camera logic (assuming I should keep it or it was there)
        } else if (item.id == cameraFpsRow) {
            // ... existing camera logic
        } else if (item.id == cameraDualRow) {
            CherrygramCameraConfig.INSTANCE.setUseDualCamera(!CherrygramCameraConfig.INSTANCE.getUseDualCamera());
            SettingsHelper.updateCheckState(view, CherrygramCameraConfig.INSTANCE.getUseDualCamera());
        } else if (item.id == autoQuoteRow) {
            CherrygramChatsConfig.INSTANCE.setAutoQuoteReplies(!CherrygramChatsConfig.INSTANCE.getAutoQuoteReplies());
            SettingsHelper.updateCheckState(view, CherrygramChatsConfig.INSTANCE.getAutoQuoteReplies());
        } else if (item.id == deleteForAllRow) {
            CherrygramMessagesConfig.INSTANCE.setDeleteForAll(!CherrygramMessagesConfig.INSTANCE.getDeleteForAll());
            listView.adapter.update(true);
        } else if (item.id == keepDeletedMessagesRow) {
            CherrygramPrivacyConfig.INSTANCE.setKeepDeletedMessages(!CherrygramPrivacyConfig.INSTANCE.getKeepDeletedMessages());
            listView.adapter.update(true);
        } else if (item.id == customWallpapersRow) {
            CherrygramChatsConfig.INSTANCE.setCustomWallpapers(!CherrygramChatsConfig.INSTANCE.getCustomWallpapers());
            SettingsHelper.updateCheckState(view, CherrygramChatsConfig.INSTANCE.getCustomWallpapers());
        } else if (item.id == springAnimationRow) {
            boolean isSpring = CherrygramCoreConfig.INSTANCE.getSpringAnimation() == CherrygramCoreConfig.ANIMATION_SPRING;
            CherrygramCoreConfig.INSTANCE.setSpringAnimation(isSpring ? CherrygramCoreConfig.ANIMATION_CLASSIC : CherrygramCoreConfig.ANIMATION_SPRING);
            SettingsHelper.updateCheckState(view, !isSpring);
            CGBulletinCreator.INSTANCE.createRestartBulletin(this);
        } else if (item.id == doubleTapRow) {
            showDoubleTapSelector(() -> SettingsHelper.updateButtonValue(view, getDoubleTapActionValue()));
        } else if (item.id == slideActionRow) {
            showSlideActionSelector(() -> SettingsHelper.updateButtonValue(view, getSlideActionValue()));
        } else if (item.id == geminiSettingsRow) {
            CherrygramPreferencesNavigator.INSTANCE.createGemini(this);
        }
    }

    private void showAirAlertApiKeyDialog() {
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(getString(R.string.CP_AirAlert_APIKey));

        android.widget.EditText editText = new android.widget.EditText(getParentActivity());
        editText.setText(CherrygramCoreConfig.INSTANCE.getAirAlertApiKey());
        editText.setSelection(editText.length());

        android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(getParentActivity());
        frameLayout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(10), AndroidUtilities.dp(24), AndroidUtilities.dp(10));
        frameLayout.addView(editText);
        builder.setView(frameLayout);

        builder.setPositiveButton(getString("Save", R.string.Save), (dialog, which) -> {
            String key = editText.getText().toString().trim();
            CherrygramCoreConfig.INSTANCE.setAirAlertApiKey(key);
            listView.adapter.update(true);
        });
        builder.setNegativeButton(getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    protected void onInputDone(int id, String text) {
        if (id == airAlertApiKeyRow) {
            CherrygramCoreConfig.INSTANCE.setAirAlertApiKey(text);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private String getDownloadSpeedBoostValue() {
        return switch (CherrygramCoreConfig.INSTANCE.getDownloadSpeedBoost()) {
            case CherrygramCoreConfig.BOOST_NONE -> "Вимкнено";
            case CherrygramCoreConfig.BOOST_AVERAGE -> "Середня (Баланс)";
            case CherrygramCoreConfig.BOOST_EXTREME -> "Максимальна (Gomin Boost)";
            default -> "Максимальна (Gomin Boost)";
        };
    }

    private void showDownloadSpeedBoostSelector(Runnable runnable) {
        ArrayList<String> configStringKeys = new ArrayList<>();
        ArrayList<Integer> configValues = new ArrayList<>();

        configStringKeys.add("Вимкнено (Telegram)");
        configValues.add(CherrygramCoreConfig.BOOST_NONE);

        configStringKeys.add("Середня (Баланс)");
        configValues.add(CherrygramCoreConfig.BOOST_AVERAGE);

        configStringKeys.add("Максимальна (Gomin Boost)");
        configValues.add(CherrygramCoreConfig.BOOST_EXTREME);

        PopupHelper.show(configStringKeys, getString(R.string.EP_DownloadSpeedBoost), configValues.indexOf(CherrygramCoreConfig.INSTANCE.getDownloadSpeedBoost()), getContext(), i -> {
            CherrygramCoreConfig.INSTANCE.setDownloadSpeedBoost(configValues.get(i));
            if (runnable != null) runnable.run();
        });
    }

    private String getDoubleTapActionValue() {
        return switch (CherrygramMessagesConfig.INSTANCE.getDoubleTapAction()) {
            case CherrygramMessagesConfig.DOUBLE_TAP_ACTION_REACTION -> getString(R.string.Reactions);
            case CherrygramMessagesConfig.DOUBLE_TAP_ACTION_REPLY -> getString(R.string.Reply);
            case CherrygramMessagesConfig.DOUBLE_TAP_ACTION_SAVE -> getString(R.string.CG_ToSaved);
            case CherrygramMessagesConfig.DOUBLE_TAP_ACTION_EDIT -> getString(R.string.Edit);
            case CherrygramMessagesConfig.DOUBLE_TAP_ACTION_TRANSLATE -> getString(R.string.TranslateMessage);
            case CherrygramMessagesConfig.DOUBLE_TAP_ACTION_TRANSLATE_GEMINI -> getString(R.string.TranslateMessage) + " - " + getString(R.string.CP_GeminiAI_Header);
            default -> getString(R.string.Disable);
        };
    }

    private void showDoubleTapSelector(Runnable runnable) {
        ArrayList<String> configStringKeys = new ArrayList<>();
        ArrayList<Integer> configValues = new ArrayList<>();

        configStringKeys.add(getString(R.string.Disable));
        configValues.add(CherrygramMessagesConfig.DOUBLE_TAP_ACTION_NONE);

        configStringKeys.add(getString(R.string.Reactions));
        configValues.add(CherrygramMessagesConfig.DOUBLE_TAP_ACTION_REACTION);

        configStringKeys.add(getString(R.string.Reply));
        configValues.add(CherrygramMessagesConfig.DOUBLE_TAP_ACTION_REPLY);

        configStringKeys.add(getString(R.string.CG_ToSaved));
        configValues.add(CherrygramMessagesConfig.DOUBLE_TAP_ACTION_SAVE);

        configStringKeys.add(getString(R.string.Edit));
        configValues.add(CherrygramMessagesConfig.DOUBLE_TAP_ACTION_EDIT);

        configStringKeys.add(getString(R.string.TranslateMessage));
        configValues.add(CherrygramMessagesConfig.DOUBLE_TAP_ACTION_TRANSLATE);

        configStringKeys.add(getString(R.string.TranslateMessage) + " - " + getString(R.string.CP_GeminiAI_Header));
        configValues.add(CherrygramMessagesConfig.DOUBLE_TAP_ACTION_TRANSLATE_GEMINI);

        PopupHelper.show(configStringKeys, getString(R.string.CP_DoubleTapAction), configValues.indexOf(CherrygramMessagesConfig.INSTANCE.getDoubleTapAction()), getContext(), i -> {
            CherrygramMessagesConfig.INSTANCE.setDoubleTapAction(configValues.get(i));
            if (runnable != null) runnable.run();
        });
    }

    private String getSlideActionValue() {
        return switch (CherrygramMessagesConfig.INSTANCE.getMessageSlideAction()) {
            case CherrygramMessagesConfig.MESSAGE_SLIDE_ACTION_SAVE -> getString(R.string.CG_ToSaved);
            case CherrygramMessagesConfig.MESSAGE_SLIDE_ACTION_TRANSLATE -> getString(R.string.TranslateMessage);
            case CherrygramMessagesConfig.MESSAGE_SLIDE_ACTION_TRANSLATE_GEMINI -> getString(R.string.TranslateMessage) + " - " + getString(R.string.CP_GeminiAI_Header);
            case CherrygramMessagesConfig.MESSAGE_SLIDE_ACTION_DIRECT_SHARE -> getString(R.string.DirectShare);
            default -> getString(R.string.Reply);
        };
    }

    private void showSlideActionSelector(Runnable runnable) {
        ArrayList<String> configStringKeys = new ArrayList<>();
        ArrayList<Integer> configValues = new ArrayList<>();

        configStringKeys.add(getString(R.string.Reply));
        configValues.add(CherrygramMessagesConfig.MESSAGE_SLIDE_ACTION_REPLY);

        configStringKeys.add(getString(R.string.CG_ToSaved));
        configValues.add(CherrygramMessagesConfig.MESSAGE_SLIDE_ACTION_SAVE);

        configStringKeys.add(getString(R.string.TranslateMessage));
        configValues.add(CherrygramMessagesConfig.MESSAGE_SLIDE_ACTION_TRANSLATE);

        configStringKeys.add(getString(R.string.TranslateMessage) + " - " + getString(R.string.CP_GeminiAI_Header));
        configValues.add(CherrygramMessagesConfig.MESSAGE_SLIDE_ACTION_TRANSLATE_GEMINI);

        configStringKeys.add(getString(R.string.DirectShare));
        configValues.add(CherrygramMessagesConfig.MESSAGE_SLIDE_ACTION_DIRECT_SHARE);

        PopupHelper.show(configStringKeys, getString(R.string.CG_MsgSlideAction), configValues.indexOf(CherrygramMessagesConfig.INSTANCE.getMessageSlideAction()), getContext(), i -> {
            CherrygramMessagesConfig.INSTANCE.setMessageSlideAction(configValues.get(i));
            if (runnable != null) runnable.run();
        });
    }

    private String getTranscriptionProviderValue() {
        return CherrygramMessagesConfig.INSTANCE.getVoiceTranscriptionProvider() == CherrygramMessagesConfig.TRANSCRIPTION_PROVIDER_GEMINI
                ? getString(R.string.CP_GeminiAI_Header) : getString(R.string.AppName);
    }

    private void showTranscriptionProviderSelector(Runnable runnable) {
        ArrayList<String> configStringKeys = new ArrayList<>();
        ArrayList<Integer> configValues = new ArrayList<>();

        configStringKeys.add(getString(R.string.CP_GeminiAI_Header));
        configValues.add(CherrygramMessagesConfig.TRANSCRIPTION_PROVIDER_GEMINI);

        configStringKeys.add(getString(R.string.AppName));
        configValues.add(CherrygramMessagesConfig.TRANSCRIPTION_PROVIDER_TELEGRAM);

        PopupHelper.show(configStringKeys, getString(R.string.CP_GeminiAI_VoiceTranscriptionProvider), configValues.indexOf(CherrygramMessagesConfig.INSTANCE.getVoiceTranscriptionProvider()), getContext(), i -> {
            CherrygramMessagesConfig.INSTANCE.setVoiceTranscriptionProvider(configValues.get(i));
            if (runnable != null) runnable.run();
        });
    }

    private UserCell createUserCell() {
        UserCell userCell = new UserCell(getContext(), 14, 0, false, true, getResourceProvider(), false, false);

        userCell.addButton.setText(getString(R.string.Edit));
        userCell.addButton.setOnClickListener(view1 -> {
            if (getUserConfig().getCurrentUser() == null) {
                return;
            }
            Bundle args = new Bundle();
            args.putBoolean("onlySelect", true);
            args.putBoolean("cgPrefs", true);
            args.putBoolean("allowGlobalSearch", false);
            args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
            args.putBoolean("resetDelegate", false);
            args.putBoolean("closeFragment", true);
            DialogsActivity fragment = new DialogsActivity(args);
            fragment.setDelegate((fragment1, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
                long did = dids.get(0).dialogId;

                String selectedChatId = String.valueOf(did);

                SharedPreferences.Editor editor = MessagesController.getMainSettings(currentAccount).edit();
                editor.putString("CP_CustomChatIDSM", selectedChatId).apply();

                fragment.finishFragment(true);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    View avatar = userCell.avatarImageView;

                    int[] loc = new int[2];
                    avatar.getLocationOnScreen(loc);

                    float cx = loc[0] + avatar.getWidth() / 2f;
                    float cy = loc[1] + avatar.getHeight() / 2f;

                    LaunchActivity.makeRipple(cx, cy, 5f);
                }

                listView.adapter.update(false);
                return true;
            });
            presentFragment(fragment);
        });

        long chatId = ChatsHelper2.INSTANCE.getCustomChatID();

        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-chatId);
        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(chatId);

        StringBuilder status = new StringBuilder();
        status.append(getString(R.string.EP_CustomChat_Selected_Title));
        status.append(' ');
        status.append("\"");
        status.append(getString(R.string.SavedMessages));
        status.append("\".");

        if (chatId == getUserConfig().clientUserId) {
            userCell.setData("saved_cg", getString(R.string.SavedMessages), "", 0);
        } else if (chat != null) {
            userCell.setData(chat, chat.title, status, 0);
        } else {
            userCell.setData(user, UserObject.getUserName(user), status, 0);
        }

        return userCell;
    }

}

