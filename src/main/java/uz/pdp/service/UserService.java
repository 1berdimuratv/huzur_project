package uz.pdp.service;

import lombok.Getter;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.pdp.model.*;
import uz.pdp.model.enams.Lang;
import uz.pdp.model.enams.UserState;
import uz.pdp.repository.UserRepository;
import uz.pdp.utils.CoreUtils;
import uz.pdp.utils.GlobalVar;
import uz.pdp.utils.Template;

import javax.swing.text.DefaultEditorKit;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static uz.pdp.utils.MessageKey.*;

public class UserService {
    @Getter
    private static final UserService instance = new UserService();
    private final UserRepository userRepository = UserRepository.getInstance();
    private final CategoryService categoryService = CategoryService.getInstance();
    private final ProductService productService = ProductService.getInstance();
    private final OrderService orderService = OrderService.getInstance();
    private static I18nService i18n = I18nService.getInstance();
    private UserService(){}

    public void mainMenu(Update update, Message message) {
        User user = GlobalVar.getUSER();
        if (!switchMenu(message,user)) {
            ResService.sendErrorMsg(user.getChatId());
        }
        userRepository.update(user.getChatId(),user);
        switch (user.getUserState()){
            case MENU -> {
                List<Category> all = categoryService.findAll(user.getLang());
                boolean basket = !orderService.getAllByUser(user.getChatId()).isEmpty();
                ResService.sendMsg(user.getChatId(),ORDER_MENU,ButtonService.categoriesMenu(all,basket));
            }
            case ORDERS -> {
                sendOrders(user.getChatId());
            }
            case FEEDBACK -> {
                ResService.sendMsg(user.getChatId(),ASK_OPINION,ButtonService.back());
            }
            case SETTINGS -> {
                ResService.sendMsg(user.getChatId(),SETTINGS,ButtonService.settings(user.getFio(),user.getPhoneNumber()));
            }
        }
    }

    public void menu(Update update, Message message) {
        User user = GlobalVar.getUSER();
        String text = message.getText();
        Long userId = user.getChatId();
        if (text.equals(i18n.getMsg(BACK))){
            user.setUserState(UserState.MAIN_MENU);
            userRepository.update(userId,user);
            ResService.sendMsg(userId, MAIN_MENU, ButtonService.mainMenu());
            return;
        }
        if (text.equals(i18n.getMsg(IN_BASKET))){
            sendOrders(userId);
            return;
        }
        if (text.equals(i18n.getMsg(TAKE_ORDER))) {
            user.setUserState(UserState.WRITE_LOCATION);
            userRepository.update(user.getChatId(), user);
            ResService.sendMsg(user.getChatId(), TAKE_VIL, ButtonService.back());
            return;
        }
        Category category = categoryService.find(text, user.getLang());
        GlobalVar.setCATEGORY(category);
        List<Product> byCategory = productService.findByCategory(category.getName(), user.getLang());
        InputFile inputFile = new InputFile(new File(category.getUrl()));
        ResService.sendPhoto(userId,CHOOSE_PROD,ButtonService.productsMenu(byCategory),inputFile);
        user.setUserState(UserState.ORDERING);
        userRepository.update(userId,user);
    }

    public void getOrder(Update update,Message message) {
        String text = message.getText();
        User user = GlobalVar.getUSER();
        Long chatId = user.getChatId();
        Optional<Product> byName = productService.findByName(text, user.getLang());
        if(byName.isEmpty()){
            user.setUserState(UserState.MENU);
            userRepository.update(chatId,user);
            List<Category> all = categoryService.findAll(user.getLang());
            boolean basket = !orderService.getAllByUser(user.getChatId()).isEmpty();
            ResService.sendMsg(chatId,ORDER_MENU,ButtonService.categoriesMenu(all,basket));
            return;
        }
        Product product = byName.get();
        GlobalVar.setPRODUCT(product);
        InputFile inputFile = new InputFile(new File(product.getUrl()));
        user.setUserState(UserState.CHOOSING_AMOUNT);
        userRepository.update(chatId,user);
        ResService.sendPhoto(chatId,Template.productCardForUser(product.getName(),product.getDesc(),product.getPrice()),ButtonService.chooseProduct(product),inputFile);
        ResService.sendMsg(chatId,HELPER);
    }

