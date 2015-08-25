package mainPackage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import com.r4studios.DataStructures.List;

public class Chart {
	
	private List<ChartSong> chartSongs;
	private List<String> failParses;
	private String savePath;
	
	public Chart(String save){
		chartSongs = new List<ChartSong>();
		failParses = new List<String>();
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
		this.chartSongs.Push(song);
	}
	
	public void AddFailedParse(String comment){
		failParses.Push(comment);
	}
	
	// Creates the two .txt files from parsed comments
	public void CreateChart(){
		this.chartSongs.QuickSort();
		try{
			// Writing chart
			BufferedWriter bw = new BufferedWriter(new FileWriter(savePath + "/Top 50 Chart.txt"));
			for (int i = 0; i < chartSongs.getSize(); i++){
				ChartSong tempSong = chartSongs.GetValueAt(i);
				bw.write((i + 1) + ". (" + tempSong.getPoints() + " points) " + tempSong.getSongName() + " - " + tempSong.getArtistName());
				bw.newLine();
			}
			bw.close();
			
			// Writing failed parses
			bw = new BufferedWriter(new FileWriter(savePath + "/Failed Comments.txt"));
			for (int i = 0; i < failParses.getSize(); i++){
				bw.write((i + 1) + failParses.GetValueAt(i));
				bw.newLine();
			}
			bw.close();
		}catch(IOException e){ e.printStackTrace(); }
	}
	
	public List<ChartSong> getChartSongs(){ return this.chartSongs; }
	public List<String> getFailParses(){ return this.failParses; }
}