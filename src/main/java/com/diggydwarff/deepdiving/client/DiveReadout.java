package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.diving.DiverService;
import com.diggydwarff.deepdiving.network.DivingPackets.Snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DiveReadout {
    static String format(String f, Object... values) {
        return String.format(Locale.ROOT, f, values);
    }

    static List<String> warnings(Snapshot s) {
        return warnings(s.warnings(), s.ceiling() > 0);
    }

    static List<String> warnings(int flags, boolean decompressionStop) {
        var lines = new ArrayList<String>();

        if ((flags & DiverService.BLACKOUT) != 0) {
            lines.add("BLACKOUT - restore oxygen");
        }

        if ((flags & DiverService.PRESSURE_SHOCK) != 0) {
            lines.add("SUDDEN PRESSURE EXPOSURE");
        }

        if ((flags & DiverService.PRESSURE_INJURY) != 0) {
            lines.add("PRESSURE INJURY");
        }

        if ((flags & DiverService.NO_SUPPLY) != 0) {
            lines.add("NO BREATHING SUPPLY");
        }

        if ((flags & DiverService.HYPOXIA) != 0) {
            lines.add("LOW OXYGEN");
        }

        if ((flags & DiverService.RATING) != 0) {
            lines.add("EQUIPMENT PRESSURE LIMIT");
        }

        if ((flags & DiverService.TOXICITY) != 0) {
            lines.add("OXYGEN TOXICITY");
        }

        if ((flags & DiverService.ASCENT) != 0) {
            lines.add("ASCENDING TOO FAST");
        }

        if ((flags & DiverService.DECO) != 0) {
            lines.add(decompressionStop ? "DECOMPRESSION REQUIRED" : "DECOMPRESSION RECOVERY");
        }

        if ((flags & DiverService.LOW_GAS) != 0) {
            lines.add("LOW GAS RESERVE");
        }

        if ((flags & DiverService.NARCOSIS) != 0) {
            lines.add("NARCOSIS");
        }

        if ((flags & DiverService.BACKUP) != 0) {
            lines.add("BACKUP CYLINDER ACTIVE");
        }

        return lines;
    }

    static String ndl(Snapshot s) {
        return s.ceiling() > 0
                ? format("Ceiling: %.0f blocks", s.ceiling())
                : "No-deco: "
                        + (s.ndl() >= 3600 ? "60m+" : s.ndl() / 60 + "m " + s.ndl() % 60 + "s");
    }

    public static String gasTime(Snapshot s) {
        double seconds = s.gasSeconds();

        if (!Double.isFinite(seconds) || seconds < 0) {
            return "--";
        }

        if (seconds == 0) {
            return "EMPTY";
        }

        if (seconds < 1) {
            return "<1s";
        }

        if (seconds >= 86400) {
            return "24h+";
        }

        int whole = (int) seconds;

        return whole >= 3600
                ? format("%dh %02dm", whole / 3600, whole / 60 % 60)
                : format("%dm %02ds", whole / 60, whole % 60);
    }

    static String ascentStatus(Snapshot s) {
        return ascentStatus(s, false);
    }

    static String compactAscentStatus(Snapshot s) {
        return ascentStatus(s, true);
    }

    static String hudWarning(Snapshot s) {
        var warnings = warnings(s);
        if (warnings.isEmpty()) return "STATUS NORMAL";
        String first =
                switch (warnings.getFirst()) {
                    case "BLACKOUT - restore oxygen" -> "BLACKOUT: NEED OXYGEN";
                    case "SUDDEN PRESSURE EXPOSURE" -> "PRESSURE SHOCK";
                    case "EQUIPMENT PRESSURE LIMIT" -> "PRESSURE LIMIT";
                    case "BACKUP CYLINDER ACTIVE" -> "BACKUP ACTIVE";
                    default -> warnings.getFirst();
                };
        return first + (warnings.size() > 1 ? " +" + (warnings.size() - 1) : "");
    }

    static String hudSecondary(Snapshot s) {
        if (s.ceiling() <= 0)
            return new com.diggydwarff.deepdiving.gas.GasMix(s.oxygen(), s.helium()).name();
        if ((s.warnings() & DiverService.NO_SUPPLY) != 0) return "NO GAS";
        if (s.depth() + .05 < s.ceiling() || s.stopSeconds() < 0) return "NEXT --";
        return "NEXT " + stopTime(s);
    }

    private static String ascentStatus(Snapshot s, boolean compact) {
        if (s.ceiling() > 0 && s.depth() + .05 < s.ceiling()) {
            return format(
                    compact ? "DESCEND TO %.0fb" : "DESCEND TO STOP %.0f BLOCKS", s.ceiling());
        }

        if ((s.warnings() & DiverService.ASCENT) != 0) {
            return "SLOW YOUR ASCENT";
        }

        if (s.ceiling() > 0
                && s.stopSeconds() < 0
                && (s.warnings() & DiverService.NO_SUPPLY) == 0) {
            return format(compact ? "MOVE TO STOP %.0fb" : "MOVE TO STOP %.0f BLOCKS", s.ceiling());
        }

        if (s.depthHold()) {
            return format(compact ? "DEPTH HOLD %.1fb" : "DEPTH HOLD %.1f BLOCKS", s.heldDepth());
        }

        if (s.finsFloating()) {
            return format(compact ? "FINS FLOAT %.1fb" : "FINS FLOAT %.1f BLOCKS", s.finDepth());
        }

        if (s.ceiling() > 0) {
            return format(compact ? "HOLD STOP %.0fb" : "HOLD STOP %.0f BLOCKS", s.ceiling());
        }

        if (s.ascent() < -.05) {
            return "DESCENDING";
        }

        if (s.ascent() > .05) {
            return compact || s.ascent() > s.safeAscent() ? "ASCENDING" : "ASCENDING WITHIN LIMIT";
        }

        return "DEPTH STEADY";
    }

    public static String stopTime(Snapshot s) {
        if (s.ceiling() <= 0) {
            return "READY";
        }

        if ((s.warnings() & DiverService.NO_SUPPLY) != 0) {
            return "NO GAS";
        }

        if (s.depth() + .05 < s.ceiling()) {
            return "DESCEND TO STOP";
        }

        if (s.stopSeconds() < 0) {
            return "MOVE TO STOP";
        }

        if (s.stopSeconds() == 0) {
            return "<1s";
        }

        return format("~%d:%02d", s.stopSeconds() / 60, s.stopSeconds() % 60);
    }

    private DiveReadout() {}
}
