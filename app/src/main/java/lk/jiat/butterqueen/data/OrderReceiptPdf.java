package lk.jiat.butterqueen.data;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import lk.jiat.butterqueen.model.OrderItem;
import lk.jiat.butterqueen.model.UnitPricing;

/** Printable A4 order record with payment status shown on every page. */
public final class OrderReceiptPdf {
    private static final int WIDTH = 595;
    private static final int HEIGHT = 842;
    private static final int MARGIN = 42;
    private static final int BOTTOM = 770;
    private static final int BROWN = Color.rgb(103, 52, 38);
    private static final int INK = Color.rgb(43, 35, 33);
    private static final int MUTED = Color.rgb(105, 96, 92);

    private OrderReceiptPdf() {}

    public static void write(OrderReceipt receipt, OutputStream output) throws IOException {
        PdfDocument document = new PdfDocument();
        try {
            Writer w = new Writer(document, receipt.id, receipt.paymentState());
            w.section("ORDER " + receipt.id);
            w.field("Placed", receipt.date());
            w.field("Order status", receipt.orderState());
            w.field("Payment method / status",
                    receipt.paymentLabel() + " / " + receipt.paymentState());
            w.paragraph(receipt.paymentNotice());

            w.section("CUSTOMER & DELIVERY");
            w.field("Name", receipt.customerName);
            w.field("Phone", receipt.customerPhone);
            w.field("Address", receipt.deliveryAddress);
            if (receipt.note != null && !receipt.note.trim().isEmpty())
                w.field("Order note", receipt.note);

            w.section("ITEMS");
            if (receipt.items.isEmpty()) w.paragraph("No line items were recorded for this order.");
            for (OrderItem item : receipt.items) {
                w.item(OrderReceipt.value(item.getProductName()),
                        UnitPricing.quantityLabel(item.getQuantity(), item.getUnitType()),
                        OrderReceipt.money(item.getUnitPrice()),
                        OrderReceipt.money(item.getItemTotal()));
                if (item.getCakeMessage() != null && !item.getCakeMessage().isEmpty())
                    w.paragraph("Cake message: " + item.getCakeMessage());
            }

            w.section("TOTALS");
            w.field("Subtotal", OrderReceipt.money(receipt.subtotal));
            w.field("Delivery", OrderReceipt.money(receipt.deliveryFee));
            w.field("Order total", OrderReceipt.money(receipt.total));
            w.paragraph("ButterQueen  |  0763227525  |  info@butterqueen.com");
            w.finish();
            document.writeTo(output);
            output.flush();
        } finally {
            document.close();
        }
    }

    private static Paint paint(int color, int size, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }

    private static final class Writer {
        private final PdfDocument document;
        private final String reference;
        private final String paymentState;
        private PdfDocument.Page page;
        private Canvas canvas;
        private int pageNumber;
        private float y;

        Writer(PdfDocument document, String reference, String paymentState) {
            this.document = document;
            this.reference = reference;
            this.paymentState = paymentState;
            newPage();
        }

        private void newPage() {
            page = document.startPage(new PdfDocument.PageInfo.Builder(
                    WIDTH, HEIGHT, ++pageNumber).create());
            canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);
            Paint bar = new Paint();
            bar.setColor(BROWN);
            canvas.drawRect(0, 0, WIDTH, 8, bar);
            canvas.drawText("BUTTERQUEEN", MARGIN, 41, paint(BROWN, 13, true));
            canvas.drawText("Order receipt", MARGIN, 79, paint(INK, 26, true));
            y = 103;
        }

        private void ensure(float height) {
            if (y + height <= BOTTOM) return;
            closePage();
            newPage();
        }

        void section(String title) {
            ensure(85);
            y += 16;
            Paint rule = new Paint();
            rule.setColor(Color.rgb(225, 216, 210));
            canvas.drawRect(MARGIN, y, WIDTH - MARGIN, y + 1, rule);
            y += 20;
            wrapped(title.toUpperCase(Locale.ROOT), paint(BROWN, 11, true), 15);
            y += 5;
        }

        void field(String label, String value) {
            ensure(48);
            y += 7;
            wrapped(label.toUpperCase(Locale.ROOT), paint(MUTED, 9, true), 13);
            wrapped(OrderReceipt.value(value), paint(INK, 12, false), 17);
            y += 7;
        }

        void paragraph(String value) {
            ensure(24);
            y += 5;
            wrapped(value, paint(MUTED, 10, false), 15);
            y += 7;
        }

        void item(String name, String quantity, String unitPrice, String total) {
            ensure(61);
            y += 7;
            wrapped(name, paint(INK, 12, true), 17);
            wrapped(quantity + "  x  " + unitPrice + "  =  " + total,
                    paint(MUTED, 10, false), 15);
            y += 8;
        }

        private void wrapped(String value, Paint p, int height) {
            String safe = value == null || value.trim().isEmpty() ? "Not recorded"
                    : value.replace('\r', ' ').replace('\t', ' ').trim();
            for (String paragraph : safe.split("\n", -1)) {
                String remaining = paragraph.trim();
                if (remaining.isEmpty()) {
                    ensure(height);
                    y += height;
                    continue;
                }
                while (!remaining.isEmpty()) {
                    int length = Math.max(1, p.breakText(remaining, true, WIDTH - MARGIN * 2f, null));
                    if (length < remaining.length()) {
                        int space = remaining.lastIndexOf(' ', length);
                        if (space > 0) length = space;
                    }
                    ensure(height);
                    canvas.drawText(remaining.substring(0, length), MARGIN, y, p);
                    y += height;
                    remaining = remaining.substring(length).trim();
                }
            }
        }

        private void closePage() {
            canvas.drawText("Order " + reference + "  •  " + pageNumber,
                    MARGIN, 811, paint(MUTED, 9, false));
            canvas.drawText("Payment: " + paymentState,
                    WIDTH - 225, 811, paint(MUTED, 9, false));
            document.finishPage(page);
            page = null;
        }

        void finish() {
            if (page != null) closePage();
        }
    }
}
