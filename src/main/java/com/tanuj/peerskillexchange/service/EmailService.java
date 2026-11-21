package com.tanuj.peerskillexchange.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Nullable
    private final JavaMailSender mailSender;

    private final String fromEmail;

    public EmailService(
            @Nullable JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@peerskillexchange.com}") String fromEmail
    ) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void sendVerificationCode(
            String toEmail,
            String subject,
            String code,
            int expiryMinutes
    ) {

        if (mailSender == null) {
            throw new IllegalStateException("Email service is unavailable.");
        }

        try {

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Peer Skill Exchange");
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <body style="
                        margin:0;
                        padding:0;
                        background-color:#f4f4f4;
                        font-family:Arial,sans-serif;
                    ">

                    <table width="100%%" cellpadding="0" cellspacing="0">
                        <tr>
                            <td align="center" style="padding:40px 0;">

                                <table width="600" cellpadding="0" cellspacing="0"
                                       style="
                                           background:#ffffff;
                                           border-radius:12px;
                                           padding:40px;
                                           box-shadow:0 2px 10px rgba(0,0,0,0.08);
                                       ">

                                    <tr>
                                        <td align="center">

                                            <h1 style="
                                                color:#2563eb;
                                                margin-bottom:10px;
                                            ">
                                                Peer Skill Exchange
                                            </h1>

                                            <p style="
                                                color:#666;
                                                font-size:16px;
                                            ">
                                                Verify your email address
                                            </p>

                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="padding-top:30px;">

                                            <p style="font-size:16px;color:#333;">
                                                Hello,
                                            </p>

                                            <p style="
                                                font-size:16px;
                                                color:#333;
                                                line-height:1.6;
                                            ">
                                                Use the verification code below
                                                to continue:
                                            </p>

                                            <div style="
                                                margin:30px 0;
                                                text-align:center;
                                            ">
                                                <span style="
                                                    display:inline-block;
                                                    background:#2563eb;
                                                    color:white;
                                                    padding:18px 32px;
                                                    font-size:32px;
                                                    font-weight:bold;
                                                    border-radius:10px;
                                                    letter-spacing:6px;
                                                ">
                                                    %s
                                                </span>
                                            </div>

                                            <p style="
                                                font-size:15px;
                                                color:#555;
                                            ">
                                                This code expires in
                                                <strong>%d minutes</strong>.
                                            </p>

                                            <p style="
                                                font-size:15px;
                                                color:#555;
                                                margin-top:25px;
                                            ">
                                                If you did not request this email,
                                                you can safely ignore it.
                                            </p>

                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="
                                            padding-top:40px;
                                            border-top:1px solid #e5e5e5;
                                            text-align:center;
                                        ">

                                            <p style="
                                                color:#999;
                                                font-size:12px;
                                            ">
                                                © 2026 Peer Skill Exchange.
                                                All rights reserved.
                                            </p>

                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>
                    </table>

                    </body>
                    </html>
                    """.formatted(code, expiryMinutes);

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Verification email sent to {}", toEmail);

        } catch (Exception e) {

            log.error("Failed to send verification email to {}", toEmail, e);

            throw new RuntimeException("Failed to send email", e);
        }
    }
}
