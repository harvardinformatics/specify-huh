/*
 * Created on Nov 13, 2011
 *
 * Copyright © 2011 President and Fellows of Harvard College
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * @Author: David B. Lowery  lowery@cs.umb.edu
 */
package edu.harvard.huh.specify.bugzilla;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.mail.MessagingException;

import mondrian.util.Bug;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author lowery
 *
 */
public class BugMail {
	public static void main(String[] args) throws FileNotFoundException, IOException, MessagingException {
		BugMail bug =  new BugMail("This is a test bug report submitted via Specify", "This is a bug description.");
		bug.send();
	}
	private static String smtp;
	private static String recipient;
	private static String from;
	private static String product;
	private static String component;
	private static Mailer bugMailer;
	
	private String subject;
	private String message;
	
	public BugMail(String summary, String description) {
		UIRegistry.loadAndPushResourceBundle("mail");
		
		smtp = UIRegistry.getResourceString("bugmail.smtp.host");
		recipient = UIRegistry.getResourceString("bugmail.recipient");
		from = UIRegistry.getResourceString("bugmail.from");
		
		// Bugzilla defaults
		product = UIRegistry.getResourceString("bugmail.product");
		component = UIRegistry.getResourceString("bugmail.component");
		
		bugMailer = new Mailer(smtp);
		
		StringBuffer sb = new StringBuffer();
		sb.append("@product = " + product + "\n");
		sb.append("@component = " + component + "\n\n");
		sb.append(description);
		
		subject = summary;
		message = sb.toString();
	}
	
	public void send() throws MessagingException {
		bugMailer.postMail(recipient, subject, message, from);
	}
}
