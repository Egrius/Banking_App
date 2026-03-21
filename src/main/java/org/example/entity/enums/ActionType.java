package org.example.entity.enums;

public enum ActionType {
    // Аутентификация и пользователи
    LOGIN,
    LOGOUT,
    REGISTER,
    PASSWORD_CHANGE,
    PROFILE_UPDATE,

    // Счета
    ACCOUNT_CREATE,
    ACCOUNT_CLOSE,
    ACCOUNT_BLOCK,
    ACCOUNT_UNBLOCK,
    BALANCE_CHECK,

    // Транзакции
    TRANSFER_INITIATE,
    TRANSFER_COMPLETE,
    TRANSFER_FAIL,
    TRANSFER_REVERSE,

    // Шаблоны
    TEMPLATE_CREATE,
    TEMPLATE_UPDATE,
    TEMPLATE_DELETE,
    TEMPLATE_EXECUTE,

    // Карты
    CARD_CREATE,
    CARD_ACTIVATE,
    CARD_BLOCK,
    CARD_DELETE,

    // Админка
    ROLE_ASSIGN,
    ROLE_REMOVE,
    USER_LOCK,
    USER_UNLOCK,

    // Ошибки и системные
    ERROR,
    SYSTEM_START,
    SYSTEM_STOP
}
