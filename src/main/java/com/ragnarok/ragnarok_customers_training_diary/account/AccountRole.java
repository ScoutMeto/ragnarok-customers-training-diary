package com.ragnarok.ragnarok_customers_training_diary.account;

/**
 * Role accountu. Mapuje se 1:1 na Spring Security autoritu ROLE_* (viz {@link AccountEntity#getAuthorities()}).
 */
public enum AccountRole {
    USER,
    ADMIN
}
