package nl.goodbytes.xmpp.xep0363.repository;

import java.io.File;
import java.util.Comparator;

/**
 * Compare files by 'last modified' time.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
class FileComparatorByLastModified implements Comparator<File>
{
    @Override
    public int compare( File o1, File o2 )
    {
        return Long.compare( o1.lastModified(), o2.lastModified() );
    }
}
