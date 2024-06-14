package uz.pdp.utils;

import uz.pdp.model.enams.Lang;

public interface CoreUtils {

    static boolean checkPhoneNumber(String phone) {
        return phone.matches(Patterns.PHONE) || phone.matches(Patterns.PHONE2);
    }
    static boolean checkNameFormat(String name, Lang lang) {
        if (lang == Lang.RU) return name.matches(Patterns.NAME_FOR_RUSSIANS);
        else return name.matches(Patterns.NAME_FOR_ENG_OR_UZ);
    }
}