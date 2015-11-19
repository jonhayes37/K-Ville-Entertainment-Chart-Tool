package mainPackage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

public class StaffVote {
	
	private String staffName = "";
	private SongVote[] votes = new SongVote[15];
	
	public StaffVote(){}
	
	public StaffVote(SongVote[] votes){
		this.votes = votes;
	}
	
	public void ProcessVote(File voteFile){
		this.staffName = voteFile.getName().split(Pattern.quote("."))[0];
		try {
			BufferedReader br = new BufferedReader(new FileReader(voteFile));
			String line = br.readLine();
			int curPts = 20;
			while (line != null){
				String[] info = line.split("<->");
				votes[20 - curPts] = new SongVote(info[0], info[1], curPts);
				curPts--;
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) { 
			JOptionPane.showMessageDialog(null, "Could not read one of the selected files.", "File Read Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace(); 
		}
	}
	
	// Checks how many points a person assigned a song
	public int getVotePoints(String artist, String song){
		for (int i = 0; i < this.votes.length; i++){
			//System.out.println(artist + " vs " + this.votes[i].getArtist() + ", " + song + " vs " + this.votes[i].getSong());
			if (this.votes[i].getArtist().equals(artist) && this.votes[i].getSong().equals(song)){
				//System.out.println("Returning " + this.votes[i].getPoints());
				return this.votes[i].getPoints();
			}
		}
		return 0;
	}
	
	public SongVote[] getSongVotes(){ return this.votes; }
	public String getStaffName(){ return this.staffName; }
	public void setStaffName(String n){ this.staffName = n; }
}
