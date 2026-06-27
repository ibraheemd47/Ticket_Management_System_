package com.sdnah.Ticket_Management_System_.Backend.DTOs;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable graphical venue layout / event map (II.4.2). Stored as JSON on
 * the Event. Each {@link Element} is a positioned rectangle on the map canvas;
 * SEATED / STANDING elements may link to an inventory area via {@code areaId}.
 */
public class VenueMapDTO {

    public int canvasWidth = 800;
    public int canvasHeight = 500;
    public List<Element> elements = new ArrayList<>();

    public static class Element {
        public String id;
        public String type;   // STAGE | ENTRANCE | SEATED | STANDING
        public String label;
        public double x;
        public double y;
        public double w;
        public double h;
        public String color;
        public String areaId;   // links to Area.id for SEATED / STANDING (nullable)
        public String areaName; // denormalized name for display (nullable)
    }
}
