/**
 * 
 */
package org.gamboni.messaged;

import static spark.Spark.get;
import static spark.Spark.post;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.Collection;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.Roster.SubscriptionMode;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

import spark.Spark;

/**
 * @author tendays
 *
 */
public class MessageD {
	private static final File SETTINGS = new File(".messaged");
	private static final String DISTANCE_FILE = "/tmp/distance";
	private static int maxDistance = 0;
	
	public static void main(String[] a) {
		Spark.port(4000);
		
		get("/", (req, res) -> "ok");
		
		post("/msg", (req, res) -> {
			String text = req.body();
			postMessage(text);
			return "ok";
		});
		
		post("/dst", (req, res) -> {
			try {
				String text = req.body();
				int dst = Integer.parseInt(text);
				System.out.println("New distance update: "+ dst +"m");
				if (dst > maxDistance) {
					maxDistance = dst;
				}
				try (Writer w = new FileWriter(DISTANCE_FILE)) {
					w.append(dst + "/" + maxDistance);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return "ko";
			}
			return "ok";
		});
		
		Spark.delete("/dst", (req, res) -> {
			System.out.println("Received end-of-data notification");
			new File(DISTANCE_FILE).delete();
			maxDistance = 0;
			return "ok";
		});

		if (SETTINGS.exists()) {

			xmppConnect();
		}
	}

	private static void postMessage(String text) throws IOException {
		try (Writer w = new FileWriter("/tmp/message")) {
			if (text.length() > 100) { text = text.substring(0, 100); }
			w.append(text);
		}
	}

	private static void xmppConnect(String user, String host, String password) {
		try {
			System.out.println("Connecting to "+ host);
			AbstractXMPPConnection connection = new XMPPTCPConnection(user, password, host);
			connection.connect();
			connection.login();
			Presence presence = PresenceBuilder.buildPresence().setMode(Mode.available).build();
			connection.sendStanza(presence);
			
			Roster roster = Roster.getInstanceFor(connection);
			roster.setSubscriptionMode(SubscriptionMode.accept_all);
			roster.addRosterListener(new RosterListener() {
				
				@Override
				public void presenceChanged(Presence presence) {
					System.out.println("presenceChanged "+ presence);
					// TODO Auto-generated method stub
				}
				
				@Override
				public void entriesUpdated(Collection<Jid> addresses) {
					System.out.println("entriesUpdated "+ addresses);
					// TODO Auto-generated method stub
				}
				
				@Override
				public void entriesDeleted(Collection<Jid> addresses) {
					System.out.println("entriesDeleted "+ addresses);
					// TODO Auto-generated method stub
				}

				@Override
				public void entriesAdded(Collection<Jid> addresses) {
					System.out.println("entriesAdded "+ addresses);
					// TODO Auto-generated method stub
				}
			});
			/*
			SignalOmemoService.acknowledgeLicense();
			SignalOmemoService.setup();
			SignalOmemoService omemo = (SignalOmemoService)SignalOmemoService.getInstance();
			omemo.setOmemoStoreBackend(new SignalCachingOmemoStore(new SignalFileBasedOmemoStore(new File("/var/cache/messaged/omemo"))));
			OmemoManager omemoMgr = OmemoManager.getInstanceFor(connection);
			omemoMgr.setTrustCallback(new OmemoTrustCallback() {
				
				@Override
				public void setTrust(OmemoDevice device, OmemoFingerprint fingerprint, TrustState state) {
					System.err.println("Trust request for "+ device +" as "+ state);
				}
				
				@Override
				public TrustState getTrust(OmemoDevice device, OmemoFingerprint fingerprint) {
					System.err.println("Trust query for "+ device);
					if (device.getJid().asDomainBareJid().toString().equals("gamboni.org")) {
						System.err.println("-> trusted");
						return TrustState.trusted;
					} else {
						System.err.println("-> undecided");
						return TrustState.undecided;
					}
				}
			});
			*/
			/*
			omemoMgr.addOmemoMessageListener(new OmemoMessageListener() {
				
				@Override
				public void onOmemoMessageReceived(Stanza stanza, Received decryptedMessage) {
					System.out.println("Received encrypted message "+ decryptedMessage.getBody());
				}
				
				@Override
				public void onOmemoCarbonCopyReceived(Direction direction, Message carbonCopy, Message wrappingMessage,
						Received decryptedCarbonCopy) {
					System.out.println("Received carbon copy "+ decryptedCarbonCopy.getBody());
				}
			});
			omemoMgr.initialize();
			*/
			ChatManager chatMgr = ChatManager.getInstanceFor(connection);
			chatMgr.addIncomingListener((EntityBareJid from, Message message, Chat chat) -> {
				System.out.println(message.getBody());
				try {
					postMessage(message.getBody());
				} catch (IOException e) {
					System.err.println("Failed displaying message.");
					e.printStackTrace();
				}
			});
			System.out.println("Connection to "+ server +" successful");
		} catch (Exception e) {
			System.err.println(Instant.now() +" Failed initialising XMPP client.");
			e.printStackTrace();
		}
	}
}
