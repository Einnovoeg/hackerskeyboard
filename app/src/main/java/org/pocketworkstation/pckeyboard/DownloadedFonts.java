package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.graphics.Typeface;
import android.util.SparseArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue and on-device store for the extra key fonts offered by the font library.
 *
 * <p>The ten built-in typefaces keep preference values 0-9. Downloaded fonts use
 * fixed values from {@link #FIRST_INDEX} upwards so a selection stays stable when
 * other fonts are installed or removed. Font files live in the app's private
 * files directory and are only written after the user asks to download one.
 */
public final class DownloadedFonts {

    public static final int FIRST_INDEX = 10;
    private static final String DIR = "fonts";
    private static final String USER_AGENT = "HACK3RS-K3YB0ARD/3.0";
    private static final int MAX_BYTES = 6 * 1024 * 1024;
    private static final SparseArray<Typeface> TYPE_CACHE = new SparseArray<>();

    public static final class Font {
        public final String id;
        public final String name;
        public final String category;
        public final String license;
        public final String url;
        public final int previewRes;

        Font(String id, String name, String category, String license, String url, int previewRes) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.license = license;
            this.url = url;
            this.previewRes = previewRes;
        }
    }

    private static final Font[] CATALOG = new Font[]{
            new Font("spacemono", "Space Mono", "Monospace", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/spacemono/SpaceMono-Regular.ttf",
                    R.drawable.font_preview_spacemono),
            new Font("bebasneue", "Bebas Neue", "Condensed display", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/bebasneue/BebasNeue-Regular.ttf",
                    R.drawable.font_preview_bebasneue),
            new Font("anton", "Anton", "Heavy display", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/anton/Anton-Regular.ttf",
                    R.drawable.font_preview_anton),
            new Font("pressstart2p", "Press Start 2P", "Pixel", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/pressstart2p/PressStart2P-Regular.ttf",
                    R.drawable.font_preview_pressstart2p),
            new Font("vt323", "VT323", "Terminal", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/vt323/VT323-Regular.ttf",
                    R.drawable.font_preview_vt323),
            new Font("righteous", "Righteous", "Retro sans", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/righteous/Righteous-Regular.ttf",
                    R.drawable.font_preview_righteous),
            new Font("russoone", "Russo One", "Tech display", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/russoone/RussoOne-Regular.ttf",
                    R.drawable.font_preview_russoone),
            new Font("pacifico", "Pacifico", "Script", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/pacifico/Pacifico-Regular.ttf",
                    R.drawable.font_preview_pacifico),
            new Font("lobster", "Lobster", "Script", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/lobster/Lobster-Regular.ttf",
                    R.drawable.font_preview_lobster),
            new Font("audiowide", "Audiowide", "Futuristic", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/audiowide/Audiowide-Regular.ttf",
                    R.drawable.font_preview_audiowide),
            new Font("archivoblack", "Archivo Black", "Bold sans", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/archivoblack/ArchivoBlack-Regular.ttf",
                    R.drawable.font_preview_archivoblack),
            new Font("playfairdisplay", "Playfair Display", "Elegant serif", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/playfairdisplay/PlayfairDisplay%5Bwght%5D.ttf",
                    R.drawable.font_preview_playfairdisplay),
            new Font("oswald", "Oswald", "Condensed sans", "Google Fonts - OFL 1.1",
                    "https://raw.githubusercontent.com/google/fonts/main/ofl/oswald/Oswald%5Bwght%5D.ttf",
                    R.drawable.font_preview_oswald),
    };

    private DownloadedFonts() {
    }

    public static int count() {
        return CATALOG.length;
    }

    public static Font get(int position) {
        return position >= 0 && position < CATALOG.length ? CATALOG[position] : null;
    }

    public static int indexOf(Font font) {
        for (int i = 0; i < CATALOG.length; i++) {
            if (CATALOG[i].id.equals(font.id)) return FIRST_INDEX + i;
        }
        return -1;
    }

    public static Font forIndex(int index) {
        int position = index - FIRST_INDEX;
        return position >= 0 && position < CATALOG.length ? CATALOG[position] : null;
    }

    public static File fontFile(Context context, Font font) {
        File dir = new File(context.getFilesDir(), DIR);
        if (!dir.isDirectory()) dir.mkdirs();
        return new File(dir, font.id + ".ttf");
    }

    public static boolean isInstalled(Context context, Font font) {
        File file = fontFile(context, font);
        return file.isFile() && file.length() > 1024;
    }

    public static List<Font> installedFonts(Context context) {
        List<Font> result = new ArrayList<>();
        for (Font font : CATALOG) {
            if (isInstalled(context, font)) result.add(font);
        }
        return result;
    }

    /**
     * Returns the cached typeface for a downloaded font slot, or null when that
     * slot is a built-in font or its file is not installed.
     */
    public static Typeface typefaceForIndex(Context context, int index) {
        Font font = forIndex(index);
        if (font == null) return null;
        return typefaceFor(context, font);
    }

    public static Typeface typefaceFor(Context context, Font font) {
        int index = indexOf(font);
        synchronized (TYPE_CACHE) {
            Typeface cached = TYPE_CACHE.get(index);
            if (cached != null) return cached;
        }
        File file = fontFile(context, font);
        if (!file.isFile() || file.length() <= 1024) return null;
        try {
            Typeface typeface = Typeface.createFromFile(file);
            synchronized (TYPE_CACHE) {
                TYPE_CACHE.put(index, typeface);
            }
            return typeface;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Downloads a font straight to private storage. The temporary file is only
     * promoted after the native typeface parser accepts it.
     */
    public static void download(Context context, Font font) throws IOException {
        File target = fontFile(context, font);
        File temp = new File(target.getParentFile(), font.id + ".ttf.part");
        if (temp.exists() && !temp.delete()) {
            throw new IOException("Could not clear the previous download");
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(font.url).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Font server returned HTTP " + status);
            }
            long total = 0;
            byte[] buffer = new byte[16 * 1024];
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(temp)) {
                int read;
                while ((read = in.read(buffer)) > 0) {
                    total += read;
                    if (total > MAX_BYTES) throw new IOException("Font file is too large");
                    out.write(buffer, 0, read);
                }
                out.getFD().sync();
            }
            if (total < 1024) throw new IOException("Downloaded font is empty");
            try {
                Typeface.createFromFile(temp);
            } catch (RuntimeException e) {
                throw new IOException("Downloaded file is not a readable font", e);
            }
            if (target.exists() && !target.delete()) {
                throw new IOException("Could not replace the installed font");
            }
            if (!temp.renameTo(target)) {
                throw new IOException("Could not save the downloaded font");
            }
            synchronized (TYPE_CACHE) {
                TYPE_CACHE.remove(indexOf(font));
            }
        } finally {
            if (connection != null) connection.disconnect();
            if (temp.exists()) temp.delete();
        }
    }

    public static void remove(Context context, Font font) {
        File file = fontFile(context, font);
        if (file.exists()) file.delete();
        File temp = new File(file.getParentFile(), font.id + ".ttf.part");
        if (temp.exists()) temp.delete();
        synchronized (TYPE_CACHE) {
            TYPE_CACHE.remove(indexOf(font));
        }
    }

    public static void clearTypefaceCache() {
        synchronized (TYPE_CACHE) {
            TYPE_CACHE.clear();
        }
    }
}
