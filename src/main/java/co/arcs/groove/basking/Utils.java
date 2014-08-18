package co.arcs.groove.basking;

import com.google.common.base.CharMatcher;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.arcs.groove.thresher.Song;

public class Utils {

    private static final Pattern ID_PATTERN = Pattern.compile("gs(\\d+)");
    private static final CharMatcher BLACKLIST = CharMatcher.anyOf("\\/:*?\"<>|");

    /**
     * Value returned when {@link #decodeId(java.io.File)} cannot find an ID.
     */
    public static final int ID_NONE = -1;

    public static String getDiskName(Song song) {
        String s = song.getArtistName() + " - " + song.getName();
        s = BLACKLIST.removeFrom(s);
        s = CharMatcher.WHITESPACE.trimAndCollapseFrom(s, ' ');
        return s + SyncOperation.FINISHED_FILE_EXTENSION;
    }

    /**
     * Find the song ID associated with a file.
     *
     * @return The song ID, or {@link #ID_NONE}.
     */
    public static int decodeId(File file) {
        Mp3File mp3File;
        try {
            mp3File = new Mp3File(file.getAbsolutePath());
            if (mp3File.hasId3v2Tag()) {
                String encoder = mp3File.getId3v2Tag().getEncoder();
                if (encoder != null) {
                    Matcher matcher = ID_PATTERN.matcher(encoder);
                    if (matcher.matches()) {
                        return Integer.parseInt(matcher.group(1));
                    }
                }
            }
        } catch (UnsupportedTagException e) {
            e.printStackTrace();
        } catch (InvalidDataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ID_NONE;
    }

    public static void encodeId(int id, ID3v2 tag) {
        tag.setEncoder("gs" + id);
    }
}
