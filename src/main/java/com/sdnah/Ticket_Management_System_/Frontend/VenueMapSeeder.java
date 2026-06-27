package com.sdnah.Ticket_Management_System_.Frontend;

import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.VenueAreaRefDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VenueMapDTO;

/**
 * Builds a default venue map (II.4.2) from an event's seating dimensions: a
 * stage, an optional standing band, and one card per seated block. Used both to
 * seed the editor and to give buyers a visual map on the event page even when
 * the owner hasn't saved a custom layout yet.
 */
public final class VenueMapSeeder {

    private VenueMapSeeder() {}

    private static final String STAGE = "STAGE", SEATED = "SEATED", STANDING = "STANDING";

    /**
     * @param dims  {@code {standingCapacity, blocks, rowsPerBlock, seatsPerRow}}
     * @param areas inventory areas to link seated/standing elements to (may be empty)
     */
    public static VenueMapDTO buildDefault(int[] dims, List<VenueAreaRefDTO> areas) {
        VenueMapDTO map = new VenueMapDTO();
        if (dims == null || dims.length < 4) return map;

        int standingCap = dims[0], blocks = dims[1], rows = dims[2], seats = dims[3];

        VenueAreaRefDTO seatedArea = first(areas, SEATED);
        VenueAreaRefDTO standingArea = first(areas, STANDING);

        double y = 20;
        addEl(map, STAGE, "Stage", (map.canvasWidth - 240) / 2.0, y, 240, 50, null);
        y += 80;

        if (standingCap > 0) {
            addEl(map, STANDING, "Standing · cap " + standingCap,
                    (map.canvasWidth - 260) / 2.0, y, 260, 50, standingArea);
            y += 80;
        }

        if (blocks > 0) {
            int drawn = Math.min(blocks, 24);
            int perRow = 4, cardW = 150, cardH = 84, gap = 18, leftPad = 40;
            for (int i = 0; i < drawn; i++) {
                double bx = leftPad + (i % perRow) * (cardW + gap);
                double by = y + (i / perRow) * (cardH + gap);
                addEl(map, SEATED, "Block " + (i + 1) + " · " + rows + "×" + seats,
                        bx, by, cardW, cardH, seatedArea);
            }
            int usedRows = (int) Math.ceil(drawn / (double) perRow);
            double neededH = y + usedRows * (cardH + gap) + 30;
            if (neededH > map.canvasHeight) map.canvasHeight = (int) neededH;
        }
        return map;
    }

    private static VenueAreaRefDTO first(List<VenueAreaRefDTO> areas, String type) {
        if (areas == null) return null;
        return areas.stream().filter(a -> type.equals(a.type)).findFirst().orElse(null);
    }

    private static void addEl(VenueMapDTO map, String type, String label,
            double x, double y, double w, double h, VenueAreaRefDTO area) {
        VenueMapDTO.Element el = new VenueMapDTO.Element();
        el.id = "el-" + UUID.randomUUID();
        el.type = type;
        el.label = label;
        el.x = Math.max(0, Math.min(x, map.canvasWidth - w));
        el.y = y;
        el.w = w;
        el.h = h;
        el.color = color(type);
        if (area != null) {
            el.areaId = area.id;
            el.areaName = area.name;
        }
        map.elements.add(el);
    }

    private static String color(String type) {
        if (type == null) return "#1565c0";
        return switch (type) {
            case STAGE    -> "#37474f";
            case SEATED   -> "#1565c0";
            case STANDING -> "#2e7d32";
            default       -> "#1565c0";
        };
    }
}
