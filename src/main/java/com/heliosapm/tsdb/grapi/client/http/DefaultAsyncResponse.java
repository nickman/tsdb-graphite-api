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

import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;

/**
 * <p>Title: DefaultAsyncResponse</p>
 * <p>Description: The default async response handler</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.client.http.DefaultAsyncResponse</code></p>
 */

public class DefaultAsyncResponse implements AsyncResponse, Runnable {
	/** The response buffer, set by the http clientwhen the request is initiated */
	protected final ChannelBuffer buffer;

	/** The response headers */
	protected Map<String, Collection<String>> headers = null;
	/** The respponse HTTP code */
	protected int responseCode = 0;
	/** The response text */
	protected String responseText = null;
	/** The request exception */
	protected Throwable requestError = null;
	/** The original request URL */
	protected URL requestURL = null;
	/** The response handler */
	protected final AtomicReference<AsyncResponseHandler> handler = new AtomicReference<AsyncResponseHandler>(null);
	/** The compoletion flag */
	protected final AtomicBoolean complete = new AtomicBoolean(false);
	/** The response handler invocation executor */
	protected final ExecutorService executor; 
	
	/**
	 * Creates a new DefaultAsyncResponse
	 * @param cb The buffer that will hold the response
	 * @param executor The response handler invocation executor
	 */
	DefaultAsyncResponse(final ChannelBuffer cb, final ExecutorService executor) {
		this.buffer = cb;
		this.executor = executor;
	}
	
	
	/**
	 * Sets the response handler
	 * @param handler The handler that will execute the response
	 * @return this response
	 */
	public DefaultAsyncResponse handler(final AsyncResponseHandler handler) {
		if(handler!=null) {
			if(this.handler.compareAndSet(null, handler)) {
				if(complete.get()) {
					handler.onResponse(this);
				}
			}
		}		
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.grapi.client.http.AsyncResponse#onError(java.net.URL, java.lang.Throwable)
	 */
	@Override
	public void onError(final URL url, final Throwable t) {
		if(complete.compareAndSet(false, true)) {
			requestURL = url;
			requestError = t;
		}
	}
	
	/**
	 * Indicates if the response is complete
	 * @return true if the response is complete, false otherwise
	 */
	public boolean isComplete() {
		return complete.get();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.grapi.client.http.AsyncResponse#onStatus(int, java.lang.String)
	 */
	@Override
	public void onStatus(final int code, final String text) {
		responseCode = code;
		responseText = text;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.grapi.client.http.AsyncResponse#onHeaders(java.util.Map)
	 */
	@Override
	public void onHeaders(final Map<String, Collection<String>> headers) {
		this.headers = headers;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.grapi.client.http.AsyncResponse#onContent(java.nio.ByteBuffer)
	 */
	@Override
	public void onContent(final ByteBuffer buffer) {
		this.buffer.writeBytes(buffer);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		handler.get().onResponse(this);		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.grapi.client.http.AsyncResponse#onComplete()
	 */
	@Override
	public void onComplete() {
		if(complete.compareAndSet(false, true)) {
			if(handler.get()!=null) {
				executor.execute(this);				
			}			
		}
	}
	
	/**
	 * Returns the response buffer
	 * @return the buffer
	 */
	public ChannelBuffer getBuffer() {
		return buffer;
	}
	
	/**
	 * Returns an outputstream that the response can be read from
	 * @return an outputstream that the response can be read from
	 */
	public OutputStream getResponse() {
		return new ChannelBufferOutputStream(buffer);
	}
	

}
