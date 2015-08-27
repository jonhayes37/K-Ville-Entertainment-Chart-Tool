package mainPackage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.r4studios.DataStructures.List;

public class Chart {
	
	private List<ChartSong> chartSongs;
	private List<String> failParses;
	private List<Integer> failParseLines;
	private String savePath;
	
	public Chart(String save){
		this.chartSongs = new List<ChartSong>();
		this.failParses = new List<String>();
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
				if (this.chartSongs.GetValueAt(i).getArtistName().equals(this.chartSongs.GetValueAt(j).getSongName()) &&
						this.chartSongs.GetValueAt(i).getSongName().equals(this.chartSongs.GetValueAt(j).getArtistName())){
					this.chartSongs.GetValueAt(i).AddPoints(this.chartSongs.GetValueAt(j).getPoints());
					this.chartSongs.RemoveAt(j);
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
		}catch(IOException e){ e.printStackTrace(); }
	}
	
	public List<ChartSong> getChartSongs(){ return this.chartSongs; }
	public List<String> getFailParses(){ return this.failParses; }
}