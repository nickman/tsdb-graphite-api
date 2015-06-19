/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package com.heliosapm.tsdb.grapi.server.http;


/**
 * <p>Title: Constants</p>
 * <p>Description: Defines the configuration property keys and default values</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.server.http.Constants</code></p>
 */

public class Constants {
	/** Configuration property key: The URL of the bosun service that will handle bosun queries */
	public static final String PROPERTY_BOSUN_URL = "grapi.bosun.url";
	/** Configuration default: The default Bosun URL */
	public static final String DEFAULT_BOSUN_URL = "http://127.0.0.1:8070";
	/** Configuration property key: The CORS headers sent with bosun query responses */
	public static final String PROPERTY_BOSUN_CORS_HEADERS = "grapi.bosun.cors.headers";
	/** Configuration default: The default Bosun CORS headers */
	public static final String DEFAULT_BOSUN_CORS_HEADERS = "Authorization, Content-Type, Accept, Origin, User-Agent, DNT, Cache-Control, X-Mx-ReqToken, Keep-Alive, X-Requested-With, If-Modified-Since";
	/** Configuration property key: The CORS domain sent with bosun query responses */
	public static final String PROPERTY_BOSUN_CORS_DOMAIN = "grapi.bosun.cors.domain";
	/** Configuration default: The default Bosun CORS domain */
	public static final String DEFAULT_BOSUN_CORS_DOMAIN = "*";
	/** Configuration property key: The default maximum number of items to be returned on a bosun query */
	public static final String PROPERTY_BOSUN_MAXITEMS = "grapi.bosun.maxitems";
	/** Configuration default: The default default maximum bumber of items */
	public static final int DEFAULT_BOSUN_MAXITEMS = 128;
	
	/** Configuration property key: The graphite adapters to install expressed as comma separated fully qualified class names */
	public static final String PROPERTY_GRAPI_ADAPTERS = "grapi.adapters";
	/** Configuration default: The default adapters to install */
	public static final String[] DEFAULT_GRAPI_ADAPTERS = {"com.heliosapm.tsdb.grapi.adapters.BosunValuesForTagKeyAdapter"};
	

	


	private Constants() {}

}
