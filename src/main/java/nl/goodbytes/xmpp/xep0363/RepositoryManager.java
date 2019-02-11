package nl.goodbytes.xmpp.xep0363;

import java.io.IOException;

public class RepositoryManager
{
    private static RepositoryManager INSTANCE;

    public synchronized static RepositoryManager getInstance()
    {
        if ( INSTANCE == null )
        {
            INSTANCE = new RepositoryManager();
        }

        return INSTANCE;
    }

    private Repository repository;

    public void initialize( final Repository repository ) throws IOException
    {
        if ( this.repository != null )
        {
            throw new IllegalArgumentException( "Already initialized." );
        }
        this.repository = repository;
        this.repository.initialize();
    }

    public Repository getRepository()
    {
        return this.repository;
    }

    public void destroy()
    {
        if ( this.repository != null )
        {
            this.repository.destroy();
            this.repository = null;
        }
    }
}
