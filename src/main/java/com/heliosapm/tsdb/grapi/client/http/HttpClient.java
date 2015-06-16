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




import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;

import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.tsdb.grapi.netty.DynamicByteBufferBackedChannelBufferFactory;
import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.jmx.JMXManagedThreadPool;
import com.heliosapm.utils.url.URLHelper;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;

/**
 * <p>Title: HttpClient</p>
 * <p>Description: A basic async HTTP client</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.client.http.HttpClient</code></p>
 */

public class HttpClient {
	/** The singleton instance */
	private static volatile HttpClient instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The http client */
	protected final AsyncHttpClient httpClient;
	/** The client thread pool */
	protected final JMXManagedThreadPool threadPool;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** The buffer factory for handling async responses */
	protected DynamicByteBufferBackedChannelBufferFactory bufferFactory = new DynamicByteBufferBackedChannelBufferFactory(1024, .5f);
	
	public static final ObjectName THREADPOOL_OBJECTNAME = JMXHelper.objectName("");
	
	/**
	 * Acquires the HttpClient singleton instance
	 * @return the HttpClient singleton instance
	 */
	public static HttpClient getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new HttpClient(); 
				}
			}
		}
		return instance;
	}
	


	/**
	 * Creates a new HttpClient
	 */
	private HttpClient() {
		log.info("Building HTTP Client...");
		threadPool = new JMXManagedThreadPool(THREADPOOL_OBJECTNAME, "GraphiteAPIThreadPool", 10, 20, 240, 60000, 100, 99);
		AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder()		
		.setAllowPoolingConnection(true)
		.setIOThreadMultiplier(1)
		.setAsyncHttpClientProviderConfig(new NettyAsyncHttpProviderConfig()
			.addProperty(NettyAsyncHttpProviderConfig.BOSS_EXECUTOR_SERVICE, threadPool)
			.addProperty(NettyAsyncHttpProviderConfig.USE_DIRECT_BYTEBUFFER, new DirectChannelBufferFactory(4096))
			.addProperty(NettyAsyncHttpProviderConfig.REUSE_ADDRESS, true)
			.addProperty(NettyAsyncHttpProviderConfig.EXECUTE_ASYNC_CONNECT, true)
		)
		.setMaximumConnectionsPerHost(5)
		.setMaximumConnectionsTotal(15)
		.setConnectionTimeoutInMs(1000)
		.setRequestTimeoutInMs(5000)
		.setExecutorService(threadPool);
		httpClient = new AsyncHttpClient(builder.build());				
		log.info("Initialized HTTP Client.");
	}
	
	/**
	 * Stops the AsyncHttpClient
	 */
	public void shutdown() {
		log.info("Stopping HttpClient .....");
		httpClient.close();
		threadPool.shutdownNow();
		log.info("HttpClient stopped.");
	}
	
	/**
	 * Returns a new request builder
	 * @return
	 */
	public RequestBuilder request() {
		return new RequestBuilder();
	}
	
	public DefaultAsyncResponse execRequest(final RequestBuilder builder, final DefaultAsyncResponseHandler handler) {
		return execRequest(builder.build(), handler);
	}
	
	public DefaultAsyncResponse execRequest(final Request request, final DefaultAsyncResponseHandler handler) {
		final DefaultAsyncResponse dar = new DefaultAsyncResponse(bufferFactory.getBuffer()).handler(handler);
		final URL requestURL = URLHelper.toURL(request.getUrl());
		try {
			httpClient.executeRequest(request, new AsyncHandler<Void>(){

				@Override
				public void onThrowable(final Throwable t) {
					dar.onError(requestURL, t);					
				}

				@Override
				public STATE onBodyPartReceived(final HttpResponseBodyPart bodyPart) throws Exception {
					if(dar.isComplete()) {
						return STATE.ABORT;
					}
					dar.onContent(bodyPart.getBodyByteBuffer());
					return STATE.CONTINUE;
				}

				@Override
				public STATE onStatusReceived(final HttpResponseStatus responseStatus) throws Exception {
					final int code = responseStatus.getStatusCode();
					final String text = responseStatus.getStatusText();
					dar.onStatus(code, text);
					if(code>=200 && code <= 299) {
						return STATE.CONTINUE;
					}
					return STATE.ABORT;
				}

				@Override
				public STATE onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
					FluentCaseInsensitiveStringsMap map = headers.getHeaders();
					final Map<String, Collection<String>> hdrs = new HashMap<String, Collection<String>>(map.size());
					for(Map.Entry<String, List<String>> entry: map.entrySet()) {
						hdrs.put(entry.getKey(), entry.getValue());
					}
					dar.headers = hdrs;
					return STATE.CONTINUE;
				}

				@Override
				public Void onCompleted() throws Exception {
					dar.onComplete();
					return null;
				}
				
			});
		} catch (IOException e) {
			dar.onError(requestURL, e);
		}
		
		return dar;
		
	}

}
