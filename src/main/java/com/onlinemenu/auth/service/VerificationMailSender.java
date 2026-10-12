package com.onlinemenu.auth.service;

import com.onlinemenu.auth.domain.EmailCodePurpose;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.util.LanguageContextHolder;
import com.onlinemenu.common.util.MessageResolver;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Sends the email with a code and link (email verification or password reset), in the language of
 * the current request.
 * Without SMTP settings (MAIL_HOST) the code and link are only logged, for local development.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailSender {

  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final MessageResolver messageResolver;

  @Value("${spring.mail.host:}")
  private String mailHost;

  @Value("${spring.mail.username:}")
  private String mailUsername;

  @Value("${app.mail.from:}")
  private String from;

  @Value("${app.auth.email-verification.link-base-url}")
  private String verificationLinkBaseUrl;

  @Value("${app.auth.password-reset.link-base-url}")
  private String passwordResetLinkBaseUrl;

  public void send(EmailCodePurpose purpose, String email, String fullName, String code,
      String token, long ttlMinutes) {
    boolean reset = purpose == EmailCodePurpose.PASSWORD_RESET;
    // System message codes email.verification.* / email.password_reset.*
    String prefix = reset ? "email.password_reset." : "email.verification.";
    String link = UriComponentsBuilder.fromUriString(
            reset ? passwordResetLinkBaseUrl : verificationLinkBaseUrl)
        .queryParam("token", token)
        .build().toUriString();

    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (!StringUtils.hasText(mailHost) || mailSender == null) {
      log.warn("Mail is not configured (MAIL_HOST); {} for {}: code {}, link {}",
          purpose, email, code, link);
      return;
    }

    String language = LanguageContextHolder.get();
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(email);
      helper.setFrom(StringUtils.hasText(from) ? from : mailUsername);
      helper.setSubject(text(prefix + "subject", language));
      helper.setText(plainBody(prefix, fullName, code, link, ttlMinutes, language),
          htmlBody(prefix, fullName, code, link, ttlMinutes, language));
      mailSender.send(message);
    } catch (MailException | MessagingException ex) {
      log.error("Failed to send the {} email to {}", purpose, email, ex);
      throw new BusinessException(ExceptionMessageCodes.EMAIL_SEND_FAILED,
          HttpStatus.SERVICE_UNAVAILABLE);
    }
  }

  private String plainBody(String prefix, String fullName, String code, String link, long ttlMinutes,
      String language) {
    return text(prefix + "greeting", language, fullName) + "\n\n"
        + text(prefix + "intro", language) + "\n\n"
        + code + "\n\n"
        + text(prefix + "or_link", language) + "\n"
        + link + "\n\n"
        + text(prefix + "expires", language, ttlMinutes);
  }

  private String htmlBody(String prefix, String fullName, String code, String link, long ttlMinutes,
      String language) {
    String safeLink = HtmlUtils.htmlEscape(link);
    return """
        <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;color:#2b211c">
          <p style="font-size:16px">%s</p>
          <p style="font-size:15px">%s</p>
          <p style="font-size:32px;font-weight:bold;letter-spacing:8px;margin:24px 0">%s</p>
          <p style="font-size:15px">%s</p>
          <p style="margin:24px 0">
            <a href="%s" style="background:#c2410c;color:#fff;padding:12px 24px;border-radius:10px;text-decoration:none;font-weight:bold">%s</a>
          </p>
          <p style="font-size:13px;color:#8a7a70">%s</p>
        </div>
        """.formatted(
        HtmlUtils.htmlEscape(text(prefix + "greeting", language, fullName)),
        HtmlUtils.htmlEscape(text(prefix + "intro", language)),
        code,
        HtmlUtils.htmlEscape(text(prefix + "or_link", language)),
        safeLink,
        HtmlUtils.htmlEscape(text(prefix + "button", language)),
        HtmlUtils.htmlEscape(text(prefix + "expires", language, ttlMinutes)));
  }

  private String text(String code, String language, Object... args) {
    return messageResolver.resolve(code, language, args);
  }
}
