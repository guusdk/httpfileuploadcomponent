package nl.goodbytes.xmpp.xep0363;

public class SecureUniqueIdFactory {
	
	public static SecureUniqueId fromString(String s) {
		try {
			return SecureUUID.fromString(s);
		} catch (IllegalArgumentException e1) {
			return LegacyUUID.fromString(s);
		}
	}

}
