package mainPackage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.r4studios.DataStructures.List;

public class Chart {

	private List<ChartSong> chartSongs;
	private List<String> failParses;
	private List<String> combinedCloseMatches;
	private List<Integer> combinedClosePts;
	private List<Integer> failParseLines;
	private final String[] SNSD_NAMES = new String[]{"girls' generation", "girls generation", "girl's generation", "snsd"};
	
	public Chart(){
		this.chartSongs = new List<ChartSong>();
		this.failParses = new List<String>();
		this.combinedCloseMatches = new List<String>();
		this.combinedClosePts = new List<Integer>();
		this.failParseLines = new List<Integer>();
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
	public void AddFailedParse(String[] comment, ArrayList<Integer> line){
		String tempString = "";
		for (int i = 0; i < comment.length - 1; i++){
			tempString += (comment[i] + "\n");
		}
		tempString += comment[comment.length - 1];
		if (tempString.substring(tempString.length() - 1).equals("?")){
			failParses.Push(tempString.substring(0, tempString.length() - 1));
		}else{
			failParses.Push(tempString);
		}
		for (int i = 0; i < line.size(); i++){
			failParseLines.Push(line.get(i));
		}
		failParseLines.Push(-114);
	}
	
	// Processes chart to improve and amalgamate results
	public void ProcessChart(JFrame parent){
		// Checks all songs for reversed values (song is artist, artist is song; if so it combines their points and removes the duplicate
		for (int k = 0; k < 5; k++){	// Processes 3 times to be sure to catch outliers
			for (int i = 0; i < this.chartSongs.getSize() - 1; i++){
				parent.setTitle("Processing Chart... (" + (int)((float)(i + 1) / chartSongs.getSize() * 20 * (k + 1)) + "%)");
				for (int j = i + 1; j < this.chartSongs.getSize(); j++){
					ChartSong s1 = this.chartSongs.GetValueAt(i);
					ChartSong s2 = this.chartSongs.GetValueAt(j);
					String song1 = s1.getSongName();
					String song2 = s2.getSongName();
					String artist1 = s1.getArtistName();
					String artist2 = s2.getArtistName();
					int comparison = AreStringsConnected(song1, song2, artist1, artist2);
					if (comparison >= 0){
						this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
						this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
						this.combinedClosePts.Push(s1.getPoints());
						this.combinedClosePts.Push(s2.getPoints());
						int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
						int minIndex = (maxIndex == i) ? j : i;
						this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
						this.chartSongs.RemoveAt(minIndex);
					}else if ((SameCoreString(song1, song2) && (IsGGName(artist1) && IsGGName(artist2))) || (SameCoreString(song1, artist2) && (IsGGName(artist1) && IsGGName(song2)))){
						this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
						this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
						this.combinedClosePts.Push(s1.getPoints());
						this.combinedClosePts.Push(s2.getPoints());
						this.chartSongs.GetValueAt(i).setArtistName("girls generation");
						this.chartSongs.GetValueAt(i).AddPoints(this.chartSongs.GetValueAt(j).getPoints());
						this.chartSongs.RemoveAt(j);
					}else if ((SameCoreString(artist1, artist2) && (IsGGName(song1) && IsGGName(song2))) || (SameCoreString(song2, artist1) && (IsGGName(artist2) && IsGGName(song1)))){
						this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
						this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
						this.combinedClosePts.Push(s1.getPoints());
						this.combinedClosePts.Push(s2.getPoints());
						this.chartSongs.GetValueAt(i).setSongName("girls generation");
						this.chartSongs.GetValueAt(i).AddPoints(this.chartSongs.GetValueAt(j).getPoints());
						this.chartSongs.RemoveAt(j);
					}else if ((SameChars(song1,song2) && SameCoreString(artist1, artist2)) || (SameChars(artist1,artist2) && SameCoreString(song1, song2)) ||
							(SameChars(song1,artist2) && SameCoreString(song2, artist1)) || (SameChars(artist1,song2) && SameCoreString(artist2, song1))){		// Checks if one set is the same without spaces
						this.combinedCloseMatches.Push(s1.getSongName() + " - " + s1.getArtistName());
						this.combinedCloseMatches.Push(s2.getSongName() + " - " + s2.getArtistName());
						this.combinedClosePts.Push(s1.getPoints());
						this.combinedClosePts.Push(s2.getPoints());
						int maxIndex = (this.chartSongs.GetValueAt(i).getPoints() > this.chartSongs.GetValueAt(j).getPoints()) ? i : j;
						int minIndex = (maxIndex == i) ? j : i;
						this.chartSongs.GetValueAt(maxIndex).AddPoints(this.chartSongs.GetValueAt(minIndex).getPoints());
						this.chartSongs.RemoveAt(minIndex);
					}else if (CombinedContainsBoth(song1, song2, artist1, artist2)){
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
	}
	
	// Creates the three.txt files from parsed comments
	public void CreateChart(String path){
		this.chartSongs = this.chartSongs.QuickSort();
		BufferedWriter bw;
		try{
			// Writing chart
			bw = new BufferedWriter(new FileWriter(path + "/Top 50 Chart.txt"));
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
		}catch(IOException e){ JOptionPane.showMessageDialog(null, "Could not write the file \"Top 50 Chart.txt\". Directory: \"" + path + "\"", "File Write Error", JOptionPane.ERROR_MESSAGE); }
		try{
			// Writing failed parses
			bw = new BufferedWriter(new FileWriter(path + "/Failed Comments.txt"));
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
				if (failParseLines.GetValueAt(0) == -114){	// Preparing for the next comment's failed lines
					failParseLines.RemoveAt(0);
				}
				bw.newLine();
			}
			bw.close();
		}catch(IOException e){ JOptionPane.showMessageDialog(null, "Could not write the file \"Failed Comments.txt\". Directory: \"" + path + "\"", "File Write Error", JOptionPane.ERROR_MESSAGE); }
		try{
			// Writing combines
			bw = new BufferedWriter(new FileWriter(path + "/Merged Chart Songs.txt"));
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
		}catch(IOException e){ JOptionPane.showMessageDialog(null, "Could not write the file \"Merged Chart Songs.txt\". Directory: \"" + path + "\"", "File Write Error", JOptionPane.ERROR_MESSAGE); }
	}
	
	// Checks if a string is a possible GG name
	private boolean IsGGName(String name){
		for (int i = 0; i < SNSD_NAMES.length; i++){
			if (name.contains(SNSD_NAMES[i]) || name.equals(SNSD_NAMES[i]) || (name.contains("girl") && name.contains("generation"))){
				return true;
			}
		}
		return false;
	}
	
	// Removes all spaces from a string
	private String RemoveChars(String str){
		String newString = str;
		int i = 0;
		while (i < newString.length()){
			if (newString.charAt(i) == ' ' || newString.charAt(i) == '-' || newString.charAt(i) == '�'){
				if (i == 0){
					newString = newString.substring(1);
				}else if (i == str.length() - 1){
					newString = newString.substring(0, newString.length() - 1);
				}else{
					newString = newString.substring(0, i) + newString.substring(i + 1, newString.length());
				}
			}else{
				i++;
			}
		}
		return newString;
	}
	
	// Returns true if the strings are equal or if one contains the other
	private boolean SameCoreString(String s1, String s2){
		return (s1.equals(s2) || (s1.contains(s2) && s2.length() > 2) || (s2.contains(s1) && s1.length() > 2));
	}
	
	// Returns 1 if the two items can be combined normally, 0 if they can be combined oppositely,
	// and -1 if they cannot be combined
	private int AreStringsConnected(String s1, String s2, String a1, String a2){
		if (SameCoreString(s1,s2) && SameCoreString(a1,a2)){
			//System.out.println("1) " + s1 + " - " + a1 + " & " + s2 + " - " + a2);
			return 1;
		}else if (SameCoreString(s1,a2) && SameCoreString(s2,a1)){
			//System.out.println("2) " + s1 + " - " + a1 + " & " + s2 + " - " + a2);
			return 0;
		}else if (SameCoreString(s1,s2) && SameWithTypo(a1,a2)){
			//System.out.println("3) " + s1 + " - " + a1 + " & " + s2 + " - " + a2);
			return 1;
		}else if (SameCoreString(s1,a2) && SameWithTypo(s2,a1)){
			//System.out.println("4) " + s1 + " - " + a1 + " & " + s2 + " - " + a2);
			return 0;
		}else if (SameCoreString(a1,a2) && SameWithTypo(s1,s2)){
			//System.out.println("5) " + s1 + " - " + a1 + " & " + s2 + " - " + a2);
			return 1;
		}else if (SameCoreString(s2,a1) && SameWithTypo(a2,s1)){
			//System.out.println("6) " + s1 + " - " + a1 + " & " + s2 + " - " + a2);
			return 0;
		}else{
			return -1;
		}
	}
	
	// Returns true if one string is mostly a substring of the other
	private boolean SameWithTypo(String str1, String str2){
		int biggestSubstring = 0;
		double proportion1 = 0, proportion2 = 0;
		String biggerString = (str1.length() > str2.length()) ? str1 : str2 ;
		String smallerString = (str1.length() > str2.length()) ? str2 : str1 ;
		int diffInLength = biggerString.length() - smallerString.length();
		if (smallerString.length() > 2 && (diffInLength / (double)smallerString.length()) < 1){	// Do not combine very short strings or ones that are very different in length; they'll get picked up by the combined feature
			for (int i = 0; i < smallerString.length(); i++){
				for (int j = 0; j < smallerString.length() - i; j++){
					String subString = smallerString.substring(j, j + i + 1);
					if (biggerString.contains(subString)){
						biggestSubstring = subString.length();
						break;
					}
				}
			}
			proportion1 = biggestSubstring / (double)smallerString.length();
		
			for (int i = 0; i < smallerString.length(); i++){
				for (int j = 0; j < biggerString.length() - i; j++){
					String subString = biggerString.substring(j, j + i + 1);
					if (smallerString.contains(subString)){
						biggestSubstring = subString.length();
						break;
					}
				}
			}
			proportion2 = biggestSubstring / smallerString.length();
		}
		return Math.max(proportion1, proportion2) > 0.6;
	}
	
	// Returns true if the two strings have nearly the same character sequence without spaces
	private boolean SameChars(String s1, String s2){
		String new1 = RemoveChars(s1);
		String new2 = RemoveChars(s2);
		if (new1.equals(new2) || new1.contains(new2) || new2.contains(new1)){
			return true;
		}else{
			return false;
		}
	}
	
	// Returns true if one of the combined strings contains both of the other strings
	private boolean CombinedContainsBoth(String s1, String s2, String a1, String a2){
		String combined = s1 + a1;
		if (combined.contains(s2) && combined.contains(a2)){
			return true;
		}
		combined = s2 + a2;
		if (combined.contains(s1) && combined.contains(a1)){
			return true;
		}
		return false;
	}
	
	public void UpdateChartFile(String path){
		List<ChartSong> tempCS = new List<ChartSong>();
		List<String> afterInfo = new List<String>();
		boolean pastMain = false;
		try{
			BufferedReader br = new BufferedReader(new FileReader(path));
			String line = br.readLine();
			while (line != null){
				if (!pastMain){
					if (line.contains("~~~~~~~~~~") || line.length() < 1){
						line = br.readLine();
					}else if (line.toLowerCase().contains("stop sorting here")){
						pastMain = true;
						line = br.readLine();
					}else{
						String part = line.split(Pattern.quote(". ("))[1];
						String[] parts = part.split(Pattern.quote(" points) "));
						int num = Integer.parseInt(parts[0]);
						part = parts[1];
						parts = part.split(Pattern.quote(" - "));
						String tempS = parts[0];
						String tempA = parts[1];
						tempCS.Push(new ChartSong(tempS, tempA, num));
						line = br.readLine();
					}
				}else{
					if (line.length() < 1){
						afterInfo.Push("\n");
					}else{
						afterInfo.Push(line);
					}
					line = br.readLine();
				}
			}
			br.close();
			tempCS = tempCS.QuickSort();
			
			// Writing chart
			//Files.delete(new Path(savePath + "/Top 50 Chart.txt"));
			BufferedWriter bw = new BufferedWriter(new FileWriter(path));
			for (int i = 0; i < tempCS.getSize(); i++){
				ChartSong tempSong = tempCS.GetValueAt(i);
				if (i == 50){
					bw.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
					bw.newLine();
				}
				bw.write((i + 1) + ". (" + tempSong.getPoints() + " points) " + tempSong.getSongName() + " - " + tempSong.getArtistName());
				bw.newLine();
			}
			bw.newLine();
			for (int i = 0; i < afterInfo.getSize(); i++){
				bw.write(afterInfo.GetValueAt(i));
				bw.newLine();
			}
			bw.close();
		}catch (IOException e){ e.printStackTrace(); }	
	}
	
	public List<ChartSong> getChartSongs(){ return this.chartSongs; }
	public List<String> getFailParses(){ return this.failParses; }
}