package nl.goodbytes.xmpp.xep0363;

import nl.goodbytes.xmpp.xep0363.repository.TempDirectoryRepository;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xmpp.packet.JID;

import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Verifies the Content-Security-Policy that {@link Servlet} sends.
 */
public class ServletTest
{
    private static final String EXPECTED_DEFAULT_POLICY = "script-src 'none'; form-action 'none'";
    private static final byte[] CONTENT = "Lorem ipsum".getBytes( StandardCharsets.UTF_8 );

    private Server server;

    @BeforeClass
    public static void initializeRepository() throws Exception
    {
        RepositoryManager.getInstance().initialize( new TempDirectoryRepository() );
    }

    @AfterClass
    public static void destroyRepository()
    {
        RepositoryManager.getInstance().destroy();
    }

    @After
    public void stopServer() throws Exception
    {
        if ( server != null )
        {
            server.stop();
        }
    }

    @Test
    public void whenContentSecurityPolicyIsNotConfigured_ThenDefaultPolicyIsSent() throws Exception
    {
        // Setup test fixture.
        final URI base = startServer( null );
        final String path = store( CONTENT, "file.txt" );

        // Execute system under test.
        final HttpResponse<Void> response = get( base, path );

        // Verify result.
        assertEquals( 200, response.statusCode() );
        assertEquals( "Without a configured policy, the servlet should send its default policy.",
            Optional.of( EXPECTED_DEFAULT_POLICY ), response.headers().firstValue( "Content-Security-Policy" ) );
    }

    @Test
    public void whenContentSecurityPolicyIsNotConfigured_ThenDefaultPolicyIsSentOnErrors() throws Exception
    {
        // Setup test fixture.
        final URI base = startServer( null );

        // Execute system under test.
        final HttpResponse<Void> response = get( base, "/" + new Slot( new JID( "unit-test", "example.org", null ), "missing.txt", 1 ).getUuid() + "/missing.txt" );

        // Verify result.
        assertEquals( 404, response.statusCode() );
        assertEquals( Optional.of( EXPECTED_DEFAULT_POLICY ), response.headers().firstValue( "Content-Security-Policy" ) );
    }

    @Test
    public void whenContentSecurityPolicyIsConfigured_ThenConfiguredPolicyIsSent() throws Exception
    {
        // Setup test fixture.
        final URI base = startServer( "default-src 'none'" );
        final String path = store( CONTENT, "file.txt" );

        // Execute system under test.
        final HttpResponse<Void> response = get( base, path );

        // Verify result.
        assertEquals( Optional.of( "default-src 'none'" ), response.headers().firstValue( "Content-Security-Policy" ) );
    }

    @Test
    public void whenContentSecurityPolicyIsConfiguredBlank_ThenNoPolicyIsSent() throws Exception
    {
        // Setup test fixture.
        final URI base = startServer( "" );
        final String path = store( CONTENT, "file.txt" );

        // Execute system under test.
        final HttpResponse<Void> response = get( base, path );

        // Verify result.
        assertFalse( "An explicitly blank policy should disable the header.", response.headers().firstValue( "Content-Security-Policy" ).isPresent() );
    }

    /**
     * Starts a web server with the servlet at its root.
     *
     * @param contentSecurityPolicy value of the servlet's contentSecurityPolicy init parameter, or null to leave it out.
     * @return the base URI of the server.
     */
    private URI startServer( final String contentSecurityPolicy ) throws Exception
    {
        server = new Server();
        final ServerConnector connector = new ServerConnector( server );
        connector.setHost( "127.0.0.1" );
        connector.setPort( 0 );
        server.addConnector( connector );

        final Map<String, String> initParameters = new HashMap<>();
        if ( contentSecurityPolicy != null )
        {
            initParameters.put( "contentSecurityPolicy", contentSecurityPolicy );
        }
        final ServletContextHandler context = new ServletContextHandler();
        final ServletHolder holder = context.addServlet( Servlet.class, "/*" );
        holder.setInitParameters( initParameters );
        server.setHandler( context );
        server.start();

        return URI.create( "http://127.0.0.1:" + connector.getLocalPort() );
    }

    /**
     * Stores content in the repository, as an upload would.
     *
     * @return the path at which the content can be downloaded.
     */
    private static String store( final byte[] content, final String filename ) throws Exception
    {
        final Slot slot = new Slot( new JID( "unit-test", "example.org", null ), filename, content.length );
        try ( final OutputStream out = RepositoryManager.getInstance().getRepository().getOutputStream( slot.getUuid() ) )
        {
            out.write( content );
        }
        return "/" + slot.getUuid() + "/" + filename;
    }

    private static HttpResponse<Void> get( final URI base, final String path ) throws Exception
    {
        final HttpRequest request = HttpRequest.newBuilder( base.resolve( path ) ).GET().build();
        return HttpClient.newHttpClient().send( request, HttpResponse.BodyHandlers.discarding() );
    }
}
