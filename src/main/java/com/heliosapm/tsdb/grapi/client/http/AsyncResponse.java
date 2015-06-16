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
package com.heliosapm.tsdb.grapi.client.http;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

/**
 * <p>Title: AsyncResponse</p>
 * <p>Description: Defines an async response</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.client.http.AsyncResponse</code></p>
 */

public interface AsyncResponse {
	/**
	 * Callback when an HTTP Error occurs
	 * @param url The URL of the failed request
	 * @param t The exception thrown
	 */
	public void onError(URL url, Throwable t);
	
	/**
	 * Callback when the status of the request is received
	 * @param code The HTTP code
	 * @param text The HTTP status text
	 */
	public void onStatus(int code, String text);
	
	/**
	 * Callback when the response headers are received
	 * @param headers The response headers
	 */
	public void onHeaders(Map<String, Collection<String>> headers);
	
	/**
	 * Callback when the whole response content is available
	 * @param buff A buffer from which the content be read
	 */
	public void onContent(ByteBuffer buff);
	
	/**
	 * Callback when the request's response handling is complete
	 */
	public void onComplete();
}