    public void ordering_amount(Update update, Message message) {
        User user = GlobalVar.getUSER();
        String text = message.getText();
        String category = GlobalVar.getCATEGORY().getName();
        if (text.equals(i18n.getMsg(BACK))) {
            user.setUserState(UserState.ORDERING);
            userRepository.update(user.getChatId(), user);
            List<Product> products = productService.findByCategory(category,GlobalVar.getUSER().getLang());
            ResService.sendMsg(user.getChatId(), ORDER_MENU, ButtonService.productsMenu(products));
            return;
        }
        Product product = productService.findById(GlobalVar.getPRODUCT().getId(), user.getLang()).get();
        Integer amount;
        try {
            amount = Integer.parseInt(text);
        } catch (Exception e) {
            ResService.sendErrorMsg(user.getChatId());
            return;
        }
        user.setUserState(UserState.ORDERING);
        userRepository.update(user.getChatId(), user);
        orderService.addOrder(amount, user.getChatId(),product);
        List<Product> all = productService.findByCategory(category,GlobalVar.getUSER().getLang());
        ResService.sendMsg(user.getChatId(), SUCCESSFUL);
        ResService.sendMsg(user.getChatId(), CHOOSE_PROD, ButtonService.productsMenu(all));
    }

    private void sendOrders(Long userId) {
        User user = GlobalVar.getUSER();
        List<OrderProduct> allByUser = orderService.getAllByUser(userId);
        String res = Template.myOrdersMessage(allByUser);
        if (allByUser.isEmpty()) {
            ResService.sendMsg(userId,EMPTY_ORDERS);
            user.setUserState(UserState.MENU);
            userRepository.update(userId,user);
            ResService.sendMsg(user.getChatId(), ORDER_MENU, ButtonService.mainMenu());
        }else {
            user.setUserState(UserState.ORDERS);
            userRepository.update(userId, user);
            ResService.sendMsg(user.getChatId(), res, ButtonService.myOrders(allByUser));
        }
    }

    public void orders(Update update, Message message) {
        String text = message.getText();
        User user = GlobalVar.getUSER();
        List<Category> all = categoryService.findAll(user.getLang());
        if (text.equals(i18n.getMsg(BACK))){
             user.setUserState(UserState.MENU);
             userRepository.update(user.getChatId(),user);
             boolean basket = !orderService.getAllByUser(user.getChatId()).isEmpty();
             ResService.sendMsg(user.getChatId(),ORDER_MENU,ButtonService.categoriesMenu(all,basket));
        }else if (text.equals(i18n.getMsg(CLEAR_BASKET))){
            orderService.clearAll(user.getChatId());
            user.setUserState(UserState.MAIN_MENU);
            userRepository.update(user.getChatId(),user);
            ResService.sendMsg(user.getChatId(),SUCCESSFUL,ButtonService.mainMenu());
        }else {
             try {
                 String prod = text.substring(2);
                 Product byName = productService.findByName(prod,GlobalVar.getUSER().getLang()).get();
                 orderService.clearProd(user.getChatId(),byName.getId());
                 List<OrderProduct> allByUser = orderService.getAllByUser(user.getChatId());
                 String res = Template.myOrdersMessage(allByUser);
                 ResService.sendMsg(user.getChatId(), res, ButtonService.myOrders(allByUser));
             }catch (Exception e){
                 ResService.sendErrorMsg(user.getChatId());
             }
         }
    }

