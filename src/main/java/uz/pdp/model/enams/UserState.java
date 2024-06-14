package uz.pdp.model.enams;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserState {
    USER_STARTED(1),
    CHOOSE_LANGUAGE(2),
    USERS_FIO(3),
    SHARE_PHONE_NUMBER(4),
    MAIN_MENU(5),
    CHANG_NUM(6),
    CHANG_NAM(7),
    MENU(8),
    ORDERS(9),
    FEEDBACK(10),
    SETTINGS(11),
    ORDERING(12),
    CHOOSING_AMOUNT(13),
    SHARE_LOCATION(14);

    private final int step;

    public static UserState getByStep(int step) {
        return values()[step-1];
    }
}
