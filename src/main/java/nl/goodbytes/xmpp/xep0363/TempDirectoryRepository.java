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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;

/**
 * A repository of files, backed by a temporary directory.
 *
 * This implementations makes no guarantees about the availability of data that is stored. Files might be purged without
 * notice.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class TempDirectoryRepository implements Repository
{
    protected Path repository;

    @Override
    public void initialize() throws IOException
    {
        repository = Files.createTempDirectory( "xmppfileupload" );
    }

    @Override
    public boolean contains( UUID uuid ) throws IOException
    {
        final Path path = Paths.get( repository.toString(), uuid.toString() );
        return Files.exists( path );
    }

    @Override
    public String calculateETagHash( UUID uuid ) throws IOException
    {
        final Path path = Paths.get( repository.toString(), uuid.toString() );
        return String.valueOf( path.hashCode() + Files.getLastModifiedTime( path ).hashCode() );
    }

    @Override
    public String getContentType( UUID uuid ) throws IOException
    {
        final Path path = Paths.get( repository.toString(), uuid.toString() );
        return Files.probeContentType( path );
    }

    @Override
    public long getSize( UUID uuid ) throws IOException
    {
        final Path path = Paths.get( repository.toString(), uuid.toString() );
        return Files.size( path );
    }

    @Override
    public InputStream getInputStream( UUID uuid ) throws IOException
    {
        final Path path = Paths.get( repository.toString(), uuid.toString() );
        return Files.newInputStream( path, READ );
    }

    @Override
    public OutputStream getOutputStream( UUID uuid ) throws IOException
    {
        final Path path = Paths.get( repository.toString(), uuid.toString() );
        return Files.newOutputStream( path, CREATE );
    }
}
