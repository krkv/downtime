import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

class SendMessage implements Runnable {
	
	String status;	
	
	String site;
	int error;
	String host;
	String username;
	String password;
	String to;
	
	public SendMessage(String status, int error, String site, String host,
			String username, String password, String to) {
		this.status = status;
		this.error = error;
		this.site = site;
		this.host = host;
		this.username = username;
		this.password = password;
		this.to = to;
	}	

	@Override
	public void run() {
		
		Properties props = new Properties();
	    Session session = Session.getInstance(props);
		
	    try {
			System.out.println("Sending a notification email.");
			MimeMessage msg = new MimeMessage(session);
	        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
	        if (status.equals("down")) {
	        	msg.setSubject("Your website is down!");				        
	        	msg.setText("Oh no! Your website " + site + " is down! Status: " + error + ". Time to fix it! WebsiteMonitor.");
	        } else if (status.equals("up")) {
	        	msg.setSubject("Your website is up again!");				        
	        	msg.setText("Good news! Your website " + site + " is up again! WebsiteMonitor.");
	        }
		    Transport t = session.getTransport("smtps");
		    try {
		    	t.connect(host, username, password);
		    	t.sendMessage(msg, msg.getAllRecipients());
		    	System.out.println("Notification email sent.");
		    } finally {
		    	t.close();
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

public class Main {
	
	static int downtime_counter = -1;
	static int error;
	
	static String site;
	static int interval;
	static int notify_at;
	
	// notification information
	static String host;
	static String username;
	static String password;
	static String to;
	
	// checks the website status
	public static int site_status(String u) {
				
		try {
			HttpURLConnection c = (HttpURLConnection)(new URL(u)).openConnection();
			return c.getResponseCode();
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
			
	};
	
	public static void start() {
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		try {
			System.out.print("Website to monitor: http://");
			site = "http://" + br.readLine();
			System.out.print("Check interval in seconds: ");
			interval = Integer.parseInt(br.readLine());
			System.out.print("Number of unsuccesful checks before notifying: ");
			notify_at = Integer.parseInt(br.readLine());
			System.out.print("SMTP host: ");
			host = br.readLine();
			System.out.print("SMTP username: ");
			username = br.readLine();
			System.out.print("SMTP password: ");
			password = br.readLine();
			System.out.print("Noification to: ");
			to = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public static void main(String[] args) {
		
		start();
	
		// create logfile if required
		File log = new File("log.txt");
		if (!log.exists()) {
			try {
				log.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// the main program loop that performs checks
		while (true) {
			
			// the website is down
			if (site_status(site) >= 300) {
				
				System.out.println("Website error. Status: " + site_status(site));
				
				downtime_counter += 1;
				
				// if the downtime just started, save the time and error type, update log
				if (downtime_counter == 0) {
					error = site_status(site);
					
					// add the last downtime to log file
					try {
						java.util.Date date = new java.util.Date();
						Timestamp went_down = new Timestamp(date.getTime());
						BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("log.txt", true)));
						bw.write(went_down + " site went down with error " + error + ".");
						bw.newLine();
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
				
				else if (downtime_counter == 3) System.out.println("Website is down for 3 tries.");
				else if (downtime_counter == 10) System.out.println("Website is down for 10 tries.");
				else if (downtime_counter == 50) System.out.println("Website is down for 50 tries.");
				else if (downtime_counter == 100) System.out.println("Website is down for 100 tries.");
				else if (downtime_counter == 500) System.out.println("Website is down for 500 tries.");
				
				if (downtime_counter == notify_at) {
					
					// Send notification that website is down.
					
					Thread t = new Thread(new SendMessage("down", error, site, host, username, password, to));
			        t.start();
					
				}
			
			// the website is up
			} else {
				
				System.out.println("Website is OK. Status: " + site_status(site));
					
				// if website was down before that
				if (downtime_counter > 0) {
					
					// add the last downtime to log file
					try {
						java.util.Date date = new java.util.Date();
						Timestamp went_up = new Timestamp(date.getTime());
						BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("log.txt", true)));
						bw.write(went_up + " site went up.");
						bw.newLine();
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					// if the downtime notification has been sent
					if (downtime_counter > notify_at) {
						
						// send notification that website is up again
						
						Thread t = new Thread(new SendMessage("up", 0, site, host, username, password, to));
				        t.start();
						
					}
					
					// reset downtime counter
					downtime_counter = -1;
				}
			}
				
			// wait before performing next check			    
			try {
				Thread.sleep(interval * 1000);
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			
		}
		
	}
		
}
	
