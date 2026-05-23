package com.ragnarok.ragnarok_customers_training_diary.account;

/**
 * Vyhozena při pokusu zaregistrovat účet s emailem, který už v DB existuje.
 * Mapuje se na HTTP 409 Conflict v {@link com.ragnarok.ragnarok_customers_training_diary.account.AccountRestController}.
 */
public class EmailAlreadyTakenException extends RuntimeException {

    public EmailAlreadyTakenException(String email) {
        super("Account with email '" + email + "' already exists");
    }
}
