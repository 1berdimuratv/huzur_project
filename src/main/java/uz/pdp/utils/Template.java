package uz.pdp.utils;

import uz.pdp.model.*;
import uz.pdp.model.enams.Lang;
import uz.pdp.service.I18nService;
import uz.pdp.service.ProductService;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public interface Template {
    I18nService i18n = I18nService.getInstance();
    static String myOrdersMessage(List<OrderProduct> products){
        ProductService  productService = ProductService.getInstance();
        Lang lang = GlobalVar.getUSER().getLang();
        StringBuilder res = new StringBuilder();
        res.append(i18n.getMsg(MessageKey.BASKET));
        long price = 0;
        for (OrderProduct product : products) {
            Product byId = productService.findById(product.getProductId(), lang).get();
            price += byId.getPrice() * product.getAmount();
            res.append(byId.getName())
                    .append("\n")
                    .append(product.getAmount())
                    .append(" x ")
                    .append(byId.getPrice())
                    .append(" = ").append(byId.getPrice() * product.getAmount())
                    .append("\n\n");
        }
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.CANADA_FRENCH);
        res.append(i18n.getMsg(MessageKey.ALL) + " " + numberFormat.format(price) + " so'm");
        return res.toString();
    }

    static String ordersMessageForAdmin(List<OrderProduct> products, User user, String orderBody){
        StringBuilder res = new StringBuilder();
        ProductService  productService = ProductService.getInstance();
        String[] address = CoreUtils.splitAddress(orderBody);
        Lang lang = user.getLang();
        res.append("""
                FIO : %s
                Number : %s
                Viloyat/Shahar: %s
                Tuman : %s
                Dom : %s
                Xonadon : %s
                """.formatted(user.getFio(),user.getPhoneNumber(),address[0],address[1],address[2],address[3])+" \n");
        long price = 0;
        for (OrderProduct product : products) {
            Product byId = productService.findById(product.getProductId(), lang).get();
            price += byId.getPrice() * product.getAmount();
            res.append(product.getAmount()).append(" * ").append(byId.getName() + "\n");
        }
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.CANADA_FRENCH);
        res.append("Jami: " + numberFormat.format(price) + " so'm");
        return res.toString();
    }
    static String productCardForUser(String name,String desc,Long price){
        StringBuilder res = new StringBuilder();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.CANADA_FRENCH);
        res.append("""
                %s
                               
                %s
                               
                %s %s cум
                """.formatted(name,desc,i18n.getMsg(MessageKey.ALL),numberFormat.format(price)));
        return res.toString();
    }
}
