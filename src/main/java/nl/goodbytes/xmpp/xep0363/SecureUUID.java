package nl.goodbytes.xmpp.xep0363;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public class SecureUUID implements SecureUniqueId {
    private static final SecureRandom random = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final String id;
    
    public SecureUUID(String id) {
    	if (id == null) {
    		throw new IllegalArgumentException();
    	}
    	this.id = id;
    }

    public static SecureUUID generate() {
        byte[] buffer = new byte[20];
        random.nextBytes(buffer);
        String result = encoder.encodeToString(buffer);
        result = result.replace('/', '-').replace('+', '_');
        return new SecureUUID(result);
    }
    
    public static SecureUUID fromString(String id) {
        return new SecureUUID(id);
    }

	@Override
	public int compareTo(SecureUniqueId o) {
		if (o != null && o instanceof SecureUUID) {
			return this.id.compareTo(((SecureUUID)o).id); 
		}
		
		return -1;
	}
    
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		if (id == null) {
			return false;
		}
		SecureUUID other = SecureUUID.class.cast(obj);
		return Objects.equals(this.id, other.id);
	}

	@Override
	public String toString() { 
		return this.id;
	}

	@Override
	public int hashCode() {
		if (id == null) {
			return System.identityHashCode(this);
		}
		return Objects.hash(this.id);
	}
    
}