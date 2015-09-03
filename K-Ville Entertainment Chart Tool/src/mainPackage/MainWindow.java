package mainPackage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.TitledBorder;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentListResponse;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.common.collect.Lists;

public class MainWindow extends JFrame implements ActionListener{

	private static final long serialVersionUID = 414975340316732097L;
	private JPanel pnlMain;
	private JPanel pnlCentre;
	private JPanel[] pnlRows = new JPanel[2];
	private JTextField[] txtRows = new JTextField[2];
	private JButton btnBrowse = new JButton("Browse");
	private JButton btnGenerate = new JButton("Generate Chart Totals");
	private String savePath = "";
	private String videoURL = "";
	private static YouTube youtube;
	private Chart chart = new Chart();
	private ArrayList<String> commentList;
	private ArrayList<ChartSong> tempSongs = new ArrayList<ChartSong>();
	private static final String VERSION_NUMBER = "0.9";
	private final String[] splitters = new String[]{"-","by","/","~","|"};
	private final String[] titles = new String[]{"Video URL","Save Directory"};
	private String artistChars = ";,?";
	private String songChars = "-1023456789.);?,";
	private JMenuBar menuBar = new JMenuBar();
	private JMenu fileMenu = new JMenu("File");
	private JMenuItem updateChartFile = new JMenuItem("Update Top 50 Chart");
	
	// Window Creation
	public MainWindow(){
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e3) { e3.printStackTrace();	}
		
		updateChartFile.addActionListener(this);
		fileMenu.add(updateChartFile);
		menuBar.add(fileMenu);
		
		pnlCentre = new JPanel();
		pnlCentre.setLayout(new GridLayout(2,1,0,5));
		pnlCentre.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
		btnBrowse.addActionListener(this);
		btnGenerate.addActionListener(this);
		
		for (int i = 0; i < 2; i++){
			txtRows[i] = new JTextField();
			txtRows[i].setBorder(BorderFactory.createLineBorder(Color.GRAY));
			txtRows[i].setFont(new Font("Arial", Font.PLAIN, 12));
			
			pnlRows[i] = new JPanel();
			pnlRows[i].setLayout(new BorderLayout());
			TitledBorder title = new TitledBorder(BorderFactory.createEmptyBorder(), titles[i]);
			title.setTitleJustification(TitledBorder.LEFT);
			pnlRows[i].setBorder(title);
		}
		pnlRows[0].add(txtRows[0]);
		pnlRows[1].add(txtRows[1]);
		pnlRows[1].add(btnBrowse, BorderLayout.EAST);
		pnlCentre.add(pnlRows[0]);
		pnlCentre.add(pnlRows[1]);
		
		pnlMain = new JPanel();
		pnlMain.setLayout(new BorderLayout());
		pnlMain.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		pnlMain.add(pnlCentre);
		pnlMain.add(btnGenerate, BorderLayout.SOUTH);
		
