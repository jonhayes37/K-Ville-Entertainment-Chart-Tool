package mainPackage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.r4studios.DataStructures.List;

public class Chart {
	
	private List<ChartSong> chartSongs;
	private List<String> failParses;
	private List<String> combinedCloseMatches;
	private List<Integer> combinedClosePts;
	private List<Integer> failParseLines;
	private String savePath;
	private final String[] SNSD_NAMES = new String[]{"girls' generation", "girls generation", "girl's generation", "gg", "snsd"};
	
	public Chart(){
		this.chartSongs = new List<ChartSong>();
		this.failParses = new List<String>();
		this.combinedCloseMatches = new List<String>();
		this.combinedClosePts = new List<Integer>();
		this.failParseLines = new List<Integer>();
		this.savePath = "";
	}
	
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
			chartSongs.Push(song);
		}else{
			chartSongs.GetValueAt(index).AddPoints(song.getPoints());
		}
		
	}
	
	// Splits comments by -2 to know when the comments end
	public void AddFailedParse(String comment, ArrayList<Integer> line){
		String tempComment = comment.substring(0, comment.length() - 1);
		failParses.Push(tempComment);
		for (int i = 0; i < line.size(); i++){
			failParseLines.Push(line.get(i));
		}
		failParseLines.Push(-2);
	}
	
	// Processes chart to improve and amalgamate results
	public void ProcessChart(){
		// Checks all songs for reversed values (song is artist, artist is song; if so it combines their points and removes the duplicate
		for (int h = 0; h < 5; h++){	// Processes five times, just to be safe
			for (int i = 0; i < this.chartSongs.getSize() - 1; i++){
				for (int j = i + 1; j < this.chartSongs.getSize(); j++){
					ChartSong s1 = this.chartSongs.GetValueAt(i);
					ChartSong s2 = this.chartSongs.GetValueAt(j);
					String song1 = s1.getSongName();
					String song2 = s2.getSongName();
					String artist1 = s1.getArtistName();
					String artist2 = s2.getArtistName();
					if (song1.equals(song2) && artist1.equals(artist2)){
						this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
						this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
						this.combinedClosePts.Push(s1.getPoints());
						this.combinedClosePts.Push(s2.getPoints());
						int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
						int minIndex = (maxIndex == i) ? j : i;
						this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
						this.chartSongs.RemoveAt(minIndex);
					}else if (artist1.equals(song2) && song1.equals(artist2)){
						this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
						this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
						this.combinedClosePts.Push(s1.getPoints());
						this.combinedClosePts.Push(s2.getPoints());
						int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
						int minIndex = (maxIndex == i) ? j : i;
						this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
						this.chartSongs.RemoveAt(minIndex);
					}else if ((song1.equals(song2) && (artist1.contains(artist2) || artist2.contains(artist1))) ||	// If one is correct and the other is close
							(artist1.equals(artist2) && (song1.contains(song2) || song2.contains(song1)))){
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
						this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
						this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
						this.combinedClosePts.Push(s1.getPoints());
						this.combinedClosePts.Push(s2.getPoints());
						int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
						int minIndex = (maxIndex == i) ? j : i;
						this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
						this.chartSongs.RemoveAt(minIndex);
					}else if ((song1.contains(song2) || song2.contains(song1)) && (IsGGName(artist1) && IsGGName(artist2))){
						this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
						this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
						this.combinedClosePts.Push(s1.getPoints());
						this.combinedClosePts.Push(s2.getPoints());
						this.chartSongs.GetValueAt(i).setArtistName("girls' generation");
						this.chartSongs.GetValueAt(i).AddPoints(this.chartSongs.GetValueAt(j).getPoints());
						this.chartSongs.RemoveAt(j);
					}else if ((artist1.contains(artist2) || artist2.contains(artist1)) && (IsGGName(song1) && IsGGName(song2))){
						this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
						this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
						this.combinedClosePts.Push(s1.getPoints());
						this.combinedClosePts.Push(s2.getPoints());
						this.chartSongs.GetValueAt(i).setSongName("girls' generation");
						this.chartSongs.GetValueAt(i).AddPoints(this.chartSongs.GetValueAt(j).getPoints());
						this.chartSongs.RemoveAt(j);
					}else if ((song1.contains(artist2) || artist2.contains(song1)) && (IsGGName(artist1) && IsGGName(song2))){
						this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
						this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
						this.combinedClosePts.Push(s1.getPoints());
						this.combinedClosePts.Push(s2.getPoints());
						this.chartSongs.GetValueAt(i).setArtistName("girls' generation");
						this.chartSongs.GetValueAt(i).AddPoints(this.chartSongs.GetValueAt(j).getPoints());
						this.chartSongs.RemoveAt(j);
					}else if ((song2.contains(artist1) || artist1.contains(song2)) && (IsGGName(artist2) && IsGGName(song1))){
						this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
						this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
						this.combinedClosePts.Push(s1.getPoints());
						this.combinedClosePts.Push(s2.getPoints());
						this.chartSongs.GetValueAt(i).setSongName("girls' generation");
						this.chartSongs.GetValueAt(i).AddPoints(this.chartSongs.GetValueAt(j).getPoints());
						this.chartSongs.RemoveAt(j);
					}
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
	
	private boolean IsGGName(String name){
		for (int i = 0; i < SNSD_NAMES.length; i++){
			if (name.contains(SNSD_NAMES[i]) || name.equals(SNSD_NAMES[i])){
				return true;
			}
		}
		return false;
	}
	
	public void UpdateChartFile(String path){
		List<ChartSong> tempCS = new List<ChartSong>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(path));
			String line = br.readLine();
			while (line != null){
				if (line.contains("~~~~~~~~~~")){
					line = br.readLine();
				}else{
					System.out.println("Line: " + line);
					String part = line.split(Pattern.quote(". ("))[1];
					System.out.println("Part: " + part);
					String[] parts = part.split(Pattern.quote(" points) "));
					int num = Integer.parseInt(parts[0]);
					part = parts[1];
					System.out.println("Part: " + part);
					parts = part.split(Pattern.quote(" - "));
					String tempS = parts[0];
					String tempA = parts[1];
					tempCS.Push(new ChartSong(tempS, tempA, num));
					line = br.readLine();
				}
			}
			br.close();
			tempCS = tempCS.QuickSort();
			
			// Writing chart
			BufferedWriter bw = new BufferedWriter(new FileWriter(savePath + "/Top 50 Chart.txt"));
			for (int i = 0; i < tempCS.getSize(); i++){
				ChartSong tempSong = tempCS.GetValueAt(i);
				if (i == 50){
					bw.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
					bw.newLine();
				}
				bw.write((i + 1) + ". (" + tempSong.getPoints() + " points) " + tempSong.getSongName() + " - " + tempSong.getArtistName());
				bw.newLine();
			}
			bw.close();
		}catch (IOException e){ e.printStackTrace(); }	
	}
	
	public List<ChartSong> getChartSongs(){ return this.chartSongs; }
	public List<String> getFailParses(){ return this.failParses; }
}