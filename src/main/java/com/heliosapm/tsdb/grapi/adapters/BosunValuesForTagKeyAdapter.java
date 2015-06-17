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
package com.heliosapm.tsdb.grapi.adapters;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.tsdb.grapi.GraphiteAdapter;
import com.heliosapm.tsdb.grapi.client.http.AsyncResponseHandler;
import com.heliosapm.tsdb.grapi.client.http.DefaultAsyncResponse;
import com.heliosapm.tsdb.grapi.client.http.HttpClient;
import com.heliosapm.utils.config.ConfigurationHelper;

/**
 * <p>Title: BosunValuesForTagKeyAdapter</p>
 * <p>Description: Accepts tag keys and returns corresponding values from the Bosun API</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.adapters.BosunValuesForTagKeyAdapter</code></p>
 */

public class BosunValuesForTagKeyAdapter implements GraphiteAdapter {
	/** The bosun root URL */
	protected final URL bosunUrl;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** If the match challenge starts with this, we take it */
	protected static final String startsWithMatch = META_URI.toLowerCase();
	
	/** The UTF8 character set */
	public static Charset UTF8 = Charset.forName("UTF8");
	
	/** We split the URI on this to get the actual query */
	protected static final String actualQueryDelim = "?query=";
	
	/** The length of the delim */
	protected static final int delimLength = actualQueryDelim.length();
	
	/** The http client */
	protected final HttpClient client = HttpClient.getInstance();
	
	public static final String CORS_HEADERS = "Authorization, Content-Type, Accept, Origin, User-Agent, DNT, Cache-Control, X-Mx-ReqToken, Keep-Alive, X-Requested-With, If-Modified-Since";
	public static final String CORS_DOMAIN = "*";
	
	/**
	 * Creates a new BosunValuesForTagKeyAdapter
	 */
	public BosunValuesForTagKeyAdapter() {
		bosunUrl = ConfigurationHelper.getURLSystemThenEnvProperty("bosun.url", "http://localhost:8070");
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.grapi.GraphiteAdapter#match(java.lang.String)
	 */
	@Override
	public boolean match(final String queryURI) {
		if(queryURI==null || queryURI.trim().isEmpty()) return false;
		return queryURI.toLowerCase().startsWith(startsWithMatch);
	}
	
	/*
	 * /api/metric/{tagk}/{tagv}
	 * /api/tagv/{tagk}					  also:  /api/tagv/{tagk}/{metric}?{tagkX=tagvX}&.....  
	 * /api/tagk/{metric}
   * /api/tagv/{tagk}/{metric}
   * /api/metadata/get
   * /api/metadata/metrics
   * 
   * Initial Segment:
   * 	* tagv
   * 		=tagk
   * 		=tagk/metric									e.g.  /tagv=disk/os.disk.fs.percent_free
   * 		=tagk/metric?k1=v1&k2=v2      e.g.  /tagv=disk/os.disk.fs.percent_free?host=<hostname>
   * 
   * 
	 */

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.grapi.GraphiteAdapter#processQuery(org.jboss.netty.handler.codec.http.HttpRequest, org.jboss.netty.channel.Channel, org.jboss.netty.channel.ChannelHandlerContext)
	 */
	@Override
	public void processQuery(final HttpRequest request, final Channel channel, final ChannelHandlerContext ctx) {
		
		try {
			final String uri = URLDecoder.decode(request.getUri(), UTF8.name());
			final int index = uri.indexOf("?query=");
			final String query = uri.substring(index + delimLength);
			final String[] pair = query.split("=");
			//"http://localhost:8070/api/tagv/host"
			client.request(newARH(request, channel, ctx)).setUrl(bosunUrl.toString() + "/api/" + pair[0] + "/" + pair[1]).execute();
		} catch (Exception ex) {
			log.error("processQuery failed", ex);
		}
	}
	
	/**
	 * Creates a new HTTP response for the passed version and adds the CORS and content-type headers
	 * @param version The http version of the response to create
	 * @return the new Http response
	 */
	protected HttpResponse newCORSResponse(final HttpVersion version) {
		final HttpResponse resp = new DefaultHttpResponse(version, HttpResponseStatus.OK);
    resp.headers().add("Access-Control-Allow-Origin", CORS_DOMAIN);
    resp.headers().add("Access-Control-Allow-Headers", CORS_HEADERS);
    resp.headers().add("Content-Type", "application/json");		
    return resp;
	}
	
	protected byte[] transform(final DefaultAsyncResponse response) {
		try {
			final String jsonText = response.getBuffer().toString(UTF8);
			final JSONArray ja = new JSONArray(jsonText);
			final JSONArray ra = new JSONArray();
			for(int i = 0; i < ja.length(); i++) {
				final JSONObject rez = new JSONObject();
				rez.put("text", ja.get(i));
				ra.put(rez);
			}
			return ra.toString(1).getBytes(UTF8);
		} catch (Exception ex) {			
			throw new RuntimeException("Failed to transform query result", ex);
		}
	}
		
		
//		[
//		  {
//		    "leaf": 0,
//		    "context": {},
//		    "text": "cpu-0",
//		    "expandable": 1,
//		    "id": "tpmint.cpu-0",
//		    "allowChildren": 1
//		  },
		
	
	
	/**
	 * Creates a new async response handler to handle the response to the query issued against bosun
	 * @param request The original http request dispatched to bosun 
	 * @param channel The channel to respond to the original caller on
	 * @param ctx The channel's handler context
	 * @return the new response handler
	 */
	protected AsyncResponseHandler newARH(final HttpRequest request, final Channel channel, final ChannelHandlerContext ctx) {
		return new AsyncResponseHandler() {
			@Override
			public void onResponse(final DefaultAsyncResponse response) {
				final HttpResponse fresp = newCORSResponse(request.getProtocolVersion());
				final ChannelFuture cf = Channels.future(channel);
				cf.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(final ChannelFuture f) throws Exception {
						if(f.isSuccess()) {
							log.info("Completed Response Write [{}]", fresp);
						} else {
							log.error("Response Write Failed", f.getCause());
						}
					}
				});				
				try {
					final byte[] content = transform(response);
					fresp.setContent(ChannelBuffers.wrappedBuffer(content));
					HttpHeaders.setContentLength(fresp, content.length);
					ctx.sendDownstream(new DownstreamMessageEvent(channel, cf, fresp, channel.getRemoteAddress()));
				} catch (Exception x) {
					final HttpResponse resp = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
					ctx.sendDownstream(new DownstreamMessageEvent(channel, cf, resp, channel.getRemoteAddress()));
					
				}
			}
		};
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
*/	

}
