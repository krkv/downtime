import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

public class Main {
	
	static int downtime_counter = -1;
	static Timestamp last_downtime;
	
	// checks if the given website is up
	public static boolean site_is_up(String u) {
				
			URL url;
			try {
				url = new URL(u);
				URLConnection c = url.openConnection();
				c.getContent();
				return true;
			} catch (IOException e) {
				return false;
			}
			
	};

	public static void main(String[] args) {
		
		// Website to check
		String site = args[0];
		// Check interval in seconds
		int interval = Integer.parseInt(args[1]);
		// Number of unsuccesful checks before notifying
		int notify_at = Integer.parseInt(args[2]);
		// SMTP host
		String host = args[3];
		// SMTP login
		String username = args[4];
		// SMTP password
		String password = args[5];
		// Notification from
		String from = args[6];
		// Notification to
		String to = args[7];
		Properties props = new Properties();
	    Session session = Session.getInstance(props);
		
		// create logfile if required
		File log = new File("log.txt");
		if (!log.exists()) {
			try {
				log.createNewFile();
			} catch (IOException e1) {}
		}
		
		// the main program loop that performs checks
		while (true) {

			System.out.println(site_is_up(site));
			
			// the website is down
			if (!site_is_up(site)) {
				
				downtime_counter += 1;
				
				// if the downtime just started, save the time
				if (downtime_counter == 0) {
					java.util.Date date = new java.util.Date();
					last_downtime = new Timestamp(date.getTime());
				}
				
				else if (downtime_counter == 3) System.out.println("The site is down for 3 tries.");
				else if (downtime_counter == 10) System.out.println("The site is down for 10 tries.");
				else if (downtime_counter == 50) System.out.println("The site is down for 50 tries.");
				else if (downtime_counter == 100) System.out.println("The site is down for 100 tries.");
				else if (downtime_counter == 500) System.out.println("The site is down for 500 tries.");
				
				if (downtime_counter == notify_at) {
					
					// Send notification that website is down.
					
					try {
						MimeMessage msg = new MimeMessage(session);
					    msg.setFrom(new InternetAddress(from));
				        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
				        msg.setSubject("Website " + site + " is down.");				        
				        msg.setText("Oh no! Your website " + site + " is down! Time to fix it!");
					    Transport t = session.getTransport("smtps");
					    try {
						t.connect(host, username, password);
						t.sendMessage(msg, msg.getAllRecipients());
					    } finally {
						t.close();
					    }
					} catch (Exception e) {}
					
				}
			
			// the website is up
			} else {
					
				// if website was down and now it's up again
				if (downtime_counter > 0) {
					
					// add the last downtime to log file
					try {
						BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("log.txt", true)));
						bw.write(last_downtime + " site was down for " + downtime_counter * interval + " seconds");
						bw.newLine();
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					// if the downtime notification has been sent
					if (downtime_counter > notify_at) {
						
						// send notification that website is up again
						
						try {
							MimeMessage msg = new MimeMessage(session);
						    msg.setFrom(new InternetAddress(from));
					        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
					        msg.setSubject("Website " + site + " is up again.");				        
					        msg.setText("Good news! Your website " + site + " is up again. "
					        		+ "It has been down for " + downtime_counter * interval + " seconds.");
						    Transport t = session.getTransport("smtps");
						    try {
							t.connect(host, username, password);
							t.sendMessage(msg, msg.getAllRecipients());
						    } finally {
							t.close();
						    }
						} catch (Exception e) {}
						
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
	
