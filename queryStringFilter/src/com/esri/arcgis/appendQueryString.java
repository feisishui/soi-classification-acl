package com.esri.arcgis;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class appendQueryString implements Filter {
	
	private static final Logger log = Logger.getLogger("appendQueryString");
	
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new SanitizedQueryStringHttpServletRequest((HttpServletRequest) request), response);
    }

    @Override
    public void destroy() {

    }


    private static class SanitizedQueryStringHttpServletRequest extends HttpServletRequestWrapper {
        public SanitizedQueryStringHttpServletRequest(final HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getQueryString() {
        	//log.log(Level.INFO, "       raw queryString >>>>>> " + super.getQueryString());
        	try {
	        	if (super.getQueryString() != null){
	        		return sanitizeQuery(super.getQueryString());
	        	}
	        	else return null;
        	} catch (Exception e) {
        		log.log(Level.SEVERE, "       >>>>> Error in getQueryString " + e.getMessage());
        		return null;
        	}
        }
        
        private static String sanitizeQuery(final String rawQuery) {
        	if (rawQuery == null | rawQuery.length() == 0) {
        		return null;
        	}
        	else {
	            StringBuilder sb = new StringBuilder();
	            for (int i = 0; i < rawQuery.length(); i++) {
	                char c = rawQuery.charAt(i);
	                sb.append(c);
	            }
	            
	            // Get clearance information and append to queryString.
	            //  This does not work with appending additional request parameters since the
	            //  Web Adaptor code does not make that call.
	            sb.append("&citizen=US&clearances=C%2CS%2CTS&accesses=TK%2CSI-G%2CHCS-P");
	            log.log(Level.INFO, "       modified queryString >>>>>> " + sb.toString());
	            return sb.toString();
	        }
        }
    }
}