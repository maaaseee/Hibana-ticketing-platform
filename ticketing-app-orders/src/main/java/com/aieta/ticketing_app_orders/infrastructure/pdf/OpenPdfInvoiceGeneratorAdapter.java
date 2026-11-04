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
 * High-definition minimalist PDF invoice generator matching the official Hibana.tk design system.
 * Uses custom Space Grotesk and Hanken Grotesk typography, atmospheric corner gradients,
 * app logo integration, full-height proportional distribution, and native Argentine Peso formatting.
 */
@Component
public class OpenPdfInvoiceGeneratorAdapter implements InvoicePdfPort {

    private static final Logger log = LoggerFactory.getLogger(OpenPdfInvoiceGeneratorAdapter.class);

    // Color Palette
    private static final Color COLOR_TITLE_WHITE = new Color(255, 255, 255);
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);       // #0f172a Slate-900
    private static final Color COLOR_TEXT_SECONDARY = new Color(51, 65, 85);  // #334155 Slate-700
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);   // #64748b Slate-500
    private static final Color COLOR_TEXT_LIGHT = new Color(148, 163, 184);   // #94a3b8
    private static final Color COLOR_RULE_DARK = new Color(15, 23, 42);       // #0f172a Solid line
    private static final Color COLOR_RULE_LIGHT = new Color(226, 232, 240);   // #e2e8f0 Divider
    private static final Color COLOR_ACCENT_BLUE = new Color(0, 104, 143);    // #00688f Hibana Deep Blue

    private static final DateTimeFormatter DATE_SPANISH = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES"))
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    private final byte[] backgroundBytes;
    private final byte[] logoBytes;
    private BaseFont baseSpaceGrotesk;
    private BaseFont baseHankenGrotesk;

    public OpenPdfInvoiceGeneratorAdapter() {
        this.backgroundBytes = loadResourceBytes("/invoice-background.png");
        this.logoBytes = loadResourceBytes("/logo.png");
        initFonts();
    }

    private byte[] loadResourceBytes(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                log.info("[INVOICE-PDF] Loaded asset {} ({} bytes)", path, bytes.length);
                return bytes;
            }
        } catch (Exception e) {
            log.warn("[INVOICE-PDF] Could not load resource {}: {}", path, e.getMessage());
        }
        return null;
    }

    private void initFonts() {
        try (InputStream is = getClass().getResourceAsStream("/fonts/SpaceGrotesk.ttf")) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                baseSpaceGrotesk = BaseFont.createFont("SpaceGrotesk.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
                log.info("[INVOICE-PDF] Loaded SpaceGrotesk font successfully");
            }
        } catch (Exception e) {
            log.warn("[INVOICE-PDF] Could not load SpaceGrotesk font, falling back: {}", e.getMessage());
        }

        try (InputStream is = getClass().getResourceAsStream("/fonts/HankenGrotesk.ttf")) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                baseHankenGrotesk = BaseFont.createFont("HankenGrotesk.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
                log.info("[INVOICE-PDF] Loaded HankenGrotesk font successfully");
            }
        } catch (Exception e) {
            log.warn("[INVOICE-PDF] Could not load HankenGrotesk font, falling back: {}", e.getMessage());
        }
    }

    private Font getDisplayFont(float size, int style, Color color) {
        if (baseSpaceGrotesk != null) {
            return new Font(baseSpaceGrotesk, size, style, color);
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private Font getBodyFont(float size, int style, Color color) {
        if (baseHankenGrotesk != null) {
            return new Font(baseHankenGrotesk, size, style, color);
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    @Override
    public byte[] generateInvoicePdf(InvoiceData data) {
        log.debug("[INVOICE-PDF] Generating full-height invoice for order: {}", data.orderId());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // A4 Document (595 x 842 pt) with 48pt horizontal margins and 36pt vertical margins
            Document document = new Document(PageSize.A4, 48, 48, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Background canvas stamped on every page
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

            // 1. Top Header: App Logo + Title "FACTURA" + Official Platform Badge
            addTopHeaderWithLogo(document);

            // 2. Buyer and Issuer Two-Column Metadata Block
            addMetadataBlock(document, data);

            // 3. Line Items Table (proportional padding and borderless rows)
            addItemsTable(document, data);

            // 4. Summary Totals & Legal / AFIP QR Block
            addSummaryAndLegalBlock(document, data);

            // 5. Full-Height Institutional Bottom Footer
            addInstitutionalFooter(document, data);

            document.close();
            byte[] pdfBytes = out.toByteArray();
            log.info("[INVOICE-PDF] Generated {} bytes PDF invoice for {}", pdfBytes.length, data.invoiceNumber());
            return pdfBytes;
        } catch (Exception e) {
            log.error("[INVOICE-PDF] Error generating PDF invoice for {}: {}", data.orderId(), e.getMessage(), e);
            throw new RuntimeException("Error generating template invoice PDF", e);
        }
    }

    private void addTopHeaderWithLogo(Document document) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{68f, 32f});
        headerTable.setSpacingBefore(12f);
        headerTable.setSpacingAfter(38f);

        // Left Cell: Circular Logo + FACTURA title
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        PdfPTable logoAndTitle = new PdfPTable(2);
        logoAndTitle.setWidthPercentage(100);
        logoAndTitle.setWidths(new float[]{22f, 78f});

        if (logoBytes != null) {
            try {
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(50f, 50f);
                PdfPCell logoCell = new PdfPCell(logo);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setPadding(0);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                logoAndTitle.addCell(logoCell);
            } catch (Exception e) {
                PdfPCell emptyLogo = new PdfPCell();
                emptyLogo.setBorder(Rectangle.NO_BORDER);
                logoAndTitle.addCell(emptyLogo);
            }
        } else {
            PdfPCell emptyLogo = new PdfPCell();
            emptyLogo.setBorder(Rectangle.NO_BORDER);
            logoAndTitle.addCell(emptyLogo);
        }

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingLeft(8f);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Font titleFont = getDisplayFont(36f, Font.BOLD, COLOR_TITLE_WHITE);
        Paragraph titleP = new Paragraph("FACTURA", titleFont);
        titleP.setLeading(36f);
        titleCell.addElement(titleP);
        logoAndTitle.addCell(titleCell);

        leftCell.addElement(logoAndTitle);
        headerTable.addCell(leftCell);

        // Right Cell: Subtle brand badge aligned right
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(0);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Font brandBadgeFont = getDisplayFont(9.5f, Font.BOLD, new Color(224, 242, 254)); // #e0f2fe
        Font brandSubFont = getBodyFont(8f, Font.NORMAL, new Color(186, 230, 253));      // #bae6fd

        Paragraph badgeP = new Paragraph("HIBANA.TK", brandBadgeFont);
        badgeP.setAlignment(Element.ALIGN_RIGHT);
        badgeP.setSpacingAfter(2f);
        rightCell.addElement(badgeP);

        Paragraph subP = new Paragraph("Plataforma Oficial de Entradas", brandSubFont);
        subP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(subP);

        headerTable.addCell(rightCell);
        document.add(headerTable);
    }

    private void addMetadataBlock(Document document, InvoiceData data) throws DocumentException {
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{55f, 45f});
        metaTable.setSpacingAfter(36f);

        // Left Column: Buyer Name, Invoice Number, Show Information
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        Font nameFont = getDisplayFont(13.5f, Font.BOLD, COLOR_TEXT_MAIN);
        Font invoiceNoFont = getDisplayFont(11f, Font.BOLD, COLOR_TEXT_MAIN);
        Font eventFont = getBodyFont(9.5f, Font.BOLD, COLOR_TEXT_SECONDARY);
        Font venueFont = getBodyFont(8.5f, Font.NORMAL, COLOR_TEXT_MUTED);

        Paragraph nameP = new Paragraph(data.buyerName().toUpperCase(), nameFont);
        nameP.setSpacingAfter(2f);
        leftCell.addElement(nameP);

        if (data.buyerTaxId() != null && !data.buyerTaxId().isBlank()) {
            Paragraph taxP = new Paragraph(data.buyerTaxId() + (data.buyerEmail() != null ? " • " + data.buyerEmail() : ""), venueFont);
            taxP.setSpacingAfter(4f);
            leftCell.addElement(taxP);
        }

        Paragraph invP = new Paragraph("FACTURA Nº " + (data.invoiceNumber() != null ? data.invoiceNumber() : "001"), invoiceNoFont);
        invP.setSpacingAfter(8f);
        leftCell.addElement(invP);

        if (data.eventTitle() != null && !data.eventTitle().isBlank()) {
            Paragraph eventP = new Paragraph(data.eventTitle(), eventFont);
            eventP.setSpacingAfter(2f);
            leftCell.addElement(eventP);
        }

        if (data.venueName() != null) {
            String venueStr = data.venueName() + (data.eventDate() != null ? " • " + data.eventDate() : "");
            leftCell.addElement(new Paragraph(venueStr, venueFont));
        }

        metaTable.addCell(leftCell);

        // Right Column: Date of issue & Issuer Details
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(0);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font labelDateFont = getBodyFont(9f, Font.NORMAL, COLOR_TEXT_MUTED);
        Font valDateFont = getBodyFont(9.5f, Font.BOLD, COLOR_TEXT_MAIN);
        Font issuerFont = getDisplayFont(9f, Font.BOLD, COLOR_TEXT_SECONDARY);
        Font legalFont = getBodyFont(8.5f, Font.NORMAL, COLOR_TEXT_MUTED);
        Font emailLinkFont = getBodyFont(8.5f, Font.NORMAL, COLOR_ACCENT_BLUE);

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
        table.setWidths(new float[]{56f, 12f, 16f, 16f});
        table.setSpacingAfter(36f);

        Font headFont = getDisplayFont(9.5f, Font.BOLD, COLOR_TEXT_MAIN);
        Font itemTitleFont = getBodyFont(9.5f, Font.NORMAL, COLOR_TEXT_MAIN);
        Font itemSubFont = getBodyFont(8.5f, Font.NORMAL, COLOR_TEXT_MUTED);
        Font qtyFont = getBodyFont(9.5f, Font.NORMAL, COLOR_TEXT_MAIN);
        Font priceFont = getBodyFont(9.5f, Font.NORMAL, COLOR_TEXT_MAIN);
        Font totalFont = getDisplayFont(9.5f, Font.BOLD, COLOR_TEXT_MAIN);

        // Header Row with top and bottom rule
        addHeaderCell(table, "DESCRIPCIÓN", headFont, Element.ALIGN_LEFT);
        addHeaderCell(table, "CANT.", headFont, Element.ALIGN_CENTER);
        addHeaderCell(table, "PRECIO", headFont, Element.ALIGN_RIGHT);
        addHeaderCell(table, "TOTAL", headFont, Element.ALIGN_RIGHT);

        // Line Items Rows with comfortable vertical breathing room
        boolean isFirst = true;
        for (InvoiceData.InvoiceItem item : data.items()) {
            PdfPCell descCell = new PdfPCell();
            descCell.setBorder(Rectangle.NO_BORDER);
            descCell.setPaddingTop(isFirst ? 14f : 10f);
            descCell.setPaddingBottom(10f);

            Paragraph p = new Paragraph(item.description(), itemTitleFont);
            if (item.sectorAndSeat() != null && !item.sectorAndSeat().isBlank()) {
                p.add(new Chunk("\n" + item.sectorAndSeat(), itemSubFont));
            }
            descCell.addElement(p);
            table.addCell(descCell);

            addContentCell(table, String.valueOf(item.quantity()), qtyFont, Element.ALIGN_CENTER, isFirst);
            addContentCell(table, formatCurrency(item.unitPrice()), priceFont, Element.ALIGN_RIGHT, isFirst);
            addContentCell(table, formatCurrency(item.totalPrice()), totalFont, Element.ALIGN_RIGHT, isFirst);
            isFirst = false;
        }

        // Service Fee row
        if (data.serviceFee() != null && data.serviceFee().compareTo(BigDecimal.ZERO) > 0) {
            PdfPCell feeDescCell = new PdfPCell();
            feeDescCell.setBorder(Rectangle.NO_BORDER);
            feeDescCell.setPaddingTop(10f);
            feeDescCell.setPaddingBottom(10f);
            feeDescCell.addElement(new Paragraph("Cargo por Servicio Oficial (Plataforma y Emisión)", itemTitleFont));
            table.addCell(feeDescCell);

            addContentCell(table, "1", qtyFont, Element.ALIGN_CENTER, false);
            addContentCell(table, formatCurrency(data.serviceFee()), priceFont, Element.ALIGN_RIGHT, false);
            addContentCell(table, formatCurrency(data.serviceFee()), totalFont, Element.ALIGN_RIGHT, false);
        }

        // Bottom border rule across the entire table
        PdfPCell borderRule = new PdfPCell();
        borderRule.setColspan(4);
        borderRule.setBorder(Rectangle.BOTTOM);
        borderRule.setBorderWidthBottom(1.2f);
        borderRule.setBorderColorBottom(COLOR_RULE_DARK);
        borderRule.setPadding(0);
        borderRule.setMinimumHeight(12f);
        table.addCell(borderRule);

        document.add(table);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setBorderWidthTop(1.2f);
        cell.setBorderWidthBottom(1.2f);
        cell.setBorderColorTop(COLOR_RULE_DARK);
        cell.setBorderColorBottom(COLOR_RULE_DARK);
        cell.setPaddingTop(10f);
        cell.setPaddingBottom(10f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addContentCell(PdfPTable table, String text, Font font, int alignment, boolean isFirst) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(isFirst ? 14f : 10f);
        cell.setPaddingBottom(10f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addSummaryAndLegalBlock(Document document, InvoiceData data) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{55f, 45f});
        summaryTable.setSpacingAfter(36f);

        // Left Column: AFIP QR Code & Legal Data + Terms
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        Font qrTitleFont = getDisplayFont(8.5f, Font.BOLD, COLOR_TEXT_MAIN);
        Font qrMetaFont = getBodyFont(8f, Font.NORMAL, COLOR_TEXT_SECONDARY);
        Font disclaimerFont = getBodyFont(7.5f, Font.NORMAL, COLOR_TEXT_MUTED);

        try {
            String qrPayload = "https://hibana.tk/verify/" + data.orderId() + "?invoice=" + data.invoiceNumber() + "&cuit=30718291049";
            Image qrImage = generateQrImage(qrPayload, 75, 75);

            PdfPTable qrLayout = new PdfPTable(2);
            qrLayout.setWidthPercentage(100);
            qrLayout.setWidths(new float[]{28f, 72f});

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

        Paragraph disclaimerP = new Paragraph(
                "Conserve este comprobante. El ingreso al recinto requiere la presentación del ticket digital con código QR dinámico emitido por Hibana.tk. Prohibida su reventa o duplicación.",
                disclaimerFont
        );
        disclaimerP.setSpacingBefore(10f);
        leftCell.addElement(disclaimerP);

        summaryTable.addCell(leftCell);

        // Right Column: Subtotal, VAT and Grand Total
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(0);

        Font totLabelFont = getBodyFont(9.5f, Font.NORMAL, COLOR_TEXT_SECONDARY);
        Font totValFont = getBodyFont(9.5f, Font.BOLD, COLOR_TEXT_MAIN);
        Font grandLabelFont = getDisplayFont(11f, Font.BOLD, COLOR_TEXT_MAIN);
        Font grandValFont = getDisplayFont(13f, Font.BOLD, COLOR_TEXT_MAIN);

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(100);
        totalsTable.setWidths(new float[]{55f, 45f});

        addSummaryLine(totalsTable, "SUBTOTAL CONCEPTOS:", formatCurrency(data.subtotal()), totLabelFont, totValFont);
        if (data.serviceFee() != null && data.serviceFee().compareTo(BigDecimal.ZERO) > 0) {
            addSummaryLine(totalsTable, "CARGO POR SERVICIO:", formatCurrency(data.serviceFee()), totLabelFont, totValFont);
        }
        addSummaryLine(totalsTable, "IMPUESTOS / IVA:", formatCurrency(data.vatTax()), totLabelFont, totValFont);
        addSummaryLine(totalsTable, "IMPORTE A PAGAR:", formatCurrency(data.grandTotal()), grandLabelFont, grandValFont);

        rightCell.addElement(totalsTable);
        summaryTable.addCell(rightCell);

        document.add(summaryTable);
    }

    private void addSummaryLine(PdfPTable table, String label, String value, Font lFont, Font vFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, lFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPaddingTop(5f);
        lCell.setPaddingBottom(5f);
        lCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value, vFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setPaddingTop(5f);
        vCell.setPaddingBottom(5f);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(vCell);
    }

    private void addInstitutionalFooter(Document document, InvoiceData data) throws DocumentException {
        PdfPTable footerTable = new PdfPTable(1);
        footerTable.setWidthPercentage(100);
        footerTable.setSpacingBefore(18f);

        PdfPCell dividerCell = new PdfPCell();
        dividerCell.setBorder(Rectangle.TOP);
        dividerCell.setBorderWidthTop(0.8f);
        dividerCell.setBorderColorTop(COLOR_RULE_LIGHT);
        dividerCell.setPaddingTop(10f);
        dividerCell.setPaddingBottom(0);

        Font footFont = getBodyFont(7f, Font.NORMAL, COLOR_TEXT_LIGHT);
        Paragraph footP = new Paragraph(
                "HIBANA.TK ENTERTAINMENT S.A. • COMPROBANTE EMITIDO CONFORME A R.G. AFIP 4291 • ATENCIÓN AL CLIENTE: SOPORTE@HIBANA.TK • WWW.HIBANA.TK",
                footFont
        );
        footP.setAlignment(Element.ALIGN_CENTER);
        dividerCell.addElement(footP);

        footerTable.addCell(dividerCell);
        document.add(footerTable);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$ 0,00";
        String formatted = String.format(Locale.GERMAN, "%,.2f", amount);
        return "$ " + formatted;
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
