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
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.heliosapm.utils.lang.StringHelper;

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
	/** The http client */
	protected final HttpClient client = HttpClient.getInstance();
	/** The CORS headers */
	protected final String corsHeaders;
	/** The CORS domain */
	protected final String corsDomain;
	/** The default maximum numer of items to return to the caller */
	protected final int defaultMaxItems;

	/** If the match challenge starts with this, we take it */
	protected static final String startsWithMatch = META_URI.toLowerCase();	
	/** The UTF8 character set */
	public static final Charset UTF8 = Charset.forName("UTF8");	
	/** We split the URI on this to get the actual query */
	protected static final String actualQueryDelim = "?query=";	
	/** The length of the delim */
	protected static final int delimLength = actualQueryDelim.length();
	/** The default CORS headers value */
	public static final String DEFAULT_CORS_HEADERS = "Authorization, Content-Type, Accept, Origin, User-Agent, DNT, Cache-Control, X-Mx-ReqToken, Keep-Alive, X-Requested-With, If-Modified-Since";
	/** The default CORS domain value */
	public static final String DEFAULT_CORS_DOMAIN = "*";	
	/** The default max items to return to the caller */
	public static final  int DEFAULT_MAX_ITEMS = 128;

	
	/** The pattern of the max items specifier */
	public static final Pattern MAX_PATTERN = Pattern.compile("/max=(\\d+)", Pattern.CASE_INSENSITIVE);
	/** The pattern of the filter items specifier */
	public static final Pattern FILTER_PATTERN = Pattern.compile("/filter=\\[(.*?)\\]", Pattern.CASE_INSENSITIVE);

	
	/**
	 * Creates a new BosunValuesForTagKeyAdapter
	 * @param config The optional configuration properties 
	 */
	public BosunValuesForTagKeyAdapter(final Properties config) {
		bosunUrl = ConfigurationHelper.getURLSystemThenEnvProperty("grapi.bosun.url", "http://localhost:8070", config);
		corsHeaders = ConfigurationHelper.getSystemThenEnvProperty("grapi.bosun.cors.headers", DEFAULT_CORS_HEADERS, config);
		corsDomain = ConfigurationHelper.getSystemThenEnvProperty("grapi.bosun.cors.domain", DEFAULT_CORS_DOMAIN, config);
		defaultMaxItems = ConfigurationHelper.getIntSystemThenEnvProperty("grapi.bosun.maxitems", DEFAULT_MAX_ITEMS, config);
		log.info(StringHelper.banner("%s Configuration\n\tBosun URL:%s\n\tCORS Headers:%s\n\tCORS Domain:%s\n\tDefault Max Items:%s", getClass().getSimpleName(), bosunUrl, corsHeaders, corsDomain, defaultMaxItems));
	}
	
	/**
	 * Creates a new BosunValuesForTagKeyAdapter 
	 */
	public BosunValuesForTagKeyAdapter() {
		this(null);
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
	
	/**
	 * Creates the bosun query URL for the passed tagv and query 
	 * @param qkey The key of the query, e.g. <b><code>tagv</code></b>
	 * @param remainder The body of the query
	 * @return the URL to query
	 */
	protected String getUrlForTagvTagk(final String qkey, final String remainder) {
		final int qindex = remainder.indexOf('?');
		final int sindex = remainder.indexOf('/');
		if(qindex!=-1) {
			// we're looking at a tagv=tagk/metric?k1=v1&k2=v2
			final String tagk =  remainder.substring(0, sindex);
			final String metric = remainder.substring(sindex+1, qindex);
			final String kvpairs = remainder.substring(qindex+1);
			return bosunUrl.toString() + "/api/" + qkey + "/" + tagk + "/" + metric + "?" + kvpairs;
		}
		if(sindex==-1) {
			// we're looking at a tagv=tagk
			return bosunUrl.toString() + "/api/" + qkey + "/" + remainder;
		}
		// we're looking at a tagv=tagk/metric
		final String tagk =  remainder.substring(0, sindex);
		final String metric = remainder.substring(sindex+1);
		return bosunUrl.toString() + "/api/" + qkey + "/" + tagk + "/" + metric;		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdb.grapi.GraphiteAdapter#processQuery(org.jboss.netty.handler.codec.http.HttpRequest, org.jboss.netty.channel.Channel, org.jboss.netty.channel.ChannelHandlerContext)
	 */
	@Override
	public void processQuery(final HttpRequest request, final Channel channel, final ChannelHandlerContext ctx) {
		try {
			int maxItems = defaultMaxItems;
			Pattern itemFilter = null;
			final String uri = URLDecoder.decode(request.getUri(), UTF8.name());
			final int index = uri.indexOf("?query=");
			String query = uri.substring(index + delimLength);
			final Matcher m = MAX_PATTERN.matcher(query);			
			if(m.find()) {
				maxItems = Integer.parseInt(m.group(1));
				query = query.replace("/max=" + maxItems, "");
			}
			final Matcher mf = FILTER_PATTERN.matcher(query);
			if(mf.find()) {
				itemFilter = Pattern.compile(mf.group(1));
				query = query.replace(mf.group(0), "");
			}
			final int eindex = query.indexOf('=');
			final String qkey = query.substring(0, eindex);
			final String remainder = query.substring(eindex+1);			
			if("tagv".equalsIgnoreCase(qkey)) {
				final String url = getUrlForTagvTagk(qkey.toLowerCase(), remainder);
				log.debug("Issuing query to bosun: [{}] with max items: [{}]", url, maxItems);
				client.request(newARH(request, channel, ctx, maxItems, itemFilter)).setUrl(url).execute();
			} else {
				throw new RuntimeException("First arg [" + qkey + "] not recognized");
			}
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
    resp.headers().add("Access-Control-Allow-Origin", corsDomain);
    resp.headers().add("Access-Control-Allow-Headers", corsHeaders);
    resp.headers().add("Content-Type", "application/json");		
    return resp;
	}
	
	/**
	 * Transforms the content returned by the client call to Graphite compliant JSON
	 * @param response The client's async response
	 * @param maxItems The maximum number of items to return to the caller
	 * @return the byte content to return to the caller
	 * @param itemFilter An optional pattern to filter in items returned by the bosun query
	 */
	protected byte[] transform(final DefaultAsyncResponse response, final int maxItems, final Pattern itemFilter) {
		try {
			final String jsonText = response.getBuffer().toString(UTF8);
			final JSONArray ja = new JSONArray(jsonText);
			final JSONArray ra = new JSONArray();
			for(int i = 0; i < ja.length(); i++) {
				final JSONObject rez = new JSONObject();
				final String item = ja.getString(i);
				if(itemFilter!=null && !itemFilter.matcher(item).matches()) {
					continue;
				}
				rez.put("text", item);
				ra.put(rez);
				if(i >= maxItems) break;
			}
			return ra.toString().getBytes(UTF8);
		} catch (Exception ex) {			
			throw new RuntimeException("Failed to transform query result", ex);
		}
	}
	
	/**
	 * Creates a new async response handler to handle the response to the query issued against bosun
	 * @param request The original http request dispatched to bosun 
	 * @param channel The channel to respond to the original caller on
	 * @param ctx The channel's handler context
	 * @param maxItems The maximum number of items to return to the caller
	 * @param itemFilter An optional pattern to filter in items returned by the bosun query
	 * @return the new response handler
	 */
	protected AsyncResponseHandler newARH(final HttpRequest request, final Channel channel, final ChannelHandlerContext ctx, final int maxItems, final Pattern itemFilter) {
		return new AsyncResponseHandler() {
			@Override
			public void onResponse(final DefaultAsyncResponse response) {
				final HttpResponse fresp = newCORSResponse(request.getProtocolVersion());
				final ChannelFuture cf = Channels.future(channel);
				cf.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(final ChannelFuture f) throws Exception {
						if(f.isSuccess()) {
							log.debug("Completed Response Write [{}]", fresp);
						} else {
							log.error("Response Write Failed", f.getCause());
						}
					}
				});				
				try {
					final byte[] content = transform(response, maxItems, itemFilter);
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
