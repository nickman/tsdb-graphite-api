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

import java.net.InetSocketAddress;
import java.util.Properties;

import javax.management.ObjectName;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.ChannelGroupFutureListener;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.tsdb.grapi.client.http.HttpClient;
import com.heliosapm.tsdb.grapi.netty.DynamicByteBufferBackedChannelBufferFactory;
import com.heliosapm.utils.concurrency.ExtendedThreadManager;
import com.heliosapm.utils.config.ConfigurationHelper;
import com.heliosapm.utils.io.StdInCommandHandler;
import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.jmx.JMXManagedThreadPool;
import static com.heliosapm.tsdb.grapi.server.http.Constants.*;

/**
 * <p>Title: HttpServer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.server.http.HttpServer</code></p>
 */

public class HttpServer implements ChannelPipelineFactory {
	/** The singleton instance */
	private static volatile HttpServer instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	
	
	/** The server boss thread pool */
	protected final JMXManagedThreadPool bossPool;
	/** The server worker thread pool */
	protected final JMXManagedThreadPool workerPool;
	
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** The service configuration */
	protected final Properties config = new Properties();
	
	/** The listening port */
	protected final int port;
	/** The binding interface */
	protected final String iface;
	/** The server socket */
	protected final InetSocketAddress serverSocket;
	/** The server bootstrap */
	protected final ServerBootstrap bootstrap;
	/** The server socket channel factory */
	protected final NioServerSocketChannelFactory channelFactory;
	/** The server channel */
	protected final Channel serverChannel;
	/** The graphite request handler */
	protected final GraphiteRequestHandler graphiteRequestHandler;
	
	
	private final Thread keepAliveThread;
	
	
	/** The open channel group */
	protected final ChannelGroup channelGroup = new DefaultChannelGroup("GraphiteAPIServer");
	
	
	
	/** The buffer factory for handling async responses */
	protected DynamicByteBufferBackedChannelBufferFactory bufferFactory = new DynamicByteBufferBackedChannelBufferFactory(1024, .5f);
	
	/** The ObjectName of the server boss thread pool */
	public static final ObjectName BOSS_THREADPOOL_OBJECTNAME = JMXHelper.objectName("com.heliosapm.tsdb.grapi:service=HttpServerThreadPool,type=Boss");
	/** The ObjectName of the server worker thread pool */
	public static final ObjectName WORKER_THREADPOOL_OBJECTNAME = JMXHelper.objectName("com.heliosapm.tsdb.grapi:service=HttpServerThreadPool,type=Worker");
	
	/**
	 * Acquires the HttpClient singleton instance.
	 * This singleton accessor should be called first
	 * @param config The service configuration
	 * @return the HttpClient singleton instance
	 */
	public static HttpServer getInstance(final Properties config) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new HttpServer(config); 
				}
			}
		}
		return instance;
	}
	
	/**
	 * Acquires the HttpClient singleton instance.
	 * This singleton accessor should not be called unti {@link #getInstance(Properties)} has been called.
	 * @return the HttpClient singleton instance
	 */
	public static HttpServer getInstance() {
		if(instance==null) {
			synchronized(instance) {
				if(instance==null) {
					throw new IllegalStateException("The HttpServer singleton has not been initialized. Please call getInstance(Properties) first");
				}
			}
		}
		return instance;
	}
	
	
	public static void main(final String[] args) {
		System.setProperty("bosun.url", "http://pdk-pt-cltsdb-01:8070");
		final HttpServer server = getInstance(System.getProperties());		
		StdInCommandHandler.getInstance().registerCommand("down", new Runnable(){
			public void run() {
				server.shutdown();
			}
		});
	}
	
	void shutdown() {		
		channelGroup.close().addListener(new ChannelGroupFutureListener() {
			@Override
			public void operationComplete(final ChannelGroupFuture future) throws Exception {
				channelFactory.shutdown();
				log.info("Channel Factory stopped");
				HttpClient.getInstance().shutdown();				
				keepAliveThread.interrupt();
			}
		});
		
	}
	
	
	/**
	 * Creates a new HttpServer
	 */
	private HttpServer(final Properties config) {
		log.info("Starting HTTP Server...");
		if(config!=null) {
			this.config.putAll(config);
		}
		graphiteRequestHandler = new GraphiteRequestHandler(this.config);
		ExtendedThreadManager.install();
		port = ConfigurationHelper.getIntSystemThenEnvProperty(PROPERTY_HTTP_LISTEN_PORT, DEFAULT_HTTP_LISTEN_PORT);
		iface = ConfigurationHelper.getSystemThenEnvProperty(PROPERTY_HTTP_LISTEN_IFACE, DEFAULT_HTTP_LISTEN_IFACE);
		serverSocket = new InetSocketAddress(iface, port);
		int cores = Runtime.getRuntime().availableProcessors();
		bossPool = new JMXManagedThreadPool(BOSS_THREADPOOL_OBJECTNAME, "ServerBossThreadPool", cores*2, cores*4, 240, 60000, 100, 99);
		bossPool.prestartAllCoreThreads();
		workerPool = new JMXManagedThreadPool(WORKER_THREADPOOL_OBJECTNAME, "ServerWorkerThreadPool", cores*2, cores*4, 240, 60000, 100, 99);
		workerPool.prestartAllCoreThreads();
		channelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
		bootstrap = new ServerBootstrap(channelFactory);
		bootstrap.setPipelineFactory(this);
		bootstrap.setOption("bufferFactory", bufferFactory);
		
		// ===== Child Channel Options =======
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.receiveBufferSize", 1048576);
		bootstrap.setOption("child.sendBufferSize", 1048576);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.setOption("child.tcpNoDelay", true);
		
		
		 
		serverChannel = bootstrap.bind(serverSocket);
		log.info("Started HTTP Server on [{}]", serverSocket);
		keepAliveThread = new Thread("KeepAliveThread"){
			public void run() {
				try {
					Thread.currentThread().join();
				} catch (Exception ex) {
					log.info("KeepAlive Thread Stopped");
				}
			}
		};
		keepAliveThread.setDaemon(false);
		keepAliveThread.setPriority(Thread.MIN_PRIORITY);
		keepAliveThread.start();
	}



	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		final ChannelPipeline pipeline = Channels.pipeline();
//		pipeline.addLast("group", this);
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());		
        pipeline.addLast("graphiteHandler", graphiteRequestHandler);
		return pipeline;
	}



}
