/*
 * Copyright (c) 2002-2017, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.gis.modules.address.service;

import fr.paris.lutece.plugins.address.business.axis.AdresseService;
import fr.paris.lutece.plugins.address.business.axis.AdresseServiceLocator;
import fr.paris.lutece.plugins.address.business.axis.AdresseServicePortType;
import fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresses;
import fr.paris.lutece.plugins.gis.business.LonLat;
import fr.paris.lutece.portal.service.util.AppLogService;

import org.apache.axis.client.Stub;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;

import java.rmi.RemoteException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.stream.StreamSource;


/**
 *
 */
public class WebServiceAddressService implements IAddressService
{
    //jaxb context
    private static final String JAXB_CONTEXT_WS_SEARCH_ADDRESS = "fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse";

    private String _strUrlWS;
    private String _strDefaultCity;
    private String _strDateSearch;
    private String _strUserName;
    private String _strPassword;
    private String _strTimeOut;


    /**
     * @throws RemoteException the RemoteExecption
     * @param strSRID the srsid
     * @param request Request
     * @param labeladresse the  label adress
     * @param strArrondissement Arrondissement
     * @return the XML flux of all adress corresponding
     * @see <a href="http://dev.lutece.paris.fr/jira/browse/ADDRESS-8">ADDRESS-8</a>
     */
    public Set<fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresse> searchAddress( HttpServletRequest request, String address, String SRID) 
    		throws RemoteException
    {
        String responseWebService = null;
        AdresseService adresseService = new AdresseServiceLocator(  );

        try
        {
            URL urlWS = null;

            Stub portType = null;

            String strUrlWS = this.getUrlWS(  );
            if ( (  strUrlWS == null ) || strUrlWS.equals( "" ) )
            {
                portType = (Stub) adresseService.getAdresseServiceHttpPort(  );
            }
            else
            {
                try
                {
                    urlWS = new URL( strUrlWS );
                }
                catch ( MalformedURLException e )
                {
                    AppLogService.error( e.getMessage(  ), e );
                }

                portType = (Stub) adresseService.getAdresseServiceHttpPort( urlWS );
            }

            portType.setUsername( getUserName(  ) );
            portType.setPassword( getPassword(  ) );

            setTimeout( portType );

            responseWebService = ( (AdresseServicePortType) portType ).searchAddress( getDefaultCity(  ), 
            		address,SRID, getDateSearch(  ) );

            // check null result and then return null list
            if ( responseWebService == null )
            {
                return null;
            }
        }
        catch ( ServiceException e )
        {
            AppLogService.error( e.getMessage(  ), e );
        }

        //traitement du flux xml		
        Adresses adresses = null;

        JAXBContext jc;

        try
        {
            jc = JAXBContext.newInstance( JAXB_CONTEXT_WS_SEARCH_ADDRESS );

            Unmarshaller u = jc.createUnmarshaller(  );
            StringBuffer xmlStr = new StringBuffer( responseWebService );
            adresses = (Adresses) u.unmarshal( new StreamSource( new StringReader( xmlStr.toString(  ) ) ) );
        }
        catch ( JAXBException e )
        {
            AppLogService.error( e.getMessage(  ), e );
        }

        List<fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresse> listAdresses = adresses.getAdresse(  );

        // Added for filter the double adresse
        Set<fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresse> setAddresses = null;

        //build the list choice
        if ( ( listAdresses != null ) && !listAdresses.isEmpty(  ) )
        {
            //Added for filter the double adresse
        	setAddresses = new LinkedHashSet<fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresse>(  );
        	setAddresses.addAll(listAdresses);
        }
        return setAddresses;
    }
    
    
    /**
     * @throws RemoteException the RemoteExecption
     * @param strSRID the srsid
     * @param request Request
     * @param coord LonLat of the point
     * @return the XML flux of closest address corresponding
     */
    public fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresse inverseGeolocalization( HttpServletRequest request, LonLat coord, String SRID) 
    		throws RemoteException
    {
        String responseWebService = null;
        AdresseService adresseService = new AdresseServiceLocator(  );
        try
        {
            URL urlWS = null;

            Stub portType = null;

            String strUrlWS = this.getUrlWS(  );
            if ( (  strUrlWS == null ) || strUrlWS.equals( "" ) )
            {
                portType = (Stub) adresseService.getAdresseServiceHttpPort(  );
            }
            else
            {
                try
                {
                    urlWS = new URL( strUrlWS );
                }
                catch ( MalformedURLException e )
                {
                    AppLogService.error( e.getMessage(  ), e );
                }

                portType = (Stub) adresseService.getAdresseServiceHttpPort( urlWS );
            }

            portType.setUsername( getUserName(  ) );
            portType.setPassword( getPassword(  ) );

            setTimeout( portType );

            responseWebService = ( (AdresseServicePortType) portType ).inverseGeolocalization( getDefaultCity(  ), 
            		coord.getLongitude(), coord.getLatitude(), SRID, getDateSearch(  ) );
            
            
            // check null result and then return null list
            if ( responseWebService == null )
            {
                return null;
            }
        }
        catch ( ServiceException e )
        {
            AppLogService.error( e.getMessage(  ), e );
        }

        //traitement du flux xml		
        fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresse adresse = new fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresse();
        
        try
        {
        	DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        	Document document = parser.parse(new InputSource(new StringReader(responseWebService)));
 
			//Get data corresponding to the address
        	String identifiant = ((Element)document.getElementsByTagName("identifiant").item(0)).getTextContent();
        	String geometry = ((Element)document.getElementsByTagName("geometry").item(0)).getTextContent();
        	String numero = ((Element)document.getElementsByTagName("numero").item(0)).getTextContent();
        	String typeVoie = ((Element)document.getElementsByTagName("typeVoie").item(0)).getTextContent();
        	String nomVoie = ((Element)document.getElementsByTagName("nomVoie").item(0)).getTextContent();
        	String commune = ((Element)document.getElementsByTagName("commune").item(0)).getTextContent();
			
        	
        	adresse.setIdentifiant(new BigInteger(identifiant));
        	adresse.setGeometry(geometry);
        	adresse.setNumero(new Short(numero));
        	adresse.setTypeVoie(typeVoie);
        	adresse.setNomVoie(nomVoie);
        	adresse.setCommune(commune);
        }
        catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return adresse;
    }

