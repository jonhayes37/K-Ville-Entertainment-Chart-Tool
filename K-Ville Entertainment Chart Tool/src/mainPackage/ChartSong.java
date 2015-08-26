package mainPackage;

public class ChartSong implements Comparable<ChartSong>{

	private String songName;
	private String artistName;
	private int points;
	
	public ChartSong(String song, String artist){
		this.songName = song;
		this.artistName = artist;
		this.points = 0;
	}
	
	public ChartSong(String song, String artist, int pts){
		this.songName = song;
		this.artistName = artist;
		this.points = pts;
	}
	
	public void AddPoints(int points){
		this.points += points;
	}
	
	// Sorts points in descending order
	public int compareTo(ChartSong cs2) {
		if (this.points < cs2.points){
			return 1;
		}else if (this.points == cs2.points){
			return 0;
		}else{
			return -1;
		}
	}
	
	public boolean isEqual(ChartSong cs2){
		return (this.artistName.equals(cs2.artistName) && this.songName.equals(cs2.songName)) ? true : false;
	}

	public String getSongName(){ return this.songName; }
	public String getArtistName(){ return this.artistName; }
	public int getPoints(){ return this.points; }
	public void setSongName(String name){ this.songName = name; }
	public void setArtistName(String name){ this.artistName = name; }
	public void setPoints(int pts){ this.points = pts; }
}
