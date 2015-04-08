package oasis;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;

import mdexplorer.Action;
import mdexplorer.Robot;
import mdexplorer.Scenario.Prototype;
import mdexplorer.Trigger;

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
	public static class StoreTrackListAction implements Action {
		
		public StoreTrackListAction() {}
		
		@Override
		public StoreTrackListAction load(Node node) {
			// No setup needed
			return new StoreTrackListAction();
		}

		@Override
		public void execute(Robot r, String user, Map<String, Object> vars) {
			System.out.println("XXXXXXXXXXXXXXX Storing track list for " + vars.get("singer") + "'s " + vars.get("album"));
			System.out.println(vars.get("tracklist"));
		}
		
		
	}
	
	/**
	 * Trigger that tries to parse a track name and updates the static map
	 * to show that has been played.
	 */
	public static class GetTrackNameTrigger implements Trigger {

		@Override
		public Prototype load(Node node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean triggeredBy(String input, Map<String, Object> vars) {
			// TODO Auto-generated method stub
			return false;
		}
		
	}

	/**
	 * Trigger that parses when track is done playing and, if done, whether all tracks
	 * on this record has been played.
	 */
	public static class ListenedAllTracksTrigger implements Trigger {

		@Override
		public Prototype load(Node node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean triggeredBy(String input, Map<String, Object> vars) {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
