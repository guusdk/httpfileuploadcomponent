package nl.goodbytes.xmpp.xep0363.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;

/**
 * Compare files by 'last access' time.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
class FileComparatorByLastAccess implements Comparator<File>
{
    private static final Logger Log = LoggerFactory.getLogger( FileComparatorByLastAccess.class );

    @Override
    public int compare( File o1, File o2 )
    {
        try
        {
            final BasicFileAttributes a1 = Files.readAttributes( o1.toPath(), BasicFileAttributes.class );
            final BasicFileAttributes a2 = Files.readAttributes( o2.toPath(), BasicFileAttributes.class );
            return a1.lastAccessTime().compareTo( a2.lastAccessTime() );
        }
        catch ( IOException e )
        {
            Log.warn( "An exception occurred while tyring to compare files {} and {} by 'last access' time.", o1, o2, e );
            return 0;
        }
    }
}