    public void takeAddress(Update update, Message message) {
        User user = GlobalVar.getUSER();
        Order order = orderService.findByUserId(user.getChatId());
        Address address = order.getAddress();
        Integer step = CoreUtils.analyzeByOrder(order);
        String text = message.getText();
        if (text.equals(i18n.getMsg(BACK))) {
            switch (step) {
                case 1 -> {
                    user.setUserState(UserState.MENU);
                    userRepository.update(user.getChatId(), user);
                    List<Category> all = categoryService.findAll(user.getLang());
                    boolean basket = !orderService.getAllByUser(user.getChatId()).isEmpty();
                    ResService.sendMsg(user.getChatId(), ORDER_MENU, ButtonService.categoriesMenu(all, basket));
                    return;
                }
                case 2 -> {
                    address.setViloyat(false);
                    address.setBody(address.getBody().substring(0,address.getBody().lastIndexOf("$")));
                    orderService.update(order);
                    ResService.sendMsg(user.getChatId(), TAKE_VIL, ButtonService.back());
                    return;
                }
                case 3 -> {
                    address.setTuman(false);
                    address.setBody(address.getBody().substring(0,address.getBody().lastIndexOf("$")));
                    orderService.update(order);
                    ResService.sendMsg(user.getChatId(), TAKE_TUM, ButtonService.back());
                    return;
                }
                case 4 -> {
                    address.setDom(false);
                    address.setBody(address.getBody().substring(0,address.getBody().lastIndexOf("$")));
                    orderService.update(order);
                    ResService.sendMsg(user.getChatId(), TAKE_DDM, ButtonService.back());
                    return;
                }
                case 5 -> {
                    address.setXonadon(false);
                    address.setBody(address.getBody().substring(0,address.getBody().lastIndexOf("$")));
                    orderService.update(order);
                    ResService.sendMsg(user.getChatId(), TAKE_XON, ButtonService.back());
                    return;
                }
            }
        } else {
            switch (step) {
                case 1 -> {
                    address.setViloyat(true);
                    address.setBody(address.getBody() + "$" + text);
                    orderService.update(order);
                    ResService.sendMsg(user.getChatId(), TAKE_TUM, ButtonService.back());
                }
                case 2 -> {
                    address.setTuman(true);
                    address.setBody(address.getBody() + "$" + text);
                    orderService.update(order);
                    ResService.sendMsg(user.getChatId(), TAKE_DDM, ButtonService.back());
                }
                case 3 -> {
                    address.setDom(true);
                    address.setBody(address.getBody() + "$" + text);
                    orderService.update(order);
                    ResService.sendMsg(user.getChatId(), TAKE_XON, ButtonService.back());
                }
                case 4 -> {
                    address.setXonadon(true);
                    address.setBody(address.getBody() + "$" + text);
                    orderService.update(order);
                    user.setUserState(UserState.SHARE_LOCATION);
                    userRepository.update(user.getChatId(),user);
                    ResService.sendMsg(user.getChatId(), MESSAGE_LOCATION, ButtonService.sendLocation());
                }
            }
        }
    }


    public void overOrder(Update update, Message message) {
        User user = GlobalVar.getUSER();
        if (message.hasText()) {
            String text = message.getText();
            if (Objects.equals(text, i18n.getMsg(BACK))) {
                user.setUserState(UserState.MAIN_MENU);
                userRepository.update(user.getChatId(),user);
                ResService.sendMsg(user.getChatId(), MAIN_MENU, ButtonService.mainMenu());
                return;
            } else {
                ResService.sendErrorMsg(user.getChatId());
            }
        }
        Location location = message.getLocation();
        Double latitude = location.getLatitude();
        Double longitude = location.getLongitude();
        List<OrderProduct> allByUser = orderService.getAllByUser(user.getChatId());
        Order order = orderService.findByUserId(user.getChatId());
        String s = Template.ordersMessageForAdmin(allByUser, user,order.getAddress().getBody());
        ResService.sendOrderRequest(s,latitude,longitude);
        ResService.sendMsg(user.getChatId(),i18n.getMsg(ORDER_ACCEPTED));
        user.setUserState(UserState.MAIN_MENU);
        userRepository.update(user.getChatId(),user);
        ResService.sendMsg(user.getChatId(),MAIN_MENU,ButtonService.mainMenu());
        orderService.clearAll(user.getChatId());
    }

