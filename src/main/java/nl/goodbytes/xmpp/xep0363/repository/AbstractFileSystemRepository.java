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

package nl.goodbytes.xmpp.xep0363.repository;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.goodbytes.xmpp.xep0363.Repository;
import nl.goodbytes.xmpp.xep0363.SecureUniqueId;

/**
 * A repository of files, backed by a (presumably local) file system.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public abstract class AbstractFileSystemRepository implements Repository
{
    private static final Logger Log = LoggerFactory.getLogger( AbstractFileSystemRepository.class );

    private Timer timer;

    protected Path repository;

    protected abstract Path initializeRepository() throws IOException;

    @Override
    public void initialize() throws IOException
    {
        repository = initializeRepository();

        // Perform a synchronous purge before start, which ensurs that a) purging is possible, b) space is available.
        purge();

        // Schedule periodic asynchronous purges.
        timer = new Timer( "xmppfileupload-cleanup", true );
        timer.schedule( new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    purge();
                }
                catch ( Exception e )
                {
                    Log.warn( "An unexpected error occurred while purging the repository.", e );
                }
            }
        }, 5 * 60 * 1000, 5 * 60 * 1000 );

        Log.info( "Initialized repository in: {}", repository );
    }

    @Override
    public void destroy()
    {
        if ( timer != null )
        {
            timer.cancel();
        }
    }

    @Override
    public boolean contains( SecureUniqueId uuid )
    {
        final Path path = Paths.get( repository.toString(), uuid.toString() );
        final boolean result = Files.exists( path );

        Log.debug( "UUID '{}' {} exist in repository.", uuid, result ? "does" : "does not" );
        return result;
    }

    @Override
    public String calculateETagHash( SecureUniqueId uuid )
    {
        final Path path = Paths.get( repository.toString(), uuid.toString() );
        try
        {
            final String result = String.valueOf( path.hashCode() + Files.getLastModifiedTime( path ).hashCode() );
            Log.debug( "UUID '{}' ETag value: {}", uuid, result );
            return result;
        }
        catch ( IOException e )
        {
            Log.warn( "UUID '{}' Unable to calculate ETag value.", uuid, e );
            return null;
        }
    }

    @Override
    public String getContentType( SecureUniqueId uuid )
    {
        try
        {
            final Path path = Paths.get( repository.toString(), uuid.toString() );

            String result;
            try ( final InputStream is = new BufferedInputStream( new FileInputStream( path.toFile() ) ) ) {
                Log.debug( "UUID '{}' Probing content type based on file content...", uuid );
                result = URLConnection.guessContentTypeFromStream( is );
            }

            if ( result == null || result.isEmpty() ) {
                Log.debug( "UUID '{}' Probing content type based on system installed file type detectors...", uuid );
                result = Files.probeContentType( path );
            }

            if ( result == null || result.isEmpty() ) {
                Log.debug( "UUID '{}' Probing content type based on file name...", uuid );
                result = URLConnection.guessContentTypeFromName( path.getFileName().toString() );
            }

            Log.debug( "UUID '{}' content type: {}", uuid, result );
            return result;
        }
        catch ( IOException e )
        {
            Log.warn( "UUID '{}' Unable to determine the content type.", uuid, e );
            return null;
        }
    }

    @Override
    public long getSize( SecureUniqueId uuid )
    {
        try
        {
            final Path path = Paths.get( repository.toString(), uuid.toString() );
            final long result = Files.size( path );

            Log.debug( "UUID '{}' size: {} bytes", uuid, result );
            return result;
        }
        catch ( IOException e )
        {
            Log.warn( "UUID '{}' Unable to determine the content size.", uuid, e );
            return -1;
        }
    }

    @Override
    public InputStream getInputStream( SecureUniqueId uuid ) throws IOException
    {
        final Path path = Paths.get( repository.toString(), uuid.toString() );
        return Files.newInputStream( path, READ );
    }

    @Override
    public OutputStream getOutputStream( SecureUniqueId uuid ) throws IOException
    {
        final Path path = Paths.get( repository.toString(), uuid.toString() );
        return Files.newOutputStream( path, CREATE );
    }

    public void purge() throws IOException
    {
        final File[] files = repository.toFile().listFiles();
        if ( files == null )
        {
            Log.debug( "No need to purge the repository, as it does not contain any files." );
            return;
        }

        final long used = getUsedSpace( repository );
        final long free = getUsableSpace( repository );
        Log.debug( "The repository currently uses {} bytes, while there's {} bytes of usable space left.", used, free );

        if ( used == 0 || used < free )
        {
            Log.debug( "No need to purge the repository, as the free space is larger than the used space." );
            return;
        }

        // Files modified the longest time ago are the first to be purged.
        Arrays.sort( files, new Comparator<File>()
        {
            @Override
            public int compare( File o1, File o2 )
            {
                return Long.compare( o1.lastModified(), o2.lastModified() );
            }
        } );

        long deletedTotal = 0;
        for ( final File file : files )
        {
            final long deleted = delete( file.toPath() );

            Log.debug( "Purging repository: deleting: {} ({} bytes)", file, deleted );

            deletedTotal += deleted;

            if ( used - deletedTotal <= 0 || used - deletedTotal < free + deletedTotal )
            {
                break;
            }
        }

        Log.info( "The repository was purged: {} bytes were deleted.", deletedTotal );
    }

    protected static long getUsableSpace( Path path ) throws IOException
    {
        return Files.getFileStore( path ).getUsableSpace();
    }

    public static long getUsedSpace( Path path ) throws IOException
    {
        final AtomicLong size = new AtomicLong( 0 );
        Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
        });

        return size.get();
    }

    protected static long delete( Path path ) throws IOException
    {
        final AtomicLong size = new AtomicLong( 0 );

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                size.addAndGet( attrs.size() );
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });

        return size.get();
    }
}
