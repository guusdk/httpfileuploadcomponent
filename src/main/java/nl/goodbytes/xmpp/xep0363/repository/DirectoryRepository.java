package nl.goodbytes.xmpp.xep0363.repository;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A repository of files, backed by a regular directory.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class DirectoryRepository extends AbstractFileSystemRepository
{
    private final Path path;
    public DirectoryRepository( final Path path, boolean doPurge, PurgeStrategy purgeStrategy, Long purgeThreshold )
    {
        super( doPurge, purgeStrategy, purgeThreshold );

        this.path = path;
    }

    @Override
    protected Path initializeRepository() throws IOException
    {
        return path;
    }
}