    public void feedback(Update update, Message message) {
        User user = GlobalVar.getUSER();
        String text = message.getText();
        if (Objects.equals(text,i18n.getMsg(BACK)))
            ResService.sendMsg(user.getChatId(),MAIN_MENU, ButtonService.mainMenu());
        else {
            ResService.sendFeedback(text,user.getFio(),user.getPhoneNumber());
            ResService.sendMsg(user.getChatId(),TAKE_OPINION,ButtonService.mainMenu());
        }
        user.setUserState(UserState.MAIN_MENU);
        userRepository.update(user.getChatId(),user);
    }
    public void settings(Update update, Message message) {
        User user = GlobalVar.getUSER();
        String text = message.getText();
        if (Objects.equals(text,i18n.getMsg(CHANGE_LANG))) {
            user.setUserState(UserState.CHOOSE_LANGUAGE);
            userRepository.update(user.getChatId(), user);
            ResService.sendMsg(user.getChatId(), CHOOSE_LANG, ButtonService.chooseLanguage());
        }else if (Objects.equals(text,i18n.getMsg(CHANGE_NAME).formatted(user.getFio()))) {
            user.setUserState(UserState.CHANG_NAM);
            userRepository.update(user.getChatId(), user);
            ResService.sendMsg(user.getChatId(), USERS_FIO);
        }else if (Objects.equals(text,i18n.getMsg(CHANGE_NUMBER).formatted(user.getPhoneNumber()))) {
            user.setUserState(UserState.CHANG_NUM);
            userRepository.update(user.getChatId(), user);
            ResService.sendMsg(user.getChatId(), SHARE_PHONE_NUMBER, ButtonService.sharePhoneNumber());
        }
        else if (Objects.equals(text,i18n.getMsg(BACK))){
            user.setUserState(UserState.MAIN_MENU);
            userRepository.update(user.getChatId(), user);
            ResService.sendMsg(user.getChatId(), MAIN_MENU, ButtonService.mainMenu());
        }
    }
    public void userRestart(Long chatId){
        Optional<User> byId = userRepository.findById(chatId);
        if (byId.isPresent()){
            userRepository.delete(chatId);
            if (orderService.findByUserId(chatId) != null)
                orderService.clearAll(chatId);
        }
    }

    public User userVerify(Long chatId) {
        Optional<User> optional = userRepository.findById(chatId);
        if (optional.isEmpty()) {
            User user = User.builder()
                    .chatId(chatId)
                    .userState(UserState.USER_STARTED)
                    .lang(Lang.UZ)
                    .build();
            userRepository.save(user);
            return user;
        }
        return optional.get();
    }

