package uz.pdp.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.pdp.model.OrderProduct;
import uz.pdp.model.User;
import uz.pdp.utils.GlobalVar;
import uz.pdp.utils.MessageKey;

import java.util.List;

@Slf4j
public class ResService {
    private static final I18nService i18n = I18nService.getInstance();
    private static final Long me = -4266295619L;
    public static void sendMsg(Long chatId, String text, ReplyKeyboard replyKeyboard) {
        try {
            GlobalVar.getMyBot().execute(
                    SendMessage.builder()
                            .text(text)
                            .chatId(chatId)
                            .replyMarkup(replyKeyboard)
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void sendMsg(Long chatId, String text) {
        sendMsg(chatId, text, null);
    }
    public static void sendFeedback(String feedback,String userFIO, String userPhone) {
        String text = """
                FIO : %s
                Number : %s
                Feedback : %s
                """.formatted(userFIO,userPhone,feedback);
        sendMsg(me, text, null);
    }
    public static void sendMsg(Long chatId, MessageKey text) {
        sendMsg(chatId, text, null);
    }
    public static void sendErrorMsg(Long chatId) {
        sendMsg(chatId, "Error!!");
    }


    public static void sendMsg(Long chatId, MessageKey key, ReplyKeyboard replyKeyboard) {
        sendMsg(chatId, i18n.getMsg(key), replyKeyboard);
    }


    public static void sendPhoto(Long chatId, MessageKey key, ReplyKeyboard replyKeyboard, InputFile inputFile) {
        try {
            GlobalVar.getMyBot().execute(
                    SendPhoto.builder()
                            .chatId(chatId)
                            .caption(i18n.getMsg(key))
                            .photo(inputFile)
                            .replyMarkup(replyKeyboard)
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }public static void sendPhoto(Long chatId, String card, ReplyKeyboard replyKeyboard, InputFile inputFile) {
        try {
            GlobalVar.getMyBot().execute(
                    SendPhoto.builder()
                            .chatId(chatId)
                            .caption(card)
                            .photo(inputFile)
                            .replyMarkup(replyKeyboard)
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
    public static void changeButtons(Long chatId, ReplyKeyboard replyKeyboard){
        try {
            GlobalVar.getMyBot().execute(
                    SendMessage.builder()
                            .chatId(chatId)
                            .replyMarkup(replyKeyboard)
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void sendOrderRequest(String s,Double lat,Double lon) {
        try {
            SendMessage build = SendMessage.builder()
                    .text(s)
                    .chatId(me)
                    .build();
            SendLocation sendLocation = new SendLocation(me.toString(),lat,lon);
            GlobalVar.getMyBot().execute(build);
            GlobalVar.getMyBot().execute(sendLocation);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
    public static void getLocation(String s, ReplyKeyboardMarkup replyKeyboard) {
        Long chatId = GlobalVar.getUSER().getChatId();
        try {
            GlobalVar.getMyBot().execute(
                    SendMessage.builder()
                            .chatId(chatId)
                            .replyMarkup(replyKeyboard)
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void deleteMsg(Long chatId,Integer message) {
        try {
            GlobalVar.getMyBot().execute(
                    DeleteMessage.builder()
                            .chatId(chatId)
                            .messageId(message)
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}
