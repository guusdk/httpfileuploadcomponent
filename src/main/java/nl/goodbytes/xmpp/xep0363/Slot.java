/*
 * Copyright (c) 2017 Guus der Kinderen. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.goodbytes.xmpp.xep0363;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import org.xmpp.packet.JID;

/**
 * Representation of a ticket that is allows a single file upload.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class Slot
{
    private final SecureUniqueId uuid = SecureUUID.generate(); // This is cryptographically 'strong'.
    private final Date creationDate = new Date();
    private final String filename;
    private final JID creator;
    private final long size;

    public Slot( JID creator, String filename, long size )
    {
        this.creator = creator;
        this.filename = filename;
        this.size = size;
    }

    public Date getCreationDate()
    {
        return creationDate;
    }

    public JID getCreator()
    {
        return creator;
    }

    public long getSize()
    {
        return size;
    }

    public SecureUniqueId getUuid()
    {
        return uuid;
    }

    public URL getPutUrl() throws URISyntaxException, MalformedURLException
    {
        return getURL();
    }

    public URL getGetUrl() throws URISyntaxException, MalformedURLException
    {
        return getURL();
    }

    private URL getURL() throws URISyntaxException, MalformedURLException
    {
        final String path;
        if ( SlotManager.getInstance().getWebContextRoot().endsWith( "/" ) )
        {
            path = SlotManager.getInstance().getWebContextRoot() + uuid.toString() + "/" + filename;
        }
        else
        {
            path = SlotManager.getInstance().getWebContextRoot() + "/" + uuid.toString() + "/" + filename;
        }

        // First, use URI to properly encode all components.
        final URI uri = new URI(
            SlotManager.getInstance().getWebProtocol(),
            null, // userinfo
            SlotManager.getInstance().getWebHost(),
            SlotManager.getInstance().getWebPort(),
            path,
            null, // query
            null // fragment
        );

        // Then, ensure that the URL contains US-ASCII characters only, to prevent issues with some clients.
        final String usascii = uri.toASCIIString();

        // Finally, transform the result into an URL.
        return new URL( usascii );
    }

    @Override
    public String toString()
    {
        return "Slot{" +
            "uuid=" + uuid +
            ", creationDate=" + creationDate +
            ", filename='" + filename + '\'' +
            ", creator=" + creator +
            ", size=" + size +
            '}';
    }
}