		this.add(pnlMain);
		this.setJMenuBar(menuBar);
		this.setIconImage(new ImageIcon("icon.png").getImage());
        this.setTitle("K-Ville Entertainment Chart Tool v" + VERSION_NUMBER);
        this.setSize(400,200);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
	}
	
	// Action Listeners
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnBrowse){			// Browse for Save Directory 
			JFileChooser opener = new JFileChooser();
			opener.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	        int ret = opener.showDialog(this, "Choose a Directory");
	        if(ret == JFileChooser.APPROVE_OPTION){    // On selection, displays and sets path
	            savePath = opener.getSelectedFile().getPath();
	            txtRows[1].setText(savePath);
	            txtRows[1].setCaretPosition(0);
	        }
		}else if (e.getSource() == btnGenerate){	// Generate the Chart and text file
			if (txtRows[0].getText().length() < 2 || txtRows[1].getText().length() < 2){
				JOptionPane.showMessageDialog(this, "Required information not set. Make sure to specify the video URL and save directory.", "Missing Information", JOptionPane.ERROR_MESSAGE);
			}else{
				videoURL = txtRows[0].getText();
				if (videoURL.split("=").length > 1){	// If full URL is included, gets ID portion
					videoURL = videoURL.split("=")[1];
				}
				chart = new Chart(savePath);
				GenerateChart();
			}
		}else if (e.getSource() == updateChartFile){	// Updating Chart .txt file after manual processing
			JFileChooser opener = new JFileChooser();
	        int ret = opener.showDialog(this, "Choose Chart File");
	        if(ret == JFileChooser.APPROVE_OPTION){   
	            String tempPath = opener.getSelectedFile().getPath();
	            chart.UpdateChartFile(tempPath);
	            JOptionPane.showMessageDialog(this, "Updated Top 50 Chart!", "Updated Chart", JOptionPane.INFORMATION_MESSAGE);
	        }
		}
	}
	
	// Generates the Chart and corresponding files from the video's comments
	private void GenerateChart(){
		this.setTitle("Fetching YouTube comments...");
		// TODO Fetch comments (15%) -> replies (75%) -> Processing comments (5%) -> Processing Chart (3%) -> Creating Chart (2%)
		FetchComments();				// Fetch comments from YouTube
		this.setTitle("Processing comments...");
		ProcessComments(commentList);	// Process fetched comments and add them to the Chart
		this.setTitle("Processing chart...");
		chart.ProcessChart();			// Chart processing to reduce manual post-editing
		this.setTitle("Creating Files...");
		chart.CreateChart();			// File creation
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
            youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-commentthreads-sample").build();

            // Call the YouTube Data API's commentThreads.list method to retrieve video comment threads.
            CommentThreadListResponse videoCommentsListResponse = youtube.commentThreads().list("snippet").setVideoId(videoURL).setTextFormat("plainText").setMaxResults((long)100).execute();
            List<CommentThread> videoComments = videoCommentsListResponse.getItems();
            String nextToken = videoCommentsListResponse.getNextPageToken();
            System.out.println("Next token: \"" + nextToken + "\"");
            while (nextToken != null){
            	videoCommentsListResponse = youtube.commentThreads().list("snippet").setVideoId(videoURL).setTextFormat("plainText").setPageToken(nextToken).setMaxResults((long)100).execute();
            	List<CommentThread> tempList = videoCommentsListResponse.getItems(); 
            	for (int i = 0; i < tempList.size(); i++){
            		videoComments.add(tempList.get(i));
            	}
                nextToken = videoCommentsListResponse.getNextPageToken();
                System.out.println("Next token: \"" + nextToken + "\"");
            }
            
            if (videoComments.isEmpty()) {
                System.out.println("Can't get video comments.");
            } else {
            	ArrayList<String> comments = new ArrayList<String>();
            	int i = 0;
                for (CommentThread videoComment : videoComments) {
                	this.setTitle("Fetching YouTube replies (" + (int)((i + 1) / (float)videoComments.size() * 75) + "%)");
                	System.out.println("Processing reply... (" + i + ")");
                	CommentSnippet snippet = videoComment.getSnippet().getTopLevelComment().getSnippet();
                    comments.add(snippet.getTextDisplay());
                	CommentListResponse commentsListResponse = youtube.comments().list("snippet").setParentId(videoComment.getId()).setTextFormat("plainText").execute();
                	if (!commentsListResponse.isEmpty()){
	                    List<Comment> comms = commentsListResponse.getItems();
	                    if (!comms.isEmpty()){
	                        for (Comment commentReply : comms) {
                        		CommentSnippet snip = commentReply.getSnippet();
                                comments.add(snip.getTextDisplay());
	                        }
	                    }
                    }
                	i++;
                }
                commentList = comments;
            }
        } catch (GoogleJsonResponseException e) {
        	JOptionPane.showMessageDialog(this, "JSON Exception encountered. Error code: " + e.getDetails().getCode() + ".", "JSON Exception", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
        	JOptionPane.showMessageDialog(this, "IO Exception encountered.", "IO Exception", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable t) {
        	JOptionPane.showMessageDialog(this, "Throwable Exception encountered. Message: " + t.getMessage() + ".", "Throwable Exception", JOptionPane.ERROR_MESSAGE);
        }
    }
	
	// Processes all comments retrieved from YouTube
	private void ProcessComments(ArrayList<String> comments){
		for (int i = 0; i < comments.size(); i++){
			String comment = comments.get(i);
			comment = comment.substring(0, comment.length() - 1); // Removes "?" at the end of each comment
			ArrayList<Integer> failedLines = ProcessComment(comment.split("\n"));	// Processes each line of the comment
			if (failedLines.size() > 0 && comment.split("\n").length - failedLines.size() < 2){	// If all but 1 lines in the comment failed, ignores the comment
				tempSongs = new ArrayList<ChartSong>();
			}
			CheckForDuplicates();	// Checks the list of comments for duplicates
			
			// Formats and adds all successful songs to the chart
			for (int j = 0; j < tempSongs.size(); j++){		
				tempSongs.get(j).setArtistName(RemoveSpaces(tempSongs.get(j).getArtistName().toLowerCase()));
				tempSongs.get(j).setSongName(RemoveSpaces(tempSongs.get(j).getSongName().toLowerCase()));
				chart.AddValue(tempSongs.get(j));
			}
			if (failedLines.size() > 0){	// If lines failed, adds them to the list of failed parses
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
				if (!success){
					failedLines.add(j);
				}
			}
			return failedLines;
		}
	}
	
	// Processes a line involving a song rank
	private boolean ProcessLine(String line, int lineNum, int attemptNum){
		if (line.contains("kville") || line.contains("k-ville")){
			return false;
		}
		String[] parts = line.split(splitters[attemptNum]);
		String song;
		String artist;
		int points;
		System.out.println("Processing line: \"" + line + "\"");
		if (parts.length < 2){ 			// Try to split by " ", find word combinations in an existing song name in the chart
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
			if (parts[0].length() < 2 || parts[1].length() < 2){
				return false;
			}
			song = ProcessSongName(parts[0]);
			artist = ProcessArtist(parts[1]);
			points = ProcessPoints(parts[0], lineNum);
			tempSongs.add(new ChartSong(song, artist, points));
			return true;
		}else if (parts.length == 3 && attemptNum == 0){		// One too many -'s (could be band name) 1. t-ara - so crazy     1. so crazy - t-ara
			if (line.toLowerCase().contains("t-ara") || line.toLowerCase().contains("g-d") ||line.toLowerCase().contains("g-dragon") || line.toLowerCase().contains("g-friend")){	// Special case processing
				if (parts[1].length() < 3){	// Format of "1. so crazy - t-ara"
					song = ProcessSongName(parts[0]);
					artist = ProcessArtist(parts[1] + "-" + parts[2]);
					points = ProcessPoints(parts[0], lineNum);
					tempSongs.add(new ChartSong(song, artist, points));
					return true;
				}else if (parts[0].split(" ").length == 2 && parts[0].split(" ")[1].length() == 1){		// Format of "1) t-ara - so crazy"
					song = ProcessSongName(parts[2]);
					artist = ProcessArtist(parts[0] + "-" + parts[1]);
					points = ProcessPoints(parts[0], lineNum);
					tempSongs.add(new ChartSong(song, artist, points));
					return true;
				}else{	// Failed Parse
					return false;
				}
			}else if (line.toLowerCase().contains("ah-ah")){ 	// Special case for ah-ah
				if (parts[0].toLowerCase().contains("ah") && parts[1].toLowerCase().contains("ah")){
					song = ProcessSongName(parts[0] + "-" + parts[1]);
					artist = ProcessArtist(parts[2]);
					points = ProcessPoints(parts[0], lineNum);
					tempSongs.add(new ChartSong(song, artist, points));
					return true;
				}else if (parts[1].toLowerCase().contains("ah") && parts[2].toLowerCase().contains("ah")){
					song = ProcessSongName(parts[1] + "-" + parts[2]);
					artist = ProcessArtist(parts[0]);
					points = ProcessPoints(parts[0], lineNum);
					tempSongs.add(new ChartSong(song, artist, points));
					return true;
				}else{
					return false;	// Failed parse
				}
			}else if (IsDigits(parts[0])){	// Format of 1- song - artist
				if (ProcessPoints(parts[0], lineNum) - lineNum < 5){	// If it's reasonable that they numbered their songs
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
					if (line.toLowerCase().contains("t-ara") || line.toLowerCase().contains("g-d") ||line.toLowerCase().contains("g-dragon") || line.toLowerCase().contains("g-friend")){	// Special case processing
						if (parts[2].length() < 3){		// Format of "1- so crazy - t-ara"
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
		while (artistChars.contains(tempArt.substring(tempArt.length() - 1))){	// Removing unwanted characters at the end
			tempArt = tempArt.substring(0, tempArt.length() - 1);
		}
		if (tempArt.split(" ").length > 1 && tempArt.split(" ")[0].length() > 0 && Character.isDigit(tempArt.split(" ")[0].charAt(0))){	// format of "1. artist song"
			tempArt = CombineRestOfParts(tempArt.split(" "), 0);
		}
		while (artistChars.contains(tempArt.substring(0,1))){	// Removing unwanted characters at the beginning
			tempArt = tempArt.substring(1, tempArt.length());
		}
		return tempArt.toLowerCase();
	}
	
	// Process how many points to give the song
	private int ProcessPoints(String song, int line){
		int lastNumIndex = 0;
		int counter = 0;
		if (Character.isDigit(song.charAt(0))){		// Number given
			while (counter < song.length() && Character.isDigit(song.charAt(counter))){
				lastNumIndex = counter;
				counter++;
			}
			int points = Integer.parseInt(song.substring(0, lastNumIndex + 1));
			System.out.println("Song earned: " + (11 - points) + " points");
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
	
	// Checks if a string is all digits
	private boolean IsDigits(String str){
		if (str.length() < 1){
			return false;
		}
		for (int i = 0; i < str.length(); i++){
			if (!Character.isDigit(str.charAt(i)) && str.charAt(i) != '.'&& str.charAt(i) != ' ' && str.charAt(i) != ')'){
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
