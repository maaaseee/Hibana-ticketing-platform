package com.aieta.ticketing_app_orders.infrastructure.pdf;

import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.aieta.ticketing_app_orders.application.ports.InvoicePdfPort;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * High-definition minimalist PDF invoice generator based on Hibana.tk design template.
 * Combines high-resolution atmospheric corner gradient background with clean typography.
 */
@Component
public class OpenPdfInvoiceGeneratorAdapter implements InvoicePdfPort {

    private static final Logger log = LoggerFactory.getLogger(OpenPdfInvoiceGeneratorAdapter.class);

    // Color Palette matching the modern white minimalist template with Hibana light blue accents
    private static final Color COLOR_TITLE_WHITE = new Color(255, 255, 255);
    private static final Color COLOR_TEXT_MAIN = new Color(17, 24, 39);      // #111827 Dark Slate / Black
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);   // #64748b Slate Muted
    private static final Color COLOR_TEXT_SECONDARY = new Color(71, 85, 105); // #475569
    private static final Color COLOR_RULE_LINE = new Color(17, 24, 39);      // #111827 Clean Dark Rule
    private static final Color COLOR_ACCENT_BLUE = new Color(0, 104, 143);    // #00688f Hibana Deep Blue Accent

    private static final DateTimeFormatter DATE_SPANISH = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES"))
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    private final byte[] backgroundBytes;

    public OpenPdfInvoiceGeneratorAdapter() {
        this.backgroundBytes = loadBackgroundImage();
    }

    private byte[] loadBackgroundImage() {
        try (InputStream is = getClass().getResourceAsStream("/invoice-background.png")) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                log.info("[INVOICE-PDF] Loaded background template from classpath ({} bytes)", bytes.length);
                return bytes;
            }
        } catch (Exception e) {
            log.warn("[INVOICE-PDF] Could not load background image from classpath: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public byte[] generateInvoicePdf(InvoiceData data) {
        log.debug("[INVOICE-PDF] Generating minimalist template PDF invoice for order: {}", data.orderId());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // A4 Page with 48pt horizontal margins and 42pt vertical margins
            Document document = new Document(PageSize.A4, 48, 48, 42, 42);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Register background page event to draw the high-resolution gradient canvas underneath
            if (backgroundBytes != null) {
                writer.setPageEvent(new PdfPageEventHelper() {
                    @Override
                    public void onEndPage(PdfWriter w, Document doc) {
                        try {
                            PdfContentByte cb = w.getDirectContentUnder();
                            Image bg = Image.getInstance(backgroundBytes);
                            bg.setAbsolutePosition(0, 0);
                            bg.scaleAbsolute(PageSize.A4.getWidth(), PageSize.A4.getHeight());
                            cb.addImage(bg);
                        } catch (Exception ex) {
                            log.warn("[INVOICE-PDF] Error stamping background image: {}", ex.getMessage());
                        }
                    }
                });
            }

            document.open();

            // 1. Giant White Title: FACTURA on the Top Aurora Gradient
            addTopTitle(document);

            // 2. Client & Issuer Two-Column Block
            addCustomerAndIssuerBlock(document, data);

            // 3. Clean Minimalist Items Table
            addItemsTable(document, data);

            // 4. Bottom Legal / AFIP QR & Totals Summary Block
            addBottomSummaryAndQr(document, data);

            document.close();
            byte[] pdfBytes = out.toByteArray();
            log.info("[INVOICE-PDF] Successfully generated template PDF invoice {} ({} bytes)", data.invoiceNumber(), pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            log.error("[INVOICE-PDF] Failed to generate invoice PDF for order {}: {}", data.orderId(), e.getMessage(), e);
            throw new RuntimeException("Error generating template invoice PDF", e);
        }
    }

    private void addTopTitle(Document document) throws DocumentException {
        // Space above the title to align perfectly with the upper mesh gradient
        Paragraph spacer = new Paragraph();
        spacer.setSpacingBefore(12f);
        document.add(spacer);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36f, COLOR_TITLE_WHITE);
        Paragraph titleP = new Paragraph("FACTURA", titleFont);
        titleP.setSpacingAfter(48f);
        document.add(titleP);
    }

    private void addCustomerAndIssuerBlock(Document document, InvoiceData data) throws DocumentException {
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{55f, 45f});
        metaTable.setSpacingAfter(36f);

        // --- LEFT COLUMN: Buyer Info & Invoice Number ---
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, COLOR_TEXT_MAIN);
        Font invoiceNoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, COLOR_TEXT_MAIN);
        Font subDetailFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, COLOR_TEXT_SECONDARY);
        Font tinyDetailFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_TEXT_MUTED);

        Paragraph nameP = new Paragraph(data.buyerName().toUpperCase(), nameFont);
        nameP.setSpacingAfter(6f);
        leftCell.addElement(nameP);

        Paragraph invP = new Paragraph("FACTURA Nº " + cleanInvoiceNumber(data.invoiceNumber()), invoiceNoFont);
        invP.setSpacingAfter(8f);
        leftCell.addElement(invP);

        if (data.eventTitle() != null && !data.eventTitle().isBlank()) {
            Paragraph eventP = new Paragraph(data.eventTitle(), subDetailFont);
            leftCell.addElement(eventP);
        }

        if (data.venueName() != null) {
            String venueStr = data.venueName() + (data.eventDate() != null ? " • " + data.eventDate() : "");
            leftCell.addElement(new Paragraph(venueStr, tinyDetailFont));
        }

        metaTable.addCell(leftCell);

        // --- RIGHT COLUMN: Issue Date & Issuer Company Details ---
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(0);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font labelDateFont = FontFactory.getFont(FontFactory.HELVETICA, 9f, COLOR_TEXT_MUTED);
        Font valDateFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_TEXT_MAIN);
        Font issuerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, COLOR_TEXT_SECONDARY);
        Font legalFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_TEXT_MUTED);
        Font emailLinkFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_ACCENT_BLUE);

        Paragraph dateLabelP = new Paragraph("Fecha de expedición:", labelDateFont);
        dateLabelP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(dateLabelP);

        Paragraph dateValP = new Paragraph(DATE_SPANISH.format(data.issuedAt()), valDateFont);
        dateValP.setAlignment(Element.ALIGN_RIGHT);
        dateValP.setSpacingAfter(10f);
        rightCell.addElement(dateValP);

        Paragraph issuerP = new Paragraph("Hibana.tk Entertainment S.A.", issuerFont);
        issuerP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(issuerP);

        Paragraph addressP = new Paragraph("Av. Figueroa Alcorta 7597, CABA, CP: 1428", legalFont);
        addressP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(addressP);

        Paragraph cuitP = new Paragraph("CUIT: 30-71829104-9 • IVA Resp. Inscripto", legalFont);
        cuitP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(cuitP);

        Paragraph contactP = new Paragraph("hola@hibana.tk • www.hibana.tk", emailLinkFont);
        contactP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(contactP);

        metaTable.addCell(rightCell);
        document.add(metaTable);
    }

    private void addItemsTable(Document document, InvoiceData data) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{55f, 13f, 16f, 16f});
        table.setSpacingAfter(32f);

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_TEXT_MAIN);
        Font itemTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, COLOR_TEXT_MAIN);
        Font itemSubFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_TEXT_MUTED);
        Font qtyFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, COLOR_TEXT_MAIN);
        Font priceFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, COLOR_TEXT_MAIN);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_TEXT_MAIN);

        // Header Row with top border line
        addHeaderCell(table, "DESCRIPCIÓN", headFont, Element.ALIGN_LEFT);
        addHeaderCell(table, "CANT.", headFont, Element.ALIGN_CENTER);
        addHeaderCell(table, "PRECIO", headFont, Element.ALIGN_RIGHT);
        addHeaderCell(table, "TOTAL", headFont, Element.ALIGN_RIGHT);

        // Items Rows
        boolean isFirst = true;
        for (InvoiceData.InvoiceItem item : data.items()) {
            PdfPCell descCell = new PdfPCell();
            descCell.setBorder(Rectangle.NO_BORDER);
            descCell.setPaddingTop(isFirst ? 14f : 8f);
            descCell.setPaddingBottom(8f);

            Paragraph p = new Paragraph(item.description(), itemTitleFont);
            if (item.sectorAndSeat() != null && !item.sectorAndSeat().isBlank()) {
                p.add(new Chunk("\n" + item.sectorAndSeat(), itemSubFont));
            }
            descCell.addElement(p);
            table.addCell(descCell);

            addContentCell(table, String.valueOf(item.quantity()), qtyFont, Element.ALIGN_CENTER, isFirst);
            addContentCell(table, formatCurrency(item.unitPrice(), data.currency()), priceFont, Element.ALIGN_RIGHT, isFirst);
            addContentCell(table, formatCurrency(item.totalPrice(), data.currency()), totalFont, Element.ALIGN_RIGHT, isFirst);
            isFirst = false;
        }

        // Service Fee row if applicable
        if (data.serviceFee() != null && data.serviceFee().compareTo(BigDecimal.ZERO) > 0) {
            PdfPCell feeDescCell = new PdfPCell();
            feeDescCell.setBorder(Rectangle.NO_BORDER);
            feeDescCell.setPaddingTop(8f);
            feeDescCell.setPaddingBottom(8f);
            feeDescCell.addElement(new Paragraph("Cargo por Servicio Oficial (Plataforma y Emisión)", itemTitleFont));
            table.addCell(feeDescCell);

            addContentCell(table, "1", qtyFont, Element.ALIGN_CENTER, false);
            addContentCell(table, formatCurrency(data.serviceFee(), data.currency()), priceFont, Element.ALIGN_RIGHT, false);
            addContentCell(table, formatCurrency(data.serviceFee(), data.currency()), totalFont, Element.ALIGN_RIGHT, false);
        }

        // Bottom border rule across the entire table
        PdfPCell borderRule = new PdfPCell();
        borderRule.setColspan(4);
        borderRule.setBorder(Rectangle.BOTTOM);
        borderRule.setBorderWidthBottom(1.2f);
        borderRule.setBorderColorBottom(COLOR_RULE_LINE);
        borderRule.setPadding(0);
        borderRule.setMinimumHeight(10f);
        table.addCell(borderRule);

        document.add(table);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setBorderWidthTop(1.2f);
        cell.setBorderWidthBottom(1.2f);
        cell.setBorderColorTop(COLOR_RULE_LINE);
        cell.setBorderColorBottom(COLOR_RULE_LINE);
        cell.setPaddingTop(8f);
        cell.setPaddingBottom(8f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addContentCell(PdfPTable table, String text, Font font, int alignment, boolean isFirst) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(isFirst ? 14f : 8f);
        cell.setPaddingBottom(8f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addBottomSummaryAndQr(Document document, InvoiceData data) throws DocumentException {
        PdfPTable bottomTable = new PdfPTable(2);
        bottomTable.setWidthPercentage(100);
        bottomTable.setWidths(new float[]{55f, 45f});

        // --- LEFT COLUMN: AFIP QR Code & Legal Data ---
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        Font qrTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, COLOR_TEXT_MAIN);
        Font qrMetaFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT_MUTED);

        try {
            String qrPayload = "https://hibana.tk/verify/" + data.orderId() + "?invoice=" + data.invoiceNumber() + "&cuit=30718291049";
            Image qrImage = generateQrImage(qrPayload, 65, 65);

            PdfPTable qrLayout = new PdfPTable(2);
            qrLayout.setWidthPercentage(100);
            qrLayout.setWidths(new float[]{25f, 75f});

            PdfPCell imgCell = new PdfPCell(qrImage);
            imgCell.setBorder(Rectangle.NO_BORDER);
            imgCell.setPadding(0);
            qrLayout.addCell(imgCell);

            PdfPCell metaCell = new PdfPCell();
            metaCell.setBorder(Rectangle.NO_BORDER);
            metaCell.setPaddingLeft(6f);
            metaCell.addElement(new Paragraph("Comprobante Fiscal Electrónico AFIP", qrTitleFont));
            metaCell.addElement(new Paragraph("CAE Nº: " + data.caeNumber(), qrMetaFont));
            metaCell.addElement(new Paragraph("Vto. CAE: " + DATE_SPANISH.format(data.caeExpiresAt()), qrMetaFont));
            metaCell.addElement(new Paragraph("Id. Orden: #" + data.orderId(), qrMetaFont));
            qrLayout.addCell(metaCell);

            leftCell.addElement(qrLayout);
        } catch (Exception e) {
            leftCell.addElement(new Paragraph("CAE Nº: " + data.caeNumber(), qrTitleFont));
            leftCell.addElement(new Paragraph("Vto. CAE: " + DATE_SPANISH.format(data.caeExpiresAt()), qrMetaFont));
        }

        bottomTable.addCell(leftCell);

        // --- RIGHT COLUMN: Totals Summary ---
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(0);

        Font totLabelFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, COLOR_TEXT_SECONDARY);
        Font totValFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_TEXT_MAIN);
        Font grandLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, COLOR_TEXT_MAIN);
        Font grandValFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12.5f, COLOR_TEXT_MAIN);

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(100);
        totalsTable.setWidths(new float[]{55f, 45f});

        addSummaryLine(totalsTable, "IMPORTE TOTAL:", formatCurrency(data.subtotal(), data.currency()), totLabelFont, totValFont);
        addSummaryLine(totalsTable, "IVA (21%):", formatCurrency(data.vatTax(), data.currency()), totLabelFont, totValFont);
        addSummaryLine(totalsTable, "IMPORTE A PAGAR:", formatCurrency(data.grandTotal(), data.currency()), grandLabelFont, grandValFont);

        rightCell.addElement(totalsTable);
        bottomTable.addCell(rightCell);

        document.add(bottomTable);
    }

    private void addSummaryLine(PdfPTable table, String label, String value, Font lFont, Font vFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, lFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPaddingTop(4f);
        lCell.setPaddingBottom(4f);
        lCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value, vFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setPaddingTop(4f);
        vCell.setPaddingBottom(4f);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(vCell);
    }

    private String cleanInvoiceNumber(String raw) {
        if (raw == null || raw.isBlank()) return "001";
        return raw;
    }

    private String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null) return "0,00";
        String formatted = String.format(Locale.GERMAN, "%,.2f", amount);
        if ("EUR".equalsIgnoreCase(currency)) {
            return formatted + " €";
        }
        if ("USD".equalsIgnoreCase(currency)) {
            return "USD " + formatted;
        }
        return "$ " + formatted + " ARS";
    }

    private Image generateQrImage(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        return Image.getInstance(baos.toByteArray());
    }
}
