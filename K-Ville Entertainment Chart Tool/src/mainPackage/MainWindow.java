package mainPackage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
	private final String VERSION_NUMBER = "1.2";
	private final String[] splitters = new String[]{"-","�","by","/","~","|","--"};
	private final String[] hyphenStrings = new String[]{"make-up","twenty-three","t-ara","g-d","g-dragon","g-friend","a-daily","ah-choo","pungdeng-e"};
	private String artistChars = ";,?";
	private String songChars = "-1023456789.);?,";
	private JMenuBar menuBar = new JMenuBar();
	private JMenu fileMenu = new JMenu("File");
	private JMenuItem updateChartFile = new JMenuItem("Update Top 50 Chart");
	private JMenuItem updateProgram = new JMenuItem("Check for Updates");
	
	// Window Creation
	public MainWindow(){
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e3) { e3.printStackTrace();	}
		
		updateChartFile.addActionListener(this);
		updateProgram.addActionListener(this);
		fileMenu.add(updateChartFile);
		fileMenu.add(updateProgram);
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
        
        Updater update = new Updater("K-Ville Entertainment Chart Tool", this.VERSION_NUMBER);
		update.CheckVersion();
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
		}else if (e.getSource() == updateProgram){
			Updater update = new Updater("K-Ville Entertainment Chart Tool", this.VERSION_NUMBER);
			update.CheckVersion();
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
                	if (snippet.getTextDisplay().contains("1. I � Taeyeon feat. Verbal Jint\n2. Dumb Dumb � Red Velvet")){
        				System.out.println("Author: " + author);
        			}
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
			comment = comment.substring(0, comment.length()); 	// Removes "?" at the end of each comment
			String[] commentLines = comment.split("\n");
			if (commentLines.length > 0 && commentLines[0].toLowerCase().equals("+k-ville entertainment")){	// Removes lines that are only tagging k-ville
				System.out.println("Removing a k-ville tagged line");
				commentLines = Arrays.copyOfRange(commentLines, 1, commentLines.length);
			}
			ArrayList<Integer> failedLines = ProcessComment(commentLines);	// Processes each line of the comment
			if (failedLines.size() > 0 && commentLines.length - failedLines.size() < 2){	// If 1 or fewer lines were successful, all lines are marked as failed, and the comment is ignored until manual addition
				for (int j = 0; j < commentLines.length; j++){
					if (!failedLines.contains(j)){
						failedLines.add(j);
					}
				}
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
				if (comment.contains("1. I � Taeyeon feat. Verbal Jint\n2. Dumb Dumb � Red Velvet")){
					for (int k = 0; k < failedLines.size(); k++){
						System.out.println("Error on line " + failedLines.get(k));
					}
				}
				chart.AddFailedParse(comment, failedLines);
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
			ArrayList<Integer> firstTen = ProcessComment(Arrays.copyOfRange(lines, 0, 10)); 	// Tries processing the first 10 lines
			if (firstTen.size() == 0){
				return firstTen;
			}
			ArrayList<Integer> lastTen = ProcessComment(Arrays.copyOfRange(lines, lines.length - 10, lines.length));	// Tries processing the last 10 lines
			if (lastTen.size() == 0){
				return firstTen;
			}else{
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
			}
		}else{	// Process correct formatting (10 lines)
			for (int j = 0; j < lines.length; j++){
				boolean success = ProcessLine(lines[j], j, 0);
				if (lines[0].toLowerCase().contains("taeyeon") && lines[2].toLowerCase().contains("3. mansae") && !success){
					System.out.println("j = " + j + ", line was \"" + lines[j] + "\"");
				}
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
		if (line.toLowerCase().contains("3. mansae")){
			System.out.println("Processing line with splitter " + splitters[attemptNum] + " : \"" + line + "\"");
			System.out.println("Parts length: " + parts.length);
		}
		if (parts.length < 2){ 			// Try to split by " ", find word combinations in an existing song name in the chart
			//System.out.println("Processing 0- line with splitter " + splitters[attemptNum] + " : \"" + line + "\"");
			parts = line.split(" ");
			if (parts.length < 2){		// Failed Parse
				return false;
			}else{		// Potentially correct, must determine if there is a number in front -> try to find word in chart
				// Format of "1. song artist" or "1 song artist" or "song artist"
				points = ProcessPoints(parts[0], lineNum);
				String tempCheck = parts[1];
				for (int i = 1; i < parts.length - 1; i++){		// Looping through words to search in chart if they exist already
					if (i > 1){
						tempCheck += (" " + parts[i]);
					}
					for (int j = 0; j < chart.getChartSongs().getSize(); j++){
						if (tempCheck.toLowerCase().equals(chart.getChartSongs().GetValueAt(j).getSongName())){	// If the song matches a chart song
							song = tempCheck;
							artist = CombineRestOfParts(parts, i);
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
			if (line.toLowerCase().contains("3. mansae")){
				System.out.println("Processing correct line with splitter " + splitters[attemptNum] + " : \"" + line + "\"");
			}
			if (parts[0].length() < 2 || parts[1].length() < 2){
				return false;
			}
			if (line.toLowerCase().contains("3. mansae")){
				System.out.println("No length issues");
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
			/*if (line.toLowerCase().contains("make-up")){
				System.out.println("Processing 3- line with splitter " + splitters[attemptNum] + " : \"" + line + "\"");
				System.out.println("Hyphen: " + hasHyphenString);
			}*/
			if (hasHyphenString){	// Special case processing
				if ((parts[0] + "-" + parts[1]).toLowerCase().contains(matchString)){
					song = ProcessSongName(parts[0] + "-" + parts[1]);
					artist = ProcessArtist(parts[2]);
					points = ProcessPoints(parts[0], lineNum);
					tempSongs.add(new ChartSong(song, artist, points));
					return true;
				}else if ((parts[1] + "-" + parts[2]).toLowerCase().contains(matchString)){
					song = ProcessSongName(parts[0]);
					artist = ProcessArtist(parts[1] + "-" + parts[2]);
					points = ProcessPoints(parts[0], lineNum);
					tempSongs.add(new ChartSong(song, artist, points));
					return true;
				}else{	// Failed Parse
					return false;
				}
				/*
				if (parts[1].length() < 3){	// Format of "1. so crazy - t-ara"
					if (line.toLowerCase().contains("make-up")){
						System.out.println("Branch 1");
					}
					song = ProcessSongName(parts[0]);
					artist = ProcessArtist(parts[1] + "-" + parts[2]);
					points = ProcessPoints(parts[0], lineNum);
					tempSongs.add(new ChartSong(song, artist, points));
					return true;
				}else if (parts[0].split(" ").length == 2 && parts[0].split(" ")[1].length() == 1){		// Format of "1) t-ara - so crazy"
					if (line.toLowerCase().contains("make-up")){
						System.out.println("Branch 2");
					}
					song = ProcessSongName(parts[2]);
					artist = ProcessArtist(parts[0] + "-" + parts[1]);
					points = ProcessPoints(parts[0], lineNum);
					tempSongs.add(new ChartSong(song, artist, points));
					return true;
				}*/
				
			}else if (IsDigits(parts[0])){	// Format of 1- song - artist
				/*if (line.contains("VIXX")){
					int test = ProcessPoints(parts[0], lineNum);
					System.out.println("Parts (Pts = " + test + ", lineNum = " + lineNum + "): " + parts[0] + " // " + parts[1] + " // " + parts[2]);
				}*/
				if (ProcessPoints(parts[0], lineNum) - (10 - lineNum) < 5){	// If it's reasonable that they numbered their songs
					/*if (line.contains("VIXX")){
						System.out.println("number checks out");
					}*/
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
			//System.out.println("Processing 4- line with splitter " + splitters[attemptNum] + " : \"" + line + "\"");
			if (IsDigits(parts[0])){	// Format of 1- song artist
				if (ProcessPoints(parts[0], lineNum) - lineNum < 5){	// If it's reasonable that they numbered their songs
					boolean hasHyphenString = false;
					for (int i = 0; i < hyphenStrings.length; i++){
						if (RemoveAllSpaces(line.toLowerCase()).contains(hyphenStrings[i])){
							hasHyphenString = true;
							break;
						}
					}
					if (hasHyphenString){	// Special case processing
						if (parts[2].length() < 3 || parts[3].length() < 3){		// Format of "1- so crazy - t-ara"
							song = ProcessSongName(parts[1]);
							artist = ProcessArtist(parts[2] + "-" + parts[3]);
							points = ProcessPoints(parts[0], lineNum);
							tempSongs.add(new ChartSong(song, artist, points));
							return true;
						}else if (parts[1].length() < 3){		// Format of "1- t-ara - so crazy"
							song = ProcessSongName(parts[3]);
							artist = ProcessArtist(parts[1] + "-" + parts[2]);
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
		while (songChars.contains(tempSong.substring(0,1))){	// removes numbers and spaces at the beginning of the name
			if (tempSong.length() > 1 && Character.isDigit(tempSong.charAt(0)) && Character.isLetter(tempSong.charAt(1))){	// Saves band names like 4Minute
				break;
			}else if (tempSong.length() > 1){
				tempSong = tempSong.substring(1, tempSong.length());
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
			//System.out.println("Song earned: " + (11 - points) + " points");
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
		return tempString.substring(0, tempString.length() - 1);
	}

	// Program starts here
	public static void main(String[] args){
		new MainWindow();
	}
}
