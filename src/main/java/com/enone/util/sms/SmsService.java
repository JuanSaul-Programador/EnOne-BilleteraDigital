package com.enone.util.sms;

public interface SmsService {
    

    void send(String toE164, String body);

    boolean isServiceAvailable();
}