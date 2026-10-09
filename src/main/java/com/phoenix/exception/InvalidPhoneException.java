package com.phoenix.exception;

public class InvalidPhoneException extends RuntimeException {
    public InvalidPhoneException() {
        super("Enter a valid 10-digit Indian mobile number");
    }
}
