package nl.goodbytes.xmpp.xep0363;

import java.util.Objects;
import java.util.UUID;

public class LegacyUUID implements SecureUniqueId {
	
	private UUID uuid;
	
	private LegacyUUID(UUID uuid) {
		this.uuid = uuid;
	}
	
	public static LegacyUUID generate() {
		UUID u = UUID.randomUUID();
		return new LegacyUUID(u);
	}

	public static LegacyUUID fromString(String s) {
		UUID u = UUID.fromString(s);
		return new LegacyUUID(u);
	}

	@Override
	public int compareTo(SecureUniqueId o) {
		if (uuid == null) {
			throw new UnsupportedOperationException();
		}
		if (o != null && o instanceof LegacyUUID) {
			return this.uuid.compareTo(((LegacyUUID)o).uuid); 
		}
		
		return -1;
	}

	@Override
	public int hashCode() {
		if (uuid == null) {
			return System.identityHashCode(this);
		}
		return Objects.hash(this.uuid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		if (uuid == null) {
			return false;
		}
		LegacyUUID other = LegacyUUID.class.cast(obj);
		return Objects.equals(this.uuid, other.uuid);
	}
	
	@Override
	public String toString() {
		if (uuid !=null) {
			return uuid.toString();
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	

}
