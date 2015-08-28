package mainPackage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import com.r4studios.DataStructures.List;

public class Chart {
	
	private List<ChartSong> chartSongs;
	private List<String> failParses;
	private List<String> combinedCloseMatches;
	private List<Integer> combinedClosePts;
	private List<Integer> failParseLines;
	private String savePath;
	private final float SIMILARITY_INDEX = (float)0.75;
	
	public Chart(String save){
		this.chartSongs = new List<ChartSong>();
		this.failParses = new List<String>();
		this.combinedCloseMatches = new List<String>();
		this.combinedClosePts = new List<Integer>();
		this.failParseLines = new List<Integer>();
		this.savePath = save;
	}
	
	public void AddValue(String song, String artist, int points){
		ChartSong tempSong = new ChartSong(song, artist, points);
		int index = chartSongs.GetIndexOf(tempSong);
		if (index == -1){		// ChartSong is not in the list
			chartSongs.Push(tempSong);
		}else{
			chartSongs.GetValueAt(index).AddPoints(points);
		}
	}
	
	public void AddValue(ChartSong song){
		int index = -1;
		for (int i = 0; i < this.chartSongs.getSize(); i++){
			if (song.isEqual(this.chartSongs.GetValueAt(i))){
				index = i;
				break;
			}
		}
		if (index == -1){		// ChartSong is not in the list
			System.out.println("Song \"" + song.getSongName() + "\" is not in the Chart");
			chartSongs.Push(song);
		}else{
			System.out.println("Adding points for song \"" + song.getSongName() + "\" (+" + song.getPoints() + " points)");
			chartSongs.GetValueAt(index).AddPoints(song.getPoints());
		}
		
	}
	
	// Splits comments by -2 to know when the comments end
	public void AddFailedParse(String comment, ArrayList<Integer> line){
		failParses.Push(comment);
		for (int i = 0; i < line.size(); i++){
			failParseLines.Push(line.get(i));
		}
		failParseLines.Push(-2);
		
	}
	
