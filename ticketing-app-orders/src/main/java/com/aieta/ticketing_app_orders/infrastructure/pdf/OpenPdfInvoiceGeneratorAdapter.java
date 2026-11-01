package com.aieta.ticketing_app_orders.infrastructure.pdf;

import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.aieta.ticketing_app_orders.application.ports.InvoicePdfPort;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class OpenPdfInvoiceGeneratorAdapter implements InvoicePdfPort {

    private static final Logger log = LoggerFactory.getLogger(OpenPdfInvoiceGeneratorAdapter.class);

    // Hibana.tk Design System Palette
    private static final Color COLOR_CANVAS_BASE = new Color(12, 20, 31);    // #0c141f
    private static final Color COLOR_SURFACE_CARD = new Color(20, 28, 39);   // #141c27
    private static final Color COLOR_SURFACE_INNER = new Color(16, 24, 37);  // #101825
    private static final Color COLOR_BORDER_SUBTLE = new Color(35, 53, 77);  // #23354d
    private static final Color COLOR_PRIMARY_BLUE = new Color(134, 207, 251);// #86cffb
    private static final Color COLOR_SECONDARY_TEAL = new Color(79, 219, 200);// #4fdbc8
    private static final Color COLOR_TEXT_WHITE = new Color(241, 245, 249);  // #f1f5f9
    private static final Color COLOR_TEXT_MUTED = new Color(148, 163, 184);  // #94a3b8

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    @Override
    public byte[] generateInvoicePdf(InvoiceData data) {
        log.debug("[INVOICE-PDF] Generating PDF invoice for order: {}", data.orderId());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 28, 28, 28, 28);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header (Brand, Document Type Box, Invoice Identifier)
            addHeader(document, data);

            // 2. Customer & Event Information Card
            addCustomerAndEventInfo(document, data);

            // 3. Line Items Table (Seats, Add-ons, Service Fee)
            addItemsTable(document, data);

            // 4. Totals, Taxes, and QR Code Validation Block
            addTotalsAndQr(document, data);

            // 5. Official Footer Note
            addFooter(document, data);

            document.close();
            byte[] pdfBytes = out.toByteArray();
            log.info("[INVOICE-PDF] Successfully generated PDF invoice {} ({} bytes)", data.invoiceNumber(), pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            log.error("[INVOICE-PDF] Failed to generate invoice PDF for order {}: {}", data.orderId(), e.getMessage(), e);
            throw new RuntimeException("Error generating invoice PDF", e);
        }
    }

    private void addHeader(Document document, InvoiceData data) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{42f, 16f, 42f});
        headerTable.setSpacingAfter(12f);

        // Left: Corporate Identity
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setBackgroundColor(COLOR_CANVAS_BASE);
        leftCell.setPadding(8f);

        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_PRIMARY_BLUE);
        Font subBrandFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_SECONDARY_TEAL);
        Font legalFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_MUTED);

        leftCell.addElement(new Paragraph("HIBANA.TK", brandFont));
        leftCell.addElement(new Paragraph("PLATAFORMA OFICIAL DE ENTRADAS", subBrandFont));
        leftCell.addElement(new Paragraph("Hibana.tk Entertainment S.A.", legalFont));
        leftCell.addElement(new Paragraph("CUIT: 30-71829104-9 • IVA Resp. Inscripto", legalFont));
        leftCell.addElement(new Paragraph("Av. Figueroa Alcorta 7597, CABA, Argentina", legalFont));
        headerTable.addCell(leftCell);

        // Center: Official Voucher Type Box ("B" Code 006)
        PdfPCell centerCell = new PdfPCell();
        centerCell.setBorder(Rectangle.BOX);
        centerCell.setBorderColor(COLOR_PRIMARY_BLUE);
        centerCell.setBorderWidth(1.5f);
        centerCell.setBackgroundColor(COLOR_SURFACE_CARD);
        centerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        centerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        centerCell.setPadding(4f);

        Font typeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, COLOR_PRIMARY_BLUE);
        Font codeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_TEXT_MUTED);

        Paragraph typeP = new Paragraph("B", typeFont);
        typeP.setAlignment(Element.ALIGN_CENTER);
        Paragraph codeP = new Paragraph("COD. 006", codeFont);
        codeP.setAlignment(Element.ALIGN_CENTER);

        centerCell.addElement(typeP);
        centerCell.addElement(codeP);
        headerTable.addCell(centerCell);

        // Right: Invoice Metadata
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setBackgroundColor(COLOR_CANVAS_BASE);
        rightCell.setPadding(8f);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font invoiceTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_TEXT_WHITE);
        Font invoiceDataFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_PRIMARY_BLUE);
        Font invoiceDetailFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_MUTED);

        Paragraph rightP = new Paragraph();
        rightP.setAlignment(Element.ALIGN_RIGHT);
        rightP.add(new Chunk("FACTURA\n", invoiceTitleFont));
        rightP.add(new Chunk(data.invoiceNumber() + "\n", invoiceDataFont));
        rightP.add(new Chunk("Fecha: " + DATE_FMT.format(data.issuedAt()) + "\n", invoiceDetailFont));
        rightP.add(new Chunk("Orden Ref: #" + data.orderId() + "\n", invoiceDetailFont));
        rightP.add(new Chunk("Moneda: " + data.currency() + "\n", invoiceDetailFont));
        rightCell.addElement(rightP);

        headerTable.addCell(rightCell);
        document.add(headerTable);
    }

    private void addCustomerAndEventInfo(Document document, InvoiceData data) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{50f, 50f});
        infoTable.setSpacingAfter(14f);

        Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_SECONDARY_TEAL);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_TEXT_MUTED);
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_WHITE);

        // Left: Buyer Info
        PdfPCell buyerCell = new PdfPCell();
        buyerCell.setBackgroundColor(COLOR_SURFACE_CARD);
        buyerCell.setBorderColor(COLOR_BORDER_SUBTLE);
        buyerCell.setPadding(8f);

        buyerCell.addElement(new Paragraph("DATOS DEL COMPRADOR", sectionHeaderFont));
        buyerCell.addElement(createKeyValue("Nombre:", data.buyerName(), labelFont, valFont));
        buyerCell.addElement(createKeyValue("Email:", data.buyerEmail(), labelFont, valFont));
        buyerCell.addElement(createKeyValue("Identificación / CUIT:", data.buyerTaxId(), labelFont, valFont));
        buyerCell.addElement(createKeyValue("Medio de Pago:", data.paymentMethod(), labelFont, valFont));
        infoTable.addCell(buyerCell);

        // Right: Event Details
        PdfPCell eventCell = new PdfPCell();
        eventCell.setBackgroundColor(COLOR_SURFACE_CARD);
        eventCell.setBorderColor(COLOR_BORDER_SUBTLE);
        eventCell.setPadding(8f);

        eventCell.addElement(new Paragraph("DETALLE DEL ESPECTÁCULO", sectionHeaderFont));
        eventCell.addElement(createKeyValue("Show:", data.eventTitle(), labelFont, valFont));
        eventCell.addElement(createKeyValue("Fecha y Hora:", data.eventDate(), labelFont, valFont));
        eventCell.addElement(createKeyValue("Recinto:", data.venueName(), labelFont, valFont));
        eventCell.addElement(createKeyValue("Ubicación:", data.venueAddress(), labelFont, valFont));
        infoTable.addCell(eventCell);

        document.add(infoTable);
    }

    private void addItemsTable(Document document, InvoiceData data) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{40f, 25f, 10f, 12f, 13f});
        table.setSpacingAfter(14f);

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_PRIMARY_BLUE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_WHITE);
        Font numFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_WHITE);

        // Table Headers
        String[] headers = {"DESCRIPCIÓN DEL ÍTEM", "SECTOR / UBICACIÓN", "CANT.", "PRECIO UNIT.", "TOTAL"};
        for (int i = 0; i < headers.length; i++) {
            PdfPCell hCell = new PdfPCell(new Phrase(headers[i], headFont));
            hCell.setBackgroundColor(COLOR_SURFACE_INNER);
            hCell.setBorderColor(COLOR_BORDER_SUBTLE);
            hCell.setPadding(6f);
            if (i >= 2) {
                hCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
            table.addCell(hCell);
        }

        // Table Rows
        for (InvoiceData.InvoiceItem item : data.items()) {
            addTableRow(table, item.description(), cellFont, Element.ALIGN_LEFT);
            addTableRow(table, item.sectorAndSeat(), cellFont, Element.ALIGN_LEFT);
            addTableRow(table, String.valueOf(item.quantity()), numFont, Element.ALIGN_RIGHT);
            addTableRow(table, "$" + item.unitPrice().setScale(2).toString(), numFont, Element.ALIGN_RIGHT);
            addTableRow(table, "$" + item.totalPrice().setScale(2).toString(), numFont, Element.ALIGN_RIGHT);
        }

        document.add(table);
    }

    private void addTableRow(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setBackgroundColor(COLOR_CANVAS_BASE);
        cell.setBorderColor(COLOR_BORDER_SUBTLE);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addTotalsAndQr(Document document, InvoiceData data) throws DocumentException {
        PdfPTable bottomTable = new PdfPTable(2);
        bottomTable.setWidthPercentage(100);
        bottomTable.setWidths(new float[]{55f, 45f});
        bottomTable.setSpacingAfter(10f);

        // Left Cell: QR Code & AFIP / Fiscal CAE
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBackgroundColor(COLOR_SURFACE_CARD);
        leftCell.setBorderColor(COLOR_BORDER_SUBTLE);
        leftCell.setPadding(8f);

        Font caeLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_TEXT_MUTED);
        Font caeVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_TEXT_WHITE);

        try {
            String qrPayload = "https://hibana.tk/verify/" + data.orderId() + "?invoice=" + data.invoiceNumber() + "&cuit=30718291049";
            Image qrImage = generateQrImage(qrPayload, 80, 80);
            qrImage.setAlignment(Element.ALIGN_LEFT);

            PdfPTable qrTable = new PdfPTable(2);
            qrTable.setWidthPercentage(100);
            qrTable.setWidths(new float[]{30f, 70f});

            PdfPCell imgCell = new PdfPCell(qrImage);
            imgCell.setBorder(Rectangle.NO_BORDER);
            imgCell.setPadding(0);
            qrTable.addCell(imgCell);

            PdfPCell metaCell = new PdfPCell();
            metaCell.setBorder(Rectangle.NO_BORDER);
            metaCell.setPaddingLeft(6f);
            metaCell.addElement(new Paragraph("CAE N°: " + data.caeNumber(), caeVal));
            metaCell.addElement(new Paragraph("Vto. CAE: " + DATE_FMT.format(data.caeExpiresAt()), caeLabel));
            metaCell.addElement(new Paragraph("Código de Control: AFIP / HIBANA SEC-01", caeLabel));
            qrTable.addCell(metaCell);

            leftCell.addElement(qrTable);
        } catch (Exception e) {
            leftCell.addElement(new Paragraph("CAE N°: " + data.caeNumber(), caeVal));
            leftCell.addElement(new Paragraph("Vto. CAE: " + DATE_FMT.format(data.caeExpiresAt()), caeLabel));
        }

        bottomTable.addCell(leftCell);

        // Right Cell: Subtotals, VAT, and Grand Total
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBackgroundColor(COLOR_SURFACE_CARD);
        rightCell.setBorderColor(COLOR_BORDER_SUBTLE);
        rightCell.setPadding(8f);

        Font totLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_TEXT_MUTED);
        Font totVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_TEXT_WHITE);
        Font grandLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY_BLUE);
        Font grandVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRIMARY_BLUE);

        PdfPTable totalsGrid = new PdfPTable(2);
        totalsGrid.setWidthPercentage(100);
        totalsGrid.setWidths(new float[]{60f, 40f});

        addTotalLine(totalsGrid, "Subtotal Neto:", "$" + data.subtotal().setScale(2), totLabel, totVal);
        addTotalLine(totalsGrid, "Cargo por Servicio (7%):", "$" + data.serviceFee().setScale(2), totLabel, totVal);
        addTotalLine(totalsGrid, "IVA Débito Fiscal (21%):", "$" + data.vatTax().setScale(2), totLabel, totVal);
        addTotalLine(totalsGrid, "TOTAL ABONADO:", "$" + data.grandTotal().setScale(2) + " " + data.currency(), grandLabel, grandVal);

        rightCell.addElement(totalsGrid);
        bottomTable.addCell(rightCell);

        document.add(bottomTable);
    }

    private void addTotalLine(PdfPTable table, String label, String val, Font lFont, Font vFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, lFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPadding(3f);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(val, vFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vCell.setPadding(3f);
        table.addCell(vCell);
    }

    private void addFooter(Document document, InvoiceData data) throws DocumentException {
        Font footFont = FontFactory.getFont(FontFactory.HELVETICA, 7, COLOR_TEXT_MUTED);
        Paragraph footP = new Paragraph();
        footP.setAlignment(Element.ALIGN_CENTER);
        footP.setSpacingBefore(8f);
        footP.add(new Chunk("Comprobante oficial emitido por Hibana.tk S.A. • Registro Electrónico AFIP RG 4291/2018\n", footFont));
        footP.add(new Chunk("Para verificar la autenticidad de esta factura escanee el código QR o visite https://hibana.tk/verify/" + data.orderId(), footFont));
        document.add(footP);
    }

    private Paragraph createKeyValue(String key, String val, Font keyFont, Font valFont) {
        Paragraph p = new Paragraph();
        p.setLeading(11f);
        p.add(new Chunk(key + " ", keyFont));
        p.add(new Chunk((val != null ? val : "-") + "\n", valFont));
        return p;
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
