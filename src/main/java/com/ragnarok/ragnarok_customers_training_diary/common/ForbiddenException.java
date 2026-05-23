package com.ragnarok.ragnarok_customers_training_diary.common;

/**
 * 403 - autentikovaný uživatel nemá oprávnění k danému prostředku
 * (např. cizí trénink).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
