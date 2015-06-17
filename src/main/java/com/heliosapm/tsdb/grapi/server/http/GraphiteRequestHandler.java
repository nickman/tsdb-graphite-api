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

import java.util.Set;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.tsdb.grapi.adapters.BosunValuesForTagKeyAdapter;

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

	final BosunValuesForTagKeyAdapter adapter = new BosunValuesForTagKeyAdapter();
	
	/**
	 * Creates a new GraphiteRequestHandler
	 */
	public GraphiteRequestHandler() {
		log.info("Created GraphiteRequestHandler");
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
			log.info(dumpHttpRequest(request));
			final HttpResponse resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT); 
			//ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), resp, e.getRemoteAddress()));
			adapter.processQuery(request, channel, ctx);
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