	// Processes chart to improve and amalgamate results
	public void ProcessChart(){
		// Checks all songs for reversed values (song is artist, artist is song; if so it combines their points and removes the duplicate
		for (int i = 0; i < this.chartSongs.getSize() - 1; i++){
			for (int j = i + 1; j < this.chartSongs.getSize(); j++){
				ChartSong s1 = this.chartSongs.GetValueAt(i);
				ChartSong s2 = this.chartSongs.GetValueAt(j);
				String song1 = s1.getSongName();
				String song2 = s2.getSongName();
				String artist1 = s1.getArtistName();
				String artist2 = s2.getArtistName();
				if (artist1.equals(song2) && song1.equals(artist2)){
					this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
					this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
					this.combinedClosePts.Push(s1.getPoints());
					this.combinedClosePts.Push(s2.getPoints());
					int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
					int minIndex = (maxIndex == i) ? j : i;
					this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
					this.chartSongs.RemoveAt(minIndex);
				}else if (AreLikelyTheSame(this.chartSongs.GetValueAt(i), this.chartSongs.GetValueAt(j))){
					int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
					int minIndex = (maxIndex == i) ? j : i;
					this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
					this.chartSongs.RemoveAt(minIndex);
				}else if ((song1.equals(song2) && (artist1.contains(artist2) || artist2.contains(artist1))) ||	// If one is correct and the other is close
						(artist1.equals(artist2) && (song1.contains(song2) || song2.contains(song1)))){
					System.out.println("~~~~~~ Partial match merge ~~~~~~");
					System.out.println(song1 + " <-> " + song2 + ", " + artist1 + " <-> " + artist2);
					this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
					this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
					this.combinedClosePts.Push(s1.getPoints());
					this.combinedClosePts.Push(s2.getPoints());
					int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
					int minIndex = (maxIndex == i) ? j : i;
					this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
					this.chartSongs.RemoveAt(minIndex);
				}else if ((song1.equals(artist2) && (song2.contains(artist1) || artist1.contains(song2))) ||
						(song2.equals(artist1) && (song1.contains(artist2) || artist2.contains(song1)))){
					System.out.println("~~~~~~ Partial opposite merge ~~~~~~");
					System.out.println(song1 + " <-> " + artist2 + ", " + artist1 + " <-> " + song2);
					this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
					this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
					this.combinedClosePts.Push(s1.getPoints());
					this.combinedClosePts.Push(s2.getPoints());
					int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
					int minIndex = (maxIndex == i) ? j : i;
					this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
					this.chartSongs.RemoveAt(minIndex);
				}else if ((song1.contains(song2) || song2.contains(song1)) && (artist1.contains("snsd") || artist1.contains("generation")) && (artist2.contains("snsd") || artist2.contains("generation"))){
					System.out.println("~~~~~~ Partial matched SNSD merge ~~~~~~");
					System.out.println(song1 + " <-> " + artist2 + ", " + artist1 + " <-> " + song2);
					this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
					this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
					this.combinedClosePts.Push(s1.getPoints());
					this.combinedClosePts.Push(s2.getPoints());
					int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
					int minIndex = (maxIndex == i) ? j : i;
					this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
					this.chartSongs.RemoveAt(minIndex);
				}else if ((artist1.contains(artist2) || artist2.contains(artist1)) && (song1.contains("snsd") || song1.contains("generation")) && (song2.contains("snsd") || song2.contains("generation"))){
					System.out.println("~~~~~~ Partial matched SNSD merge ~~~~~~");
					System.out.println(song1 + " <-> " + artist2 + ", " + artist1 + " <-> " + song2);
					this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
					this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
					this.combinedClosePts.Push(s1.getPoints());
					this.combinedClosePts.Push(s2.getPoints());
					int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
					int minIndex = (maxIndex == i) ? j : i;
					this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
					this.chartSongs.RemoveAt(minIndex);
				}else if ((song1.contains(artist2) || artist2.contains(song1)) && (artist1.contains("snsd") || artist1.contains("generation")) && (song2.contains("snsd") || song2.contains("generation"))){
					System.out.println("~~~~~~ Partial matched SNSD merge ~~~~~~");
					System.out.println(song1 + " <-> " + artist2 + ", " + artist1 + " <-> " + song2);
					this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
					this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
					this.combinedClosePts.Push(s1.getPoints());
					this.combinedClosePts.Push(s2.getPoints());
					int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
					int minIndex = (maxIndex == i) ? j : i;
					this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
					this.chartSongs.RemoveAt(minIndex);
				}else if ((song2.contains(artist1) || artist1.contains(song2)) && (artist2.contains("snsd") || artist2.contains("generation")) && (song1.contains("snsd") || song1.contains("generation"))){
					System.out.println("~~~~~~ Partial matched SNSD merge ~~~~~~");
					System.out.println(song1 + " <-> " + artist2 + ", " + artist1 + " <-> " + song2);
					this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
					this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
					this.combinedClosePts.Push(s1.getPoints());
					this.combinedClosePts.Push(s2.getPoints());
					int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
					int minIndex = (maxIndex == i) ? j : i;
					this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
					this.chartSongs.RemoveAt(minIndex);
				}
			}
		}
	}
	
	// Creates the two .txt files from parsed comments
	public void CreateChart(){
		this.chartSongs = this.chartSongs.QuickSort();
		try{
			// Writing chart
			BufferedWriter bw = new BufferedWriter(new FileWriter(savePath + "/Top 50 Chart.txt"));
			for (int i = 0; i < chartSongs.getSize(); i++){
				ChartSong tempSong = chartSongs.GetValueAt(i);
				if (i == 50){
					bw.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
					bw.newLine();
				}
				bw.write((i + 1) + ". (" + tempSong.getPoints() + " points) " + tempSong.getSongName() + " - " + tempSong.getArtistName());
				bw.newLine();
			}
			bw.close();
			
			// Writing failed parses
			bw = new BufferedWriter(new FileWriter(savePath + "/Failed Comments.txt"));
			for (int i = 0; i < failParses.getSize(); i++){
				bw.write("~~~~~ Comment " + (i + 1) + "~~~~~ (Failed on line(s) marked with ***)");
				bw.newLine();
				String[] lines = failParses.GetValueAt(i).split("\n");
				for (int j = 0; j < lines.length; j++){
					if (j == failParseLines.GetValueAt(0)){	// Formats failed lines
						bw.write("*** " + lines[j]);
						failParseLines.RemoveAt(0);
					}else{
						bw.write(lines[j]);
					}
					bw.newLine();
				}
				if (failParseLines.GetValueAt(0) == -2){	// Preparing for the next comment's failed lines
					failParseLines.RemoveAt(0);
				}
				bw.newLine();
			}
			bw.close();
			
			// Writing combines
			bw = new BufferedWriter(new FileWriter(savePath + "/Merged Chart Songs.txt"));
			for (int i = 0; i < this.combinedCloseMatches.getSize(); i++){
				bw.write("~~~~~~~ Merge " + (i / 2) + " ~~~~~~~");
				bw.newLine();
				bw.write("Song 1: " + combinedCloseMatches.GetValueAt(i) + " (" + combinedClosePts.GetValueAt(i) + " points)");
				bw.newLine();
				bw.write("Song 2: " + combinedCloseMatches.GetValueAt(i + 1) + " (" + combinedClosePts.GetValueAt(i + 1) + " points)");
				bw.newLine();
				bw.newLine();
				i++;
			}
			bw.close();
		}catch(IOException e){ e.printStackTrace(); }
	}
	
