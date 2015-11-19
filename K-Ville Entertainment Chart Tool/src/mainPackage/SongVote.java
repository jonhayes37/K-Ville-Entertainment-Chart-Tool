package mainPackage;

public class SongVote implements Comparable<SongVote>{
	
	private int points;
	private String artist;
	private String song;
	private int numVotes;
	
	public SongVote(String artist, String song, int pts){
		this.points = pts;
		this.song = song;
		this.artist = artist;
		this.numVotes = 1;
	}
	
	public SongVote(SongVote s){
		this.points = s.getPoints();
		this.song = s.getSong();
		this.artist = s.getArtist();
		this.numVotes = s.getNumVotes();
	}
	
	public boolean isEqual(SongVote s2){
		return (this.artist.equals(s2.getArtist()) && this.song.equals(s2.getSong()));
	}
	
	public int getPoints(){ return this.points; }
	public String getArtist(){ return this.artist; }
	public String getSong(){ return this.song; }
	public int getNumVotes(){ return this.numVotes; }
	public void setPoints(int pts){ this.points = pts; }
	public void setNumVotes(int num2){ this.numVotes = num2; }
	public void setArtist(String artist){ this.artist = artist; }
	public void setSong(String song){ this.song = song; }

	@Override
	public int compareTo(SongVote s2) {
		if (this.points > s2.getPoints()){
			return 1;
		}else if (this.points == s2.getPoints()){
			return 0;
		}else{
			return -1;
		}
	}
}
