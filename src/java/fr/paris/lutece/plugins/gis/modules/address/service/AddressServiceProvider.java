package fr.paris.lutece.plugins.gis.modules.address.service;

import fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresse;
import fr.paris.lutece.plugins.gis.business.LonLat;
import fr.paris.lutece.portal.service.spring.SpringContextService;


import java.rmi.RemoteException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;


/**
 *
 */
public final class AddressServiceProvider
{
	/**
	 * private static members
	 */
	private static IAddressService gisAddressService = null;
	
	/**
	 * Initializes private static members
	 */
	static{
		AddressServiceProvider.gisAddressService= (IAddressService) 
				SpringContextService.getBean( "gisAdresseService" );
	};
	
	
	public static Set<Adresse> searchAddress( HttpServletRequest request, String address, String SRID) 
    		throws RemoteException
   {
		return gisAddressService.searchAddress(request, address, SRID);
   }
	
	public static Adresse inverseGeolocalization( HttpServletRequest request, LonLat coord, String SRID) 
    		throws RemoteException
   {
		return gisAddressService.inverseGeolocalization(request, coord, SRID);
   }
}
