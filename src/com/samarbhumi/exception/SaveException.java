package com.samarbhumi.exception;

public class SaveException extends GameException {
    public SaveException(String msg) { super(msg); }
    public SaveException(String msg, Throwable cause) { super(msg, cause); }
}