	// Returns true if two strings are so similar that they are likely the same
	// Check matched, and opposite song to artist
	private boolean AreLikelyTheSame(ChartSong s1, ChartSong s2){
		float songSimilarity = 0;
		float artistSimilarity = 0;
		HashMap<Character,Integer> song1 = CreateCharMap(s1, true);
		HashMap<Character,Integer> song2 = CreateCharMap(s2, true);
		HashMap<Character,Integer> art1 = CreateCharMap(s1, false);
		HashMap<Character,Integer> art2 = CreateCharMap(s2, false);
		List<Integer> songDifferences = new List<Integer>();
		List<Integer> artistDifferences = new List<Integer>();
		
		for (char key : song1.keySet()){	// Creates song differences
			if (song2.keySet().contains(key)){
				songDifferences.Push(Math.abs(song1.get(key) - song2.get(key)));
				song2.remove(key);
			}else{
				songDifferences.Push(song1.get(key));
			}
		}
		for (char key : song2.keySet()){
			songDifferences.Push(song2.get(key));
		}
		for (char key : art1.keySet()){		// Creates artist differences
			if (art2.keySet().contains(key)){
				artistDifferences.Push(Math.abs(art1.get(key) - art2.get(key)));
				art2.remove(key);
			}else{
				artistDifferences.Push(art1.get(key));
			}
		}
		for (char key : art2.keySet()){
			artistDifferences.Push(art2.get(key));
		}
		
		int maxSongLength = Math.max(song1.size(),song2.size());
		int maxArtLength = Math.max(art1.size(),art2.size());
		int songDiffLength = Math.abs(song1.size() - song2.size());
		int artDiffLength = Math.abs(art1.size() - art2.size());
		float songAverage = Average(songDifferences);
		float artistAverage = Average(artistDifferences);
		float maxSongDiff = songDifferences.FindMaxValue();
		float maxArtDiff = artistDifferences.FindMaxValue();
		songSimilarity = (1 - songAverage / maxSongLength) * (1 - (float)songDiffLength / maxSongLength) * (1 - (float)maxSongDiff / maxSongLength);
		artistSimilarity = (1 - artistAverage / maxSongLength) * (1 - (float)artDiffLength / maxArtLength) * (1 - (float)maxArtDiff / maxArtLength);
		/*if (songSimilarity > 0.5 || artistSimilarity > 0.5){
			System.out.println("``````````````````````````````````````````````");
			System.out.println("Close song: " + s1.getSongName() + " & " + s2.getSongName());
			System.out.println("Close artist: " + s1.getArtistName() + " & " + s2.getArtistName());
			System.out.println(maxSongLength + "," + maxArtLength + "," + songDiffLength + "," + artDiffLength + "," + songAverage + "," +artistAverage + "," + maxSongDiff + "," + maxArtDiff);
			System.out.println("Matched Similarity: " + songSimilarity + " (songs), " + artistSimilarity + " (artists)");
		}*/
		if (songSimilarity > SIMILARITY_INDEX && artistSimilarity > SIMILARITY_INDEX){
			this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
			this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
			this.combinedClosePts.Push(s1.getPoints());
			this.combinedClosePts.Push(s2.getPoints());
			return true;
		}else{	// Checks song1 vs artist2, in case one is reversed
			song1 = CreateCharMap(s1, true);
			song2 = CreateCharMap(s2, true);
			art1 = CreateCharMap(s1, false);
			art2 = CreateCharMap(s2, false);
			songDifferences = new List<Integer>();
			artistDifferences = new List<Integer>();
			
			for (char key : song1.keySet()){	// Creates song differences
				if (art2.keySet().contains(key)){
					songDifferences.Push(Math.abs(song1.get(key) - art2.get(key)));
					art2.remove(key);
				}else{
					songDifferences.Push(song1.get(key));
				}
			}
			for (char key : art2.keySet()){
				songDifferences.Push(art2.get(key));
			}
			for (char key : art1.keySet()){		// Creates artist differences
				if (song2.keySet().contains(key)){
					artistDifferences.Push(Math.abs(art1.get(key) - song2.get(key)));
					song2.remove(key);
				}else{
					artistDifferences.Push(art1.get(key));
				}
			}
			for (char key : song2.keySet()){
				artistDifferences.Push(song2.get(key));
			}
			
			maxSongLength = Math.max(song1.size(),art2.size());
			maxArtLength = Math.max(art1.size(),song2.size());
			songDiffLength = Math.abs(song1.size() - art2.size());
			artDiffLength = Math.abs(art1.size() - song2.size());
			songAverage = Average(songDifferences);
			artistAverage = Average(artistDifferences);
			maxSongDiff = songDifferences.FindMaxValue();
			maxArtDiff = artistDifferences.FindMaxValue();
			songSimilarity = (1 - songAverage / maxSongLength) * (1 - (float)songDiffLength / maxSongLength) * (1 - (float)maxSongDiff / maxSongLength);
			artistSimilarity = (1 - artistAverage / maxSongLength) * (1 - (float)artDiffLength / maxArtLength) * (1 - (float)maxArtDiff / maxArtLength);
			/*if (songSimilarity > 0.5 || artistSimilarity > 0.5){
				System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
				System.out.println("Close song: " + s1.getSongName() + " & " + s2.getArtistName());
				System.out.println("Close artist: " + s1.getArtistName() + " & " + s2.getSongName());
				System.out.println(maxSongLength + "," + maxArtLength + "," + songDiffLength + "," + artDiffLength + "," + songAverage + "," +artistAverage + "," + maxSongDiff + "," + maxArtDiff);
				System.out.println("Opposite Similarity: " + songSimilarity + " (songs), " + artistSimilarity + " (artists)");
			}*/
			if (songSimilarity > SIMILARITY_INDEX && artistSimilarity > SIMILARITY_INDEX){
				this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
				this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
				this.combinedClosePts.Push(s1.getPoints());
				this.combinedClosePts.Push(s2.getPoints());
				return true;
			}else{
				return false;
			}
		}
	}
	
	private float Average(List<Integer> nums){
		int sum = 0;
		for (int i = 0; i < nums.getSize(); i++){
			sum += nums.GetValueAt(i);
		}
		return (float)sum / nums.getSize(); 
	}
	
	private HashMap<Character,Integer> CreateCharMap(ChartSong song, boolean isSong){
		HashMap<Character,Integer> newMap = new HashMap<Character,Integer>();
		if (isSong){
			for (int i = 0; i < song.getSongName().length(); i++){
				char tempChar = song.getSongName().charAt(i);
				if (newMap.keySet().contains(tempChar)){
					newMap.put(tempChar, newMap.get(tempChar) + 1);
				}else{
					newMap.put(tempChar, 1);
				}
			}
			return newMap;
		}else{
			for (int i = 0; i < song.getArtistName().length(); i++){
				char tempChar = song.getArtistName().charAt(i);
				if (newMap.keySet().contains(tempChar)){
					newMap.put(tempChar, newMap.get(tempChar) + 1);
				}else{
					newMap.put(tempChar, 1);
				}
			}
			return newMap;
		}
	}
	
	public List<ChartSong> getChartSongs(){ return this.chartSongs; }
	public List<String> getFailParses(){ return this.failParses; }
}