package nl.goodbytes.xmpp.xep0363;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;

public class LegacyUUIDTest
{
    @Test
    public void whenSerializingAndDeserializing_ThenObjectIsTheSame() throws Exception
    {
        // Setup test fixture.
        final LegacyUUID input = LegacyUUID.generate();

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
        assertEquals(input, result);
    }
}
