package com.ragnarok.ragnarok_customers_training_diary.reservation;

/** Selhání volání rezervačního systému (HTTP error, timeout, validation, ...). */
public class ReservationException extends RuntimeException {
    public ReservationException(String message) {
        super(message);
    }
}
