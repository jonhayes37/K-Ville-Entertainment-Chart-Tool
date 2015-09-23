package mainPackage;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class Updater {

	private String projectName;
	private String localVersion;
	private String remoteVersion;
	private static final String FTP_SERVER = "www.jonathanhayes.ca";
	private static final int FTP_PORT = 21;
	private static final String FTP_USERNAME = "jonhayes37";
	private static final String FTP_PASSWORD = "Canon7DFTW";
	private final String path = System.getProperty("user.dir");
	
	public Updater(String name, String ver){
		this.projectName = name;
		this.localVersion = ver;
		this.remoteVersion = "init";
	}
	
	// Returns true if the local version is up to date
	public void CheckVersion(){
		FTPDownload(true);
		try {
			BufferedReader br = new BufferedReader(new FileReader(path + "/latest_versions.txt"));
			String line = br.readLine();
			while (line != null){
				if (line.split("<->")[0].equals(this.projectName)){
					this.remoteVersion = line.split("<->")[1];
					break;
				}else{
					line = br.readLine();
				}
			}
			br.close();
		} catch (IOException e) { 
			JOptionPane.showMessageDialog(null, "Could not read the latest version file.", "File Read Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace(); 
		}
		if (!remoteVersion.equals(localVersion)){	// Update Available
			int choice = JOptionPane.showConfirmDialog(null, "<html>An update to " + projectName + " is available.<br><br>Your version: " + localVersion + "<br>Latest version: " + remoteVersion + "<br><br>Would you like to download the latest version?", projectName + " Updater", JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.YES_OPTION){
				DownloadLatestVersion();
			}else{	// Deletes the latest_versions file
				File f = new File(path + "/latest_versions.txt");
				if (f.exists()){
					f.delete();
				}
			}
		}else{	// No updates
			JOptionPane.showMessageDialog(null, "You are up to date!", projectName + " Updater", JOptionPane.INFORMATION_MESSAGE);
			File f = new File(path + "/latest_versions.txt");
			if (f.exists()){
				f.delete();
			}
		}
	}
	
	// Downloads the latest version into the directory, unzips the contents into
	// the program's working directory, and deletes the original downloaded zip
	public void DownloadLatestVersion(){
		FTPDownload(false);
		Unzip();
		Cleanup();
		JOptionPane.showMessageDialog(null, "The latest version has been downloaded. Please close the program, delete this version's .jar file, and run the new version's .jar file.", projectName + " Updater", JOptionPane.INFORMATION_MESSAGE);
	}
	
	// Downloads either the latest_versions file, or the latest
	// version of the running program
	private void FTPDownload(boolean checking){
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(FTP_SERVER, FTP_PORT);
            ftpClient.login(FTP_USERNAME, FTP_PASSWORD);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
 
            String remoteFile;
            File downloadFile;
            if (checking){
            	remoteFile = "/public_html/Download_Files/latest_versions.txt";
            	downloadFile = new File(path + "/latest_versions.txt");
            }else{
            	remoteFile = "/public_html/Download_Files/" + projectName + " " + remoteVersion + ".zip";
            	downloadFile = new File(path + "/" + projectName + " " + remoteVersion + ".zip");
            }
            
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile));
            ftpClient.retrieveFile(remoteFile, outputStream);
            outputStream.close();

        } catch (IOException ex) {
        	String fileName = (checking) ? "latest version" : "new version";
        	JOptionPane.showMessageDialog(null, "Could not retrieve the " + fileName + " file.", "FTP Error", JOptionPane.ERROR_MESSAGE);
        	ex.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
            	JOptionPane.showMessageDialog(null, "Could not close the active FTP connection.", "FTP Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
	}
	
	// Unzips a zip folder
	private void Unzip(){
		
		try{	    			        
            File destDir = new File(path);
            if (!destDir.exists()) {
                destDir.mkdir();
            }
            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(path + "/" + projectName + " " + remoteVersion + ".zip"));
            ZipEntry entry = zipIn.getNextEntry();

            File mainDir = new File(path + "/" + this.projectName + " " + this.remoteVersion);
            if (!mainDir.exists()){
            	mainDir.mkdir();
            }
            
            while (entry != null) {		// Iterates over entries in the zip file
                String filePath = path + File.separator + entry.getName();
                if (!entry.isDirectory()) { 	// If the entry is a file, extracts it
                    ExtractFile(zipIn, filePath);
                } else {	// If the entry is a directory, make the directory
                    File dir = new File(filePath);
                    dir.mkdir();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();  		
	    }catch(IOException ex){ 
	    	JOptionPane.showMessageDialog(null, "Could not extract the new version's file.", "Unzip Error", JOptionPane.ERROR_MESSAGE);
	    	ex.printStackTrace(); 
	    }
	}

	// Extracts a file from a zip folder
	private void ExtractFile(ZipInputStream zipIn, String filePath) throws IOException {
	    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
	    byte[] bytesIn = new byte[4096];
	    int read = 0;
	    while ((read = zipIn.read(bytesIn)) != -1) {
	        bos.write(bytesIn, 0, read);
	    }
	    bos.close();
	}
	
	// Cleans up downloaded files and moves new files to the appropriate place
	private void Cleanup(){
		ArrayList<String> directories = new ArrayList<String>();
		
		// Deleting the downloaded .zip and latest_versions file which was used to check the version
		File f = new File(path + "/" + projectName + " " + remoteVersion + ".zip");
		if (f.exists()){
			f.delete();
		}
		f = new File(path + "/latest_versions.txt");
		if (f.exists()){
			f.delete();
		}
		
		// Moving files to the working directory
		File oldFolder = new File(path + "/" + projectName + " " + remoteVersion);
		File[] contents = oldFolder.listFiles();
		for (File file : contents){
			if (file.isDirectory()){
				directories.add(file.getName());
				File newDir = new File(path + "/" + file.getName());
				newDir.mkdir();
				File[] innerFiles = file.listFiles();
				for (File inFile : innerFiles){
					if (!inFile.getName().equals(file.getName())){
						inFile.renameTo(new File(newDir.getAbsolutePath() + "/" + file.getName()));
					}
				}
			}else{
				file.renameTo(new File(path + "/" + file.getName()));
			}
		}
		for (int i = 0; i < directories.size(); i++){
			File toDelete = new File(path + "/" + directories.get(i) + "/" + directories.get(i));
			if (toDelete.exists()){
				toDelete.delete();
			}
		}
		DeleteDirectory(oldFolder);
	}
	
	// Deletes all contents of a directory
	public static boolean DeleteDirectory(File directory) {
	    if(directory.exists()){
	        File[] files = directory.listFiles();
	        if(files != null){
	            for(int i = 0; i < files.length; i++) {
	                if(files[i].isDirectory()) {
	                    DeleteDirectory(files[i]);
	                }
	                else {
	                    files[i].delete();
	                }
	            }
	        }
	    }
	    return(directory.delete());
	}
}
