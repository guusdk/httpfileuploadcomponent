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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Responsible for the HTTP(s) processing as defined in XEP-0363.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class Servlet extends HttpServlet
{
    private static final Logger Log = LoggerFactory.getLogger( Servlet.class );

    public static SecureUniqueId uuidFromPath( String path )
    {
        if ( path == null || path.isEmpty() )
        {
            return null;
        }

        final String[] parts = path.split( "/" );
        if ( parts.length < 2 )
        {
            return null;
        }

        try
        {
            return SecureUniqueIdFactory.fromString( parts[ parts.length - 2 ] );
        }
        catch ( IllegalArgumentException e )
        {
            return null;
        }
    }

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        Log.info( "Processing GET request... ({} requesting from {})", req.getRemoteAddr(), req.getRequestURI() );
        final Repository repository = RepositoryManager.getInstance().getRepository();
        if ( repository == null )
        {
            resp.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            Log.warn( "... responded with INTERNAL_SERVER_ERROR. The repository is null." );
            return;
        }

        final SecureUniqueId uuid = uuidFromPath( req.getRequestURI() );
        if ( uuid == null )
        {
            resp.sendError( HttpServletResponse.SC_NOT_FOUND );
            Log.info( "... responded with NOT_FOUND. Unable to parse UUID from request URI." );
            return;
        }

        if ( !repository.contains( uuid ) )
        {
            resp.sendError( HttpServletResponse.SC_NOT_FOUND );
            Log.info( "... responded with NOT_FOUND. The repository does not contain a path to the UUID that is parsed from request URI: {}", uuid.toString() );
            return;
        }

        final String eTagRequest = req.getHeader( "If-None-Match" );
        if ( eTagRequest != null )
        {
            final String calculatedETagHash = repository.calculateETagHash( uuid );
            if ( eTagRequest.equals( calculatedETagHash ) )
            {
                resp.setStatus( HttpServletResponse.SC_NOT_MODIFIED );
                Log.info( "... responded with NOT_MODIFIED. Provided ETag value matches the hash in the repository." );
                return;
            }
        }

        final String contentType = repository.getContentType( uuid );
        if ( contentType != null && !contentType.isEmpty() )
        {
            resp.setContentType( contentType );
            Log.debug( "... setting content type '{}'.", contentType );
        }

        final long size = repository.getSize( uuid );
        if ( size > 0 && size <= Integer.MAX_VALUE )
        {
            resp.setContentLength( (int) size );
            Log.debug( "... setting content length '{}'.", size );
        }

        resp.setHeader( "Cache-Control", "max-age=31536000" );
        final String etag = repository.calculateETagHash( uuid );
        if ( etag != null )
        {
            resp.setHeader( "ETag", etag );
            Log.debug( "... setting ETag '{}'.", etag );
        }

        try ( final InputStream in = new BufferedInputStream( repository.getInputStream( uuid ) );
              final OutputStream out = resp.getOutputStream() )
        {
            final byte[] buffer = new byte[ 1024 * 4 ];
            int bytesRead;
            while ( ( bytesRead = in.read( buffer ) ) != -1 )
            {
                out.write( buffer, 0, bytesRead );
            }
        }
        Log.info( "... responded with OK and included the data in the response body." );
    }

    @Override
    protected void doPut( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        Log.info( "Processing PUT request... ({} submitting to {})", req.getRemoteAddr(), req.getRequestURI() );
        final Repository repository = RepositoryManager.getInstance().getRepository();
        if ( repository == null )
        {
            resp.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            Log.warn( "... responded with INTERNAL_SERVER_ERROR. The repository is null." );
            return;
        }

        final SecureUniqueId uuid = uuidFromPath( req.getRequestURI() );
        if ( uuid == null )
        {
            resp.sendError( HttpServletResponse.SC_BAD_REQUEST, "The request lacks a slot identifier on its path." );
            Log.info( "... responded with BAD_REQUEST. The request lacks a slot identifier on its path." );
            return;
        }

        final Slot slot = SlotManager.getInstance().consumeSlotForPut( uuid );
        if ( slot == null )
        {
            resp.sendError( HttpServletResponse.SC_BAD_REQUEST, "The requested slot is not available. Either it does not exist, or has already been used." );
            Log.info( "... responded with BAD_REQUEST. The requested slot is not available. Either it does not exist, or has already been used." );
            return;
        }

        if ( req.getContentLength() != slot.getSize() )
        { // This can be faked by the client, but XEP says to be brutal.
            resp.sendError( HttpServletResponse.SC_BAD_REQUEST, "Content length in request does not correspond with slot size." );
            Log.info( "... responded with BAD_REQUEST. Content length in request ({}) does not correspond with slot size ({}).", req.getContentLength(), slot.getSize() );
            return;
        }

        try ( final InputStream in = req.getInputStream();
              final OutputStream out = new BufferedOutputStream( repository.getOutputStream( slot.getUuid() ) ) )
        {
            final byte[] buffer = new byte[ 1024 * 4 ];
            int bytesRead;
            while ( ( bytesRead = in.read( buffer ) ) != -1 )
            {
                out.write( buffer, 0, bytesRead );
            }
        }

        try
        {
            resp.setHeader( "Location", slot.getGetUrl().toExternalForm() );
        }
        catch ( URISyntaxException | MalformedURLException e )
        {
            Log.warn( "Unable to calculate GET URL for {}", slot, e );
        }

        resp.setStatus( HttpServletResponse.SC_CREATED );
        Log.info( "... responded with CREATED. Stored data from the request body in the repository." );
    }
}
