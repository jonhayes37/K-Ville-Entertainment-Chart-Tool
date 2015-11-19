package mainPackage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentListResponse;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.common.collect.Lists;
// TODO Improve failed comment parser
// TODO DONE Fixed rare bug with indicating which comment lines failed
// TODO DONE File now saved in working directory
// TODO DONE Simplified UI, cut back on variables
public class MainWindow extends JFrame implements ActionListener{

	private static final long serialVersionUID = 414975340316732097L;
	private JPanel pnlMain;
	private JPanel pnlCentre;
	private JTextField[] txtRows = new JTextField[2];
	private JButton btnGenerate = new JButton("Generate Chart Totals");
	private JLabel lblURL = new JLabel("Video URL:");
	private String videoURL = "";
	private static YouTube youtube;
	private Chart chart = new Chart();
	private HashMap<String,String> authorComms = new HashMap<String,String>();
	private ArrayList<String> commentList;
	private ArrayList<ChartSong> tempSongs = new ArrayList<ChartSong>();
	private final String VERSION_NUMBER = "1.3";
	private final String[] splitters = new String[]{"-","–","by","/","~","|","--","="};
	private final String[] hyphenStrings = new String[]{"ah-choo","a-choo","click-b","ooh-aah","ooh-ahh","ohh-ahh","ohh-aah","make-up","twenty-three","t-ara","g-d","g-dragon","g-friend","a-daily","ah-choo","pungdeng-e"};
	private String artistChars = ";,?";
	private String songChars = "-1023456789.);?,";
	private JMenuBar menuBar = new JMenuBar();
	private JMenu fileMenu = new JMenu("File");
	private JMenuItem updateChartFile = new JMenuItem("Update Top 50 Chart File");
	private JMenuItem processVotes = new JMenuItem("Process Staff Votes");
	private ArrayList<Tie> ties = new ArrayList<Tie>();
	
	// Window Creation
	public MainWindow(){
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e3) { e3.printStackTrace();	}
		
