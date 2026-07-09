package com.digniche.muntum.global.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 서비스
 */
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[MUNTUM] 비밀번호 재설정 인증번호");
        message.setText("인증번호는 [" + code + "] 입니다.\n인증번호는 5분간 유효합니다.");
        mailSender.send(message);
    }
}
