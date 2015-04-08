package oasis;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Triggers for iterating through all the tracks in all the records at Roccos.
 * 
 * NOTE: This class is not thread safe.
 * It assumes there is only one record reading scenario running at any one time
 * (otherwise we'd need some injection mechanism so the triggers could talk to each other)
 */
public class RecordReader {

	private static List<String> trackList = null;
	
	/**
	 * Trigger that tries to parse out the track list
	 * for a record.  Stores that in a static map.
	 */
	public static class GetTrackListTrigger {
		Pattern firstLineRegex = Pattern.compile("This is the .* album by .*, released in \\d{4}\\.");
		Pattern secondLineRegex = Pattern.compile("Tracks:");
		
	}
	
	/**
	 * Trigger that tries to parse a track name and updates the static map
	 * to show that has been played.
	 */
	public static class GetTrackNameTrigger {
		
	}

	/**
	 * Trigger that parses when track is done playing and, if done, whether all tracks
	 * on this record has been played.
	 */
	public static class ListenedAllTracksTrigger {
		
	}

	public static void main(String[] args) {
		String testString = "This is the The Joshua Tree album by U2, released in 1987.\n" +
				"\n" +
				"Tracks:\n" +
				"\n" +
				"1. Where the Streets Have No Name\n" +
				"2. I Still Haven't Found What I'm Looking For\n" +
				"3. With or Without You\n" +
				"4. Bullet the Blue Sky\n" +
				"5. Running to Stand Still\n" +
				"6. Red Hill Mining Town\n" +
				"7. In God's Country\n" +
				"8. Trip Through Your Wires\n" +
				"9. One Tree Hill\n" +
				"10. Exit\n" +
				"11. Mothers of the Disappeared\n" +
				"\n" +
				"hp: 390/390  mp: 1000/1000  sp: 456/456 > ";
	}
}
