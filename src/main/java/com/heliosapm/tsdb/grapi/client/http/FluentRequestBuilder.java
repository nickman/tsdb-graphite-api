/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2015, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.tsdb.grapi.client.http;

import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;

/**
 * <p>Title: FluentRequestBuilder</p>
 * <p>Description: An extended and executable request builder</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.client.http.FluentRequestBuilder</code></p>
 */

public class FluentRequestBuilder extends RequestBuilder {
	/** The client the request will be invoked against */
	protected final HttpClient client;
	/** The response handler */
	protected AsyncResponseHandler responseHandler = null;
	

	/**
	 * Creates a new FluentRequestBuilder
	 * @param client The underlying client to execute against
	 */
	FluentRequestBuilder(final HttpClient client) {
		this.client = client;
	}
	
	/**
	 * Creates a new FluentRequestBuilder
	 * @param client The underlying client to execute against
	 * @param template A pre-prepared template builder
	 */
	FluentRequestBuilder(final HttpClient client, final Request template) {
		this.client = client;
	}
	
	
	/**
	 * Creates a new FluentRequestBuilder
	 * @param client The underlying client to execute against
	 * @param responseHandler The async response handler
	 */
	FluentRequestBuilder(final HttpClient client, final AsyncResponseHandler responseHandler) {
		this.client = client;
		this.responseHandler = responseHandler;
	}
	
	
	/**
	 * Executes the built request
	 * @return the async response
	 */
	public DefaultAsyncResponse execute() {
		return client.execRequest(this.build(), responseHandler);
	}
	
	/**
	 * Executes the built request
	 * @param responseHandler The response handler
	 * @return the async response
	 */
	public DefaultAsyncResponse execute(final AsyncResponseHandler responseHandler) {
		this.responseHandler = responseHandler;
		return client.execRequest(this.build(), responseHandler);
	}
	
	/**
	 * Sets the response handler
	 * @param responseHandler the responseHandler to set
	 * @return this builder
	 */
	public FluentRequestBuilder responseHandler(AsyncResponseHandler responseHandler) {
		this.responseHandler = responseHandler;
		return this;
	}


//	/**
//	 * Creates a new FluentRequestBuilder
//	 * @param method
//	 */
//	public FluentRequestBuilder(final String method) {
//		super(method);
//		// TODO Auto-generated constructor stub
//	}
//
//	/**
//	 * Creates a new FluentRequestBuilder
//	 * @param prototype
//	 */
//	public FluentRequestBuilder(Request prototype) {
//		super(prototype);
//		// TODO Auto-generated constructor stub
//	}
//
//	/**
//	 * Creates a new FluentRequestBuilder
//	 * @param method
//	 * @param useRawUrl
//	 */
//	public FluentRequestBuilder(String method, boolean useRawUrl) {
//		super(method, useRawUrl);
//		// TODO Auto-generated constructor stub
//	}

}
