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
	
	public void AddFailedParse(String comment){
		failParses.Push(comment);
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
				}
				bw.write((i + 1) + ". (" + tempSong.getPoints() + " points) " + tempSong.getSongName() + " - " + tempSong.getArtistName());
				bw.newLine();
			}
			bw.close();
			
			// Writing failed parses
			bw = new BufferedWriter(new FileWriter(savePath + "/Failed Comments.txt"));
			for (int i = 0; i < failParses.getSize(); i++){
				bw.write("~~~~~ Comment " + (i + 1) + "~~~~~");// + failParses.GetValueAt(i));
				bw.newLine();
				String[] lines = failParses.GetValueAt(i).split("\n");
				for (int j = 0; j < lines.length; j++){
					bw.write(lines[j]);
					bw.newLine();
				}
				bw.newLine();
			}
			bw.close();
		}catch(IOException e){ e.printStackTrace(); }
	}
	
	public List<ChartSong> getChartSongs(){ return this.chartSongs; }
	public List<String> getFailParses(){ return this.failParses; }
}