    /**
    *
    * @return the date for parameter methodes of web service
    */
    public String getDateSearch(  )
    {
        return _strDateSearch;
    }

    /**
     *
     * @param strDateSearch the new date search
     */
    public void setDateSearch( String strDateSearch )
    {
        _strDateSearch = strDateSearch;
    }

    /**
     *
     * @return the default city for parameter methodes of web service
     */
    public String getDefaultCity(  )
    {
        return _strDefaultCity;
    }

    /**
     *
     * @param strDefaultCity the new default city
     */
    public void setDefaultCity( String strDefaultCity )
    {
        _strDefaultCity = strDefaultCity;
    }

    /**
     *
     * @return the url of the web service
     */
    public String getUrlWS(  )
    {
        return _strUrlWS;
    }

    /**
     *
     * @param strUrlWS the new web service url
     */
    public void setUrlWS( String strUrlWS )
    {
        _strUrlWS = strUrlWS;
    }

    /**
     *
     * @return the password
     */
    public String getPassword(  )
    {
        return _strPassword;
    }

    /**
     *
     * @param password the password
     */
    public void setPassword( String password )
    {
        _strPassword = password;
    }

    /**
     *
     * @return the user name
     */
    public String getUserName(  )
    {
        return _strUserName;
    }

    /**
     *
     * @param userName the user name
     */
    public void setUserName( String userName )
    {
        _strUserName = userName;
    }

    /**
    *
    * @return the timeout
    */
    public String getTimeOut(  )
    {
        return _strTimeOut;
    }

    /**
     *
     * @param timeOut the timeout
     */
    public void setTimeOut( String timeOut )
    {
        _strTimeOut = timeOut;
    }

    /**
     * Sets the timeout to the stub
     * @param portType
     */
    private void setTimeout( Stub portType )
    {
        try
        {
            portType.setTimeout( Integer.parseInt( getTimeOut(  ) ) );
        }
        catch ( NumberFormatException e )
        {
            AppLogService.error( 
                "WebServiceAddressService : timeOut is not set correctly for WebServiceAddressService. Please check address_context.xml. Will use no timeout" );
        }
    }
}
