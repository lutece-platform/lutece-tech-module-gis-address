package fr.paris.lutece.plugins.gis.modules.address.service;

import java.rmi.RemoteException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import fr.paris.lutece.plugins.address.business.jaxb.wsSearchAdresse.Adresse;

public interface IAddressService 
{
    Set<Adresse> searchAddress( HttpServletRequest request, String address, String SRID) 
    		throws RemoteException;
}
