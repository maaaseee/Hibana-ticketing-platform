package com.aieta.ticketing_app_orders.infrastructure.pdf;

import com.aieta.ticketing_app_orders.application.common.InvoiceDataMapper;
import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.lowagie.text.pdf.BaseFont;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenPdfInvoiceGeneratorAdapterTest {

    @Test
    void shouldLoadCustomFontsSuccessfully() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/fonts/SpaceGrotesk.ttf")) {
            assertNotNull(is, "SpaceGrotesk font must be on classpath");
            byte[] bytes = is.readAllBytes();
            BaseFont bf = BaseFont.createFont("SpaceGrotesk.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
            assertNotNull(bf);
        }

        try (InputStream is = getClass().getResourceAsStream("/fonts/HankenGrotesk.ttf")) {
            assertNotNull(is, "HankenGrotesk font must be on classpath");
            byte[] bytes = is.readAllBytes();
            BaseFont bf = BaseFont.createFont("HankenGrotesk.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
            assertNotNull(bf);
        }
    }

    @Test
    void shouldGenerateInvoicePdfSuccessfully() {
        OpenPdfInvoiceGeneratorAdapter adapter = new OpenPdfInvoiceGeneratorAdapter();
        InvoiceData sample = InvoiceDataMapper.createSampleInvoice();

        byte[] pdf = adapter.generateInvoicePdf(sample);

        assertNotNull(pdf);
        assertTrue(pdf.length > 50000, "PDF should include background image and be > 50KB");
    }
}
