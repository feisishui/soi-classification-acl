package com.esri.gw.security;

/*
 COPYRIGHT 1995-2014 ESRI
 TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 Unpublished material - all rights reserved under the 
 Copyright Laws of the United States and applicable international
 laws, treaties, and conventions.

 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts and Legal Services Department
 380 New York Street
 Redlands, California, 92373
 USA

 email: contracts@esri.com
 */

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.interop.extn.ArcGISExtension;
import com.esri.arcgis.interop.extn.ServerObjectExtProperties;
import com.esri.arcgis.server.IServerObject;
import com.esri.arcgis.server.IServerObjectExtension;
import com.esri.arcgis.server.IServerObjectHelper;
import com.esri.arcgis.server.SOIHelper;
import com.esri.arcgis.server.json.JSONObject;
import com.esri.arcgis.system.ILog;
import com.esri.arcgis.system.IRESTRequestHandler;
import com.esri.arcgis.system.IRequestHandler;
import com.esri.arcgis.system.IRequestHandler2;
import com.esri.arcgis.system.IWebRequestHandler;
import com.esri.arcgis.system.IWebRequestHandlerProxy;
import com.esri.arcgis.system.ServerUtilities;
import com.esri.arcgis.carto.MapServer;

import java.io.BufferedReader;
import java.io.FileReader;

/*
 * For an SOE to act as in interceptor, it needs to implement all request handler interfaces
 * IRESTRequestHandler, IWebRequestHandler, IRequestHandler2, IRequestHandler now the SOE/SOI can
 * intercept all types of calls to ArcObjects or custom SOEs.
 * 
 * This sample SOI can be used as the starting point to writing new SOIs. It is a basic example
 * which implements all request handlers and logs calls to ArcObjects or custom SOEs.
 */

/*
 * For an interceptor you need to set additional properties for @ServerObjectExtProperties the annotation.
 * 1. interceptor = true, is used to identify an SOI
 * 2. servicetype = MapService | ImageService, can be used to assign an interceptor to an Image or Map Service.
 */
