package me.animatednametags;

import java.util.List;

/**
 * Un formato de nametag (lineas + permiso + prioridad + velocidad de animacion).
 */
public record NametagFormat(String id, String permission, int priority, List<String> lines, double speed) {
}
