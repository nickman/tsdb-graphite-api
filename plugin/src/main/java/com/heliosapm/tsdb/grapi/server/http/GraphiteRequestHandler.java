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
package com.heliosapm.tsdb.grapi.server.http;

import static com.heliosapm.tsdb.grapi.server.http.Constants.DEFAULT_GRAPI_ADAPTERS;
import static com.heliosapm.tsdb.grapi.server.http.Constants.PROPERTY_GRAPI_ADAPTERS;

import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.tsdb.grapi.GraphiteAdapter;
import com.heliosapm.utils.config.ConfigurationHelper;
import com.heliosapm.utils.lang.StringHelper;

/**
 * <p>Title: GraphiteRequestHandler</p>
 * <p>Description: Receives an HTTP request and routes it according to the URI and parameters</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.server.http.GraphiteRequestHandler</code></p>
 */
@ChannelHandler.Sharable
public class GraphiteRequestHandler extends SimpleChannelUpstreamHandler {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** A set of configured adapters */
	protected final Set<GraphiteAdapter> adapters = new LinkedHashSet<GraphiteAdapter>();
	

	/**
	 * Creates a new GraphiteRequestHandler
	 * @param config The service configuration
	 */
	public GraphiteRequestHandler(final Properties config) {
		log.info("Created GraphiteRequestHandler");
		final String[] adapters = ConfigurationHelper.getArraySystemThenEnvProperty(PROPERTY_GRAPI_ADAPTERS, DEFAULT_GRAPI_ADAPTERS, config);
		final StringBuilder b = new StringBuilder();
		for(String adapter: adapters) {
			try {
				@SuppressWarnings("unchecked")
				Class<? extends GraphiteAdapter> aclazz = (Class<? extends GraphiteAdapter>) Class.forName(adapter, true, getClass().getClassLoader());
				Constructor<? extends GraphiteAdapter> ctor = aclazz.getDeclaredConstructor(Properties.class);
				GraphiteAdapter ga = ctor.newInstance(config);
				this.adapters.add(ga);
				log.info("Created and configured GraphiteAdapter [{}]", adapter);
				b.append("\n\t\t").append(aclazz.getSimpleName());
			} catch (Exception ex) {
				log.error("Failed to create configured adapter [{}]", adapter, ex);
			}
		}
		log.info(StringHelper.banner("Graphite Request HandlerConfiguration\n\tAdapters:%s", b.toString()));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
	 */
	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, ExceptionEvent ex) throws Exception {
		log.error("Exception caught in GraphiteRequestHandler", ex.getCause());
	}
	
	/**
	 * Returns the first registered GraphiteAdapter that matches the passed URI
	 * @param uri The requested URI
	 * @return The first matching GraphiteAdapter or null if one was not found
	 */
	protected GraphiteAdapter findMatch(final String uri) {
		if(uri==null || uri.trim().isEmpty()) return null;
		for(GraphiteAdapter ga: adapters) {
			if(ga.match(uri)) return ga;
		}
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		final Object o = e.getMessage();
		if(o!=null && (o instanceof HttpRequest)) {
			final HttpRequest request = (HttpRequest)o;
			final Channel channel = e.getChannel();
			if(log.isDebugEnabled()) log.debug(dumpHttpRequest(request));
			final GraphiteAdapter ga = findMatch(request.getUri());
			if(ga==null) {
				ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.NOT_FOUND), e.getRemoteAddress()));
			} else {
				ga.processQuery(request, channel, ctx);
			}
		} else {
			super.messageReceived(ctx, e);
		}
	}
	
	private String dumpHttpRequest(final HttpRequest r) {
		final StringBuilder b = new StringBuilder("\n\t================================================\n\tHTTP Request\n\t================================================");
		b.append("\n\t\t").append("Method:").append(r.getMethod());
		b.append("\n\t\t").append("HTTP Version:").append(r.getProtocolVersion());
		b.append("\n\t\t").append("URI:").append(r.getUri());
		b.append("\n\t\t").append("Headers:");
		@SuppressWarnings("deprecation")
		final Set<String> headerNames = r.getHeaderNames();
		for(String header: headerNames) {
			b.append("\n\t\t\t").append(header).append(":").append(HttpHeaders.getHeader(r, header));
		}
		b.append("\n\t================================================\n");
		return b.toString();
		
	}

}
