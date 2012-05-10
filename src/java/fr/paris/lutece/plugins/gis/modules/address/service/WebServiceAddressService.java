package fr.paris.lutece.plugins.gis.modules.address.service;

import fr.paris.lutece.plugins.address.business.axis.AdresseService;
import fr.paris.lutece.plugins.address.business.axis.AdresseServiceLocator;
import fr.paris.lutece.plugins.address.business.axis.AdresseServicePortType;
import fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresses;
import fr.paris.lutece.portal.service.util.AppLogService;

import org.apache.axis.client.Stub;

import java.io.StringReader;

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
