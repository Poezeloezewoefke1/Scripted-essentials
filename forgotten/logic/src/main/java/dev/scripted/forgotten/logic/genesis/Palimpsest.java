package dev.scripted.forgotten.logic.genesis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Memory that survives the world. When a run ends, the world writes down fragments of it:
 * where the players lived, what their first home was made of, what they said. A world created
 * afterwards on the same installation reads them back and grows echoes of the old run: a
 * half-deleted foundation at the same coordinates, glitched creatures repeating old words, and
 * the Compiler addressing players by the names of whoever came before.
 *
 * <p>Stored as plain text so it can be inspected and edited by hand.
 */
public final class Palimpsest {
    public static final int MAX_RUNS = 8;
    public static final int MAX_LINES = 40;

    /**
     * @param world   name of the world the run happened in
     * @param ending  how it ended
     * @param players player names
     * @param echoes  places worth echoing: x, y, z and a block palette
     * @param lines   things players said
     */
    public record Run(String world, String ending, List<String> players, List<Echo> echoes, List<String> lines) {
        public Run {
            players = List.copyOf(players);
            echoes = List.copyOf(echoes);
            lines = List.copyOf(lines);
        }
    }

    /**
     * A place from an old run.
     *
     * @param x      block x of the snapshot's origin
     * @param y      block y
     * @param z      block z
     * @param blocks the snapshot as "dx,dy,dz=block_id" cells
     */
    public record Echo(int x, int y, int z, List<String> blocks) {
        public Echo {
            blocks = List.copyOf(blocks);
        }
    }

    private final List<Run> runs = new ArrayList<>();

    public List<Run> runs() {
        return List.copyOf(runs);
    }

    public boolean isEmpty() {
        return runs.isEmpty();
    }

    public void add(Run run) {
        runs.add(run);
        while (runs.size() > MAX_RUNS) {
            runs.remove(0);
        }
    }

    public List<String> allLines() {
        List<String> out = new ArrayList<>();
        for (Run r : runs) {
            out.addAll(r.lines());
        }
        return out;
    }

    public List<String> allPlayers() {
        List<String> out = new ArrayList<>();
        for (Run r : runs) {
            for (String p : r.players()) {
                if (!out.contains(p)) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    public void write(Writer out) throws IOException {
        out.write("palimpsest 1\n");
        for (Run r : runs) {
            out.write("run\t" + esc(r.world()) + "\t" + esc(r.ending()) + "\n");
            for (String p : r.players()) {
                out.write("player\t" + esc(p) + "\n");
            }
            for (String l : r.lines()) {
                out.write("line\t" + esc(l) + "\n");
            }
            for (Echo e : r.echoes()) {
                out.write("echo\t" + e.x() + "\t" + e.y() + "\t" + e.z() + "\t" + esc(String.join(";", e.blocks())) + "\n");
            }
            out.write("end\n");
        }
    }

    public static Palimpsest read(Reader in) throws IOException {
        Palimpsest p = new Palimpsest();
        BufferedReader r = new BufferedReader(in);
        String header = r.readLine();
        if (header == null || !header.startsWith("palimpsest")) {
            return p;
        }
        String world = null;
        String ending = null;
        List<String> players = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        List<Echo> echoes = new ArrayList<>();
        String line;
        while ((line = r.readLine()) != null) {
            String[] f = line.split("\t", -1);
            switch (f[0]) {
                case "run" -> {
                    world = unesc(f[1]);
                    ending = unesc(f[2]);
                    players = new ArrayList<>();
                    lines = new ArrayList<>();
                    echoes = new ArrayList<>();
                }
                case "player" -> players.add(unesc(f[1]));
                case "line" -> {
                    if (lines.size() < MAX_LINES) {
                        lines.add(unesc(f[1]));
                    }
                }
                case "echo" -> {
                    String cells = unesc(f[4]);
                    echoes.add(new Echo(Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]),
                            cells.isEmpty() ? List.of() : List.of(cells.split(";"))));
                }
                case "end" -> {
                    if (world != null) {
                        p.add(new Run(world, ending, players, echoes, lines));
                    }
                    world = null;
                }
                default -> {
                    // Unknown lines are ignored so older and newer files stay readable.
                }
            }
        }
        return p;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
    }

    private static String unesc(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                sb.append(n == 't' ? '\t' : n == 'n' ? '\n' : n);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
