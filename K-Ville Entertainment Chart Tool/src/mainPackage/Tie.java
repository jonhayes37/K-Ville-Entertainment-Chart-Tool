package mainPackage;

import java.util.ArrayList;

public class Tie {
	
	private ArrayList<String> songs;
	private ArrayList<String> artists;
	private int points;
	
	public Tie(ArrayList<String> s, ArrayList<String> a, int p){
		this.artists = a;
		this.songs = s;
		this.points = p;
	}
	
	public ArrayList<String> getSongs(){ return this.songs; }
	public ArrayList<String> getArtists(){ return this.artists; }
	public int getPoints(){ return this.points; }
}