    public void started( Message message) {
        User user = GlobalVar.getUSER();
        if(     !message.hasText() ||
                !message.getText().equals("/start") ) {
            try {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(GlobalVar.getUSER().getChatId());
                deleteMessage.setMessageId(message.getMessageId());
                GlobalVar.getMyBot().execute(deleteMessage);
                return;
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        user.setUserState(UserState.CHOOSE_LANGUAGE);
        userRepository.update(user.getChatId(), user);
        ResService.sendMsg(user.getChatId(), STARTED_MSG, ButtonService.chooseLanguage());
    }

    public void usersFIO(Update update, Message message) {
        User user = GlobalVar.getUSER();
        String name = message.getText();
        if (name == null || !CoreUtils.checkNameFormat(name,user.getLang())){
            ResService.sendMsg(user.getChatId(),NAME_ERROR);
            return;
        }
        user.setFio(name);
        user.setUserState(UserState.MAIN_MENU);
        userRepository.update(user.getChatId(), user);

        ResService.sendMsg(user.getChatId(), MAIN_MENU,ButtonService.mainMenu());
    }


        public void setUserLang(Update update, Message message) {
        User user = GlobalVar.getUSER();
        String text = message.getText();

        if (!setLang(text, user)) {
            ResService.sendErrorMsg(user.getChatId());
            return;
        }
        if (user.getFio()!=null){
            user.setUserState(UserState.SETTINGS);
            userRepository.update(user.getChatId(),user);
            ResService.sendMsg(user.getChatId(), SETTINGS, ButtonService.settings(user.getFio(),user.getPhoneNumber()));
            return;
        }
        user.setUserState(UserState.SHARE_PHONE_NUMBER);
        userRepository.update(user.getChatId(), user);

        ResService.sendMsg(user.getChatId(), SHARE_PHONE_NUMBER,ButtonService.sharePhoneNumber());
    }

    public void setPhoneNumber(Update update, Message message) {
        User user = GlobalVar.getUSER();
        String phone;
        if (message.hasContact()) {
            phone = message.getContact().getPhoneNumber();
            user.setUsername(message.getContact().getFirstName());
        }
        else
            phone = message.getText();

        if (!CoreUtils.checkPhoneNumber(phone)) {
            ResService.sendErrorMsg(user.getChatId());
            return;
        }

        user.setPhoneNumber(phone);
        user.setUserState(UserState.USERS_FIO);
        userRepository.update(user.getChatId(), user);
        ResService.sendMsg(user.getChatId(), USERS_FIO);
    }

    private boolean setLang(String text, User user) {
        if (Objects.equals(text, LANG_UZ.getVal()))
            user.setLang(Lang.UZ);
        else if (Objects.equals(text, LANG_RU.getVal()))
            user.setLang(Lang.RU);
        else
            return false;
        return true;
    }

    private boolean switchMenu(Message message, User user) {
        String text = message.getText();
        if (Objects.equals(text,i18n.getMsg(MENU))){
            user.setUserState(UserState.MENU);
        }else if (Objects.equals(text,i18n.getMsg(ORDERS))){
            user.setUserState(UserState.ORDERS);
        } else if (Objects.equals(text,i18n.getMsg(SETTINGS))) {
            user.setUserState(UserState.SETTINGS);
        } else if (Objects.equals(text,i18n.getMsg(FEEDBACK))) {
            user.setUserState(UserState.FEEDBACK);
        } else
            return false;
        return true;
    }

    public void changing(Update update, Message message) {
        User user = GlobalVar.getUSER();
        if (user.getUserState() == UserState.CHANG_NUM){
            String phone;
            if (message.hasContact()) {
                phone = message.getContact().getPhoneNumber();
                user.setUsername(message.getContact().getFirstName());
            }
            else {
                phone = message.getText();
            }

            if (!CoreUtils.checkPhoneNumber(phone)) {
                ResService.sendErrorMsg(user.getChatId());
                return;
            }

            user.setPhoneNumber(phone);
            user.setUserState(UserState.SETTINGS);
            userRepository.update(user.getChatId(), user);
            ResService.sendMsg(user.getChatId(), SETTINGS,ButtonService.settings(user.getFio(),user.getPhoneNumber()));
        }
        else {
            String name = message.getText();
            if (!CoreUtils.checkNameFormat(name,user.getLang())){
                ResService.sendErrorMsg(user.getChatId());
                return;
            }
            user.setFio(name);
            user.setUserState(UserState.SETTINGS);
            userRepository.update(user.getChatId(), user);
            ResService.sendMsg(user.getChatId(), SETTINGS, ButtonService.settings(user.getFio(),user.getPhoneNumber()));
        }
    }
}
