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
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;

import uz.unnarsx.cherrygram.chats.helpers.ChatsHelper2;
import uz.unnarsx.cherrygram.core.configs.CherrygramAppearanceConfig;
import uz.unnarsx.cherrygram.core.configs.CherrygramCoreConfig;
import uz.unnarsx.cherrygram.core.configs.CherrygramMessagesConfig;
import uz.unnarsx.cherrygram.core.configs.CherrygramPrivacyConfig;
import uz.unnarsx.cherrygram.core.configs.CherrygramChatsConfig;
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

    // Speed & Network
    private final int downloadSpeedBoostRow = 20;
    private final int uploadSpeedBoostRow = 21;
    private final int slowNetworkModeRow = 22;

    // Appearance
    private final int hideSearchBarRow = 30;
    private final int hideStoriesRow = 31;
    private final int showMainTabsRow = 32;
    private final int springAnimationRow = 33;

    // Chats
    private final int customChatRow = 40;
    private final int disableSwipeToNextRow = 41;
    private final int deleteForAllRow = 42;
    private final int doubleTapRow = 43;
    private final int slideActionRow = 44;

    // AI
    private final int geminiSettingsRow = 50;
    private final int voiceTranscriptionRow = 51;

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
        // ☕ Підтримка проекту
        items.add(UItem.asHeader("Підтримка проекту ☕"));
        items.add(UItem.asButton(monobankRow, R.drawable.msg_fave_solar, "Пригостити автора кавою", "https://send.monobank.ua/jar/4ecLBi7WaZ"));
        items.add(UItem.asShadow("Якщо вам подобається Гомін, ви можете підтримати автора гривнею на каву або оплату серверів оновлень!"));

        // 👻 Режим Привида (Gomin Ghost)
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

        // ⚡ Швидкість та Мережа (Speed Engine)
        items.add(UItem.asHeader(getString(R.string.EP_Network)));
        items.add(UItem.asButton(downloadSpeedBoostRow, getString(R.string.EP_DownloadSpeedBoost), getDownloadSpeedBoostValue()));
        items.add(SettingsHelper.asSwitchCG(uploadSpeedBoostRow, getString(R.string.EP_UploadloadSpeedBoost), "Оптимізує буфер передачі до 512 КБ для швидкої та стабільної відправки великих файлів.")
                .setChecked(CherrygramCoreConfig.INSTANCE.getUploadSpeedBoost())
        );
        items.add(SettingsHelper.asSwitchCG(slowNetworkModeRow, getString(R.string.EP_SlowNetworkMode), "Суперстабільний однопотоковий режим пакетами по 32 КБ для бомбосховищ чи укриттів.")
                .setChecked(CherrygramCoreConfig.INSTANCE.getSlowNetworkMode())
        );
        items.add(UItem.asShadow(null));

        // 🎨 Зовнішній вигляд (Appearance)
        items.add(UItem.asHeader(getString(R.string.AP_Header_Appearance)));
        items.add(SettingsHelper.asSwitchCG(hideSearchBarRow, getString(R.string.AP_HideSearchBar), "Прибирає пошуковий рядок зі списку чатів для чистішого інтерфейсу.")
                .setChecked(CherrygramAppearanceConfig.INSTANCE.getHideSearchFiled())
        );
        items.add(SettingsHelper.asSwitchCG(hideStoriesRow, getString(R.string.CP_HideStories), getString(R.string.CP_HideStories_Desc))
                .setChecked(CherrygramCoreConfig.INSTANCE.getHideStories())
        );
        items.add(SettingsHelper.asSwitchCG(showMainTabsRow, "Нижні вкладки навігації", "Вмикає зручні нижні вкладки, як у iOS або WhatsApp.")
                .setChecked(CherrygramAppearanceConfig.INSTANCE.getShowMainTabs())
        );
        items.add(SettingsHelper.asSwitchCG(springAnimationRow, "Пружні анімації", "Вмикає фірмові пружинні переходи інтерфейсу Гомону.")
                .setChecked(CherrygramCoreConfig.INSTANCE.getSpringAnimation() == CherrygramCoreConfig.ANIMATION_SPRING)
        );
        items.add(UItem.asShadow(null));

        // 💬 Поведінка чатів (Chat Settings)
        items.add(UItem.asHeader(getString(R.string.FilterChats)));
        items.add(SettingsHelper.asSwitchCG(customChatRow, getString(R.string.EP_CustomChat), getString(R.string.EP_CustomChat_Desc))
                .setChecked(CherrygramChatsConfig.INSTANCE.getCustomChatForSavedMessages())
        );
        if (CherrygramChatsConfig.INSTANCE.getCustomChatForSavedMessages()) {
            items.add(SettingsHelper.asCustomWithBackground(createUserCell()));
        }
        items.add(SettingsHelper.asSwitchCG(disableSwipeToNextRow, getString(R.string.CP_DisableSwipeToNext), getString(R.string.CP_DisableSwipeToNext_Desc))
                .setChecked(CherrygramChatsConfig.INSTANCE.getDisableSwipeToNext())
        );
        items.add(SettingsHelper.asSwitchCG(deleteForAllRow, getString(R.string.CP_DeleteForAll), getString(R.string.CP_DeleteForAll_Desc))
                .setChecked(CherrygramMessagesConfig.INSTANCE.getDeleteForAll())
        );
        items.add(UItem.asButton(doubleTapRow, getString(R.string.CP_DoubleTapAction), getDoubleTapActionValue()));
        items.add(UItem.asButton(slideActionRow, getString(R.string.CG_MsgSlideAction), getSlideActionValue()));
        items.add(UItem.asShadow(null));

        // 🧠 Штучний Інтелект (Gomin AI)
        items.add(UItem.asHeader("Gomin AI"));
        items.add(UItem.asButton(geminiSettingsRow, R.drawable.msg_bot, "Налаштування Gomin AI (Gemini)"));
        items.add(UItem.asButton(voiceTranscriptionRow, getString(R.string.CP_GeminiAI_VoiceTranscriptionProvider), getTranscriptionProviderValue()));
        items.add(UItem.asShadow(null));
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
            CGBulletinCreator.INSTANCE.createRestartBulletin(this);
        } else if (item.id == showMainTabsRow) {
            CherrygramAppearanceConfig.INSTANCE.setShowMainTabs(!CherrygramAppearanceConfig.INSTANCE.getShowMainTabs());
            SettingsHelper.updateCheckState(view, CherrygramAppearanceConfig.INSTANCE.getShowMainTabs());
            CGBulletinCreator.INSTANCE.createRestartBulletin(this);
        } else if (item.id == springAnimationRow) {
            boolean isSpring = CherrygramCoreConfig.INSTANCE.getSpringAnimation() == CherrygramCoreConfig.ANIMATION_SPRING;
            CherrygramCoreConfig.INSTANCE.setSpringAnimation(isSpring ? CherrygramCoreConfig.ANIMATION_CLASSIC : CherrygramCoreConfig.ANIMATION_SPRING);
            SettingsHelper.updateCheckState(view, !isSpring);
            CGBulletinCreator.INSTANCE.createRestartBulletin(this);
        } else if (item.id == customChatRow) {
            CherrygramChatsConfig.INSTANCE.setCustomChatForSavedMessages(!CherrygramChatsConfig.INSTANCE.getCustomChatForSavedMessages());
            SettingsHelper.updateCheckState(view, CherrygramChatsConfig.INSTANCE.getCustomChatForSavedMessages());
            listView.adapter.update(true);
        } else if (item.id == disableSwipeToNextRow) {
            CherrygramChatsConfig.INSTANCE.setDisableSwipeToNext(!CherrygramChatsConfig.INSTANCE.getDisableSwipeToNext());
            SettingsHelper.updateCheckState(view, CherrygramChatsConfig.INSTANCE.getDisableSwipeToNext());
        } else if (item.id == deleteForAllRow) {
            CherrygramMessagesConfig.INSTANCE.setDeleteForAll(!CherrygramMessagesConfig.INSTANCE.getDeleteForAll());
            SettingsHelper.updateCheckState(view, CherrygramMessagesConfig.INSTANCE.getDeleteForAll());
        } else if (item.id == doubleTapRow) {
            showDoubleTapSelector(() -> SettingsHelper.updateButtonValue(view, getDoubleTapActionValue()));
        } else if (item.id == slideActionRow) {
            showSlideActionSelector(() -> SettingsHelper.updateButtonValue(view, getSlideActionValue()));
        } else if (item.id == geminiSettingsRow) {
            CherrygramPreferencesNavigator.INSTANCE.createGemini(this);
        } else if (item.id == voiceTranscriptionRow) {
            showTranscriptionProviderSelector(() -> SettingsHelper.updateButtonValue(view, getTranscriptionProviderValue()));
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

