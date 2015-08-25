package mainPackage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.TitledBorder;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.common.collect.Lists;

/*
 * TODO:
 * - Expand first 100 to as many comments as needed
 * - Develop algorithm to parse comments
 * - determine if replies are being included
 * 
 * - Detect flipped songs / artists?
 * - track duplicate songs in a comment
 * - add processing label to show progress
 */

public class MainWindow extends JFrame implements ActionListener{

	private static final long serialVersionUID = 414975340316732097L;
	private JPanel pnlMain;
	private JPanel pnlCentre;
	private JPanel pnlMaxCom;
	private JPanel[] pnlRows = new JPanel[2];
	private JTextField[] txtRows = new JTextField[2];
	private JLabel lblMaxCom = new JLabel("Max # of Comments:");
	private JSpinner spnCom;
	private JButton btnBrowse = new JButton("Browse");
	private JButton btnGenerate = new JButton("Generate Chart Totals");
	private String savePath = "";
	private String videoURL = "";
	private static YouTube youtube;
	private long maxComments = 1;
	private Chart chart;
	private ArrayList<String> commentList;
	private ArrayList<ChartSong> tempSongs = new ArrayList<ChartSong>();
	private static final String VERSION_NUMBER = "0.1";
	private final String[] titles = new String[]{"Video URL","Save Directory"};
	private String artistChars = " ;,";
	private String songChars = " 1023456789.);,";
	private String numChars = "1023456789";
	
	// Window Creation
	public MainWindow(){
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e3) { e3.printStackTrace();	}
		
		pnlCentre = new JPanel();
		pnlCentre.setLayout(new GridLayout(3,1,0,5));
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
		
		pnlMaxCom = new JPanel();
		pnlMaxCom.setLayout(new BorderLayout());
		pnlMaxCom.add(lblMaxCom);
		spnCom = new JSpinner(new SpinnerNumberModel(500,100,5000,100));
		pnlMaxCom.add(spnCom, BorderLayout.EAST);
		pnlCentre.add(pnlMaxCom);
		
		pnlMain = new JPanel();
		pnlMain.setLayout(new BorderLayout());
		pnlMain.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		pnlMain.add(pnlCentre);
		pnlMain.add(btnGenerate, BorderLayout.SOUTH);
		
