package com.esri.gw.security;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
//import java.util.logging.Logger;
//import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class AddSecurityAccessWrapper extends HttpServletRequestWrapper
{
    private final Map<String, String[]> modifiableParameters;
    private Map<String, String[]> allParameters = null;
//    private static final Logger log = Logger.getLogger("AddSecurityAccessWrapper");

    /**
     * Create a new request wrapper that will merge additional parameters into
     * the request object without prematurely reading parameters from the
     * original request.
     * 
     * @param request
     * @param additionalParams
     */
    public AddSecurityAccessWrapper(final HttpServletRequest request, 
                                    final Map<String, String[]> additionalParams)
    {
        super(request);
        modifiableParameters = new TreeMap<String, String[]>();
        modifiableParameters.putAll(additionalParams);
//        log.log(Level.INFO, "**** FILTER CALLED ****");
        
//        Enumeration<String> pnames = request.getParameterNames();
//        while (pnames.hasMoreElements()) {
//            String pname = pnames.nextElement();
//            String pvalues[] = request.getParameterValues(pname);
//            StringBuilder result = new StringBuilder(pname);
//            result.append('=');
//            for (int i = 0; i < pvalues.length; i++) {
//                if (i > 0) {
//                    result.append(", ");
//                }
//                result.append(pvalues[i]);
//            }
//            log.log(Level.INFO, "         parameter>> " + result.toString());
//        }
    }

    @Override
    public String getParameter(final String name)
    {
        String[] strings = getParameterMap().get(name);
        if (strings != null)
        {
            return strings[0];
        }
        return super.getParameter(name);
    }

    @Override
    public Map<String, String[]> getParameterMap()
    {
        if (allParameters == null)
        {
            allParameters = new TreeMap<String, String[]>();
            allParameters.putAll(super.getParameterMap());
            allParameters.putAll(modifiableParameters);
        }
        //og.log(Level.INFO, "**** FILTER CALLED ****");
        //Return an unmodifiable collection because we need to uphold the interface contract.
        return Collections.unmodifiableMap(allParameters);
    }

    @Override
    public Enumeration<String> getParameterNames()
    {
        return Collections.enumeration(getParameterMap().keySet());
    }

    @Override
    public String[] getParameterValues(final String name)
    {
        return getParameterMap().get(name);
    }
}