@ArcGISExtension
@ServerObjectExtProperties(displayName = "ServerObjectInt1 Display Name", description = "ServerObjectInt1 Description", interceptor = true, servicetype = "MapService")
public class ServerObjectInt1 implements IServerObjectExtension,
		IRESTRequestHandler, IWebRequestHandler, IRequestHandler2,
		IRequestHandler {
	private static final long serialVersionUID = 1L;
	private static final String ARCGISHOME_ENV = "AGSSERVER";
	private ILog serverLog;
	private IServerObject so;
	private SOIHelper soiHelper;
	
	private JSONObject ConfigMap = null;
	private String mapsvcOutputDir = null;
	private String[] svc_citizen = null;
	private String[] svc_clearances = null;
	private String[] svc_accesses = null;
	

	/**
	 * Default constructor.
	 *
	 * @throws Exception
	 */
	public ServerObjectInt1() throws Exception {
		super();
	}

	/**
	 * init() is called once, when the instance of the SOE/SOI is created.
	 *
	 * @param soh the IServerObjectHelper
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AutomationException the automation exception
	 */
	public void init(IServerObjectHelper soh) throws IOException,
			AutomationException {
		/*
		 * An SOE should retrieve a weak reference to the Server Object from the Server Object Helper in
		 * order to make any method calls on the Server Object and release the reference after making
		 * the method calls.
		 */
		// Get reference to server logger utility
		this.serverLog = ServerUtilities.getServerLogger();
		// Log message with server
		this.serverLog.addMessage(3, 200, "Initialized "
				+ this.getClass().getName() + " SOI.");
		this.so = soh.getServerObject();
		String arcgisHome = getArcGISHomeDir();
		/* If null, throw an exception */
		if (arcgisHome == null) {
			serverLog.addMessage(1, 200,
					"Could not get ArcGIS home directory. Check if environment variable "
							+ ARCGISHOME_ENV + " is set.");
			throw new IOException(
					"Could not get ArcGIS home directory. Check if environment variable "
							+ ARCGISHOME_ENV + " is set.");
		}
		if (arcgisHome != null && !arcgisHome.endsWith(File.separator))
			arcgisHome += File.separator;
		// Load the SOI helper.    
		this.soiHelper = new SOIHelper(arcgisHome + "XmlSchema"
				+ File.separator + "MapServer.wsdl");
		
		// Read in the SOLR config file for this particular service.
		getConfigFromFile(this.so);
	}

	/**
	 * This method is called to handle REST requests.
	 *
	 * @param capabilities the capabilities
	 * @param resourceName the resource name
	 * @param operationName the operation name
	 * @param operationInput the operation input
	 * @param outputFormat the output format
	 * @param requestProperties the request properties
	 * @param responseProperties the response properties
	 * @return the response as byte[]
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AutomationException the automation exception
	 */
	@Override
	public byte[] handleRESTRequest(String capabilities, String resourceName,
			String operationName, String operationInput, String outputFormat,
			String requestProperties, String[] responseProperties)
			throws IOException, AutomationException {
	
		serverLog
			.addMessage(3, 200, "Access Control SOI. User: "
				+ getLoggedInUserName() + ", Operation: "
				+ operationName + ", Operation Input: "
				+ operationInput + ", requestProperties: "
				+ requestProperties + ", capabilities: "
				+ capabilities);
		

		// Find the correct delegate to forward the request too
		IRESTRequestHandler restRequestHandler = soiHelper.findRestRequestHandlerDelegate(so);
		if (restRequestHandler != null) {
			
			// If the config file for the SOLR implementation didn't get read properly or does
			//   not exist, then don't do any SOI'ing.  Just let the request pass all the way through
			//   to the underlying service.  An ArcGIS Server admin will need to look at the Logs in
			//   ArcGIS Server Manager to troubleshoot why this file wasn't read properly
			if (ConfigMap == null)
			{
				serverLog.addMessage(3, 200, "ACL SOI:  ConfigMap not properly initialized.  Passing through.");
				return restRequestHandler.handleRESTRequest(capabilities,
						resourceName, operationName, operationInput, outputFormat,
						requestProperties, responseProperties);
			}
			
			if (operationName.length() == 0 || operationName == "" || operationName == null){
				serverLog.addMessage(3, 200, "ACL SOI:  No data operation requestion.  Fullfilling request.");
				return restRequestHandler.handleRESTRequest(capabilities,
						resourceName, operationName, operationInput, outputFormat,
						requestProperties, responseProperties);
			}
			
			boolean hasCitizen = new JSONObject(operationInput.toString()).has("citizen");
			boolean hasClearances = new JSONObject(operationInput.toString()).has("clearances");
			boolean hasAccesses = new JSONObject(operationInput.toString()).has("accesses");
			String[] req_citizen;
			String[] req_clearances;
			String[] req_accesses;
			boolean citizen_valid = false;
			boolean clearances_valid = false;
			boolean accesses_valid = false;
			
			if (hasCitizen){
				// Not likely, but input could be US,CA ??
				req_citizen = new JSONObject(operationInput).getString("citizen").split(",");
				List<String> valid = Arrays.asList(svc_citizen);  // List from the config json file
				for (String s: req_citizen) {   
					if (valid.contains(s)) {
						serverLog.addMessage(3, 200, "ACL SOI:  Citizenship Match on value >> " + s);
						citizen_valid = true;
					}
				}
			}
			
			if (hasClearances){
				// Input could be C,S,TS
				req_clearances = new JSONObject(operationInput).getString("clearances").split(",");
				List<String> valid = Arrays.asList(svc_clearances);  // List from the config json file, only 1 value
				for (String s: req_clearances) {   
					if (valid.contains(s)) {
						serverLog.addMessage(3, 200, "ACL SOI:  Clearance Match on value >> " + s);
						clearances_valid = true;
					}
				}
			}
			if (hasAccesses){
				// Input could be SI-G,TK,HCS-P
				req_accesses = new JSONObject(operationInput).getString("accesses").split(",");
				List<String> valid = Arrays.asList(req_accesses);  // List from the request
				for (String s: svc_accesses) {   
					if (valid.contains(s)) {
						serverLog.addMessage(3, 200, "ACL SOI:  Accesses Match on value >> " + s);
						accesses_valid = true;
					}
					else {
						accesses_valid = false;
						break;
					}
				}
			}
			
			if (citizen_valid && clearances_valid && accesses_valid){
				serverLog.addMessage(3, 200, "ACL SOI:  Clearances pass!!");
				// Return the response
				return restRequestHandler.handleRESTRequest(capabilities,
					resourceName, operationName, operationInput, outputFormat,
					requestProperties, responseProperties);
			}
			else{
				serverLog.addMessage(3, 200, "ACL SOI:  Clearances didnt pass...");
				JSONObject response_new = new JSONObject();
				response_new.put("error",  new JSONObject().put("code", 403).put("message", "Invalid accesses"));
				serverLog.addMessage(3, 200, response_new.toString());
				return response_new.toString().getBytes("UTF-8");
			}
		}

		return null;
	}

	/**
	 * This method is called to handle SOAP requests.
	 *
	 * @param capabilities the capabilities
	 * @param request the request
	 * @return the response as String
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AutomationException the automation exception
	 */
	@Override
	public String handleStringRequest(String capabilities, String request)
			throws IOException, AutomationException {
		// Log message with server
		serverLog
				.addMessage(3, 200,
						"Request received in Sample Object Interceptor for handleStringRequest");

		/*
		 * Add code to manipulate SOAP requests here
		 */

		// Find the correct delegate to forward the request too
		IRequestHandler requestHandler = soiHelper
				.findRequestHandlerDelegate(so);
		if (requestHandler != null) {
			// Return the response
			return requestHandler.handleStringRequest(capabilities, request);
		}

		return null;
	}

	/**
	 * This method is called by SOAP handler to handle OGC requests.
	 *
	 * @param httpMethod
	 * @param requestURL the request URL
	 * @param queryString the query string
	 * @param capabilities the capabilities
	 * @param requestData the request data
	 * @param responseContentType the response content type
	 * @param respDataType the response data type
	 * @return the response as byte[]
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AutomationException the automation exception
	 */
	@Override
	public byte[] handleStringWebRequest(int httpMethod, String requestURL,
			String queryString, String capabilities, String requestData,
			String[] responseContentType, int[] respDataType)
			throws IOException, AutomationException {
		serverLog
				.addMessage(3, 200,
						"Request received in Sample Object Interceptor for handleStringWebRequest");

		/*
		 * Add code to manipulate OGC (WMS, WFC, WCS etc) requests here
		 */

		IWebRequestHandler webRequestHandler = soiHelper
				.findWebRequestHandlerDelegate(so);
		if (webRequestHandler != null) {
			return webRequestHandler.handleStringWebRequest(httpMethod,
					requestURL, queryString, capabilities, requestData,
					responseContentType, respDataType);
		}

		return null;
	}

	/**
	 * This method is called to handle binary requests from desktop.
	 *
	 * @param capabilities the capabilities
	 * @param request
	 * @return the response as byte[]
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AutomationException the automation exception
	 */
	@Override
	public byte[] handleBinaryRequest2(String capabilities, byte[] request)
			throws IOException, AutomationException {
		serverLog
				.addMessage(3, 200,
						"Request received in Sample Object Interceptor for handleBinaryRequest2");

		/*
		 * Add code to manipulate Binary requests from desktop here
		 */

		IRequestHandler2 requestHandler = soiHelper
				.findRequestHandler2Delegate(so);
		if (requestHandler != null) {
			return requestHandler.handleBinaryRequest2(capabilities, request);
		}

		return null;
	}

	/**
	 * Return the logged in user's user name.
	 * 
	 * @return
	 */
	private String getLoggedInUserName() {
		try {
			/*
			 * Get the user information.
			 */
			String userName = ServerUtilities.getServerUserInfo().getName();

			if (userName.isEmpty()) {
				return new String("Anonymous User");
			}
			return userName;
		} catch (Exception ignore) {
		}

		return new String("Anonymous User");
	}


	/**
	 * This method is called to handle schema requests for custom SOE's.
	 *
	 * @return the schema as String
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AutomationException the automation exception
	 */
	@Override
	public String getSchema() throws IOException, AutomationException {
		serverLog.addMessage(3, 200,
				"Request received in Sample Object Interceptor for getSchema");

		/*
		 * Add code to manipulate schema requests here
		 */

		IRESTRequestHandler restRequestHandler = soiHelper
				.findRestRequestHandlerDelegate(so);
		if (restRequestHandler != null) {
			return restRequestHandler.getSchema();
		}

		return null;
	}

	/**
	 * This method is called to handle binary requests from desktop. It calls the
	 * <code>handleBinaryRequest2</code> method with capabilities equal to null.
	 *
	 * @param request
	 * @return the response as the byte[]
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AutomationException the automation exception
	 */
	@Override
	public byte[] handleBinaryRequest(byte[] request) throws IOException,
			AutomationException {
		serverLog
				.addMessage(3, 200,
						"Request received in Sample Object Interceptor for handleBinaryRequest");

		/*
		 * Add code to manipulate Binary requests from desktop here
		 */

		IRequestHandler requestHandler = soiHelper
				.findRequestHandlerDelegate(so);
		if (requestHandler != null) {
			return requestHandler.handleBinaryRequest(request);
		}

		return null;
	}

	/**
	 * shutdown() is called once when the Server Object's context is being shut down and is about to
	 * go away.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AutomationException the automation exception
	 */
	public void shutdown() throws IOException, AutomationException {
		/*
		 * The SOE should release its reference on the Server Object Helper.
		 */
		this.serverLog.addMessage(3, 200, "Shutting down "
				+ this.getClass().getName() + " SOI.");
		this.serverLog = null;
		this.so = null;
		this.soiHelper = null;
	}

	/**
	 * Returns the ArcGIS home directory path.
	 * 
	 * @return
	 * @throws Exception
	 */
	private String getArcGISHomeDir() throws IOException {
		String arcgisHome = null;
		/* Not found in env, check system property */
		if (System.getProperty(ARCGISHOME_ENV) != null) {
			arcgisHome = System.getProperty(ARCGISHOME_ENV);
		}
		if (arcgisHome == null) {
			/* To make env lookup case insensitive */
			Map<String, String> envs = System.getenv();
			for (String envName : envs.keySet()) {
				if (envName.equalsIgnoreCase(ARCGISHOME_ENV)) {
					arcgisHome = envs.get(envName);
				}
			}
		}
		if (arcgisHome != null && !arcgisHome.endsWith(File.separator)) {
			arcgisHome += File.separator;
		}
		return arcgisHome;
	}
	
	 /**
	   * Reads a permission file and return the defined permissions.
	   * 
	   * @param serverobject
	   * @throws IOException
	   */
	  private void getConfigFromFile(IServerObject serverobject) throws IOException {
	    //String serverDir = null;
	    MapServer mapserver= (MapServer)serverobject;
	    String physicalOutputDir= mapserver.getPhysicalOutputDirectory();
	    int index = physicalOutputDir.indexOf(File.separator + "directories" + File.separator + "arcgisoutput");
	    if(index > 0) {
	      serverLog.addMessage(4, 200,  "SOI ACL: The physical directory for output files: " + physicalOutputDir);
	      //serverDir = physicalOutputDir.substring(0,index);
	    } 
	    else {
	      serverLog.addMessage(1, 200,"SOI ACL: Incorrect physical directory for output files: " + physicalOutputDir);
	      throw new IOException("Incorrect physical directory for output files: " + physicalOutputDir);   
	    }
	    /*
	     * Permission are read from this external file. Advantage of an external file is that same SOI can
	     * be used for multiple services and permission for all of these services is read from the
	     * permission.json file.
	     */
	    String permssionFilePath = physicalOutputDir + File.separator +  "soi_acl.json";
	    // Read the permissions file
	    if (new File(permssionFilePath).exists()) {
	      serverLog.addMessage(3, 200, "SOI ACL:  The ACL config file is located at : " + permssionFilePath);
	      mapsvcOutputDir = physicalOutputDir;
	      ConfigMap = readConfigFile(permssionFilePath);
	    } else {
	      serverLog.addMessage(1, 200,"SOI ACL: Cannot find the ACL Config file at " + permssionFilePath);
	      throw new IOException("Cannot find the ACL Config file at " + permssionFilePath);   
	    }
	    if (ConfigMap.getString("citizen").length() > 0) {
	    	svc_citizen = ConfigMap.getString("citizen").split(",");
	    }
	    if (ConfigMap.getString("clearances").length() > 0) {
	    	svc_clearances = ConfigMap.getString("clearances").split(",");
	    }
	    if (ConfigMap.getString("accesses").length() > 0) {
	    	svc_accesses = ConfigMap.getString("accesses").split(",");
	    }
	  }
	  
	  /**
	   * Read config information from disk
	   * 
	   * @param fileName path and name of the file to read permissions from
	   * @return
	 * @throws IOException 
	 * @throws AutomationException 
	   */
	  private JSONObject readConfigFile(String fileName) throws AutomationException, IOException {
	    // read the permissions file
	    BufferedReader reader;
	    JSONObject aclconfig = null;
	    try {
	      reader = new BufferedReader(new FileReader(fileName));
	      String line = null;
	      String permissionFileDataString = "";
	      while ((line = reader.readLine()) != null) {
	        permissionFileDataString += line;
	      }
	      serverLog.addMessage(4, 200, "SOI ACL:  ACL Config File Read : " + permissionFileDataString);
	      aclconfig = new JSONObject(permissionFileDataString);
	      reader.close();
	    } catch (Exception ignore) {
	    	serverLog.addMessage(2, 200, "SOI ACL:  Error occurred in readConfigFile, Exception ignored.");
	    }
	    return aclconfig;
	  }
}