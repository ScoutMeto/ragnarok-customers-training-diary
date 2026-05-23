package com.ragnarok.ragnarok_customers_training_diary.common;

/**
 * 404 - entita nenalezena.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
