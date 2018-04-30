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

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import fr.paris.lutece.plugins.gis.business.LonLat;
import fr.paris.lutece.plugins.gis.service.IAddressServiceFacade;
import fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresse;
import fr.paris.lutece.plugins.address.util.LibraryAddressUtils;
import fr.paris.lutece.portal.service.util.AppLogService;


import org.apache.commons.lang.StringUtils;

public class ParisAddressServiceFacade implements IAddressServiceFacade
{
    public static final String CONSTANT_ONE_SPACE = " ";
    public static final String CONSTANT_OPEN_PARENTHESIS = "(";
    public static final String VALID_GEOMETRY_REGEX = ".*\\(.+ .+\\)";
    
	/**
	 * {@inheritDoc}
	 */
	public LonLat getGeolocalization(final HttpServletRequest request, String address, String strSRID)
			throws RemoteException, IllegalArgumentException 
	{	
		Set<Adresse> setAddresses = AddressServiceProvider.searchAddress(request, address, strSRID);

		if ( setAddresses == null){ return null; } // no results
		
		LonLat lonLat = null;				

		String trimedAddress = address.replaceAll(" ","");
		int bestAddressMatchCompareTo = Integer.MAX_VALUE;
		Adresse bestMatchAddress = null;
		Iterator<Adresse> iterator = setAddresses.iterator();		
		while( iterator.hasNext() && bestAddressMatchCompareTo != 0 )
		{
				Adresse currentAddress = iterator.next();				
				String comparedAddress = getLabelAddress(currentAddress).replace(" ","");

//				int currentCompareTo = Math.abs(comparedAddress.compareToIgnoreCase(trimedAddress));
				int currentCompareTo = Math.abs(StringUtils.getLevenshteinDistance(comparedAddress, trimedAddress));
				
				if( currentCompareTo < bestAddressMatchCompareTo  )
				{
					bestMatchAddress = currentAddress;
					bestAddressMatchCompareTo = currentCompareTo;
				}
				AppLogService.debug(getLabelAddress(currentAddress));
				AppLogService.debug(currentAddress.getGeometry());
		}
		lonLat = getAddressLonLat(bestMatchAddress.getGeometry());
		return lonLat;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getInverseGeolocalization(HttpServletRequest request, LonLat lonLat, String strSRID) 
			throws RemoteException 
	{
		Adresse adresse = AddressServiceProvider.inverseGeolocalization(request, lonLat, strSRID);
		
		if ( adresse == null){ return ""; } // no results
		
		return getLabelAddress(adresse);
	}
	
	/**
	 * GetLabelAddress( ).
	 * 
	 * @param address The address
	 * @return The concatenate string of the given address information.
	 */
	private static String getLabelAddress(final Adresse address)
	{
		StringBuilder addressBuilder = new StringBuilder();
    
        addressBuilder.append(address.getNumero(  ))
        			  .append(CONSTANT_ONE_SPACE)
        			  .append(( address.getSuffixe(  ) != null )? address.getSuffixe(  ) :"" );
        // add TypeVoie 
        if( address.getTypeVoie(  )  != null ) 
        {
	        if ( !LibraryAddressUtils.isTerminateByApostrophe(  address.getTypeVoie(  ) ) )
	        {
	        	addressBuilder.append(CONSTANT_ONE_SPACE);
	        }	        	
        	addressBuilder.append(address.getTypeVoie(  ));      	
        }
        addressBuilder.append(CONSTANT_ONE_SPACE)
		  			  .append(address.getNomVoie(  ))
		  			  .append("/")
		  			  .append(getAddressLonLat(address.getGeometry(  )));
        
        return addressBuilder.toString();
	}
	
	/**
	 * GetAddressLonLat( ).
	 * 
	 * @param strGeometry
	 * @return 
	 * @throws RemoteException
	 */
	private static LonLat getAddressLonLat(String strGeometry)	
	{	
		LonLat lonLat = null;
        if ( StringUtils.isNotBlank( strGeometry ) && strGeometry.matches( VALID_GEOMETRY_REGEX ) )
        {
            String strCleanedGeometry = strGeometry.substring( 
            		strGeometry.lastIndexOf( CONSTANT_OPEN_PARENTHESIS ) +1, 
            		strGeometry.length(  ) - 1
            );

            try
            {
            	Float lon =  Float.parseFloat( strCleanedGeometry.substring( 0, 
            			strCleanedGeometry.lastIndexOf( CONSTANT_ONE_SPACE ) ) );
            	
            	Float lat = Float.parseFloat( strCleanedGeometry.substring( strCleanedGeometry.lastIndexOf( 
                        CONSTANT_ONE_SPACE ), strCleanedGeometry.length(  ) ) );
            	
            	lonLat = new LonLat(lon, lat);
            }
            catch ( NumberFormatException nfe )
            {
                // set to 0
                AppLogService.error( "LibraryAddressUtils.fillAddressGeolocation failed for " + strGeometry + " " +
                    nfe.getLocalizedMessage(  ) );
                lonLat = new LonLat(0, 0);
            }
        }
        return lonLat;
	}
}
