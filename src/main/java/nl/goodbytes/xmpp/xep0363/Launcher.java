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

import nl.goodbytes.xmpp.xep0363.repository.DirectoryRepository;
import nl.goodbytes.xmpp.xep0363.repository.TempDirectoryRepository;
import org.apache.commons.cli.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.jivesoftware.whack.ExternalComponentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;

/**
 * Serves as the 'main class', which will start the application.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class Launcher
{
    private static final Logger Log = LoggerFactory.getLogger( Launcher.class );
    private final String xmppHost;
    private final Integer xmppPort;
    private final String webContextRoot;
    private final String domain;
    private final String sharedSecret;
    private final String webHost;
    private final Integer webPort;
    private final String announcedWebProtocol;
    private final String announcedWebHost;
    private final Integer announcedWebPort;
    private final String announcedWebContextRoot;
    private final Repository repository;
    private final Long maxFileSize;

    public Launcher( String xmppHost, Integer xmppPort, String domain, String sharedSecret, String webHost, Integer webPort, String webContextRoot, String announcedWebProtocol, String announcedWebHost, Integer announcedWebPort, String announcedWebContextRoot, Repository repository, Long maxFileSize )
    {
        this.xmppHost = xmppHost != null ? xmppHost : "localhost";
        this.xmppPort = xmppPort != null ? xmppPort : 5275;
        this.domain = domain != null ? domain : "upload";
        this.sharedSecret = sharedSecret;
        this.webHost = webHost != null ? webHost : getPublicAddress();
        this.webPort = webPort != null ? webPort : 12121;
        this.webContextRoot = webContextRoot != null ? (webContextRoot.startsWith( "/" ) ? webContextRoot : "/" + webContextRoot) : "/";
        this.announcedWebProtocol = announcedWebProtocol != null ? announcedWebProtocol : "http";
        this.announcedWebHost = announcedWebHost != null ? announcedWebHost : this.webHost;
        this.announcedWebPort = announcedWebPort != null ? announcedWebPort : this.webPort;
        this.announcedWebContextRoot = announcedWebContextRoot != null ? announcedWebContextRoot : this.webContextRoot;
        this.repository = repository != null ? repository : new TempDirectoryRepository();
        this.maxFileSize = maxFileSize != null ? maxFileSize : SlotManager.DEFAULT_MAX_FILE_SIZE;
    }

    public static void main( String[] args )
    {
        final Options options = new Options();

        options.addOption(
                Option.builder( "h"     )
                        .longOpt( "help" )
                        .desc( "Displays this help text." )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt( "webHost" )
                        .hasArg()
                        .desc( "The hostname or IP address on which the webserver will be ran. Defaults to an arbitrary, non-local address of this machine." )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt( "webPort" )
                        .hasArg()
                        .desc( "The TCP port number of the webserver. Defaults to 12121." )
                        .type( Integer.class )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt( "webContextRoot" )
                        .hasArg()
                        .desc( "The context root of the web server through which the web frontend will be made avialable. Defaults to '/', the root context." )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt( "announcedWebProtocol" )
                        .hasArg()
                        .desc( "The Protocol that is to be used by the end users. Defaults to http" )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt( "announcedWebHost" )
                        .hasArg()
                        .desc( "The hostname or IP address that is to be used by the end users (when different from webHost). Defaults to the webHost address. " )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt( "announcedWebPort" )
                        .hasArg()
                        .desc( "The TCP port number that is to be used by the end users (when different from webPort). Defaults to the webPort value." )
                        .type( Integer.class )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt( "announcedWebContextRoot" )
                        .hasArg()
                        .desc( "The context root that is to be used by the end users (when different from webContextRoot). Defaults to webContextRoot value." )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt( "xmppHost" )
                        .hasArg()
                        .desc( "The FQDN or IP address (not XMPP domain name) of the XMPP domain that this component will connect to. Defaults to 'localhost'." )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt( "xmppPort" )
                        .hasArg()
                        .desc( "The TCP port number on the xmppHost, to which a connection will be made. Defaults to 5275." )
                        .type( Integer.class )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt( "domain" )
                        .hasArg()
                        .desc( "The domain that will be used for the component with the XMPP domain." )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt( "sharedSecret" )
                        .hasArg()
                        .desc( "The shared secret, that authenticates this component with the XMPP domain." )
                        .build()
        );

        final OptionGroup repoType = new OptionGroup();

        repoType.addOption(
                Option.builder()
                        .longOpt( "tempFileRepo" )
                        .hasArg( false )
                        .desc( "Store files in the temporary directory provided by the file system." )
                        .build()
        );
        repoType.addOption(
                Option.builder()
                        .longOpt( "fileRepo" )
                        .hasArg()
                        .desc( "Store files in a directory provided by the file system. Provide the desired path as a value. Path must exist." )
                        .build()
        );
        options.addOptionGroup( repoType );

        options.addOption(
                Option.builder()
                        .longOpt( "maxFileSize" )
                        .hasArg()
                        .desc( "The maximum allowed size per file, in bytes. Use -1 to disable file size limit. Defaults to 5242880 (five MB)." )
                        .optionalArg( true )
                        .type( Long.class )
                        .build()
        );

        try
        {
            final CommandLineParser parser = new DefaultParser();
            final CommandLine line = parser.parse( options, args );

            if ( line.hasOption( "h" ) )
            {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "arguments", options );
            }
            else
            {
                final String webHost = line.getOptionValue( "webHost" );
                final Integer webPort = line.hasOption( "webPort" ) ? Integer.parseInt(line.getOptionValue( "webPort" )) : null;
                final String webContextRoot = line.hasOption( "webContextRoot" ) ? line.getOptionValue( "webContextRoot" ) : "/";
                final String announcedWebProtocol = line.getOptionValue( "announcedWebProtocol" );
                final String announcedWebHost = line.getOptionValue( "announcedWebHost" );
                final Integer announcedWebPort = line.hasOption( "announcedWebPort" ) ? Integer.parseInt(line.getOptionValue( "announcedWebPort" )) : null;
                final String announcedWebContextRoot = line.hasOption( "announcedWebContextRoot" ) ? line.getOptionValue( "announcedWebContextRoot" ) : "/";
                final String xmppHost = line.getOptionValue( "xmppHost" );
                final Integer xmppPort = line.hasOption( "xmppPort" ) ? Integer.parseInt(line.getOptionValue( "xmppPort" )) : null;
                final String domain = line.getOptionValue( "domain" );
                final String sharedSecret = line.getOptionValue( "sharedSecret" );
                final Long maxFileSize = line.hasOption( "maxFileSize" ) ? Long.parseLong(line.getOptionValue( "maxFileSize" )) : null;

                final Repository repository;
                if ( line.hasOption( "tempFileRepo" ) )
                {
                    repository = new TempDirectoryRepository();
                }
                else if (line.hasOption( "fileRepo"))
                {
                    final String directory = line.getOptionValue( "fileRepo" );
                    final Path path;
                    try {
                        path = Paths.get( directory );
                    } catch ( InvalidPathException e ) {
                        throw new ParseException( "Invalid value for 'fileRepo' option: " + e.getMessage() );
                    }
                    repository = new DirectoryRepository( path );
                }
                else
                {
                    repository = null;
                }

                final Launcher launcher = new Launcher( xmppHost, xmppPort, domain, sharedSecret, webHost, webPort, webContextRoot, announcedWebProtocol, announcedWebHost, announcedWebPort, announcedWebContextRoot, repository, maxFileSize );
                launcher.start();
            }
        }
        catch( ParseException e ) {
            // oops, something went wrong
            System.err.println( "Command line parsing failed: " + e.getMessage() );

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "arguments", options );
            System.exit( 1 );
        }
    }

    private static String getPublicAddress()
    {
        final Deque<String> hostnames = new ArrayDeque<>();
        try
        {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while ( e.hasMoreElements() )
            {
                NetworkInterface ni = e.nextElement();
                if ( ni.isLoopback() || ni.isPointToPoint() )
                {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while ( addresses.hasMoreElements() )
                {
                    InetAddress address = addresses.nextElement();
                    if ( address instanceof Inet4Address )
                    {
                        Inet4Address a = (Inet4Address) address;
                        if ( a.isAnyLocalAddress() )
                        {
                            continue;
                        }
                        hostnames.addFirst( a.getHostAddress() );
                    }
                    if ( address instanceof Inet6Address )
                    {
                        Inet6Address a = (Inet6Address) address;
                        if ( a.isAnyLocalAddress() )
                        {
                            continue;
                        }
                        hostnames.addLast( a.getHostAddress() );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            Log.warn( "An exception occurred while identifying public addresses.", e );
        }

        if ( hostnames.isEmpty() )
        {
            Log.info( "Unable to identify a public address." );
            return null;
        }

        Log.info( "Public address(es): " );
        for ( final String address : hostnames )
        {
            Log.info( "* {}", address );
        }

        return hostnames.getFirst();
    }

    public void start()
    {
        final String local = "http://" + webHost + ":" + webPort + webContextRoot;
        final String announced = announcedWebProtocol + "://" + announcedWebHost + ":" + announcedWebPort + announcedWebContextRoot;

        Log.info( "Starting external component with HTTP endpoint {} (which is announced as: {})", local, announced );
        SlotManager.getInstance().setWebProtocol( announcedWebProtocol );
        SlotManager.getInstance().setWebHost( announcedWebHost );
        SlotManager.getInstance().setWebPort( announcedWebPort );
        SlotManager.getInstance().setWebContextRoot( announcedWebContextRoot );

        if ( maxFileSize != null )
        {
            SlotManager.getInstance().setMaxFileSize( maxFileSize );
        }
        Log.info( "maxFileSize: {}", SlotManager.getInstance().getMaxFileSize() );

        Server jetty = null;
        ExternalComponentManager manager = null;
        try
        {
            Log.info( "Starting repository..." );
            RepositoryManager.getInstance().initialize( repository );

            Log.info( "Starting webserver..." );

            jetty = new Server();

            final SelectChannelConnector connector = new SelectChannelConnector();
            connector.setHost( webHost );
            connector.setPort( webPort );
            jetty.addConnector( connector );

            final ServletContextHandler servletContextHandler = new ServletContextHandler();
            servletContextHandler.addServlet( Servlet.class, webContextRoot );
            jetty.setHandler( servletContextHandler );
            jetty.start();

            Log.info( "Webserver started at {}:{}", connector.getHost(), connector.getLocalPort() );

            final Component component = new Component( domain );
            manager = new ExternalComponentManager( xmppHost, xmppPort );
            if ( sharedSecret != null )
            {
                manager.setSecretKey( domain, sharedSecret );
            }
            manager.addComponent( domain, component );
            Log.info( "External component registered to XMPP domain." );

            Log.info( "Ready!" );
            while ( true )
            {
                try
                {
                    Thread.sleep( 500 );
                }
                catch ( InterruptedException e )
                {
                    break;
                }
            }
        }
        catch ( Exception e )
        {
            Log.error( "An unexpected exception occurred!", e );
        }
        finally
        {
            try
            {
                Log.debug( "Shutting down..." );
                if ( jetty != null )
                {
                    jetty.stop();
                }

                if ( manager != null )
                {
                    manager.removeComponent( domain );
                }

                RepositoryManager.getInstance().destroy();
            }
            catch ( Exception e )
            {
                Log.error( "An unexpected error occurred while shutting down.", e );
            }
        }
    }
}
