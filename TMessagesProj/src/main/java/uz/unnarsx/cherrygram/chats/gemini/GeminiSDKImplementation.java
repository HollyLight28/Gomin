/**
 * This is the source code of Cherrygram for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Please, be respectful and credit the original author if you use this code.
 *
 * Copyright github.com/arsLan4k1390, 2022-2026.
 */

package uz.unnarsx.cherrygram.chats.gemini;

import static org.telegram.messenger.LocaleController.getString;
import static uz.unnarsx.cherrygram.chats.gemini.GeminiResultsBottomSheet.capitalFirst;
import static uz.unnarsx.cherrygram.chats.gemini.GeminiResultsBottomSheet.languageName;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlobPart;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import uz.unnarsx.cherrygram.core.configs.CherrygramMessagesConfig;
import uz.unnarsx.cherrygram.core.configs.CherrygramCoreConfig;

public class GeminiSDKImplementation {

    public static void initGeminiConfig(
            BaseFragment baseFragment,
            ChatActivity chatActivity,
            CharSequence inputText, boolean translateText, boolean summarize,
            File mediaFile,
            boolean ocr, boolean transcribe
    ) {

        if (baseFragment == null || baseFragment.getParentActivity() == null || baseFragment.getContext() == null) return;

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = (float) CherrygramMessagesConfig.INSTANCE.getGeminiTemperatureValue() / 10;
        configBuilder.topK = 10;
        configBuilder.topP = 0.5f;
        configBuilder.maxOutputTokens = 8192;

        ArrayList<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE));

        createGenerativeModel(
                configBuilder,
                safetySettings,
                baseFragment,
                chatActivity,
                inputText, translateText, summarize,
                mediaFile,
                ocr, transcribe
        );

    }

    private static void createGenerativeModel(
            GenerationConfig.Builder configBuilder,
            ArrayList<SafetySetting> safetySettings,
            BaseFragment baseFragment,
            ChatActivity chatActivity,
            CharSequence inputText, boolean translateText, boolean summarize,
            File mediaFile,
            boolean ocr, boolean transcribe
    ) {

        String modelName = (translateText || summarize || transcribe || ocr) 
                ? CherrygramMessagesConfig.INSTANCE.getGeminiSystemModelName().isEmpty() ? "gemini-3.1-flash-lite" : CherrygramMessagesConfig.INSTANCE.getGeminiSystemModelName()
                : CherrygramMessagesConfig.INSTANCE.getGeminiModelName().isEmpty() ? "gemini-3.5-flash" : CherrygramMessagesConfig.INSTANCE.getGeminiModelName();

        GenerativeModel gm = new GenerativeModel(
                modelName,
                CherrygramMessagesConfig.INSTANCE.getGeminiApiKey(),
                configBuilder.build(),
                safetySettings
        );

        generateContent(
                gm,
                baseFragment,
                chatActivity,
                inputText, translateText, summarize,
                mediaFile,
                ocr, transcribe
        );

    }

    private static void generateContent(
            GenerativeModel gm,
            BaseFragment baseFragment,
            ChatActivity chatActivity,
            CharSequence inputText, boolean translateText, boolean summarize,
            File mediaFile,
            boolean ocr, boolean transcribe
    ) {

        AlertDialog progressDialog = new AlertDialog(
                baseFragment.getParentActivity(),
                AlertDialog.ALERT_TYPE_SPINNER,
                baseFragment.getResourceProvider()
        );

        AndroidUtilities.runOnUIThread(() -> {
            try {
                if (!baseFragment.getParentActivity().isFinishing()) progressDialog.show();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });

        Content content = buildContent(inputText, mediaFile, ocr, translateText, transcribe, summarize);

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Executor executor = ContextCompat.getMainExecutor(baseFragment.getContext());

        progressDialog.setOnDismissListener((dialogInterface) -> shutdownGeminiSDK(response));

        Futures.addCallback(response, new FutureCallback<>() {
                    @Override
            public void onSuccess(GenerateContentResponse result) {
                if (result.getText() != null) {
                    String resultText = result.getText().strip();
                    
                    // Senior Cleaner: Прибираємо теги різонінгу, якщо модель їх видала
                    resultText = resultText.replaceAll("(?s)<thought>.*?</thought>", "").trim();
                    resultText = resultText.replaceAll("(?s)\\[reasoning\\].*?\\[/reasoning\\]", "").trim();

                    if (CherrygramCoreConfig.isDevBuild()) FileLog.e("успешный ответ: " + resultText);

                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            if (!baseFragment.getParentActivity().isFinishing() && progressDialog.isShowing()) progressDialog.dismiss();
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    });

                    Bitmap inputBitmap = getBitmapFromFile(mediaFile);

                    if (translateText || transcribe || summarize || inputBitmap != null || ocr) {
                        int subtitle = getBottomSheetSubtitle(translateText, transcribe, summarize, inputBitmap, ocr);
                        GeminiResultsBottomSheet.showAlert(baseFragment, chatActivity, resultText, subtitle);
                    } else {
                        BulletinFactory.global()
                                .createSuccessBulletin(getString(R.string.YourPasswordSuccess), baseFragment.getResourceProvider())
                                .setDuration(Bulletin.DURATION_SHORT)
                                .show();

                        if (chatActivity != null && chatActivity.getChatActivityEnterView() != null && chatActivity.getChatActivityEnterView().getEditField() != null) {
                            chatActivity.getChatActivityEnterView().getEditField().setText(resultText);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                FileLog.e(t);

                GoogleGenerativeAIException ex = GoogleGenerativeAIException.Companion.from(t);

                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        if (!baseFragment.getParentActivity().isFinishing() && progressDialog.isShowing()) progressDialog.dismiss();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                });

                if (ex.getMessage() != null && ex.getMessage().contains("Unexpected")) {
                    // Cause fucking Google
                    // throws an "com.google.ai.client.generativeai.type.ServerException: Unexpected Response"

                    AndroidUtilities.runOnUIThread(() -> {
                        if (baseFragment.getParentActivity() != null && !baseFragment.getParentActivity().isFinishing()) {
                            new AlertDialog.Builder(baseFragment.getContext(), AlertDialog.ALERT_TYPE_MESSAGE, baseFragment.getResourceProvider())
                                    .setTitle(getString(R.string.CP_GeminiAI_Header))
                                    .setMessage(ex.getMessage())
                                    .setPositiveButton(getString(R.string.OK), null)
                                    .create()
                                    .show();
                        }
                    });
                } else {
                    BulletinFactory.global()
                            .createErrorBulletin(ex.getMessage(), baseFragment.getResourceProvider())
                            .setDuration(Bulletin.DURATION_SHORT)
                            .show();

                }

            }
        }, executor);

    }

    private static Content buildContent(
            CharSequence inputText,
            File mediaFile,
            boolean ocr,
            boolean translateText,
            boolean transcribe,
            boolean summarize) {

        Content.Builder content = new Content.Builder();

        Bitmap inputBitmap = getBitmapFromFile(mediaFile);

        if (inputBitmap != null || ocr) {
            String imagePrompt;

            if (ocr) { // OCR - Optical Character Recognition
                imagePrompt = "You are an OCR assistant. Extract and return only the exact text written in the image. " +
                        "Preserve the original language and formatting as much as possible. " +
                        "Do not translate, interpret, or explain the text. Output only the plain extracted text, without any introductions or extra words.";
                content.addText(imagePrompt);
            } else { // Describing picture
                String lang = LocaleController.getInstance().getCurrentLocale().getLanguage();
                imagePrompt = "Describe the content of the image clearly and concisely in the " + lang + " language. " +
                        "Focus on the main objects, people, or scene. Do not add any explanations or introductions. " +
                        "Output only the description.";
                content.addText(imagePrompt);
            }

            content.addImage(inputBitmap);

            if (CherrygramCoreConfig.isDevBuild()) FileLog.e("промпт: " + imagePrompt);
        } else if (translateText) { // Message translation
            String lang = capitalFirst(languageName(CherrygramMessagesConfig.INSTANCE.getTranslationTargetGemini()));
            String translationPrompt = "You are a professional linguist and translator. Your goal is to translate the provided text into " +
                    lang + " with absolute naturalness, preserving any slang, cultural nuances, and the original emotional tone. " +
                    "Do not add any meta-comments, explanations, or quotes. Just provide the final translated text. " +
                    "Source text: " + inputText;
            content.addText(translationPrompt);

            if (CherrygramCoreConfig.isDevBuild()) FileLog.e("промпт: " + translationPrompt);
        } else if (transcribe) { // Voice to text
            byte[] audioBytes = readBytesCompat(mediaFile);
            Part audioPart = new BlobPart("audio/ogg", audioBytes);

            String voiceToTextPrompt;

            if (summarize) {
                voiceToTextPrompt = "You are an AI analyst. Listen to this voice message, transcribe it fully in its original language, " +
                        "and then provide a structured, bullet-point summary of the key points. " +
                        "Output ONLY the summary in the same language as the audio. Do not include 'Summary' titles. " +
                        "Be concise but capture all essential facts.";
            } else {
                voiceToTextPrompt = "You are a professional speech-to-text system. Accurately transcribe every word from this audio. " +
                        "Keep the original language and punctuation. Do not translate. Output ONLY the plain text.";
            }

            content.addText(voiceToTextPrompt);
            content.addPart(audioPart);

            if (CherrygramCoreConfig.isDevBuild()) FileLog.e("промпт: " + voiceToTextPrompt);
        } else { // Answer only to text
            String textPrompt;
            if (summarize) {
                String summarizeString = "Analyze the text below. Extract the main ideas, intentions, and key facts. " +
                        "Present them as a brief, coherent summary in the original language. " +
                        "Avoid generic phrases. Return only the summary text. Content to analyze:";
                textPrompt = summarizeString + inputText.toString();
            } else {
                String systemPrompt = CherrygramMessagesConfig.INSTANCE.getGeminiSystemPrompt();
                textPrompt = systemPrompt + " " + inputText;
            }

            content.addText(textPrompt);

            if (CherrygramCoreConfig.isDevBuild()) FileLog.e("промпт: " + textPrompt);
        }

        return content.build();
    }

    public static void injectGeminiForMedia(
            BaseFragment baseFragment,
            ChatActivity chatActivity,
            MessageObject messageObject,
            boolean isOCR,
            boolean transcribe,
            boolean summarize
    ) {
        if (getMediaFile(messageObject, baseFragment).exists()) {
            initGeminiConfig(
                    baseFragment,
                    chatActivity,
                    "", false, summarize,
                    getMediaFile(messageObject, baseFragment),
                    isOCR, transcribe
            );
            if ((transcribe || summarize) && !messageObject.isOut() && messageObject.isContentUnread()) baseFragment.getMessagesController().markMessageContentAsRead(messageObject);
        } else {
            showDownloadAlert(baseFragment);
        }
    }

    public static File getMediaFile(MessageObject messageObject, BaseFragment baseFragment) {
        File file = null;
        if (!TextUtils.isEmpty(messageObject.messageOwner.attachPath)) {
            file = new File(messageObject.messageOwner.attachPath);
            if (!file.exists()) {
                file = null;
            }
        }
        if (file == null) {
            file = FileLoader.getInstance(baseFragment.getCurrentAccount()).getPathToMessage(messageObject.messageOwner, false, true);
        }
        if (file != null && !file.exists()) {
            file = FileLoader.getInstance(baseFragment.getCurrentAccount()).getPathToMessage(messageObject.messageOwner, true, true);
        }
        return file;
    }

    private static int getBottomSheetSubtitle(boolean translateText, boolean transcribe, boolean summarize, Bitmap inputBitmap, boolean ocr) {
        if (summarize) return GeminiResultsBottomSheet.GEMINI_TYPE_SUMMARIZE;
        if (translateText) return GeminiResultsBottomSheet.GEMINI_TYPE_TRANSLATE;
        if (transcribe) return GeminiResultsBottomSheet.GEMINI_TYPE_TRANSCRIBE;
        if (inputBitmap != null && !ocr) return GeminiResultsBottomSheet.GEMINI_TYPE_EXPLANATION;
        if (inputBitmap != null && ocr) return GeminiResultsBottomSheet.GEMINI_TYPE_OCR;
        return 0;
    }

    private static Bitmap getBitmapFromFile(File file) {
        if (file != null && file.exists()) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } else {
            return null;
        }
    }

    private static byte[] readBytesCompat(File file) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = fis.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
        } catch (IOException e) {
            FileLog.e(e);
            return null;
        }
        return buffer.toByteArray();
    }

    private static void shutdownGeminiSDK(ListenableFuture<GenerateContentResponse> response) {
        if (response != null && !response.isCancelled()) {
            response.cancel(true);
        }
    }

    private static void showDownloadAlert(BaseFragment baseFragment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(baseFragment.getParentActivity(), baseFragment.getResourceProvider());
        builder.setTitle(getString(R.string.CP_GeminiAI_Header));
        builder.setPositiveButton(getString(R.string.OK), null);
        builder.setMessage(getString(R.string.PleaseDownload));
        baseFragment.showDialog(builder.create());
    }

}
