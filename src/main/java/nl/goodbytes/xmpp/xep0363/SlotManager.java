/*
 * Copyright (c) 2017-2022 Guus der Kinderen. All rights reserved.
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
 */

package nl.goodbytes.xmpp.xep0363;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.xmpp.packet.JID;

import javax.annotation.Nonnull;

/**
 * A manager of HTTP slots.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class SlotManager
{
    public static final long DEFAULT_MAX_FILE_SIZE = 50 * 1024 * 1024;
    private long maxFileSize = DEFAULT_MAX_FILE_SIZE;

    private static SlotManager INSTANCE = null;

    public synchronized static SlotManager getInstance()
    {
        if ( INSTANCE == null )
        {
            INSTANCE = new SlotManager();
        }

        return INSTANCE;
    }

    private SlotProvider slotProvider;

    public void initialize( final SlotProvider slotProvider )
    {
        if ( this.slotProvider != null )
        {
            throw new IllegalArgumentException( "Already initialized." );
        }
        this.slotProvider = slotProvider;
    }

    private String webProtocol;
    private String webHost;
    private Integer webPort;
    private String webContextRoot;

    public long getMaxFileSize()
    {
        return maxFileSize;
    }

    public void setMaxFileSize( long maxFileSize )
    {
        this.maxFileSize = maxFileSize;
    }

    public Slot getSlot( JID from, String fileName, long fileSize ) throws TooLargeException
    {
        if ( maxFileSize > 0 && fileSize > maxFileSize )
        {
            throw new TooLargeException( fileSize, maxFileSize );
        }

        final Slot slot = new Slot( from, fileName, fileSize );

        slotProvider.create(slot);

        return slot;
    }

    public Slot consumeSlotForPut( SecureUniqueId uuid )
    {
        return slotProvider.consume(uuid);
    }


    public static URL getPutUrl(@Nonnull final Slot slot) throws URISyntaxException, MalformedURLException
    {
        return getURL(slot);
    }

    public static URL getGetUrl(@Nonnull final Slot slot) throws URISyntaxException, MalformedURLException
    {
        return getURL(slot);
    }

    private static URL getURL(@Nonnull final Slot slot) throws URISyntaxException, MalformedURLException
    {
        final String path;
        if ( SlotManager.getInstance().getWebContextRoot().endsWith( "/" ) )
        {
            path = SlotManager.getInstance().getWebContextRoot() + slot.getUuid() + "/" + slot.getFilename();
        }
        else
        {
            path = SlotManager.getInstance().getWebContextRoot() + slot.getUuid() + "/" + slot.getFilename();
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

    public void setWebProtocol( final String webProtocol )
    {
        this.webProtocol = webProtocol;
    }

    public String getWebProtocol()
    {
        return webProtocol;
    }

    public void setWebHost( final String webHost )
    {
        this.webHost = webHost;
    }

    public String getWebHost()
    {
        return webHost;
    }

    public void setWebPort( final int webPort )
    {
        this.webPort = webPort;
    }

    public int getWebPort()
    {
        return webPort;
    }

    public String getWebContextRoot()
    {
        return webContextRoot;
    }

    public void setWebContextRoot( final String webContextRoot )
    {
        this.webContextRoot = webContextRoot;
    }
}
