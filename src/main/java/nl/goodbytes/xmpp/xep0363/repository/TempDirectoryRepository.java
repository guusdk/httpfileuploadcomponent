package nl.goodbytes.xmpp.xep0363.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A repository of files, backed by a temporary directory.
 *
 * This implementations makes no guarantees about the availability of data that is stored. Files might be purged without
 * notice.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class TempDirectoryRepository extends AbstractFileSystemRepository
{
    @Override
    protected Path initializeRepository() throws IOException
    {
        final Path repository = Files.createTempDirectory( "xmppfileupload" );
        repository.toFile().deleteOnExit();

        return repository;
    }
}
