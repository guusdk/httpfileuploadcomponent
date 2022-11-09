/*
 * Copyright (c) 2017-2022 Guus der Kinderen. All rights reserved.
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

import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.DocumentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.AbstractComponent;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import java.io.FileReader;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

/**
 * A XMPP component that implements XEP-0363.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363</a>
 */
public class Component extends AbstractComponent
{
    // Earlier namespace, used before v0.3.0 of XEP-0363
    public final static String NAMESPACE_EXP = "urn:xmpp:http:upload";

    // Namespace from version 0.3.0 onwards.
    public final static String NAMESPACE = "urn:xmpp:http:upload:0";

    private static final Logger Log = LoggerFactory.getLogger( Component.class );
    private final String name;

    /**
     * Instantiates a new component.
     *
     * @param name     The component name (cannot be null or an empty String).
     */
    public Component( String name )
    {
        super();

        if ( name == null || name.trim().isEmpty() )
        {
            throw new IllegalArgumentException( "Argument 'name' cannot be null or an empty String." );
        }

        this.name = name.trim();
    }

    @Override
    public String getDescription()
    {
        return "HTTP File Upload, an implementation of XEP-0363, supporting exchange of files between XMPP entities.";
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    protected String discoInfoIdentityCategory()
    {
        return "store"; // TODO: the XEP example reads 'store' but I'm unsure if this is a registered type.
    }

    @Override
    protected String discoInfoIdentityCategoryType()
    {
        return "file"; // TODO: the XEP example reads 'file' but I'm unsure if this is a registered type.
    }

    @Override
    protected String[] discoInfoFeatureNamespaces()
    {
        return new String[] { NAMESPACE, NAMESPACE_EXP };
    }

    @Override
    protected IQ handleDiscoInfo( IQ iq )
    {
        final IQ response = super.handleDiscoInfo( iq );

        // Add service configuration / preconditions if these exist.
        if ( SlotManager.getInstance().getMaxFileSize() > 0 )
        {
            final Element configForm = response.getChildElement().addElement( "x", "jabber:x:data" );
            configForm.addAttribute( "type", "result" );
            configForm.addElement( "field" ).addAttribute( "var", "FORM_TYPE" ).addAttribute( "type", "hidden" ).addElement( "value" ).addText( NAMESPACE );
            configForm.addElement( "field" ).addAttribute( "var", "max-file-size" ).addElement( "value" ).addText( Long.toString( SlotManager.getInstance().getMaxFileSize() ) );
        }

        return response;
    }

    @Override
    protected IQ handleIQGet( IQ iq ) throws Exception
    {
        final Element request = iq.getChildElement();
        final Collection<String> namespaces = Arrays.asList( NAMESPACE, NAMESPACE_EXP );
        // Implements the TYPE_IQ jabber:iq:version protocol (version info xep-0092). Allows
        // XMPP entities to query each other's application versions.  The server
        // will respond with its current version info.
        if ("query".equals(request.getQName().getName()) && "jabber:iq:version".equals( request.getNamespaceURI())) {
            try {
                Element answerElement = DocumentHelper.createElement(QName.get("query", "jabber:iq:version"));
                MavenXpp3Reader reader = new MavenXpp3Reader();
                Model model = reader.read(new FileReader("pom.xml"));
                if (model.getName() != null){
                    answerElement.addElement("name").setText(model.getName());
                }
                if(model .getDescription() != null){
                    answerElement.addElement("description").setText(model.getDescription());
                }
                if (model.getVersion() != null){
                    answerElement.addElement("version").setText(model.getVersion());
                }
                final String os = System.getProperty("os.name") + ' ' 
                        + System.getProperty("os.version") + " ("
                        + System.getProperty("os.arch") + ')';
                final String java = "Java " + System.getProperty("java.version");
                answerElement.addElement("os").setText(os + " - " + java);
                IQ result = IQ.createResultIQ(iq);
                result.setChildElement(answerElement);
                return result;
            } catch (Exception ex) {
                final IQ result = IQ.createResultIQ( iq );
                final PacketError error = new PacketError( PacketError.Condition.not_acceptable);
                result.setError( error );
                return result;
            }
        } 
        if ( !namespaces.contains( request.getNamespaceURI() ) || !request.getName().equals( "request" ) )
        {
            return null;
        }
        final boolean isPre030Style = NAMESPACE_EXP.equals( request.getNamespaceURI() );

        Log.info( "Entity '{}' tries to obtain slot.", iq.getFrom() );
        String fileName = null;
        if ( request.attributeValue( "filename" ) != null && !request.attributeValue( "filename" ).trim().isEmpty() )
        {
            fileName = request.attributeValue( "filename" ).trim();
        }

        if ( request.element( "filename" ) != null && !request.element( "filename" ).getTextTrim().isEmpty() )
        {
            fileName = request.element( "filename" ).getTextTrim();
        }

        if ( fileName == null )
        {
            final IQ response = IQ.createResultIQ( iq );
            response.setError( PacketError.Condition.bad_request );
            return response;
        }

        // TODO validate the file name (path traversal, etc).

        String size = null;
        if ( request.attributeValue( "size" ) != null && !request.attributeValue( "size" ).isEmpty() )
        {
            size = request.attributeValue( "size" ).trim();
        }

        if ( request.element( "size" ) != null && !request.element( "size" ).getTextTrim().isEmpty() )
        {
            size = request.element( "size" ).getTextTrim();
        }

        if ( size == null )
        {
            final IQ response = IQ.createResultIQ( iq );
            response.setError( PacketError.Condition.bad_request );
            return response;
        }

        final long fileSize;
        try
        {
            fileSize = Long.parseLong( size );
        }
        catch ( NumberFormatException e )
        {
            final IQ response = IQ.createResultIQ( iq );
            response.setError( PacketError.Condition.bad_request );
            return response;
        }

        final SlotManager manager = SlotManager.getInstance();
        final Slot slot;
        try
        {
            slot = manager.getSlot( iq.getFrom(), fileName, fileSize );
        }
        catch ( TooLargeException ex )
        {
            final IQ response = IQ.createResultIQ( iq );
            final PacketError error = new PacketError( PacketError.Condition.not_acceptable, PacketError.Type.modify, "File too large. Maximum file size is " + ex.getMaximum() + " bytes." );
            error.getElement().addElement( "file-too-large", iq.getChildElement().getNamespaceURI() ).addElement( "max-file-size" ).addText( Long.toString( ex.getMaximum() ) );
            response.setError( error );
            return response;
        }

        final URL putUrl = SlotManager.getPutUrl(slot);
        final URL getUrl = SlotManager.getGetUrl(slot);

        Log.info( "Entity '{}' obtained slot for '{}' ({} bytes). PUT-URL: {} GET-URL: {}", iq.getFrom(), fileName, fileSize, putUrl, getUrl );

        final IQ response = IQ.createResultIQ( iq );
        final Element slotElement = response.setChildElement( "slot", iq.getChildElement().getNamespaceURI() );
        if ( isPre030Style )
        {
            slotElement.addElement( "put" ).setText( putUrl.toExternalForm() );
            slotElement.addElement( "get" ).setText( getUrl.toExternalForm() );
        }
        else
        {
            slotElement.addElement( "put" ).addAttribute( "url", putUrl.toExternalForm() );
            slotElement.addElement( "get" ).addAttribute( "url", getUrl.toExternalForm() );
        }
        return response;
    }
}
