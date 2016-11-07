package com.esri.gw.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
//import javax.servlet.annotation.WebFilter;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Servlet Filter implementation class AddSecurityAccess
 */
//@WebFilter(description = "Adds Security Classification and Accesses to AGS Service Requests", urlPatterns = { "/AddSecurityAccess" })
public class AddSecurityAccess implements Filter {

	private static final Logger log = Logger.getLogger("AddSecurityAccess");
	private static final String specailFieldsRegex = "(?i)^searchtext$";
	
    /**
     * Default constructor. 
     */
    public AddSecurityAccess() {
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
		// TODO Auto-generated method stub
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// TODO Auto-generated method stub
		// place your code here

		Map<String, String[]> additionalParams = new HashMap<String, String[]>();
		additionalParams.put("finegraincontrols", new String[] {"HCS-P", "SI", "TK"});
		additionalParams.put("clearance", new String[] {"TS", "S", "C"});
		additionalParams.put("citizenship", new String[] {"USA"});
		AddSecurityAccessWrapper enhancedHttpRequest = new AddSecurityAccessWrapper((HttpServletRequest)request, additionalParams);
		
		Map<String, String[]> params = enhancedHttpRequest.getParameterMap();
		for(String name : params.keySet()) {
			String[] vals = params.get(name);
			if (name.matches(specailFieldsRegex) && vals[0] != null) {
				log.log(Level.INFO, "       #1 parameter >>>>>> " + name + " :: " + vals[0]);
				continue;
			}
			Object value = toJsonValue(vals[0]);
		    if(value == null) {
		        continue;
		    }
		    log.log(Level.INFO, "       #2 parameter >>>>>> " + name + " :: " + value);
			//log.log(Level.INFO, "       Enhanced parameter >>>>>> " + name + " :: " + vals[0]);
		}
//        Enumeration<String> pnames = enhancedHttpRequest.getParameterNames();
//        while (pnames.hasMoreElements()) {
//            String pname = pnames.nextElement();
//            String pvalues[] = enhancedHttpRequest.getParameterValues(pname);
//            StringBuilder result = new StringBuilder(pname);
//            result.append('=');
//            for (int i = 0; i < pvalues.length; i++) {
//                if (i > 0) {
//                    result.append(", ");
//                }
//                result.append(pvalues[i]);
//            }
//            log.log(Level.INFO, "       Enhanced parameter >>>>>> " + result.toString());
//        }

		// pass the request along the filter chain
		chain.doFilter(enhancedHttpRequest, response);
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		// TODO Auto-generated method stub
	}
	  /**
	   * Attempts to coerce the input string value to a valid JSON type: 
	   * number, boolean, JSON object, JSON array. 
	   * If it cannot be coerced to any of the types mentioned above, it will return the string as is.
	   */
	  public static Object toJsonValue(String strVal) {
	    if(isEmptyString(strVal) || strVal.equalsIgnoreCase("null"))
	      return null;
	    
	    //trim strVal as beginning / trailing spaces can throw off the regex parser
	    //beginning / trailing spaces can also cause NumberFormatExceptions     
	    strVal = strVal.trim();
	    
	    //int / long
	    if(strVal.matches("([\\+-]?\\d+)")) {
	      if(strVal.matches("[\\+-]?0+.*")){
	        return strVal;
	      }
	      return Long.parseLong(strVal);
	    }
	    
	    //double / float
	    if(strVal.matches("([\\+-]?\\d*\\.\\d+)([eE][\\+-]?\\d+)?") || strVal.matches("([\\+-]?\\d+)([eE][\\+-]?\\d+)")) {
	        return Double.parseDouble(strVal);
	    }
	    
	    //boolean
	    if(strVal.matches("(?i)(true|false)")) {
	      return Boolean.parseBoolean(strVal);
	    }
	    
	    //JSONObject?
	    if(isJsonString(strVal)) {
	      try {
	        return new JSONObject(strVal);
	      } catch(Exception e) {
	    	  log.log(Level.INFO, "Unable to coerce to JSONObject");
	      }
	    }
	    
	    //JSONArray?
	    if(isJsonArrayString(strVal)) {
	      try {
	        return new JSONArray(strVal);
	      } catch(Exception e) {
	    	  log.log(Level.INFO, "Unable to coerce to JSONArray");
	      }
	    }
	    
	    //string
	    return strVal;
	  }
	  
	  /**
	   * Returns true if the input string could be a JSON string. 
	   * This method does not do a deep evaluation. It simply checks if the string starts and ends with parentheses (i.e. { and })
	   * @param json the input string
	   * @return true, if the input string could be a JSON string
	   */
	  public static boolean isJsonString(String json) {
	    return json != null && json.matches("(?s)^\\s*\\{.*\\}\\s*$");
	  }
	  
	  /**
	   * Returns true if the input string could be a JSON array. 
	   * This method does not do a deep evaluation. It simply checks if the string starts and ends with parentheses (i.e. [ and ])
	   * @param json the input string
	   * @return true, if the input string could be a JSON array
	   */
	  public static boolean isJsonArrayString(String json) {
	    return json != null && json.matches("(?s)^\\s*\\[.*\\]\\s*$");
	  }
	  
	  /**
	   * Returns true if the string is null or its trimmed length is 0.
	   * @param str the string to be tested
	   * @return true if the string is null or its trimmed length is 0
	   */
	  public static boolean isEmptyString(String str) {
	    return str == null || str.trim().length() == 0;
	  }
}
