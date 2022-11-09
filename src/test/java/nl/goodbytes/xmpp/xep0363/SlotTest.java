package nl.goodbytes.xmpp.xep0363;

import org.junit.Test;
import org.xmpp.packet.JID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SlotTest
{
    @Test
    public void whenSerializingAndDeserializing_ThenObjectIsTheSame() throws Exception
    {
        // Setup test fixture.
        final Slot input = new Slot(new JID("unit-test", "example.org", "test"), "unittest", 41);

        // Execute system under test.
        byte[] buffer;
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream();
             final ObjectOutputStream oos = new ObjectOutputStream(bos) ) {
            oos.writeObject(input);
            buffer = bos.toByteArray();
        }

        final Object result;
        try (final ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
             final ObjectInputStream ois = new ObjectInputStream(bis)) {
            result = ois.readObject();
        }

        // Verify result.
        assertTrue(result instanceof Slot);
        assertEquals(input.getSize(), ((Slot) result).getSize());
        assertEquals(input.getUuid(), ((Slot) result).getUuid());
        assertEquals(input.getCreator(), ((Slot) result).getCreator());
        assertEquals(input.getCreationDate(), ((Slot) result).getCreationDate());
    }
}
