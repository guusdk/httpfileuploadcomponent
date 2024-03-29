HTTP File Upload Component
========

About
-----
This application allows occupants of an [XMPP](https://xmpp.org) multi-user chat room to share data (images and such) with each-other.

More specifically, this is a [Jabber Component](https://xmpp.org/extensions/xep-0114.html) that implements [XEP-0363: HTTP File Upload](https://xmpp.org/extensions/xep-0363.html)

Download
--------

The compiled application, as well as the source code, is available for download on the ['releases' page](https://github.com/guusdk/httpfileuploadcomponent/releases) of this project.

Building
--------

This project is using a Maven-based build process. To build this project yourself, ensure that the following are available on your local host:

* A Java Development Kit, version 8.
* Apache Maven 3

To build this project, invoke on a command shell:

    $ mvn clean package

Upon completion, the application will be available in the `target` directory.

Usage
-----
To run the application, execute

    java -jar httpfileuploadcomponent-<versionnumber>-jar-with-dependencies.jar

A number of arguments can (and probably should) be added. A common execution is:

    java -jar httpfileuploadcomponent-<versionnumber>-jar-with-dependencies.jar \
       --xmppHost openfire1.example.org \
       --sharedSecret hqcUrfHtgE73FktcXwfrP

This will start the application, connect it to an XMPP server with the provided
shared secret, and launch a webserver on a non-local interface of the machine on
which the application is executed.

When end-users should interact with the webserver using a different address, the
``announcedWebHost`` argument can be used. This provides a convenient way to work
with remote proxies or port-forwarded network topologies.

A full set of usage instructions are provided by adding the ``--help`` argument:

    $ java -jar httpfileuploadcomponent-1.0-jar-with-dependencies.jar --help
        usage: arguments
            --announcedWebContextRoot <arg>   The context root that is to be used
                                              by the end users (when different
                                              from webContextRoot). Defaults to
                                              webContextRoot value.
            --announcedWebHost <arg>          The hostname or IP address that is
                                              to be used by the end users (when
                                              different from webHost). Defaults to
                                              the webHost address.
            --announcedWebPort <arg>          The TCP port number that is to be
                                              used by the end users (when
                                              different from webPort). Defaults to
                                              the webPort value.
            --announcedWebProtocol <arg>      The Protocol that is to be used by
                                              the end users. Defaults to the
                                              webProtocol value
            --clamavHost <arg>                The FQDN or IP address of the host
                                              running the optional ClamAV malware
                                              scanner, if any.
            --clamavPort <arg>                The TCP port number for the optional
                                              ClamAV malware scanner, if any.
            --domain <arg>                    The domain that will be used for the
                                              component with the XMPP domain.
            --fileRepo <arg>                  Store files in a directory provided
                                              by the file system. Provide the
                                              desired path as a value. Path must
                                              exist.
            -h,--help                         Displays this help text.
            --maxFileSize <arg>               The maximum allowed size per file,
                                              in bytes. Use -1 to disable file
                                              size limit. Defaults to 5242880
                                              (five MB).
            --sharedSecret <arg>              The shared secret, that
                                              authenticates this component with
                                              the XMPP domain.
            --tempFileRepo                    Store files in the temporary
                                              directory provided by the file
                                              system.
            --webContextRoot <arg>            The context root of the web server
                                              through which the web frontend will
                                              be made available. Defaults to '/',
                                              the root context.
            --webHost <arg>                   The hostname or IP address on which
                                              the webserver will be ran. Defaults
                                              to an arbitrary, non-local address
                                              of this machine.
            --webPort <arg>                   The TCP port number of the
                                              webserver. Defaults to 12121.
            --webProtocol <arg>               The protocol that is used to expose
                                              services by the webservice. Defaults
                                              to http
            --wildcardCORS                    Add CORS headers that define a
                                              liberal access control regime
                                              (wildcard origin, various headers
                                              and methods).
            --xmppHost <arg>                  The FQDN or IP address (not XMPP
                                              domain name) of the XMPP domain that
                                              this component will connect to.
                                              Defaults to 'localhost'.
            --xmppPort <arg>                  The TCP port number on the xmppHost,
                                              to which a connection will be made.
                                              Defaults to 5275.

Scanning for Malware
--------------------
To facilitate virus scanning, you can configure the application to use ClamAV. ClamAV is a third-party, open source
(GPLv2) anti-virus toolkit, available at https://www.clamav.net/

To configure this application to use ClamAV, install, configure and run clamav-daemon, the scanner daemon of ClamAV.
Configure the daemon in such a way that Openfire can access it via TCP.

Note: ClamAV is configured with a maximum file size. Ensure that this is at least as big as the `maxFileSize` that is
provided as an argument to the HTTP File Upload Component.

Then, start the HTTP File Upload Component application with the `clamavHost` and `clamavPort` arguments. When these are
provided, the application will supply each file that is being uploaded to the ClamAV daemon for scanning. A file upload
will fail when the ClamAV daemon could not be reached, or, obviously, when it detects malware.

While malware scanning can offer some protection against distributing unwanted content, it has limitations. Particularly
when the uploaded data is encrypted, the scanner is unlikely able to detect any malware in it.