		this.add(pnlMain);
		this.setIconImage(new ImageIcon("icon.png").getImage());
        this.setTitle("K-Ville Entertainment Chart Tool v" + VERSION_NUMBER);
        this.setSize(400,230);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
	}
	
	// Action Listeners
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnBrowse){			// Browse for Save Directory 
			JFileChooser opener = new JFileChooser();
			opener.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	        int ret = opener.showOpenDialog(this);
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
				maxComments = (int)spnCom.getValue();
				chart = new Chart(savePath);
				GenerateChart();
			}
		}
	}
	
	/* Generates chart according to the following procedure:
		1. Fetches YouTube video comments
		2. Processes comment into strings (and tracks comment which could not be understood)
		3. Creates Chart object, FailedComments object
		4. Saves Chart and FailedComments objects in .txt files
	*/
	private void GenerateChart(){
		FetchComments();
		ProcessComments(commentList);
		/*try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(savePath + "/comments.txt"));
			for (int i = 0; i < commentList.size(); i++){
				bw.write("#" + (i + 1) + " - " + commentList.get(i));
				bw.newLine();
			}
			bw.close();
		}catch(IOException e){ e.printStackTrace();	}*/
		JOptionPane.showMessageDialog(this, "Finished generation!", "Finished", JOptionPane.INFORMATION_MESSAGE);
	}
	
	/* Processes all comments into the rankings by:
		1) Splitting by newline character
		2) Processing each line
	*/
	private void ProcessComments(ArrayList<String> comments){
		for (int i = 0; i < comments.size(); i++){
			String comment = comments.get(i);
			boolean success = ProcessComment(comment);
			if (success){	// Adds all temporary ChartSong's to the Chart, resets temporary cache
				for (int j = 0; j < tempSongs.size(); j++){
					chart.AddValue(tempSongs.get(j));
				}
				tempSongs = new ArrayList<ChartSong>();
			}else{			// Parse failed
				chart.AddFailedParse(comment);
			}
		}
	}
	
	// Returns false if it fails parse
	private boolean ProcessComment(String comment){
		String[] lines = comment.split("\n");
		System.out.println("Number of lines: " + lines.length);
		if (lines.length < 10){			// Process not enough songs here -> process as if splitting 55 points among number of songs
			
		}else if (lines.length == 10){	// Process correct number of songs here
			for (int j = 0; j < lines.length; j++){
				boolean success = ProcessLine(lines[j], j);
				if (!success){
					return false;
				}
			}
			return true;
		}else{							// Process too many lines here (likely a list + comment)
			// Try reproccesing with first ten, last ten lines
		}
		return false;
	}
	
	// Processes a line involving a song rank
	private boolean ProcessLine(String line, int lineNum){
		String[] parts = line.split("-");
		String song;
		String artist;
		int points;
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
				return false;	// If the song was not found, parse failed
			}
		}else if (parts.length == 2){	// Correct formatting
			song = ProcessSongName(parts[0]);
			artist = ProcessArtist(parts[1]);
			points = ProcessPoints(parts[0], lineNum);
			tempSongs.add(new ChartSong(song, artist, points));
			return true;
		}else if (parts.length == 3){		// One too many -'s (could be band name) 1. t-ara - so crazy     1. so crazy - t-ara
			if (line.toLowerCase().contains("t-ara") || line.toLowerCase().contains("g-dragon") || line.toLowerCase().contains("g-friend")){	// Special case processing
				if (parts[1].length() == 1){	// Format of "1. so crazy - t-ara"
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
				}
			}else{		// Failed Parse
				return false;
			}
		}else{		// 4 or more -'s -> Failed Parse
			return false;
		}
		return true;
	}
	
	private String ProcessSongName(String song){
		String tempSong = song;
		while (tempSong.substring(tempSong.length() - 1).equals(" ")){	// removes spaces at the end of the name
			tempSong = tempSong.substring(0, tempSong.length() - 1);
		}
		while (songChars.contains(tempSong.substring(0,1))){	// removes numbers and spaces at the beginning of the name
			if (Character.isDigit(tempSong.charAt(0)) && Character.isLetter(tempSong.charAt(1))){	// Saves band names like 4Minute
				break;
			}
			tempSong = tempSong.substring(0, tempSong.length() - 1);
		}
		return tempSong.toLowerCase();
	}
	
	private String ProcessArtist(String artist){
		String tempArt = artist;
		while (artistChars.contains(tempArt.substring(tempArt.length() - 1))){	// Removing unwanted characters at the end
			tempArt = tempArt.substring(0, tempArt.length() - 1);
		}
		if (tempArt.split(" ").length > 1 && Character.isDigit(tempArt.split(" ")[0].charAt(0))){	// format of "1. artist song"
			tempArt = CombineRestOfParts(tempArt.split(" "), 0);
		}
		while (artistChars.contains(tempArt.substring(0,1))){	// Removing unwanted characters at the beginning
			tempArt = tempArt.substring(1, tempArt.length());
		}
		return tempArt.toLowerCase();
	}
	
	private int ProcessPoints(String song, int line){
		int lastNumIndex = 0;
		int counter = 0;
		if (Character.isDigit(song.charAt(0))){		// Number given
			while (Character.isDigit(song.charAt(counter))){
				lastNumIndex = counter;
				counter++;
			}
			return Integer.parseInt(song.substring(0, lastNumIndex + 1)); 
		}else{		// Numbers not included, just a list of songs and artists
			return 10 - line;
		}
	}
	
	private String CombineRestOfParts(String[] parts, int index){
		String tempString = "";
		for (int i = index + 1; i < parts.length; i++){
			tempString += (parts[i] + " ");
		}
		return tempString.substring(0, tempString.length() - 1);
	}
	
	private void FetchComments(){
		List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl");
		scopes.add("https://www.googleapis.com/auth/youtube.force-ssl");

        try {
            // Authorize the request.
            Credential credential = Auth.authorize(scopes, "commentthreads");

            // This object is used to make YouTube Data API requests.
            youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-commentthreads-sample").build();

            // Prompt the user for the ID of a video to comment on.
            // Retrieve the video ID that the user is commenting to.
            String videoId = videoURL;
            System.out.println("You chose " + videoId + " to subscribe.");

            // Prompt the user for the comment text.
            // Retrieve the text that the user is commenting.
            String text = "";
            System.out.println("You chose " + text + " to subscribe.");

            // All the available methods are used in sequence just for the sake
            // of an example.

            // Call the YouTube Data API's commentThreads.list method to
            // retrieve video comment threads.
            CommentThreadListResponse videoCommentsListResponse = youtube.commentThreads().list("snippet").setVideoId(videoId).setTextFormat("plainText").setMaxResults((long)100).execute();
            List<CommentThread> videoComments = videoCommentsListResponse.getItems();

            if (videoComments.isEmpty()) {
                System.out.println("Can't get video comments.");
            } else {
            	ArrayList<String> comments = new ArrayList<String>();
                // Print information from the API response.
                System.out.println("\n================== Returned Video Comments ==================\n");
                for (CommentThread videoComment : videoComments) {
                    CommentSnippet snippet = videoComment.getSnippet().getTopLevelComment().getSnippet();
                    System.out.println("  - Author: " + snippet.getAuthorDisplayName());
                    System.out.println("  - Comment: \n" + snippet.getTextDisplay());
                    System.out.println("\n-------------------------------------------------------------\n");
                    comments.add(snippet.getTextDisplay());
                }
                commentList = comments;
             /*   CommentThread firstComment = videoComments.get(0);

                // Will use this thread as parent to new reply.
                String parentId = firstComment.getId();

                // Create a comment snippet with text.
                CommentSnippet commentSnippet = new CommentSnippet();
                commentSnippet.setTextOriginal(text);
                commentSnippet.setParentId(parentId);

                // Create a comment with snippet.
                Comment comment = new Comment();
                comment.setSnippet(commentSnippet);

                // Call the YouTube Data API's comments.insert method to reply
                // to a comment.
                // (If the intention is to create a new top-level comment,
                // commentThreads.insert
                // method should be used instead.)
                Comment commentInsertResponse = youtube.comments().insert("snippet", comment)
                        .execute();

                // Print information from the API response.
                System.out
                        .println("\n================== Created Comment Reply ==================\n");
                CommentSnippet snippet = commentInsertResponse.getSnippet();
                System.out.println("  - Author: " + snippet.getAuthorDisplayName());
                System.out.println("  - Comment: " + snippet.getTextDisplay());
                System.out
                        .println("\n-------------------------------------------------------------\n");

                // Call the YouTube Data API's comments.list method to retrieve
                // existing comment replies.
                CommentListResponse commentsListResponse = youtube.comments().list("snippet")
                        .setParentId(parentId).setTextFormat("plainText").execute();
                List<Comment> comments = commentsListResponse.getItems();

                if (comments.isEmpty()) {
                    System.out.println("Can't get comment replies.");
                } else {
                    // Print information from the API response.
                    System.out
                            .println("\n================== Returned Comment Replies ==================\n");
                    for (Comment commentReply : comments) {
                        snippet = commentReply.getSnippet();
                        System.out.println("  - Author: " + snippet.getAuthorDisplayName());
                        System.out.println("  - Comment: " + snippet.getTextDisplay());
                        System.out
                                .println("\n-------------------------------------------------------------\n");
                    }
                    Comment firstCommentReply = comments.get(0);
                    firstCommentReply.getSnippet().setTextOriginal("updated");
                    Comment commentUpdateResponse = youtube.comments()
                            .update("snippet", firstCommentReply).execute();
                    // Print information from the API response.
                    System.out
                            .println("\n================== Updated Video Comment ==================\n");
                    snippet = commentUpdateResponse.getSnippet();
                    System.out.println("  - Author: " + snippet.getAuthorDisplayName());
                    System.out.println("  - Comment: " + snippet.getTextDisplay());
                    System.out
                            .println("\n-------------------------------------------------------------\n");
                }*/
            }
        } catch (GoogleJsonResponseException e) {
            System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode()
                    + " : " + e.getDetails().getMessage());
            e.printStackTrace();

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getMessage());
            t.printStackTrace();
        }
    }


	// Program starts here
	public static void main(String[] args){
		new MainWindow();
	}
}
