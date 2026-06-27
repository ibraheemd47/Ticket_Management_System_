package com.sdnah.Ticket_Management_System_.Frontend;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * Renders a live seat-map sketch (II.4.2) from plain Vaadin components into a
 * target container: a stage bar, an optional standing band, and one sampled
 * seat-grid card per seated block (capped for large layouts). Shared by the
 * show dialogs in {@code EventCreationView} and {@code EventDetailsView}.
 */
final class SeatPreviewRenderer {

    private SeatPreviewRenderer() {}

    static void render(Div target, Integer capI, Integer blocksI, Integer rowsI, Integer seatsI) {
        target.removeAll();
        int cap    = capI    == null ? 0 : Math.max(0, capI);
        int blocks = blocksI == null ? 0 : Math.max(0, blocksI);
        int rows   = rowsI   == null ? 0 : Math.max(0, rowsI);
        int seats  = seatsI  == null ? 0 : Math.max(0, seatsI);

        boolean hasStanding = cap > 0;
        boolean hasSeated   = blocks > 0 && rows > 0 && seats > 0;

        target.add(bar("STAGE", "#37474f"));

        if (hasStanding) {
            target.add(bar("Standing area · capacity " + cap, "#2e7d32"));
        }

        if (hasSeated) {
            Div header = new Div();
            header.setText("Seated · " + blocks + " block" + (blocks == 1 ? "" : "s")
                    + " × " + rows + " × " + seats + " = " + ((long) blocks * rows * seats) + " seats");
            header.getStyle().set("font-size", "12px").set("color", "#444")
                    .set("font-weight", "600").set("margin", "4px 0");
            target.add(header);

            int drawn = Math.min(blocks, 10);
            Div blocksWrap = new Div();
            blocksWrap.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "10px");
            for (int i = 0; i < drawn; i++) {
                blocksWrap.add(blockCard(i + 1, rows, seats));
            }
            target.add(blocksWrap);

            if (blocks > drawn) {
                Div more = new Div();
                more.setText("+ " + (blocks - drawn) + " more block(s)");
                more.getStyle().set("font-size", "11px").set("color", "#888").set("margin-top", "6px");
                target.add(more);
            }
        }

        if (!hasSeated && !hasStanding) {
            Div hint = new Div();
            hint.setText("Enter capacity or blocks/rows/seats to preview the layout");
            hint.getStyle().set("font-size", "12px").set("color", "#999").set("padding", "6px 0");
            target.add(hint);
        }
    }

    /** A full-width coloured label bar (stage / standing). */
    private static Div bar(String text, String bg) {
        Div d = new Div();
        d.setText(text);
        d.getStyle()
                .set("background", bg).set("color", "white").set("text-align", "center")
                .set("padding", "6px 8px").set("border-radius", "6px")
                .set("font-weight", "700").set("font-size", "12px").set("margin-bottom", "8px");
        return d;
    }

    /** One seated block rendered as a sampled dot grid + a rows×seats label. */
    private static Div blockCard(int n, int rows, int seats) {
        int cols = Math.min(seats, 8), rws = Math.min(rows, 5);

        Div grid = new Div();
        grid.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(" + cols + ", 6px)").set("gap", "3px");
        for (int k = 0; k < cols * rws; k++) {
            Div dot = new Div();
            dot.getStyle().set("width", "6px").set("height", "6px")
                    .set("background", "#1565c0").set("border-radius", "1px");
            grid.add(dot);
        }

        Span label = new Span("B" + n + " · " + rows + "×" + seats);
        label.getStyle().set("font-size", "10px").set("color", "#666");

        Div card = new Div(grid, label);
        card.getStyle()
                .set("border", "1px solid #90c2ff").set("background", "#eaf2ff").set("border-radius", "6px")
                .set("padding", "8px").set("display", "flex").set("flex-direction", "column")
                .set("align-items", "center").set("gap", "5px");
        return card;
    }
}
