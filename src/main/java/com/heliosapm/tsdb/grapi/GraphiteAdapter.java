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
package com.heliosapm.tsdb.grapi;

import org.jboss.netty.handler.codec.http.HttpRequest;

/**
 * <p>Title: GraphiteAdapter</p>
 * <p>Description: Defines an adapter that accepts requests for data from the core plugin service 
 * and returns the results in the standard GraphiteAPI RPC format.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.GraphiteAdapter</code></p>
 */

public interface GraphiteAdapter {
	/**
	 * Processes the passed query, returning the results as an array of objects
	 * @param request The Http request
	 * @return the results
	 */
	public Object[] processQuery(HttpRequest request);
}


/*
Example from a graphite loaded browser.
=======================================

Request:   http://localhost:8080/metrics/find/?query=tpmint.cpu-*

Response:

[
  {
    "leaf": 0,
    "context": {},
    "text": "cpu-0",
    "expandable": 1,
    "id": "tpmint.cpu-0",
    "allowChildren": 1
  },
  {
    "leaf": 0,
    "context": {},
    "text": "cpu-1",
    "expandable": 1,
    "id": "tpmint.cpu-1",
    "allowChildren": 1
  },
  {
    "leaf": 0,
    "context": {},
    "text": "cpu-2",
    "expandable": 1,
    "id": "tpmint.cpu-2",
    "allowChildren": 1
  },
  {
    "leaf": 0,
    "context": {},
    "text": "cpu-3",
    "expandable": 1,
    "id": "tpmint.cpu-3",
    "allowChildren": 1
  }
]

*/


/*
Netty Server sees the request like this:

//290717:26:43,152 EDT [New I/O worker #1] [c.h.t.g.s.h.GraphiteRequestHandler] [INFO ] 
//	================================================
//	HTTP Request
//	================================================
//		Method:GET
//		HTTP Version:HTTP/1.1
//		URI:/metrics/find?query=XXX.YY.Z.*
//		Headers:
//			Host:localhost:2907
//			User-Agent:Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.30 Safari/537.36
//			Accept:application/json, text/plain, */*
//			Accept-Encoding:gzip, deflate, sdch
//			Accept-Language:en-US,en;q=0.8,it;q=0.6
//			Cookie:grafana_sess=940ce3202cc25423; grafana_user=admin; grafana_remember=09e7c1203f2a4e7724bcb70b1e177d5724d2d9dfe8e7a13e
//			Referer:http://localhost:3000/dashboard/new?editview=templating
//			X-Forwarded-For:::1
//	================================================
//	
	
*/