		updateChartFile.addActionListener(this);
		processVotes.addActionListener(this);
		fileMenu.add(updateChartFile);
		fileMenu.add(processVotes);
		menuBar.add(fileMenu);
		menuBar.setBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.GRAY));
		
		pnlCentre = new JPanel();
		pnlCentre.setLayout(new BorderLayout());
		pnlCentre.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		
		btnGenerate.addActionListener(this);
		txtRows[0] = new JTextField();
		txtRows[0].setBorder(BorderFactory.createLineBorder(Color.GRAY));
		txtRows[0].setFont(new Font("Arial", Font.PLAIN, 12));
		pnlCentre.add(txtRows[0]);
		
		pnlMain = new JPanel();
		pnlMain.setLayout(new BorderLayout());
		pnlMain.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		pnlMain.add(lblURL, BorderLayout.NORTH);
		pnlMain.add(pnlCentre);
		pnlMain.add(btnGenerate, BorderLayout.SOUTH);
		
		this.add(pnlMain);
		this.setJMenuBar(menuBar);
		this.setIconImage(new ImageIcon("icon.png").getImage());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("K-Ville Entertainment Chart Tool v" + VERSION_NUMBER);
        this.setSize(390,152);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
	}
	
	// Action Listeners
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnGenerate){	// Generate the Chart and text file
			if (txtRows[0].getText().length() < 2){
				JOptionPane.showMessageDialog(this, "Invalid URL.", "Missing URL", JOptionPane.ERROR_MESSAGE);
			}else{
				videoURL = txtRows[0].getText();
				if (videoURL.split("=").length > 1){	// If full URL is included, gets ID portion
					videoURL = videoURL.split("=")[1];
				}
				GenerateChart();
				this.setTitle("K-Ville Entertainment Chart Tool v" + VERSION_NUMBER);
			}
		}else if (e.getSource() == updateChartFile){	// Updating Chart .txt file after manual processing
			JFileChooser opener = new JFileChooser();
	        int ret = opener.showDialog(this, "Choose Chart File");
	        if(ret == JFileChooser.APPROVE_OPTION){   
	            String tempPath = opener.getSelectedFile().getPath();
	            chart.UpdateChartFile(tempPath);
	            JOptionPane.showMessageDialog(this, "Updated Top 50 Chart!", "Updated Chart", JOptionPane.INFORMATION_MESSAGE);
	        }
		}else if (e.getSource() == processVotes){
			JFileChooser opener = new JFileChooser();
			opener.setMultiSelectionEnabled(true);
			opener.addChoosableFileFilter(new FileNameExtensionFilter("Text Files",".txt"));
	        int ret = opener.showDialog(this, "Choose Staff Vote Files");
	        if(ret == JFileChooser.APPROVE_OPTION){   
	        	File[] chosenFiles = opener.getSelectedFiles();
	        	StaffVote[] allVotes = new StaffVote[chosenFiles.length];
	        	for (int i = 0; i < chosenFiles.length; i++){	// Processes individual lists
	            	allVotes[i] = new StaffVote();
	            	allVotes[i].ProcessVote(chosenFiles[i]);
	            }
	            
	        	// Combine list into overall list of points
	        	StaffVote[] allVoteCopy = new StaffVote[allVotes.length];
	        	for (int i = 0; i < allVotes.length; i++){
	        		SongVote[] tempVotes = new SongVote[allVotes[i].getSongVotes().length];
	        		for (int j = 0; j < allVotes[i].getSongVotes().length; j++){
	        			tempVotes[j] = new SongVote(allVotes[i].getSongVotes()[j]);
	        		}
	        		allVoteCopy[i] = new StaffVote(tempVotes); 
	        	}
	        	ArrayList<SongVote> combinedVotes = CombineLists(allVoteCopy);
        		
	        	// Detect and attempt to break ties, save output
	        	BreakTies(combinedVotes, allVotes);
	        	
	            JOptionPane.showMessageDialog(this, "Process staff votes!", "Processed Vote", JOptionPane.INFORMATION_MESSAGE);
	        }
		}
	}
	
	// Combines the individual staff lists into a sorted, cut off list ready for tiebreaker analysis
	private ArrayList<SongVote> CombineLists(StaffVote[] sVotes){
		ArrayList<SongVote> combined = new ArrayList<SongVote>();
		for (int i = 0; i < sVotes.length; i++){	// Process each staff vote
			StaffVote curStaffVote = sVotes[i];
			for (int j = 0; j < curStaffVote.getSongVotes().length; j++){	// Process each song vote
				SongVote curVote = curStaffVote.getSongVotes()[j];
				boolean alreadyIn = false;
				int existingInd = 0;
				for (int k = 0; k < combined.size(); k++){	// Check if song is already in the list
					if (combined.get(k).isEqual(curVote)){ // Need to add points
						alreadyIn = true;
						existingInd = k;
					}
				}
				if (alreadyIn){ // Need to add points
					combined.get(existingInd).setPoints(combined.get(existingInd).getPoints() + curVote.getPoints());
				}else{	// Add the song to the arraylist
					combined.add(curVote);
				}
			}
		}
		
		// Sorting and keeping only the necessary (top 25 + extending tiebreak songs)
		Collections.sort(combined);
		Collections.reverse(combined);
		ArrayList<SongVote> topVotes = new ArrayList<SongVote>();
		for (int i = 0; i < combined.size(); i++){
			if (i > 24){	// Past 25 songs
				if (combined.get(i).getPoints() == topVotes.get(topVotes.size() - 1).getPoints()){	// Tiebreaker continuing past 25th
					topVotes.add(combined.get(i));
				}else{
					break;
				}
			}else{
				topVotes.add(combined.get(i));
			}
		}
		return topVotes;
	}
	
	// Detects any ties and tries to break them. Creates a file for the list
	// and a file of conflicts needing to be resolved
	private void BreakTies(ArrayList<SongVote> topList, StaffVote[] allVotes){
		ArrayList<SongVote> fixedVotes = new ArrayList<SongVote>();
		for (int i = 0; i < topList.size(); i++){
			if (i + 1 < topList.size() && topList.get(i).getPoints() == topList.get(i + 1).getPoints()){	// Tie
				int numSongsTied = 2;
				while (i + numSongsTied < topList.size() && topList.get(i + numSongsTied - 1).getPoints() == topList.get(i + numSongsTied).getPoints()){
					numSongsTied++;
				}
				ArrayList<SongVote> tempList = new ArrayList<SongVote>();
				for (int j = 0; j < numSongsTied; j++){		// Create list of tied songs
					tempList.add(topList.get(i + j));
				}
				
				// Process and add songs to fixedVotes
				tempList = ProcessTie(tempList, allVotes);
				fixedVotes.addAll(tempList);
				i += (numSongsTied - 1);
			}else{
				fixedVotes.add(topList.get(i));
			}
		}
		try{
			// Save top 25 list in a file
			BufferedWriter bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/Top 25 Chart.txt"));
			for (int i = 0; i < fixedVotes.size(); i++){
				//if (i < 25){
					bw.write((i + 1) + ". (" + fixedVotes.get(i).getPoints() + " points) " + fixedVotes.get(i).getArtist() + " - " + fixedVotes.get(i).getSong());
					bw.newLine();
				//}
			}
			bw.close();
			
			// Save unresolved ties in a file
			if (ties.size() > 0){
				bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/Unresolved Ties.txt"));
				for (int i = 0; i < ties.size(); i++){
					Tie curTie = ties.get(i);
					bw.write("~~~~~ Tie at " + curTie.getPoints() + " points ~~~~~\n");
					for (int j = 0; j < curTie.getArtists().size(); j++){
						bw.write(curTie.getArtists().get(j) + " - " + curTie.getSongs().get(j) + "\n");
					}
					bw.newLine();
				}
				bw.close();
			}
		}catch (IOException e){ e.printStackTrace(); }		
	}
	
	// Processes a tie if possible. If processed, reorders the songs according
	// to unique votes and returns a sorted array. If not, returns the original
	// array and adds a new tie to the tiebreaker
	private ArrayList<SongVote> ProcessTie(ArrayList<SongVote> tiedSongs, StaffVote[] allVotes){
		int benVoteIndex = 0;
		int[] votes = new int[tiedSongs.size()];
		int numVotesCast = 0;
		
		// Simulates a vote from each staff by checking the relative rating of the songs involved
		for (int i = 0; i < allVotes.length; i++){
			int maxIndex = 0, maxValue = 0;
			for (int j = 0; j < tiedSongs.size(); j++){
				int tempPts = allVotes[i].getVotePoints(tiedSongs.get(j).getArtist(), tiedSongs.get(j).getSong());
				if (tempPts > maxValue){
					maxIndex = j;
					maxValue = tempPts;
				}
			}
			if (allVotes[i].getStaffName().contains("ben")){
				benVoteIndex = maxIndex;
			}
			if (maxValue > 0){
				votes[maxIndex]++;
				numVotesCast++;
			}
		}
		
		// Check if there is a unique max. If so, add that + addAll(processties(smaller amount))
		int maxInd = 0, max = 0, numMaxes = 0;
		for (int i = 0; i < votes.length; i++){
			if (votes[i] > max){
				maxInd = i;
				max = votes[i];
				numMaxes = 1;
			}else if (votes[i] == max){
				numMaxes++;
			}
		}
		
		if (numMaxes == 1 && (max > Math.floor(allVotes.length / (double)2) || numVotesCast == allVotes.length)){		// Tie successfully broken
			ArrayList<SongVote> newTiedSongs = tiedSongs;
			ArrayList<SongVote> sortVotes = new ArrayList<SongVote>();
			sortVotes.add(tiedSongs.get(maxInd));
			newTiedSongs.remove(maxInd);
			if (tiedSongs.size() > 2){
				sortVotes.addAll(ProcessTie(newTiedSongs, allVotes));
				return sortVotes;
			}else{
				sortVotes.addAll(newTiedSongs);
				return sortVotes;
			}
		}else if (votes[benVoteIndex] == max && (max >= Math.floor(allVotes.length / (double)2) || numVotesCast == allVotes.length)){	// Ben has a vote tied for first, so that song wins
			ArrayList<SongVote> newTiedSongs = tiedSongs;
			ArrayList<SongVote> sortVotes = new ArrayList<SongVote>();
			sortVotes.add(tiedSongs.get(benVoteIndex));
			newTiedSongs.remove(benVoteIndex);
			if (tiedSongs.size() > 2){
				sortVotes.addAll(ProcessTie(newTiedSongs, allVotes));
				return sortVotes;
			}else{
				sortVotes.addAll(newTiedSongs);
				return sortVotes;
			}
		}else{	// if unresolved, add info about tiebreak (points, which songs)
			ArrayList<String> tSongs = new ArrayList<String>();
			ArrayList<String> tArtists = new ArrayList<String>();
			for (int i = 0; i < tiedSongs.size(); i++){
				tSongs.add(tiedSongs.get(i).getSong());
				tArtists.add(tiedSongs.get(i).getArtist());
			}
			int pts = tiedSongs.get(0).getPoints();
			ties.add(new Tie(tSongs, tArtists, pts));
			return tiedSongs;
		}
	}
	
	// Generates the Chart and corresponding files from the video's comments
	private void GenerateChart(){
		chart = new Chart();
		this.setTitle("Establishing Connection...");
		FetchComments();				// Fetch comments from YouTube
		this.setTitle("Processing Comments...");
		ProcessComments(commentList);	// Process fetched comments and add them to the Chart
		this.setTitle("Processing Chart...");
		chart.ProcessChart(this);			// Chart processing to reduce manual post-editing
		this.setTitle("Creating Files...");
		chart.CreateChart(System.getProperty("user.dir"));			// File creation
		this.setTitle("Done!");
		JOptionPane.showMessageDialog(this, "Finished generation!", "Finished", JOptionPane.INFORMATION_MESSAGE);
	}
	
	// Gets comments from YouTube via a URL
	private void FetchComments(){
		List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl");
		scopes.add("https://www.googleapis.com/auth/youtube.force-ssl");

        try {
            // Authorize the request.
            Credential credential = Auth.authorize(scopes, "commentthreads");

            // This object is used to make YouTube Data API requests.
            youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential).setApplicationName("youtube-cmdline-commentthreads-sample").build();

            // Call the YouTube Data API's commentThreads.list method to retrieve video comment threads.
            HttpHeaders headers = new HttpHeaders();
            headers.setUserAgent("My User Agent");
            CommentThreadListResponse videoCommentsListResponse = youtube.commentThreads().list("snippet").setVideoId(videoURL).setTextFormat("plainText").setMaxResults((long)100).execute();
            List<CommentThread> videoComments = videoCommentsListResponse.getItems();
            String nextToken = videoCommentsListResponse.getNextPageToken();
            while (nextToken != null){
            	videoCommentsListResponse = youtube.commentThreads().list("snippet").setVideoId(videoURL).setTextFormat("plainText").setPageToken(nextToken).setMaxResults((long)100).execute();
            	List<CommentThread> tempList = videoCommentsListResponse.getItems(); 
            	for (int i = 0; i < tempList.size(); i++){
            		videoComments.add(tempList.get(i));
            	}
                nextToken = videoCommentsListResponse.getNextPageToken();
            }
            
            if (videoComments.isEmpty()) {
                System.out.println("Can't get video comments.");
            }else{
            	ArrayList<String> comments = new ArrayList<String>();
            	int i = 0;
            	int j = 0;
                for (CommentThread videoComment : videoComments) {
                	this.setTitle("Fetching YouTube Comments (" + (int)((i + 1) / (float)videoComments.size() * 100) + "%)");
                	
                	// Adding the main comment to the list
                	CommentSnippet snippet = videoComment.getSnippet().getTopLevelComment().getSnippet();
                	String author = snippet.getAuthorDisplayName().toLowerCase(); 
                	if (!author.toLowerCase().contains("k-ville")){
                		if (!authorComms.keySet().contains(author)){	// Author has not made a comment yet
                			authorComms.put(author, snippet.getTextDisplay());
                		}else if (authorComms.keySet().contains(author) && !snippet.getTextDisplay().equals(authorComms.get(author))){		// If a different comment is stored, only keep the longest one
                			if (snippet.getTextDisplay().length() > authorComms.get(author).length()){
                				authorComms.put(author, snippet.getTextDisplay());
                			}
                		}
                	}
                    
                	if (videoComment.getSnippet().getTotalReplyCount() > 0){
	                    // Starting a search for replies
	                	CommentListResponse commentsListResponse = youtube.comments().list("snippet").setParentId(videoComment.getId()).setTextFormat("plainText").execute();
	                	if (!commentsListResponse.isEmpty()){
		                    List<Comment> comms = commentsListResponse.getItems();
		                    if (!comms.isEmpty()){
		                        for (Comment commentReply : comms) {
		                        	j++;
	                        		CommentSnippet snip = commentReply.getSnippet();
	                        		author = snip.getAuthorDisplayName().toLowerCase();
	                        		if (!author.toLowerCase().contains("k-ville")){
	                            		if (!authorComms.keySet().contains(author)){	// Author has not made a comment yet
	                            			authorComms.put(author, snip.getTextDisplay());
	                            		}else if (authorComms.keySet().contains(author) && !snip.getTextDisplay().equals(authorComms.get(author))){		// If a different comment is stored, only keep the longest one
	                            			if (snippet.getTextDisplay().length() > authorComms.get(author).length()){
	                            				authorComms.put(author, snip.getTextDisplay());
	                            			}
	                            		}
	                            	}
		                        }
		                    }
	                    }
                	}
                	i++;
                }
                
                for (String auth : authorComms.keySet()){	// Adding all comments to the comment list
                	comments.add(authorComms.get(auth));
                }
                System.out.println(j + " replies added. There are " + comments.size() + " comments to process.");
                commentList = comments;
            }
        } catch (GoogleJsonResponseException e) {
        	JOptionPane.showMessageDialog(this, "JSON Exception encountered. Error code: " + e.getDetails().getCode() + ".", "JSON Exception", JOptionPane.ERROR_MESSAGE);
        	e.printStackTrace();
        } catch (IOException e) {
        	JOptionPane.showMessageDialog(this, "IO Exception encountered.", "IO Exception", JOptionPane.ERROR_MESSAGE);
        	e.printStackTrace();
        } catch (Throwable t) {
        	JOptionPane.showMessageDialog(this, "Throwable Exception encountered. Message: " + t.getMessage() + ".", "Throwable Exception", JOptionPane.ERROR_MESSAGE);
        	t.printStackTrace();
        }
    }
	
	// Processes all comments retrieved from YouTube
	private void ProcessComments(ArrayList<String> comments){
		for (int i = 0; i < comments.size(); i++){
			String comment = comments.get(i);
			String[] commentLines = comment.split("\n");
			
			if (commentLines[commentLines.length-1].lastIndexOf("?") == commentLines[commentLines.length-1].length() - 2){
				commentLines[commentLines.length-1] = commentLines[commentLines.length-1].substring(0, commentLines[commentLines.length-1].length() - 1);
			}
			
			// Removing empty lines and k-ville tags from comments
			ArrayList<String> tempLines = new ArrayList<String>();
			Collections.addAll(tempLines, commentLines);
			for (int j = commentLines.length - 1; j >= 0; j--){
				if (tempLines.get(j).length() < 1 || tempLines.get(j).toLowerCase().contains("+k-ville entertainment") || (tempLines.get(j).toLowerCase().contains("http") && tempLines.get(j).toLowerCase().contains("://"))){	// Problem line
					tempLines.remove(j);
				}
			}
			commentLines = tempLines.toArray(new String[tempLines.size()]);
			
			ArrayList<Integer> failedLines = ProcessComment(commentLines);	// Processes each line of the comment
			if (failedLines.size() > 0 && commentLines.length - failedLines.size() < 2){	// If 1 or fewer lines were successful, all lines are marked as failed, and the comment is ignored until manual addition
				for (int j = 0; j < commentLines.length; j++){
					if (!failedLines.contains(j)){
						failedLines.add(j);
					}
				}
				Collections.sort(failedLines);
				tempSongs = new ArrayList<ChartSong>();
			}
			CheckForDuplicates();	// Checks the list of comments for duplicates
			
			// Formats and adds all successful songs to the chart
			for (int j = 0; j < tempSongs.size(); j++){		
				tempSongs.get(j).setArtistName(RemoveSpaces(tempSongs.get(j).getArtistName().toLowerCase()));
				tempSongs.get(j).setSongName(RemoveSpaces(tempSongs.get(j).getSongName().toLowerCase()));
				if (tempSongs.get(j).getPoints() > 0){	// Avoids strange negative point errors
					chart.AddValue(tempSongs.get(j));
				}
			}
			if (failedLines.size() > 0){	// If lines failed, adds them to the list of failed parses
				chart.AddFailedParse(commentLines, failedLines);
			}
		}
	}
	
	// Returns a list of line numbers in which parsing failed
	private ArrayList<Integer> ProcessComment(String[] comment){
		tempSongs = new ArrayList<ChartSong>();
		ArrayList<Integer> failedLines = new ArrayList<Integer>();
		String[] lines = comment;
		if (lines.length < 10){			// Process improper formatting here
			int successfulLines = 0;	// successfulLines keeps track of how many points a song will get if the number cannot be properly processed 
			for (int j = 0; j < lines.length; j++){
				boolean success = ProcessLine(lines[j], successfulLines, 0);
				if (!success){
					failedLines.add(j);
				}else{
					successfulLines++;
				}
			}
			return failedLines;
		}else if (lines.length > 10){	// The comment has too many lines
			
			for (int k = 0; k <= lines.length - 10; k++){  // Tries processing every sequence of 10 lines in the comment
				ArrayList<Integer> currentTen = ProcessComment(Arrays.copyOfRange(lines, k, k + 10)); 	
				if (currentTen.size() == 0){	// If any sequence of 10 lines worked perfectly, return it
					return currentTen;
				}
			}
			
			int successfulLines = 0;	// successfulLines keeps track of how many points a song will get if the number cannot be properly processed 
			for (int j = 0; j < lines.length; j++){
				boolean success = ProcessLine(lines[j], successfulLines, 0);
				if (!success){
					failedLines.add(j);
				}else{
					successfulLines++;
				}
			}
			return failedLines;
			//}
		}else{	// Process correct formatting (10 lines)
			for (int j = 0; j < lines.length; j++){
				boolean success = ProcessLine(lines[j], j, 0);
				if (!success){
					failedLines.add(j);
				}
			}
			return failedLines;
		}
	}
	
	// Processes a line involving a song rank
	private boolean ProcessLine(String line, int lineNum, int attemptNum){
		if (line.toLowerCase().contains("k-ville entertainment") || (line.toLowerCase().contains("http") && line.toLowerCase().contains("://"))){	// Don't count this properly formatted line
			return false;
		}
		String[] parts = line.split(Pattern.quote(splitters[attemptNum]));
		String song, artist;
		int points;
		if (parts.length < 2){ 			// Try to split by " ", find word combinations in an existing song name in the chart
			parts = line.split(" ");
			if (parts.length < 2){		// Failed Parse
				if (attemptNum + 1 < splitters.length){
					return ProcessLine(line, lineNum, ++attemptNum);
				}else{
					return false;
				}
				
			}else{		// Potentially correct, must determine if there is a number in front -> try to find word in chart
				// Format of "1. song artist" or "1 song artist" or "song artist"
				points = ProcessPoints(line, lineNum);
				String tempCheck = ProcessSongName(parts[0]);
				for (int i = 0; i < parts.length - 1; i++){		// Looping through words to search in chart if they exist already
					if (i > 0){
						if (tempCheck.length() < 1){
							tempCheck += (parts[i]);
						}else{
							tempCheck += (" " + parts[i]);	
						}
					}
					for (int j = 0; j < chart.getChartSongs().getSize(); j++){
						if ((tempCheck.length() > 0 && chart.getChartSongs().GetValueAt(j).getSongName().equals(tempCheck)) || (tempCheck.length() > 0 && chart.getChartSongs().GetValueAt(j).getArtistName().equals(tempCheck))){	// If the song matches a chart song
							song = tempCheck;
							artist = CombineRestOfParts(parts, i + 1);
							tempSongs.add(new ChartSong(song, artist, points));
							return true;
						}
					}
				}
				if (attemptNum + 1 < splitters.length){	// Tries to parse with next splitter
					return ProcessLine(line, lineNum, ++attemptNum);
				}else{
					return false;	// If the song was not found, parse failed					
				}
			}
		}else if (parts.length == 2){	// Correct formatting
			if ((parts[0].length() < 2 && !parts[0].toLowerCase().equals("i")) || (parts[1].length() < 2 && !parts[1].toLowerCase().equals("i"))){
				if (parts[0].length() < 2){
					return ProcessLine(parts[1], lineNum, ++attemptNum);
				}
			}
			song = ProcessSongName(parts[0]);
			artist = ProcessArtist(parts[1]);
			points = ProcessPoints(parts[0], lineNum);
			tempSongs.add(new ChartSong(song, artist, points));
			return true;
		}else if (parts.length == 3 && attemptNum == 0){		// One too many -'s (could be band name) 1. t-ara - so crazy     1. so crazy - t-ara
			boolean hasHyphenString = false;
			String matchString = "";
			for (int i = 0; i < hyphenStrings.length; i++){
				if (RemoveAllSpaces(line.toLowerCase()).contains(hyphenStrings[i])){
					hasHyphenString = true;
					matchString = hyphenStrings[i];
					break;
				}
			}
			if (hasHyphenString){	// Special case processing
				if ((RemoveAllSpaces(parts[0]) + "-" + RemoveAllSpaces(parts[1])).toLowerCase().contains(matchString)){
					song = ProcessSongName(parts[0] + "-" + parts[1]);
					artist = ProcessArtist(parts[2]);
					points = ProcessPoints(parts[0], lineNum);
					tempSongs.add(new ChartSong(song, artist, points));
					return true;
				}else if ((RemoveAllSpaces(parts[1]) + "-" + RemoveAllSpaces(parts[2])).toLowerCase().contains(matchString)){
					song = ProcessSongName(parts[0]);
					artist = ProcessArtist(parts[1] + "-" + parts[2]);
					points = ProcessPoints(parts[0], lineNum);
					tempSongs.add(new ChartSong(song, artist, points));
					return true;
				}else{	// Failed Parse
					return false;
				}				
			}else if (IsDigits(parts[0])){	// Format of 1- song - artist
				if (ProcessPoints(parts[0], lineNum) - (10 - lineNum) < 5){	// If it's reasonable that they numbered their songs
					song = ProcessSongName(parts[1]);
					artist = ProcessArtist(parts[2]);
					points = ProcessPoints(parts[0], lineNum);
					tempSongs.add(new ChartSong(song, artist, points));
					return true;
				}else{
					return false;
				}
			}else{
				return false;	// Failed parse
			}
		}else if (parts.length == 4){	// 4 -'s > could be 1- song	- ar-tist
			if (IsDigits(parts[0])){	// Format of 1- song artist
				if (ProcessPoints(parts[0], lineNum) - lineNum < 5){	// If it's reasonable that they numbered their songs
					boolean hasHyphenString = false;
					String matchString = "";
					for (int i = 0; i < hyphenStrings.length; i++){
						if (RemoveAllSpaces(line.toLowerCase()).contains(hyphenStrings[i])){
							hasHyphenString = true;
							matchString = hyphenStrings[i];
							break;
						}
					}
					if (hasHyphenString){
						if ((RemoveAllSpaces(parts[1]) + "-" + RemoveAllSpaces(parts[2])).toLowerCase().contains(matchString)){
							song = ProcessSongName(parts[1] + "-" + parts[2]);
							artist = ProcessArtist(parts[3]);
							points = ProcessPoints(parts[0], lineNum);
							tempSongs.add(new ChartSong(song, artist, points));
							return true;
						}else if ((RemoveAllSpaces(parts[2]) + "-" + RemoveAllSpaces(parts[3])).toLowerCase().contains(matchString)){
							song = ProcessSongName(parts[1]);
							artist = ProcessArtist(parts[2] + "-" + parts[3]);
							points = ProcessPoints(parts[0], lineNum);
							tempSongs.add(new ChartSong(song, artist, points));
							return true;
						}else{	// Failed Parse
							return false;
						}
					}
				}
			}
			return false;
		}else{ 	// 5 or more -'s -> Failed Parse
			return false;
		}
	}
	
	// Processes a song name
	private String ProcessSongName(String song){
		String tempSong = RemoveSpaces(song);
		while (song.length() > 0 && songChars.contains(tempSong.substring(0,1))){	// removes numbers and spaces at the beginning of the name
			if (tempSong.length() > 1 && Character.isDigit(tempSong.charAt(0)) && Character.isLetter(tempSong.charAt(1))){	// Saves band names like 4Minute
				break;
			}else if (tempSong.length() > 1){
				tempSong = tempSong.substring(1, tempSong.length());
			}else if (tempSong.length() > 0 && songChars.contains(tempSong)){
				return "";
			}else{
				return tempSong.toLowerCase();
			}
		}
		return tempSong.toLowerCase();
	}
	
	// Process an artist
	private String ProcessArtist(String artist){
		String tempArt = RemoveSpaces(artist);
		while (tempArt.length() > 1 && artistChars.contains(tempArt.substring(tempArt.length() - 1))){	// Removing unwanted characters at the end
			tempArt = tempArt.substring(0, tempArt.length() - 1);
		}
		if (tempArt.split(" ").length > 1 && tempArt.split(" ")[0].length() > 0 && Character.isDigit(tempArt.split(" ")[0].charAt(0))){	// format of "1. artist song"
			tempArt = CombineRestOfParts(tempArt.split(" "), 0);
		}
		while (tempArt.length() > 1 && artistChars.contains(tempArt.substring(0,1))){	// Removing unwanted characters at the beginning
			tempArt = tempArt.substring(1, tempArt.length());
		}
		return tempArt.toLowerCase();
	}
	
	// Process how many points to give the song
	private int ProcessPoints(String song, int line){
		int lastNumIndex = 0;
		int counter = 0;
		if (song.length() > 0 && Character.isDigit(song.charAt(0))){		// Number given
			while (counter < song.length() && Character.isDigit(song.charAt(counter))){
				lastNumIndex = counter;
				counter++;
			}
			int points = Integer.parseInt(song.substring(0, lastNumIndex + 1));
			return 11 - points; 
		}else{		// Numbers not included, just a list of songs and artists
			return 10 - line;
		}
	}
	
	// Removes any duplicate votes in a comment
	private void CheckForDuplicates(){
		for (int i = 0; i < tempSongs.size() - 1; i++){
			for (int j = i + 1; j < tempSongs.size(); j++){
				if (tempSongs.get(i).isEqual(tempSongs.get(j))){
					tempSongs.remove(j);
				}
			}
		}
	}
	
	// Removes spaces at the beginning and end of the string
	private String RemoveSpaces(String str){
		String tempStr = str;
		if (tempStr.length() < 1){
			return str;
		}
		while (tempStr.length() > 0 && tempStr.substring(tempStr.length() - 1).equals(" ")){	// Removes spaces at the end of the name
			tempStr = tempStr.substring(0, tempStr.length() - 1);
		}
		while (tempStr.length() > 0 && tempStr.substring(0,1).equals(" ")){	// Removes spaces at the beginning of the name
			tempStr = tempStr.substring(1, tempStr.length());
		}
		return tempStr;
	}
	
	// Removes all spaces from a string
	private String RemoveAllSpaces(String str){
		String tempStr = RemoveSpaces(str);
		if (tempStr.length() < 1){
			return str;
		}
		for (int i = tempStr.length() - 1; i >= 0; i--){
			if (tempStr.charAt(i) == ' '){
				tempStr = tempStr.substring(0, i) + tempStr.substring(i + 1, tempStr.length());
			}
		}
		return tempStr;
	}
	
	// Checks if a string is all digits
	private boolean IsDigits(String str){
		if (str.length() < 1){
			return false;
		}
		if (!Character.isDigit(str.charAt(0))){ 	// If it doesn't start with a number
			return false;
		}
		for (int i = 1; i < str.length(); i++){
			if (!Character.isDigit(str.charAt(i)) && str.charAt(i) != '.' && str.charAt(i) != ' ' && str.charAt(i) != ')'){
				return false;
			}
		}
		return true;
	}
	
	// Recombines split artist / song info
	private String CombineRestOfParts(String[] parts, int index){
		String tempString = "";
		for (int i = index + 1; i < parts.length; i++){
			tempString += (parts[i] + " ");
		}
		if (tempString.length() > 0){
			return tempString.substring(0, tempString.length() - 1);
		}else{
			return tempString;
		}
	}

	// Program starts here
	public static void main(String[] args){
		new MainWindow();
	}
}
