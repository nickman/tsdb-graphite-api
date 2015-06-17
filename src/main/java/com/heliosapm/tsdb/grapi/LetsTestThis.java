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
package com.heliosapm.tsdb.grapi;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.tsdb.grapi.client.http.AsyncResponseHandler;
import com.heliosapm.tsdb.grapi.client.http.DefaultAsyncResponse;
import com.heliosapm.tsdb.grapi.client.http.FluentRequestBuilder;
import com.heliosapm.tsdb.grapi.client.http.HttpClient;
import com.heliosapm.utils.concurrency.ExtendedThreadManager;

/**
 * <p>Title: LetsTestThis</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.LetsTestThis</code></p>
 */

public class LetsTestThis implements AsyncResponseHandler {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	protected final Set<Thread> waiters = new CopyOnWriteArraySet<Thread>();
	
	public static final Charset UTF8 = Charset.forName("UTF8");

	/**
	 * Creates a new LetsTestThis
	 */
	public LetsTestThis() {
		log.info("Created a LetsTestThis Instance");
	}
	
	public void run() {
		FluentRequestBuilder frb = HttpClient.getInstance().request(this);
		frb.setUrl("http://localhost:8070/api/tagv/host");
		frb.execute();	
		waitForResponse();
		log.info("DONE");
		
		
	}
	
	protected void waitForResponse() {
		waiters.add(Thread.currentThread());
		try { Thread.currentThread().join(); } catch (Exception x) {/* No Op */}
		if(Thread.interrupted()) Thread.interrupted();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ExtendedThreadManager.install();
		LetsTestThis tt = new LetsTestThis();
		tt.run();
		HttpClient.getInstance().shutdown();
	}
	
	public void onResponse(final DefaultAsyncResponse response) {
		try {
			final String json = response.getBuffer().toString(UTF8);
			JSONArray jo = new JSONArray(json);
			log.info("Received Response: [\n{}\n]", jo.toString(1));
		} finally {
			for(Iterator<Thread> iter = waiters.iterator(); iter.hasNext();) {
				Thread t = iter.next();				
				t.interrupt();
			}
			waiters.clear();
		}
	}

}
