package com.aieta.ticketing_app_orders.infrastructure.mail;

import com.aieta.ticketing_app_orders.application.dto.OrderConfirmationEmail;
import com.aieta.ticketing_app_orders.application.ports.EmailNotificationPort;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class SpringMailNotificationAdapter implements EmailNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(SpringMailNotificationAdapter.class);

    private final JavaMailSender mailSender;

    @Value("${hibana.mailing.from-email:no-reply@hibana.tk}")
    private String fromEmail;

    @Value("${hibana.mailing.from-name:Hibana.tk Entradas}")
    private String fromName;

    public SpringMailNotificationAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void sendOrderConfirmation(OrderConfirmationEmail emailData) {
        log.info("[EMAIL-DISPATCHER] Preparing order confirmation email for order {} to {}", emailData.orderId(), emailData.recipientEmail());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail, fromName);
            helper.setTo(emailData.recipientEmail());
            helper.setSubject("¡Compra Confirmada! Tus entradas y factura para " + emailData.eventTitle() + " (#" + emailData.orderId() + ")");

            String htmlBody = buildHtmlEmailTemplate(emailData);
            helper.setText(htmlBody, true);

            // Attach PDF invoice if present
            if (emailData.pdfInvoiceBytes() != null && emailData.pdfInvoiceBytes().length > 0) {
                String filename = "Factura_Hibana_" + emailData.orderId() + ".pdf";
                helper.addAttachment(filename, new ByteArrayResource(emailData.pdfInvoiceBytes()), "application/pdf");
                log.debug("[EMAIL-DISPATCHER] Attached PDF invoice: {} ({} bytes)", filename, emailData.pdfInvoiceBytes().length);
            }

            mailSender.send(message);
            log.info("[EMAIL-DISPATCHER] Successfully dispatched confirmation email for order {} to {}", emailData.orderId(), emailData.recipientEmail());

        } catch (Exception e) {
            log.warn("[EMAIL-DISPATCHER] SMTP delivery skipped or failed for order {}: {}. (Operating in local/resilient fallback mode)",
                    emailData.orderId(), e.getMessage());
        }
    }

    public String buildHtmlEmailTemplate(OrderConfirmationEmail data) {
        String seatsList = (data.seats() != null && !data.seats().isEmpty())
                ? String.join(", ", data.seats())
                : "General VIP";

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Confirmación de Compra Hibana.tk</title>
              <style>
                body { margin: 0; padding: 0; background-color: #0c141f; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f1f5f9; }
                .wrapper { width: 100%%; max-width: 600px; margin: 0 auto; padding: 32px 16px; box-sizing: border-box; }
                .card { background-color: #141c27; border: 1px solid #23354d; border-radius: 20px; padding: 32px 24px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                .header-logo { font-size: 24px; font-weight: 900; letter-spacing: -0.02em; color: #86cffb; margin-bottom: 4px; text-decoration: none; }
                .header-sub { font-size: 11px; text-transform: uppercase; letter-spacing: 0.15em; color: #4fdbc8; font-weight: bold; margin-bottom: 24px; }
                .badge { display: inline-block; padding: 4px 12px; border-radius: 9999px; background-color: rgba(79, 219, 200, 0.15); border: 1px solid rgba(79, 219, 200, 0.4); color: #4fdbc8; font-size: 12px; font-weight: bold; margin-bottom: 16px; }
                .title { font-size: 22px; font-weight: bold; color: #ffffff; margin: 0 0 8px 0; }
                .intro { font-size: 14px; color: #94a3b8; line-height: 1.5; margin: 0 0 24px 0; }
                .details-box { background-color: #0c141f; border: 1px solid #23354d; border-radius: 14px; padding: 18px; margin-bottom: 24px; }
                .detail-row { display: flex; justify-content: space-between; font-size: 13px; padding: 6px 0; border-bottom: 1px solid #1c2b3d; }
                .detail-row:last-child { border-bottom: none; }
                .label { color: #94a3b8; }
                .value { color: #f1f5f9; font-weight: 600; }
                .value-highlight { color: #86cffb; font-weight: bold; }
                .btn { display: block; text-align: center; background-color: #00688f; color: #ffffff !important; padding: 14px 24px; border-radius: 12px; font-weight: bold; font-size: 14px; text-decoration: none; box-shadow: 0 4px 14px rgba(0, 104, 143, 0.4); margin: 24px 0 16px 0; }
                .notice { font-size: 12px; color: #94a3b8; line-height: 1.4; border-left: 3px solid #86cffb; padding-left: 12px; margin: 20px 0; }
                .footer { text-align: center; font-size: 11px; color: #64748b; margin-top: 24px; line-height: 1.5; }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="card">
                  <div style="text-align: center;">
                    <div class="header-logo">HIBANA.TK</div>
                    <div class="header-sub">PLATAFORMA OFICIAL DE ENTRADAS</div>
                    <div class="badge">✔ PAGO APROBADO & ENTRADAS ASIGNADAS</div>
                    <h1 class="title">¡Gracias por tu compra, %s!</h1>
                    <p class="intro">Tu orden <strong>#%s</strong> ha sido confirmada con éxito. Ya puedes acceder a tus entradas digitales con código QR dinámico desde tu cuenta.</p>
                  </div>

                  <div class="details-box">
                    <table style="width: 100%%; border-collapse: collapse; font-size: 13px;">
                      <tr>
                        <td style="padding: 6px 0; color: #94a3b8;">Espectáculo:</td>
                        <td style="padding: 6px 0; color: #f1f5f9; font-weight: bold; text-align: right;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 6px 0; color: #94a3b8;">Fecha y Hora:</td>
                        <td style="padding: 6px 0; color: #f1f5f9; text-align: right;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 6px 0; color: #94a3b8;">Recinto:</td>
                        <td style="padding: 6px 0; color: #f1f5f9; text-align: right;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 6px 0; color: #94a3b8;">Ubicaciones:</td>
                        <td style="padding: 6px 0; color: #4fdbc8; font-weight: bold; text-align: right;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px 0; color: #94a3b8; border-top: 1px solid #23354d; font-size: 14px;">Total Abonado:</td>
                        <td style="padding: 8px 0; color: #86cffb; font-weight: bold; text-align: right; border-top: 1px solid #23354d; font-size: 16px;">$%s ARS</td>
                      </tr>
                    </table>
                  </div>

                  <div class="notice">
                    <strong>Factura Oficial Adjunta:</strong> Hemos adjuntado en este correo tu factura electrónica oficial (comprobante PDF) con validez fiscal y detalle contable de tus entradas.
                  </div>

                  <a href="https://www.hibana.tk/tickets" class="btn">
                    Acceder a Mis Entradas Digitales
                  </a>

                  <div class="footer">
                    <p>Hibana.tk Entertainment S.A. • CUIT: 30-71829104-9 • Av. Figueroa Alcorta 7597, Núñez, CABA</p>
                    <p>Canal oficial de soporte: <a href="https://www.hibana.tk/help" style="color: #86cffb;">Centro de Ayuda</a> o escríbenos a <a href="mailto:soporte@hibana.tk" style="color: #86cffb;">soporte@hibana.tk</a></p>
                  </div>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                data.recipientName() != null ? data.recipientName() : "Espectador",
                data.orderId(),
                data.eventTitle(),
                data.eventDate(),
                data.venueName(),
                seatsList,
                data.totalAmount().setScale(2)
        );
    }
}
