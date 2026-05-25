package com.ragnarok.ragnarok_customers_training_diary.account;

/** Confirmation flow failed — neplatný / vypršelý / nesedící kód. */
public class EmailConfirmationException extends RuntimeException {
    public EmailConfirmationException(String message) {
        super(message);
    }
}
