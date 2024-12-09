package de.bund.digitalservice.ris.caselaw.adapter;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import de.bund.digitalservice.ris.caselaw.domain.HttpMailSender;
import de.bund.digitalservice.ris.caselaw.domain.MailAttachment;
import de.bund.digitalservice.ris.caselaw.domain.MailStoreFactory;
import de.bund.digitalservice.ris.domain.export.juris.response.JurisMailMockBuilder;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JurisStub implements MailStoreFactory, HttpMailSender {

  public static final String UUID_REGEX =
      "([0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12})";
  private final String username;
  private final String password;
  private final GreenMail server;
  private static final Logger LOGGER = LoggerFactory.getLogger(JurisStub.class);

  public JurisStub(String username, String password) {
    this.username = username;
    this.password = password;

    server =
        new GreenMail(
            new ServerSetup[] {
              new ServerSetup(0, null, ServerSetup.PROTOCOL_SMTP),
              new ServerSetup(0, null, ServerSetup.PROTOCOL_IMAP)
            });
    server.setUser(username, username, password);
    server.start();
  }

  public Store createStore() throws MessagingException {
    Session imapSession = server.getImap().createSession();
    Store store = imapSession.getStore("imap");
    store.connect(username, password);

    Folder processed = store.getFolder("processed");
    if (!processed.exists()) processed.create(Folder.HOLDS_MESSAGES);

    Folder unprocessable = store.getFolder("unprocessable");
    if (!unprocessable.exists()) unprocessable.create(Folder.HOLDS_MESSAGES);

    return store;
  }

  public String getUsername() {
    return username;
  }

  public void sendMail(
      String senderAddress,
      String receiverAddress,
      String subject,
      String content,
      List<MailAttachment> mailAttachments,
      String tag) {

    LOGGER.info("Message sent: {}", subject);
    if (isPublication(subject)) {
      String documentNumber = getDocumentNumber(subject);
      if (documentNumber == null) {
        LOGGER.warn("Document number could not be found in subject: {}", subject);
        return;
      }
      addMessage(
          JurisMailMockBuilder.generateImportMessage(
              server.getSmtp().createSession(), documentNumber, hasErrors(mailAttachments)));
      addMessage(
          JurisMailMockBuilder.generateProcessMessage(
              server.getSmtp().createSession(), documentNumber, hasErrors(mailAttachments)));

      LOGGER.info("Publication received and created mocked response");
    } else if (isEdition(subject)) {
      UUID editionId = getEditionId(subject);
      if (editionId == null) {
        LOGGER.warn("EditionId could not be found in subject: {}", subject);
        return;
      }
      // TODO
      addMessage(
          JurisMailMockBuilder.generateImportMessage(
              server.getSmtp().createSession(), "Ausgabe", hasErrors(mailAttachments)));
      addMessage(
          JurisMailMockBuilder.generateProcessMessage(
              server.getSmtp().createSession(), "Ausgabe", hasErrors(mailAttachments)));

      LOGGER.info("Edition received and created mocked response");
    }
  }

  private boolean isPublication(String subject) {
    return subject.contains("id=juris name=NeuRIS da=R df=X dt=N mod=T")
        && !subject.contains("FWD: ");
  }

  private boolean isEdition(String subject) {
    return subject.contains("id=juris name=NeuRIS da=R df=X dt=F mod=T")
        && !subject.contains("FWD: ");
  }

  private String getDocumentNumber(String subject) {
    Matcher matcher = Pattern.compile("vg=([A-Z0-9]{13})").matcher(subject);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  private UUID getEditionId(String subject) {
    Matcher matcher = Pattern.compile("vg=edition-" + UUID_REGEX).matcher(subject);
    if (matcher.find()) {
      return UUID.fromString(matcher.group(1));
    }
    return null;
  }

  private void addMessage(Message message) {
    try {
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(username));
      Transport.send(message);
    } catch (MessagingException e) {
      LOGGER.error("Error adding Message to JurisMock: {}", message);
    }
  }

  private boolean hasErrors(List<MailAttachment> mailAttachments) {
    return mailAttachments.get(0).fileContent().contains("<aktenzeichen>ERROR</aktenzeichen>");
  }
}
