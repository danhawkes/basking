package co.arcs.groove.basking;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.arcs.groove.thresher.Song;

import com.google.common.base.CharMatcher;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

public class Utils {

	private static final Pattern ID_PATTERN = Pattern.compile("gs(\\d+)");
	private static final CharMatcher BLACKLIST = CharMatcher.anyOf("\\/:*?\"<>|");

	public static String getDiskName(Song song) {
		String s = song.artistName + " - " + song.name;
		s = BLACKLIST.removeFrom(s);
		s = CharMatcher.WHITESPACE.trimAndCollapseFrom(s, ' ');
		return s + SyncService.FINISHED_FILE_EXTENSION;
	}

	public static long decodeId(File file) {
		Mp3File mp3File;
		try {
			mp3File = new Mp3File(file.getAbsolutePath());
			if (mp3File.hasId3v2Tag()) {
				String encoder = mp3File.getId3v2Tag().getEncoder();
				if (encoder != null) {
					Matcher matcher = ID_PATTERN.matcher(encoder);
					if (matcher.matches()) {
						return Long.parseLong(matcher.group(1));
					}
				}
			}
		} catch (UnsupportedTagException e) {
		} catch (InvalidDataException e) {
		} catch (IOException e) {
		}
		return -1;
	}

	public static void encodeId(long id, ID3v2 tag) {
		tag.setEncoder("gs" + id);
	}
}