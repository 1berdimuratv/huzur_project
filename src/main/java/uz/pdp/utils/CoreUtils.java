package uz.pdp.utils;

import org.xml.sax.helpers.ParserAdapter;
import uz.pdp.model.Address;
import uz.pdp.model.Order;
import uz.pdp.model.enams.Lang;

public interface CoreUtils {

    static boolean checkPhoneNumber(String phone) {
        return phone.matches(Patterns.PHONE) || phone.matches(Patterns.PHONE2);
    }
    static boolean checkNameFormat(String name, Lang lang) {
        if (lang == Lang.RU) return name.matches(Patterns.NAME_FOR_RUSSIANS);
        else return name.matches(Patterns.NAME_FOR_ENG_OR_UZ);
    }
    static String[] splitAddress(String body) {
        body = body.substring(body.indexOf("$")+1);
        String[] address = new String[4];
        for (int i = 0; i < 3; i++) {
            address[i] = body.substring(0,body.indexOf("$"));
            body = body.substring(body.indexOf("$")+1);
        }
        address[3] = body;
        return address;
    }

    static Integer analyzeByOrder(Order order) {
        Address address = order.getAddress();
        if (!address.getViloyat())
            return 1;
        else if (!address.getTuman())
            return 2;
        else if (!address.getDom())
            return 3;
        else if (!address.getXonadon())
            return 4;
        else
            return 5;
    }
}
